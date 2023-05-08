package edu.cornell.gdiac.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.*;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.math.Path2;
import edu.cornell.gdiac.math.PathExtruder;
import edu.cornell.gdiac.math.PathFactory;
import edu.cornell.gdiac.math.PolyFactory;
import edu.cornell.gdiac.math.*;

/**
 * Primary view class for the game, abstracting the basic graphics calls.
 * <br><br>
 * This version of GameCanvas only supports both rectangular and polygonal Sprite
 * drawing.  It also supports a debug mode that draws polygonal outlines.  However,
 * that mode must be done in a separate begin/end pass.
 * <br><br>
 * Adapted from Walker M. White's GameCanvas.java in Cornell CS 3152, Spring 2023.
 */
public class GameCanvas {
	/** Enumeration to track which pass we are in */
	private enum DrawPass {
		/** We are not drawing */
		INACTIVE,
		/** We are drawing sprites */
		STANDARD,
		/** We are drawing outlines */
		DEBUG
	}
	
	/**
	 * Enumeration of supported BlendStates.
	 * <br><br>
	 * For reasons of convenience, we do not allow user-defined blend functions.
	 * 99% of the time, we find that the following blend modes are sufficient
	 * (particularly with 2D games).
	 */
	public enum BlendState {
		/** Alpha blending on, assuming the colors have pre-multiplied alpha (DEFAULT) */
		ALPHA_BLEND,
		/** Alpha blending on, assuming the colors have no pre-multiplied alpha */
		NO_PREMULT,
		/** Color values are added together, causing a white-out effect */
		ADDITIVE,
		/** Color values are draw on top of one another with no transparency support */
		OPAQUE
	}	

	public static final float STANDARD_WIDTH = 1024f;
	public static final float STANDARD_HEIGHT = 576f;
	
	/** Drawing context to handle textures AND POLYGONS as sprites */
	protected PolygonSpriteBatch spriteBatch;

	/** Path rendering */
	private PathFactory pathFactory;

	/** extruder for path rendering */
	private PathExtruder extruder;

	/** Polygon rendering */
	private PolyFactory polyFactory;

	/** region used for drawing paths */
	private TextureRegion region;

	/** draws beziers */
	private SplinePather pather;
	
	/** Rendering context for the debug outlines */
	private ShapeRenderer debugRender;
	
	/** Track whether we are active (for error checking) */
	private DrawPass active;
	
	/** The current color blending mode */
	private BlendState blend;
	
	/** Camera for the underlying SpriteBatch */
	private Camera camera;

	/** ExtendViewport, used during gameplay */
	private Viewport viewport;

	/** Value to cache window width (if we are currently full screen) */
	int width;
	/** Value to cache window height (if we are currently full screen) */
	int height;

	// CACHE OBJECTS
	/** Affine cache for current sprite to draw */
	private Affine2 local;
	/** Affine cache for all sprites this drawing pass */
	private Matrix4 global;
	private Vector2 vertex;
	/** Cache object to handle raw textures */
	private TextureRegion holder;
	private final float CAMERA_ZOOM = 0.6f;
	protected ShaderProgram spiritModeShader;
	protected ShaderProgram greyscaleShader;
	private FrameBuffer frameBuffer;
	private final Matrix4 IDENTITY = new Matrix4().setToOrtho2D(0,0,1,1);

	/**
	 * Creates a new GameCanvas determined by the application configuration.
	 * <br><br>
	 * Width, height, and fullscreen are taken from the LWGJApplicationConfig
	 * object used to start the application.  This constructor initializes all
	 * the necessary graphics objects.
	 */
	public GameCanvas() {
		active = DrawPass.INACTIVE;
		spriteBatch = new PolygonSpriteBatch();
		debugRender = new ShapeRenderer();
		pathFactory = new PathFactory();
		polyFactory = new PolyFactory();
		pather = new SplinePather();
		extruder = new PathExtruder();
		region = new TextureRegion(new Texture("shared/white.png"));
		
		// Set the projection matrix (for proper scaling)
		camera = new Camera(STANDARD_WIDTH, STANDARD_HEIGHT);
//		camera = new Camera(getWidth(), getHeight(), CAMERA_ZOOM);
		viewport = new FitViewport(STANDARD_WIDTH, STANDARD_HEIGHT, camera.getCamera());
//		viewport = new FitViewport(getWidth(), getHeight(), camera.getCamera());
		viewport.apply(true);
		spriteBatch.setProjectionMatrix(camera.getCamera().combined);
		debugRender.setProjectionMatrix(camera.getCamera().combined);

		// Initialize the cache objects
		holder = new TextureRegion();
		local  = new Affine2();
		global = new Matrix4();
		vertex = new Vector2();

		//shaders
		ShaderProgram.pedantic =false;
		spiritModeShader = new ShaderProgram(spriteBatch.getShader().getVertexShaderSource(),
				Gdx.files.internal("shaders/portal.frag").readString());
		greyscaleShader = new ShaderProgram(spriteBatch.getShader().getVertexShaderSource(),
				Gdx.files.internal("shaders/greyscale.frag").readString());

		frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), false);

		setBlendState(BlendState.NO_PREMULT);

	}
	/**
	* Eliminate any resources that should be garbage collected manually.
	*/
	public void dispose() {
		if (active != DrawPass.INACTIVE) {
			Gdx.app.error("GameCanvas", "Cannot dispose while drawing active", new IllegalStateException());
			return;
		}
		spriteBatch.dispose();
		spriteBatch = null;
		debugRender.dispose();
		frameBuffer.dispose();
		debugRender = null;
		frameBuffer = null;
		local  = null;
		global = null;
		vertex = null;
		holder = null;
	}

	/**
	 * Returns the width of this canvas
	 * <br><br>
	 * This currently gets its value from Gdx.graphics.getWidth()
	 *
	 * @return the width of this canvas
	 */
	public int getWidth() {
		return Gdx.graphics.getWidth();
	}
	
	/**
	 * Changes the width of this canvas
	 * <br><br>
	 * This method raises an IllegalStateException if called while drawing is
	 * active (e.g. in-between a begin-end pair).
	 *
	 * @param width the canvas width
	 */
	public void setWidth(int width) {
		if (active != DrawPass.INACTIVE) {
			Gdx.app.error("GameCanvas", "Cannot alter property while drawing active", new IllegalStateException());
			return;
		}
		this.width = width;
		if (!isFullscreen()) {
			Gdx.graphics.setWindowedMode(width, getHeight());
		}
		resize();
	}
	
	/**
	 * Returns the height of this canvas
	 * <br><br>
	 * This currently gets its value from Gdx.graphics.getHeight()
	 *
	 * @return the height of this canvas
	 */
	public int getHeight() {
		return Gdx.graphics.getHeight();
	}
	
	/**
	 * Changes the height of this canvas
	 * <br><br>
	 * This method raises an IllegalStateException if called while drawing is
	 * active (e.g. in-between a begin-end pair).
	 *
	 * @param height the canvas height
	 */
	public void setHeight(int height) {
		if (active != DrawPass.INACTIVE) {
			Gdx.app.error("GameCanvas", "Cannot alter property while drawing active", new IllegalStateException());
			return;
		}
		this.height = height;
		if (!isFullscreen()) {
			Gdx.graphics.setWindowedMode(getWidth(), height);	
		}
		resize();
	}
	
	/**
	 * Returns the dimensions of this canvas
	 *
	 * @return the dimensions of this canvas
	 */
	public Vector2 getSize() {
		return new Vector2(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
	}

	/**
	 * Changes the width and height of this canvas
ef	 * <br><br>
	 * This method raises an IllegalStateException if called while drawing is
	 * active (e.g. in-between a begin-end pair).
	 *
	 * @param width the canvas width
	 * @param height the canvas height
	 */
	public void setSize(int width, int height) {
		if (active != DrawPass.INACTIVE) {
			Gdx.app.error("GameCanvas", "Cannot alter property while drawing active", new IllegalStateException());
			return;
		}
		this.width = width;
		this.height = height;
		if (!isFullscreen()) {
			Gdx.graphics.setWindowedMode(width, height);
		}
		resize();

	}

	/**
	 * @return Instance of Camera wrapper
	 */
	public Camera getCamera(){
		return camera;
	}
	/**
	 * Returns whether this canvas is currently fullscreen.
	 *
	 * @return whether this canvas is currently fullscreen.
	 */	 
	public boolean isFullscreen() {
		return Gdx.graphics.isFullscreen(); 
	}
	
	/**
	 * Sets whether this canvas should change to fullscreen.
	 * <br><br>
	 * If desktop is true, it will use the current desktop resolution for
	 * fullscreen, and not the width and height set in the configuration
	 * object at the start of the application. This parameter has no effect
	 * if fullscreen is false.
	 * <br><br>
	 * This method raises an IllegalStateException if called while drawing is
	 * active (e.g. in-between a begin-end pair).
	 *
//	 * @param value Whether this canvas should change to fullscreen.
	 * @param desktop 	 Whether to use the current desktop resolution
	 */	 
	public void setFullscreen(boolean fullscreen, boolean desktop) {
		if (active != DrawPass.INACTIVE) {
			Gdx.app.error("GameCanvas", "Cannot alter property while drawing active", new IllegalStateException());
			return;
		}
		if (fullscreen) {
			Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
		} else {
			Gdx.graphics.setWindowedMode(width, height);
		}
	}

	/** Activates the ExtendViewport for drawing to canvas */
	public void applyViewport(boolean centered) {
		viewport.apply(centered);
	}

	public Viewport getViewport() {
		return viewport;
	}

	/**
	 * Resets the SpriteBatch camera when this canvas is resized.
	 * <br><br>
	 * If you do not call this when the window is resized, you will get
	 * weird scaling issues.
	 */
	 public void resize() {
		 width = getWidth();
		 height = getHeight();
		 spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);

		 if (getWidth() != 0 && getHeight() != 0) {
			 frameBuffer.dispose();
			 frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), false);
		 }

		 viewport.update(width, height, true);
	}
	
	/**
	 * Returns the current color blending state for this canvas.
	 * <br><br>
	 * Textures draw to this canvas will be composited according
	 * to the rules of this blend state.
	 *
	 * @return the current color blending state for this canvas
	 */
	public BlendState getBlendState() {
		return blend;
	}
	
	/**
	 * Sets the color blending state for this canvas.
	 * <br><br>
	 * Any texture draw after this call will use the rules of this blend
	 * state to composite with other textures.  Unlike the other setters, if it is 
	 * perfectly safe to use this setter while  drawing is active (e.g. in-between 
	 * a begin-end pair).  
	 *
	 * @param state the color blending rule
	 */
	public void setBlendState(BlendState state) {
		if (state == blend) {
			return;
		}
		switch (state) {
		case NO_PREMULT:
			spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);
			break;
		case ALPHA_BLEND:
			spriteBatch.setBlendFunction(GL20.GL_ONE,GL20.GL_ONE_MINUS_SRC_ALPHA);
			break;
		case ADDITIVE:
			spriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA,GL20.GL_ONE);
			break;
		case OPAQUE:
			spriteBatch.setBlendFunction(GL20.GL_ONE,GL20.GL_ZERO);
			break;
		}
		blend = state;
	}

	/**
	 * Clear the screen, so we can start a new animation frame
	 */
	public void clear() {
    		// Clear the screen
		Gdx.gl.glClearColor(0, 0, 0, 1.0f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
	}

	/**
	 * Start a standard drawing sequence.
	 * <br><br>
	 * Nothing is flushed to the graphics card until the method end() is called.
	 *
	 * @param affine the global transform apply to the camera
	 */
	public void begin(Affine2 affine) {
		global.setAsAffine(affine);
		global.mulLeft(camera.getCamera().combined);
		spriteBatch.setProjectionMatrix(global);

		setBlendState(BlendState.NO_PREMULT);
		spriteBatch.begin();
		active = DrawPass.STANDARD;
	}

	/**
	 * Start a standard drawing sequence.
	 * <br><br>
	 * Nothing is flushed to the graphics card until the method end() is called.
	 *
	 * @param sx the amount to scale the x-axis
	 * @param sy the amount to scale the y-axis
	 */
	public void begin(float sx, float sy) {
		global.idt();
		global.scl(sx,sy,1.0f);
		global.mulLeft(camera.getCamera().combined);
		spriteBatch.setProjectionMatrix(global);

		spriteBatch.begin();
		active = DrawPass.STANDARD;
	}
    
	/**
	 * Start a standard drawing sequence.
	 * <br><br>
	 * Nothing is flushed to the graphics card until the method end() is called.
	 */
	public void begin() {
		spriteBatch.setProjectionMatrix(camera.getCamera().combined);
//		vfxManager.update(Gdx.graphics.getDeltaTime());
		spriteBatch.begin();
		viewport.apply();
		active = DrawPass.STANDARD;
	}

	public void beginFrameBuffer(){
		begin();
		frameBuffer.begin();
		ScreenUtils.clear(Color.BLACK);
	}

	public void endFrameBuffer() {
		spriteBatch.flush();
		frameBuffer.end();
        spriteBatch.setColor(Color.WHITE);
		spriteBatch.setProjectionMatrix(IDENTITY);
		spriteBatch.draw(frameBuffer.getColorBufferTexture(), 0, 0, 1, 1, 0, 0, 1, 1);
		spriteBatch.setProjectionMatrix(camera.getCamera().combined);
	}

	public void setShader(ShaderProgram shader) { spriteBatch.setShader(shader); }

	public void setSpiritModeShader(float radius, float thickness, Color bgColor, Color edgeColor, float time) {
		spriteBatch.setShader(spiritModeShader);
		spiritModeShader.setUniformf("u_radius", radius);
		spiritModeShader.setUniformf("u_thickness", thickness);
		spiritModeShader.setUniformf("u_bgColor", bgColor);
		spiritModeShader.setUniformf("u_edgeColor", edgeColor);
		spiritModeShader.setUniformf("u_time", time);
	}

	public void setGreyscaleShader(float greyScale) {
		spriteBatch.setShader(greyscaleShader);
		greyscaleShader.setUniformf("u_greyscale", greyScale);
	}

	/**
	 * Sets up the spritebatch for drawing. This should only be used if you want to draw textures without VFX
	 * after calling <code>endVFX()</code> - if you want to begin drawing at a drawing loop you should call <code>begin()</code>.
	 */
	public void batchBegin(){ spriteBatch.begin(); }

	/**
	 * Projects a vector in world units into pixel units, then returns the ratio of its position with respect to
	 * the screen dimensions. This is specifically used by the shockwave shader effect to get the position of the
	 * shockwave center.
	 *
	 * @param vec  Vector to project
	 * @return     Ratio of projected vector to screen dimensions
	 */
	public Vector2 projectRatio(Vector2 vec) {
		Vector3 proj3 = camera.getCamera().project(new Vector3(vec.x, vec.y, 0),
				viewport.getScreenX(), viewport.getScreenY(),
				viewport.getScreenWidth(), viewport.getScreenHeight());
		return vec.set(proj3.x/getWidth(), proj3.y/getHeight());
	}

	/**
	 * Ends a drawing sequence, flushing textures to the graphics card.
	 */
	public void end() {
		spriteBatch.end();
		active = DrawPass.INACTIVE;
	}

	public void flush() { spriteBatch.flush(); }

	//////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// DRAWING MODES ///////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Draws the texture at the given position.
	 * <br><br>
	 * Unless otherwise transformed by the global transform (@see begin(Affine2)),
	 * the texture will be unscaled.  The bottom left of the texture will be positioned
	 * at the given coordinates.
	 *
	 * @param image The texture to draw
	 * @param x 	The x-coordinate of the bottom left corner
	 * @param y 	The y-coordinate of the bottom left corner
	 */
	public void draw(Texture image, float x, float y) {
		if (active != DrawPass.STANDARD) {
			Gdx.app.error("GameCanvas", "Cannot draw without active begin()", new IllegalStateException());
			return;
		}

		// Unlike Lab 1, we can shortcut without a master drawing method
		spriteBatch.setColor(Color.WHITE);
		spriteBatch.draw(image, x,  y);
	}
	
	/**
	 * Draws the tinted texture at the given position.
	 * <br><br>
	 * The texture colors will be multiplied by the given color.  This will turn
	 * any white into the given color.  Other colors will be similarly affected.
	 * <br><br>
	 * Unless otherwise transformed by the global transform (@see begin(Affine2)),
	 * the texture will be unscaled.  The bottom left of the texture will be positioned
	 * at the given coordinates.
	 *
	 * @param image The texture to draw
	 * @param tint  The color tint
	 * @param x 	The x-coordinate of the bottom left corner
	 * @param y 	The y-coordinate of the bottom left corner
	 * @param width	The texture width
	 * @param height The texture height
	 */
	public void draw(Texture image, Color tint, float x, float y, float width, float height) {
		if (active != DrawPass.STANDARD) {
			Gdx.app.error("GameCanvas", "Cannot draw without active begin()", new IllegalStateException());
			return;
		}
		
		// Unlike Lab 1, we can shortcut without a master drawing method
    		spriteBatch.setColor(tint);
		spriteBatch.draw(image, x,  y, width, height);
	}
	
	/**
	 * Draws the tinted texture at the given position.
	 * <br><br>
	 * The texture colors will be multiplied by the given color.  This will turn
	 * any white into the given color.  Other colors will be similarly affected.
	 * <br><br>
	 * Unless otherwise transformed by the global transform (@see begin(Affine2)),
	 * the texture will be unscaled.  The bottom left of the texture will be positioned
	 * at the given coordinates.
	 *
	 * @param image The texture to draw
	 * @param tint  The color tint
	 * @param ox 	The x-coordinate of texture origin (in pixels)
	 * @param oy 	The y-coordinate of texture origin (in pixels)
	 * @param x 	The x-coordinate of the texture origin (on screen)
	 * @param y 	The y-coordinate of the texture origin (on screen)
	 * @param width	The texture width
	 * @param height The texture height
	 */
	public void draw(Texture image, Color tint, float ox, float oy, float x, float y, float width, float height) {
		if (active != DrawPass.STANDARD) {
			Gdx.app.error("GameCanvas", "Cannot draw without active begin()", new IllegalStateException());
			return;
		}
		
		// Call the master drawing method (more efficient that base method)
		holder.setRegion(image);
		draw(holder, tint, x-ox, y-oy, width, height);
	}


	/**
	 * Draws the tinted texture with the given transformations
	 * <br><br>
	 * The texture colors will be multiplied by the given color.  This will turn
	 * any white into the given color.  Other colors will be similarly affected.
	 * <br><br>
	 * The transformations are BEFORE after the global transform (@see begin(Affine2)).  
	 * As a result, the specified texture origin will be applied to all transforms 
	 * (both the local and global).
	 * <br><br>
	 * The local transformations in this method are applied in the following order: 
	 * scaling, then rotation, then translation (e.g. placement at (sx,sy)).
	 *
	 * @param image The texture to draw
	 * @param tint  The color tint
	 * @param ox 	The x-coordinate of texture origin (in pixels)
	 * @param oy 	The y-coordinate of texture origin (in pixels)
	 * @param x 	The x-coordinate of the texture origin (on screen)
	 * @param y 	The y-coordinate of the texture origin (on screen)
	 * @param angle The rotation angle (in degrees) about the origin.
	 * @param sx 	The x-axis scaling factor
	 * @param sy 	The y-axis scaling factor
	 */	
	public void draw(Texture image, Color tint, float ox, float oy, 
					float x, float y, float angle, float sx, float sy) {
		if (active != DrawPass.STANDARD) {
			Gdx.app.error("GameCanvas", "Cannot draw without active begin()", new IllegalStateException());
			return;
		}
		
		// Call the master drawing method (more efficient that base method)
		holder.setRegion(image);
		draw(holder,tint,ox,oy,x,y,angle,sx,sy);
	}
	
	/**
	 * Draws the tinted texture with the given transformations
	 * <br><br>
	 * The texture colors will be multiplied by the given color.  This will turn
	 * any white into the given color.  Other colors will be similarly affected.
	 * <br><br>
	 * The transformations are BEFORE after the global transform (@see begin(Affine2)).  
	 * As a result, the specified texture origin will be applied to all transforms 
	 * (both the local and global).
	 * <br><br>
	 * The local transformations in this method are applied in the following order: 
	 * scaling, then rotation, then translation (e.g. placement at (sx,sy)).
	 *
	 * @param image The texture to draw
	 * @param tint  The color tint
	 * @param ox 	The x-coordinate of texture origin (in pixels)
	 * @param oy 	The y-coordinate of texture origin (in pixels)
	 * @param transform  The image transform
	 */	
	public void draw(Texture image, Color tint, float ox, float oy, Affine2 transform) {
		if (active != DrawPass.STANDARD) {
			Gdx.app.error("GameCanvas", "Cannot draw without active begin()", new IllegalStateException());
			return;
		}
		
		// Call the master drawing method (we have to for transforms)
		holder.setRegion(image);
		draw(holder,tint,ox,oy,transform);
	}
	
	/**
	 * Draws the texture region (filmstrip) at the given position.
	 * <br><br>
	 * A texture region is a single texture file that can hold one or more textures.
	 * It is used for filmstrip animation.
	 * <br><br>
	 * Unless otherwise transformed by the global transform (@see begin(Affine2)),
	 * the texture will be unscaled.  The bottom left of the texture will be positioned
	 * at the given coordinates.
	 *
	 * @param region The texture to draw
	 * @param x 	The x-coordinate of the bottom left corner
	 * @param y 	The y-coordinate of the bottom left corner
	 */
	public void draw(TextureRegion region, float x, float y) {
		if (active != DrawPass.STANDARD) {
			Gdx.app.error("GameCanvas", "Cannot draw without active begin()", new IllegalStateException());
			return;
		}
		// Unlike Lab 1, we can shortcut without a master drawing method
		spriteBatch.setColor(Color.WHITE);
		spriteBatch.draw(region, x,  y);
	}

	/**
	 * Draws the tinted texture at the given position.
	 * <br><br>
	 * The texture colors will be multiplied by the given color.  This will turn
	 * any white into the given color.  Other colors will be similarly affected.
	 * <br><br>
	 * Unless otherwise transformed by the global transform (@see begin(Affine2)),
	 * the texture will be unscaled.  The bottom left of the texture will be positioned
	 * at the given coordinates.
	 *region
	 * @param region The texture to draw
	 * @param tint  The color tint
	 * @param x 	The x-coordinate of the bottom left corner
	 * @param y 	The y-coordinate of the bottom left corner
	 * @param width	The texture width
	 * @param height The texture height
	 */
	public void draw(TextureRegion region, Color tint, float x, float y, float width, float height) {
		if (active != DrawPass.STANDARD) {
			Gdx.app.error("GameCanvas", "Cannot draw without active begin()", new IllegalStateException());
			return;
		}
		
		// Unlike Lab 1, we can shortcut without a master drawing method
    		spriteBatch.setColor(tint);
		spriteBatch.draw(region, x,  y, width, height);
	}
	
	/**
	 * Draws the tinted texture at the given position.
	 * <br><br>
	 * The texture colors will be multiplied by the given color.  This will turn
	 * any white into the given color.  Other colors will be similarly affected.
	 * <br><br>
	 * Unless otherwise transformed by the global transform (@see begin(Affine2)),
	 * the texture will be unscaled.  The bottom left of the texture will be positioned
	 * at the given coordinates.
	 *
	 * @param region The texture to draw
	 * @param tint  The color tint
	 * @param ox 	The x-coordinate of texture origin (in pixels)
	 * @param oy 	The y-coordinate of texture origin (in pixels)
	 * @param x 	The x-coordinate of the texture origin (on screen)
	 * @param y 	The y-coordinate of the texture origin (on screen)
	 * @param width	The texture width
	 * @param height The texture height
	 */	
	public void draw(TextureRegion region, Color tint, float ox, float oy, float x, float y, float width, float height) {
		if (active != DrawPass.STANDARD) {
			Gdx.app.error("GameCanvas", "Cannot draw without active begin()", new IllegalStateException());
			return;
		}
		
		// Unlike Lab 1, we can shortcut without a master drawing method
    		spriteBatch.setColor(tint);
		spriteBatch.draw(region, x-ox, y-oy, width, height);
	}

	/**
	 * Draws the tinted texture region (filmstrip) with the given transformations
	 * <br><br>
	 * A texture region is a single texture file that can hold one or more textures.
	 * It is used for filmstrip animation.
	 * <br><br>
	 * The texture colors will be multiplied by the given color.  This will turn
	 * any white into the given color.  Other colors will be similarly affected.
	 * <br><br>
	 * The transformations are BEFORE after the global transform (@see begin(Affine2)).  
	 * As a result, the specified texture origin will be applied to all transforms 
	 * (both the local and global).
	 * <br><br>
	 * The local transformations in this method are applied in the following order: 
	 * scaling, then rotation, then translation (e.g. placement at (sx,sy)).
	 *
	 * @param region The texture to draw
	 * @param tint  The color tint
	 * @param ox 	The x-coordinate of texture origin (in pixels)
	 * @param oy 	The y-coordinate of texture origin (in pixels)
	 * @param x 	The x-coordinate of the texture origin (on screen)
	 * @param y 	The y-coordinate of the texture origin (on screen)
	 * @param angle The rotation angle (in degrees) about the origin.
	 * @param sx 	The x-axis scaling factor
	 * @param sy 	The y-axis scaling factor
	 */	
	public void draw(TextureRegion region, Color tint, float ox, float oy, 
					 float x, float y, float angle, float sx, float sy) {
		if (active != DrawPass.STANDARD) {
			Gdx.app.error("GameCanvas", "Cannot draw without active begin()", new IllegalStateException());
			return;
		}

		// BUG: The draw command for texture regions does not work properly.
		// There is a workaround, but it will break if the bug is fixed.
		// For now, it is better to set the affine transform directly.
		computeTransform(ox,oy,x,y,angle,sx,sy);
		spriteBatch.setColor(tint);
		spriteBatch.draw(region, region.getRegionWidth(), region.getRegionHeight(), local);
	}

	/**
	 * Draws the tinted texture with the given transformations
	 * <br><br>
	 * The texture colors will be multiplied by the given color.  This will turn
	 * any white into the given color.  Other colors will be similarly affected.
	 * <br><br>
	 * The transformations are BEFORE after the global transform (@see begin(Affine2)).  
	 * As a result, the specified texture origin will be applied to all transforms 
	 * (both the local and global).
	 * <br><br>
	 * The local transformations in this method are applied in the following order: 
	 * scaling, then rotation, then translation (e.g. placement at (sx,sy)).
	 *
	 * @param region The region to draw
	 * @param tint  The color tint
	 * @param ox 	The x-coordinate of texture origin (in pixels)
	 * @param oy 	The y-coordinate of texture origin (in pixels)
	 * @param affine  The image transform
	 */	
	public void draw(TextureRegion region, Color tint, float ox, float oy, Affine2 affine) {
		if (active != DrawPass.STANDARD) {
			Gdx.app.error("GameCanvas", "Cannot draw without active begin()", new IllegalStateException());
			return;
		}

		local.set(affine);
		local.translate(-ox,-oy);				
		spriteBatch.setColor(tint);
		spriteBatch.draw(region, region.getRegionWidth(), region.getRegionHeight(), local);
	}

	/**
	 * Draws the polygonal region with the given transformations
	 * <br><br>
	 * A polygon region is a texture region with attached vertices so that it draws a
	 * textured polygon. The polygon vertices are relative to the texture file.
	 * <br><br>
	 * The transformations are BEFORE after the global transform (@see begin(Affine2)).  
	 * As a result, the specified texture origin will be applied to all transforms 
	 * (both the local and global).
	 * <br><br>
	 * The local transformations in this method are applied in the following order: 
	 * scaling, then rotation, then translation (e.g. placement at (sx,sy)).
	 *
	 * @param region The polygon to draw
	 * @param x 	The x-coordinate of the bottom left corner
	 * @param y 	The y-coordinate of the bottom left corner
	 */	
	public void draw(PolygonRegion region, float x, float y) {
		if (active != DrawPass.STANDARD) {
			Gdx.app.error("GameCanvas", "Cannot draw without active begin()", new IllegalStateException());
			return;
		}
		
		// Unlike Lab 1, we can shortcut without a master drawing method
    		spriteBatch.setColor(Color.WHITE);
		spriteBatch.draw(region, x,  y);
	}
	
	/**
	 * Draws the polygonal region with the given transformations
	 * <br><br>
	 * A polygon region is a texture region with attached vertices so that it draws a
	 * textured polygon. The polygon vertices are relative to the texture file.
	 * <br><br>
	 * The texture colors will be multiplied by the given color.  This will turn
	 * any white into the given color.  Other colors will be similarly affected.
	 * <br><br>
	 * The transformations are BEFORE after the global transform (@see begin(Affine2)).  
	 * As a result, the specified texture origin will be applied to all transforms 
	 * (both the local and global).
	 * <br><br>
	 * The local transformations in this method are applied in the following order: 
	 * scaling, then rotation, then translation (e.g. placement at (sx,sy)).
	 *
	 * @param region The polygon to draw
	 * @param tint  The color tint
	 * @param x 	The x-coordinate of the bottom left corner
	 * @param y 	The y-coordinate of the bottom left corner
	 * @param width	The texture width
	 * @param height The texture height
	 */	
	public void draw(PolygonRegion region, Color tint, float x, float y, float width, float height) {
		if (active != DrawPass.STANDARD) {
			Gdx.app.error("GameCanvas", "Cannot draw without active begin()", new IllegalStateException());
			return;
		}
		
		// Unlike Lab 1, we can shortcut without a master drawing method
    		spriteBatch.setColor(tint);
		spriteBatch.draw(region, x,  y, width, height);
	}
	
	/**
	 * Draws the polygonal region with the given transformations
	 * <br><br>
	 * A polygon region is a texture region with attached vertices so that it draws a
	 * textured polygon. The polygon vertices are relative to the texture file.
	 * <br><br>
	 * The texture colors will be multiplied by the given color.  This will turn
	 * any white into the given color.  Other colors will be similarly affected.
	 * <br><br>
	 * The transformations are BEFORE after the global transform (@see begin(Affine2)).  
	 * As a result, the specified texture origin will be applied to all transforms 
	 * (both the local and global).
	 * <br><br>
	 * The local transformations in this method are applied in the following order: 
	 * scaling, then rotation, then translation (e.g. placement at (sx,sy)).
	 *
	 * @param region The polygon to draw
	 * @param tint  The color tint
	 * @param ox 	The x-coordinate of texture origin (in pixels)
	 * @param oy 	The y-coordinate of texture origin (in pixels)
	 * @param x 	The x-coordinate of the texture origin (on screen)
	 * @param y 	The y-coordinate of the texture origin (on screen)
	 * @param width	The texture width
	 * @param height The texture height
	 */	
	public void draw(PolygonRegion region, Color tint, float ox, float oy, float x, float y, float width, float height) {
		if (active != DrawPass.STANDARD) {
			Gdx.app.error("GameCanvas", "Cannot draw without active begin()", new IllegalStateException());
			return;
		}
		
		// Unlike Lab 1, we can shortcut without a master drawing method
    		spriteBatch.setColor(tint);
		spriteBatch.draw(region, x-ox, y-oy, width, height);
	}
	
	/**
	 * Draws the polygonal region with the given transformations
	 * <br><br>
	 * A polygon region is a texture region with attached vertices so that it draws a
	 * textured polygon. The polygon vertices are relative to the texture file.
	 * <br><br>
	 * The texture colors will be multiplied by the given color.  This will turn
	 * any white into the given color.  Other colors will be similarly affected.
	 * <br><br>
	 * The transformations are BEFORE after the global transform (@see begin(Affine2)).  
	 * As a result, the specified texture origin will be applied to all transforms 
	 * (both the local and global).
	 * <br><br>
	 * The local transformations in this method are applied in the following order: 
	 * scaling, then rotation, then translation (e.g. placement at (sx,sy)).
	 *
	 * @param region The polygon to draw
	 * @param tint  The color tint
	 * @param ox 	The x-coordinate of texture origin (in pixels)
	 * @param oy 	The y-coordinate of texture origin (in pixels)
	 * @param x 	The x-coordinate of the texture origin (on screen)
	 * @param y 	The y-coordinate of the texture origin (on screen)
	 * @param angle The rotation angle (in degrees) about the origin.
	 * @param sx 	The x-axis scaling factor
	 * @param sy 	The y-axis scaling factor
	 */	
	public void draw(PolygonRegion region, Color tint, float ox, float oy, 
					 float x, float y, float angle, float sx, float sy) {
		if (active != DrawPass.STANDARD) {
			Gdx.app.error("GameCanvas", "Cannot draw without active begin()", new IllegalStateException());
			return;
		}
		TextureRegion bounds = region.getRegion();
		spriteBatch.setColor(tint);
		spriteBatch.draw(region, x, y, ox, oy, 
				 bounds.getRegionWidth(), bounds.getRegionHeight(),
				 sx, sy, 180.0f*angle/(float)Math.PI);
	}

	/**
	 * Draws the polygonal region with the given transformations
	 * <br><br>
	 * A polygon region is a texture region with attached vertices so that it draws a
	 * textured polygon. The polygon vertices are relative to the texture file.
	 * <br><br>
	 * The texture colors will be multiplied by the given color.  This will turn
	 * any white into the given color.  Other colors will be similarly affected.
	 * <br><br>
	 * The transformations are BEFORE after the global transform (@see begin(Affine2)).  
	 * As a result, the specified texture origin will be applied to all transforms 
	 * (both the local and global).
	 * <br><br>
	 * The local transformations in this method are applied in the following order: 
	 * scaling, then rotation, then translation (e.g. placement at (sx,sy)).
	 *
	 * @param region The polygon to draw
	 * @param tint  The color tint
	 * @param ox 	The x-coordinate of texture origin (in pixels)
	 * @param oy 	The y-coordinate of texture origin (in pixels)
	 * @param affine  The image transform
	 */	
	public void draw(PolygonRegion region, Color tint, float ox, float oy, Affine2 affine) {
		if (active != DrawPass.STANDARD) {
			Gdx.app.error("GameCanvas", "Cannot draw without active begin()", new IllegalStateException());
			return;
		}

		local.set(affine);
		local.translate(-ox,-oy);
		computeVertices(local,region.getVertices());

		spriteBatch.setColor(tint);
		spriteBatch.draw(region, 0, 0);
		
		// Invert and restore
		local.inv();
		computeVertices(local,region.getVertices());
	}
	
	/**
	 * Transform the given vertices by the affine transform
	 */
	private void computeVertices(Affine2 affine, float[] vertices) {
		for(int ii = 0; ii < vertices.length; ii += 2) {
			vertex.set(vertices[2*ii], vertices[2*ii+1]);
			affine.applyTo(vertex);
			vertices[2*ii  ] = vertex.x;
			vertices[2*ii+1] = vertex.y;
		}
	}

	/**
	* Draws text on the screen.
	*
	* @param text The string to draw
	* @param font The font to use
	* @param x The x-coordinate of the lower-left corner
	* @param y The y-coordinate of the lower-left corner
	*/
	public void drawText(String text, BitmapFont font, float x, float y) {
		if (active != DrawPass.STANDARD) {
			Gdx.app.error("GameCanvas", "Cannot draw without active begin()", new IllegalStateException());
			return;
		}
		GlyphLayout layout = new GlyphLayout(font,text);
		font.draw(spriteBatch, layout, x, y);
	}

	/**
	* Draws text centered on the screen.
	*
	* @param text The string to draw
	* @param font The font to use
	* @param offset The y-value offset from the center of the screen.
	*/
	public void drawTextCentered(String text, BitmapFont font, float offset) {
		if (active != DrawPass.STANDARD) {
			Gdx.app.error("GameCanvas", "Cannot draw without active begin()", new IllegalStateException());
			return;
		}

		GlyphLayout layout = new GlyphLayout(font,text);
		float x = (getWidth()  - layout.width) / 2.0f;
		float y = (getHeight() + layout.height) / 2.0f;
		font.draw(spriteBatch, layout, x, y+offset);
	}
    
	/**
	* Start the debug drawing sequence.
	* <br><br>
	* Nothing is flushed to the graphics card until the method end() is called.
	*
	* @param affine the global transform apply to the camera
	*/
	public void beginDebug(Affine2 affine) {
		global.setAsAffine(affine);
		global.mulLeft(camera.getCamera().combined);
		debugRender.setProjectionMatrix(global);

		debugRender.begin(ShapeRenderer.ShapeType.Line);
		active = DrawPass.DEBUG;
	}
    
	/**
	* Start the debug drawing sequence.
	* <br><br>
	* Nothing is flushed to the graphics card until the method end() is called.
	*
	* @param sx the amount to scale the x-axis
	* @param sy the amount to scale the y-axis
	*/
	public void beginDebug(float sx, float sy) {
		global.idt();
		global.scl(sx,sy,1.0f);
		global.mulLeft(camera.getCamera().combined);
		debugRender.setProjectionMatrix(global);

		debugRender.begin(ShapeRenderer.ShapeType.Line);
		active = DrawPass.DEBUG;
	}

	/**
	 * Start the debug drawing sequence.
	 * <br><br>
	 * Nothing is flushed to the graphics card until the method end() is called.
	 */
	public void beginDebug() {
		debugRender.setProjectionMatrix(camera.getCamera().combined);
		debugRender.begin(ShapeRenderer.ShapeType.Filled);
		debugRender.setColor(Color.RED);
		debugRender.circle(0, 0, 10);
		debugRender.end();

		debugRender.begin(ShapeRenderer.ShapeType.Line);
		active = DrawPass.DEBUG;
	}

	/**
	 * Ends the debug drawing sequence, flushing textures to the graphics card.
	 */
	public void endDebug() {
		debugRender.end();
		active = DrawPass.INACTIVE;
	}
    
	/**
	* Draws the outline of the given shape in the specified color
	*
	* @param shape The Box2D shape
	* @param color The outline color
	* @param x  The x-coordinate of the shape position
	* @param y  The y-coordinate of the shape position
	*/
	public void drawPhysics(PolygonShape shape, Color color, float x, float y) {
		if (active != DrawPass.DEBUG) {
			Gdx.app.error("GameCanvas", "Cannot draw without active beginDebug()", new IllegalStateException());
			return;
		}

		float x0, y0, x1, y1;
		debugRender.setColor(color);
		for(int ii = 0; ii < shape.getVertexCount()-1; ii++) {
			shape.getVertex(ii  ,vertex);
			x0 = x+vertex.x; y0 = y+vertex.y;
			shape.getVertex(ii+1,vertex);
			x1 = x+vertex.x; y1 = y+vertex.y;
			debugRender.line(x0, y0, x1, y1);
		}
		// Close the loop
		shape.getVertex(shape.getVertexCount()-1,vertex);
		x0 = x+vertex.x; y0 = y+vertex.y;
		shape.getVertex(0,vertex);
		x1 = x+vertex.x; y1 = y+vertex.y;
		debugRender.line(x0, y0, x1, y1);
	}

	/**
	* Draws the outline of the given shape in the specified color
	*
	* @param shape The Box2D shape
	* @param color The outline color
	* @param x  The x-coordinate of the shape position
	* @param y  The y-coordinate of the shape position
	* @param angle  The shape angle of rotation
	*/
	public void drawPhysics(PolygonShape shape, Color color, float x, float y, float angle) {
		if (active != DrawPass.DEBUG) {
			Gdx.app.error("GameCanvas", "Cannot draw without active beginDebug()", new IllegalStateException());
			return;
		}

		local.setToTranslation(x,y);
		local.rotateRad(angle);

		float x0, y0, x1, y1;
		debugRender.setColor(color);
		for(int ii = 0; ii < shape.getVertexCount()-1; ii++) {
			shape.getVertex(ii  ,vertex);
			local.applyTo(vertex);
			x0 = vertex.x; y0 = vertex.y;
			shape.getVertex(ii+1,vertex);
			local.applyTo(vertex);
			x1 = vertex.x; y1 = vertex.y;
			debugRender.line(x0, y0, x1, y1);
		}
		// Close the loop
		shape.getVertex(shape.getVertexCount()-1,vertex);
		local.applyTo(vertex);
		x0 = vertex.x; y0 = vertex.y;
		shape.getVertex(0,vertex);
		local.applyTo(vertex);
		x1 = vertex.x; y1 = vertex.y;
		debugRender.line(x0, y0, x1, y1);
	}

	/**
	* Draws the outline of the given shape in the specified color
	*
	* @param shape The Box2D shape
	* @param color The outline color
	* @param x  The x-coordinate of the shape position
	* @param y  The y-coordinate of the shape position
	* @param angle  The shape angle of rotation
	* @param sx The amount to scale the x-axis
	*/
	public void drawPhysics(PolygonShape shape, Color color, float x, float y, float angle, float sx, float sy) {
		if (active != DrawPass.DEBUG) {
			Gdx.app.error("GameCanvas", "Cannot draw without active beginDebug()", new IllegalStateException());
			return;
		}

		local.setToScaling(sx,sy);
		local.translate(x,y);
		local.rotateRad(angle);

		float x0, y0, x1, y1;
		debugRender.setColor(color);
		for(int ii = 0; ii < shape.getVertexCount()-1; ii++) {
			shape.getVertex(ii  ,vertex);
			local.applyTo(vertex);
			x0 = vertex.x; y0 = vertex.y;
			shape.getVertex(ii+1,vertex);
			local.applyTo(vertex);
			x1 = vertex.x; y1 = vertex.y;
			debugRender.line(x0, y0, x1, y1);
		}
		// Close the loop
		shape.getVertex(shape.getVertexCount()-1,vertex);
		local.applyTo(vertex);
		x0 = vertex.x; y0 = vertex.y;
		shape.getVertex(0,vertex);
		local.applyTo(vertex);
		x1 = vertex.x; y1 = vertex.y;
		debugRender.line(x0, y0, x1, y1);
	}
    
	/**
	* Draws the outline of the given shape in the specified color
	* <br><br>
	* The position of the circle is ignored.  Only the radius is used. To move the
	* circle, change the x and y parameters.
	*
	* @param shape The Box2D shape
	* @param color The outline color
	* @param x  The x-coordinate of the shape position
	* @param y  The y-coordinate of the shape position
	*/
	public void drawPhysics(CircleShape shape, Color color, float x, float y) {
		if (active != DrawPass.DEBUG) {
			Gdx.app.error("GameCanvas", "Cannot draw without active beginDebug()", new IllegalStateException());
			return;
		}

		debugRender.setColor(color);
		debugRender.circle(x, y, shape.getRadius(),12);
	}
    
	/**
	* Draws the outline of the given shape in the specified color
	* <br><br>
	* The position of the circle is ignored.  Only the radius is used. To move the
	* circle, change the x and y parameters.
	*
	* @param shape The Box2D shape
	* @param color The outline color
	* @param x  The x-coordinate of the shape position
	* @param y  The y-coordinate of the shape position
	* @param sx The amount to scale the x-axis
	*/
	public void drawPhysics(CircleShape shape, Color color, float x, float y, float sx, float sy) {
		if (active != DrawPass.DEBUG) {
			Gdx.app.error("GameCanvas", "Cannot draw without active beginDebug()", new IllegalStateException());
			return;
		}

		float x0 = x*sx;
		float y0 = y*sy;
		float w = shape.getRadius()*sx;
		float h = shape.getRadius()*sy;
		debugRender.setColor(color);
		debugRender.ellipse(x0-w, y0-h, 2*w, 2*h, 12);
	}

	/**
	 * Draws a line of a specific color between two points in the debug pass.
	 *
	 * @param p1 Endpoint of the line
	 * @param p2 Endpoint of the line
	 * @param color The outline color
	 * @param sx The amount to scale the x-axis
	 * @param sy The amount to scale the y-axis
	 */
	public void drawLineDebug(Vector2 p1, Vector2 p2, Color color, float sx, float sy){
		if (active != DrawPass.DEBUG) {
			Gdx.app.error("GameCanvas", "Cannot draw without active beginDebug()", new IllegalStateException());
			return;
		}
		debugRender.setColor(color);
		debugRender.line(p1.scl(new Vector2(sx, sy)), p2.scl(new Vector2(sx, sy)));
	}

	/**
	 * Draws a line of a specific color between two points using PathFactory.
	 *
	 * @param p1 Endpoint of the line
	 * @param p2 Endpoint of the line
	 * @param color The outline color
	 * @param sx The amount to scale the x-axis
	 * @param sy The amount to scale the y-axis
	 */
	public void drawFactoryLine(Vector2 p1, Vector2 p2, float thickness, Color color, float sx, float sy){
		if (active != DrawPass.STANDARD) {
			Gdx.app.error("GameCanvas", "Cannot draw without active begin", new IllegalStateException());
			return;
		}
//		float xTranslate = camera.centerLevelTranslation().x;
//		float yTranslate = camera.centerLevelTranslation().y;
		Path2 path = pathFactory.makeLine(0, 0, (p2.x-p1.x)*sx+0, (p2.y-p1.y)*sy+0);
		extruder.set(path);
		extruder.calculate(thickness);
		spriteBatch.setColor(color);
		spriteBatch.draw(extruder.getPolygon().makePolyRegion(region), p1.x*sx, p1.y*sx);
	}

	/**
	 * Draws a path of a specified by an array of points.
	 *
	 * @param points The array of points
	 * @param color The outline color
	 * @param sx The amount to scale the x-axis
	 * @param sy The amount to scale the y-axis
	 */
	public void drawFactoryPath(Array<Vector2> points, float thickness, Color color, float sx, float sy){
		if (active != DrawPass.STANDARD) {
			Gdx.app.error("GameCanvas", "Cannot draw without active begin", new IllegalStateException());
			return;
		}
		Vector2 start = points.get(0);
		for(int i=1; i< points.size; i++){
			drawFactoryLine(start, points.get(i), thickness, color, sx, sy);
			start = points.get(i);
		}
		points.iterator();
	}

	public void drawRectangle(float x, float y, float w, float h, Color color, float sx, float sy){
		if (active != DrawPass.STANDARD) {
			Gdx.app.error("GameCanvas", "Cannot draw without active begin", new IllegalStateException());
			return;
		}
		PolygonRegion rect = polyFactory.makeRect(x*sx, y*sy, w*sx, h*sy).makePolyRegion(region);
		spriteBatch.setColor(color);
		spriteBatch.draw(rect, 0,0);
	}
	/**
	 *
	 */
	public void drawSpline(Array<Vector2> points, float thickness, Color color, float sx, float sy){
		if (active != DrawPass.STANDARD) {
			Gdx.app.error("GameCanvas", "Cannot draw without active begin", new IllegalStateException());
			return;
		}

		if (points.size <= 2 || points.size % 3 != 1){
			Gdx.app.error("GameCanvas", "Incorrect number of points for spline", new IllegalStateException());
			return;
		}
		float[] vert = getPoints(points, sx, sy);
		Spline2 spline2 = new Spline2(vert);
		pather = new SplinePather(spline2);
		pather.calculate();
		Path2 splinePath = pather.getPath();
		extruder.set(splinePath);
		extruder.calculate(thickness);
		spriteBatch.setColor(color);
		spriteBatch.draw(extruder.getPolygon().makePolyRegion(region), 0, 0);
	}

	private float[] getPoints(Array<Vector2> points, float sx, float sy){
		float[] vert = new float[points.size*2];
		for (int i=0; i<points.size; i++){
			vert[2*i] = points.get(i).x*sx;
			vert[2*i+1] = points.get(i).y*sy;
		}
		return vert;
	}
    
	/**
	 * Compute the affine transform (and store it in local) for this image.
	 * 
	 * @param ox 	The x-coordinate of texture origin (in pixels)
	 * @param oy 	The y-coordinate of texture origin (in pixels)
	 * @param x 	The x-coordinate of the texture origin (on screen)
	 * @param y 	The y-coordinate of the texture origin (on screen)
	 * @param angle The rotation angle (in degrees) about the origin.
	 * @param sx 	The x-axis scaling factor
	 * @param sy 	The y-axis scaling factor
	 */
	private void computeTransform(float ox, float oy, float x, float y, float angle, float sx, float sy) {
		local.setToTranslation(x,y);
		local.rotate(180.0f*angle/(float)Math.PI);
		local.scale(sx,sy);
		local.translate(-ox,-oy);
	}
}