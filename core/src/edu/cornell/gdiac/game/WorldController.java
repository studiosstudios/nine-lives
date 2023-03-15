/*
 * WorldController.java
 *
 * This is the most important new class in this lab.  This class serves as a combination 
 * of the CollisionController and GameplayController from the previous lab.  There is not 
 * much to do for collisions; Box2d takes care of all of that for us.  This controller 
 * invokes Box2d and then performs any after the fact modifications to the data 
 * (e.g. gameplay).
 *
 * If you study this class, and the contents of the edu.cornell.cs3152.physics.obstacles
 * package, you should be able to understand how the Physics engine works.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * LibGDX version, 2/6/2015
 */
package edu.cornell.gdiac.game;

import java.util.HashMap;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.*;

/**
 * Base class for a world-specific controller.
 *
 *
 * A world has its own objects, assets, and input controller.  Thus this is 
 * really a mini-GameEngine in its own right.  The only thing that it does
 * not do is create a GameCanvas; that is shared with the main application.
 *
 * You will notice that asset loading is not done with static methods this time.  
 * Instance asset loading makes it easier to process our game modes in a loop, which 
 * is much more scalable. However, we still want the assets themselves to be static.
 * This is the purpose of our AssetState variable; it ensures that multiple instances
 * place nicely with the static assets.
 */
public class WorldController implements Screen {

	/** Exit code for quitting the game */
	public static final int EXIT_QUIT = 0;
	/** Width of the game world in Box2d units */
	protected static final float DEFAULT_WIDTH  = 32.0f;
	/** Height of the game world in Box2d units */
	protected static final float DEFAULT_HEIGHT = 18.0f;
	/** The default value of gravity (going down) */
	protected static final float DEFAULT_GRAVITY = -4.9f;
	
	/** Reference to the game canvas */
	protected GameCanvas canvas;
	/** Listener that will update the player mode when we are done */
	private ScreenListener listener;

	/** The hashmap for texture regions */
	private HashMap<String, TextureRegion> textureRegionAssetMap;
	/** The hashmap for sounds */
	private HashMap<String, Sound> soundAssetMap;
	/** The hashmap for fonts */
	private HashMap<String, BitmapFont> fontAssetMap;
	/** The JSON value constants */
	private JsonValue constants;
	/** JSON representing the level */
	private JsonValue levelJV;
	/** Level number **/
	private int levelNum;

	private final int TOTAL_LEVELS;
	private LevelController prevLevel;
	private LevelController currLevel;
	private LevelController nextLevel;

	private AssetDirectory directory;

	/**
	 * Returns the canvas associated with this controller
	 *
	 * The canvas is shared across all controllers
	 *
	 * @return the canvas associated with this controller
	 */
	public GameCanvas getCanvas() {
		return currLevel.getCanvas();
	}


	/**
	 * Sets the canvas associated with this controller
	 *
	 * The canvas is shared across all controllers.  Setting this value will compute
	 * the drawing scale from the canvas size.
	 *
	 * @param canvas the canvas associated with this controller
	 */
	public void setCanvas(GameCanvas canvas) {
		currLevel.setCanvas(canvas);
		nextLevel.setCanvas(canvas);
	}

	/**
	 * Creates a new game world with the default values.
	 *
	 * The game world is scaled so that the screen coordinates do not agree
	 * with the Box2d coordinates.  The bounds are in terms of the Box2d
	 * world, not the screen.
	 */
	protected WorldController(int numLevels) {
		this(new Rectangle(0,0,DEFAULT_WIDTH,DEFAULT_HEIGHT),
			 new Vector2(0,DEFAULT_GRAVITY), numLevels);
	}


	/**
	 * Creates a new game world
	 *
	 * The game world is scaled so that the screen coordinates do not agree
	 * with the Box2d coordinates.  The bounds are in terms of the Box2d
	 * world, not the screen.
	 *
	 * @param bounds	The game bounds in Box2d coordinates
	 * @param gravity	The gravitational force on this Box2d world
	 */
	protected WorldController(Rectangle bounds, Vector2 gravity, int numLevels) {
		prevLevel = null;
		currLevel = new LevelController(bounds, gravity);
		nextLevel = new LevelController(bounds, gravity);
		TOTAL_LEVELS = numLevels;
		levelNum = 1;
	}

	public LevelController getCurrLevel() { return currLevel; }

	public void nextLevel(){
		if (levelNum < TOTAL_LEVELS) {
			levelNum++;
			prevLevel = currLevel;
			currLevel = nextLevel;
			currLevel.setRet(false);
			currLevel.reset(prevLevel.getLevel().getCat());
			loadLevel(levelNum, nextLevel);
		}
	}

	public void prevLevel(){
		if (levelNum > 1) {
			levelNum--;
			nextLevel = currLevel;
			currLevel = prevLevel;
			currLevel.setRet(true);
			currLevel.reset(nextLevel.getLevel().getCat());
			loadLevel(levelNum, prevLevel);
		}
	}

	private void loadLevel(int levelNum, LevelController level){
		levelJV = directory.getEntry("platform:level" + levelNum, JsonValue.class);
		level.setAssets(textureRegionAssetMap, fontAssetMap, soundAssetMap, constants, levelJV);
		level.setBackground(textureRegionAssetMap.get("background").getTexture());
	}
	
	/**
	 * Dispose of all (non-static) resources allocated to this mode.
	 */
	public void dispose() {
		currLevel.dispose();
	}

	/**
	 * Gather the assets for this controller.
	 *
	 * This method extracts the asset variables from the given asset directory. It
	 * should only be called after the asset directory is completed.
	 *
	 * @param directory	Reference to global asset manager.
	 */
	public void gatherAssets(AssetDirectory directory) {
		// Allocate the tiles
		// Creating the hashmaps
		this.directory = directory;
		textureRegionAssetMap = new HashMap<String, TextureRegion>();
		soundAssetMap = new HashMap<String, Sound>();
		fontAssetMap = new HashMap<String, BitmapFont>();

		textureRegionAssetMap.put("cat", new TextureRegion(directory.getEntry("platform:cat",Texture.class)));
		textureRegionAssetMap.put("barrier", new TextureRegion(directory.getEntry("platform:barrier",Texture.class)));
		textureRegionAssetMap.put("rope", new TextureRegion(directory.getEntry("platform:rope",Texture.class)));
		textureRegionAssetMap.put("spikes", new TextureRegion(directory.getEntry("platform:spikes", Texture.class)));
		textureRegionAssetMap.put("button", new TextureRegion(directory.getEntry("platform:button", Texture.class)));
		textureRegionAssetMap.put("flame", new TextureRegion(directory.getEntry("platform:flame", Texture.class)));
		textureRegionAssetMap.put("flamethrower", new TextureRegion(directory.getEntry("platform:flamethrower", Texture.class)));
		textureRegionAssetMap.put("laserleft", new TextureRegion(directory.getEntry("platform:laserLeft", Texture.class)));
		textureRegionAssetMap.put("laserbeam", new TextureRegion(directory.getEntry("platform:laserBeam", Texture.class)));
		textureRegionAssetMap.put("laserright", new TextureRegion(directory.getEntry("platform:laserRight", Texture.class)));
		textureRegionAssetMap.put("deadcat", new TextureRegion(directory.getEntry("platform:deadCat", Texture.class)));
		textureRegionAssetMap.put("background", new TextureRegion(directory.getEntry("platform:background", Texture.class)));

		soundAssetMap.put("jump", directory.getEntry( "platform:jump", Sound.class ));
		soundAssetMap.put("pew", directory.getEntry( "platform:pew", Sound.class ));
		soundAssetMap.put("plop", directory.getEntry( "platform:plop", Sound.class ));
		soundAssetMap.put("meow", directory.getEntry( "platform:meow", Sound.class ));

		constants = directory.getEntry("platform:constants", JsonValue.class);



		// Allocate the tiles
		textureRegionAssetMap.put("steel", new TextureRegion(directory.getEntry("shared:steel", Texture.class)));
		textureRegionAssetMap.put("goal", new TextureRegion(directory.getEntry("shared:goal", Texture.class)));
		fontAssetMap.put("display", directory.getEntry( "shared:retro" ,BitmapFont.class));

		// Giving assets to levelController
		loadLevel(1, currLevel);
		loadLevel(2, nextLevel);
	}

	/**
	 * Returns whether to process the update loop
	 *
	 * At the start of the update loop, we check if it is time
	 * to switch to a new game mode.  If not, the update proceeds
	 * normally.
	 *
	 * @param dt	Number of seconds since last animation frame
	 *
	 * @return whether to process the update loop
	 */
	public boolean preUpdate(float dt) {
		if (listener == null) {
			return true;
		}

		// Now it is time to maybe switch screens.
		if (currLevel.preUpdate(dt)) {
			pause();
			listener.exitScreen(this, EXIT_QUIT);
			return false;
		}
		if (currLevel.getLevel().isFailure()) {
			currLevel.reset(null);
		} else if (currLevel.getLevel().isComplete()) {
			pause();
			nextLevel();
			return false;
		} else if (currLevel.isRet()) {
			pause();
			prevLevel();
			return false;
		}
		return true;
	}

	/**
	 * Called when the Screen is resized. 
	 *
	 * This can happen at any point during a non-paused state but will never happen 
	 * before a call to show().
	 *
	 * @param width  The new width in pixels
	 * @param height The new height in pixels
	 */
	public void resize(int width, int height) {
		// IGNORE FOR NOW
	}

	/**
	 * Called when the Screen should render itself.
	 *
	 * We defer to the other methods update() and draw().  However, it is VERY important
	 * that we only quit AFTER a draw.
	 *
	 * @param delta Number of seconds since last animation frame
	 */
	public void render(float delta){
		if (preUpdate(delta)) {
			currLevel.update(delta); // This is the one that must be defined.
			currLevel.postUpdate(delta);
		}
		currLevel.draw(delta);

	}

	/**
	 * Called when the Screen is paused.
	 * 
	 * This is usually when it's not active or visible on screen. An Application is 
	 * also paused before it is destroyed.
	 */
	public void pause() {
		// TODO Auto-generated method stub
	}

	/**
	 * Called when the Screen is resumed from a paused state.
	 *
	 * This is usually when it regains focus.
	 */
	public void resume() {
		// TODO Auto-generated method stub
	}
	
	/**
	 * Called when this screen becomes the current screen for a Game.
	 */
	public void show() {
		// Useless if called in outside animation loop
	}

	/**
	 * Called when this screen is no longer the current screen for a Game.
	 */
	public void hide() {
		// Useless if called in outside animation loop
	}

	/**
	 * Sets the ScreenListener for this mode
	 *
	 * The ScreenListener will respond to requests to quit.
	 */
	public void setScreenListener(ScreenListener listener) {
		this.listener = listener;
	}

}