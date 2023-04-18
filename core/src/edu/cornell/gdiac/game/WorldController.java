package edu.cornell.gdiac.game;

import java.util.HashMap;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.*;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.util.*;

/**
 * Controller for the active world, along with the associated set of levels contained within that world.
 * <br><br>
 * A world has its own objects, assets, and input controller.  Thus this is 
 * really a mini-GameEngine in its own right.  The only thing that it does
 * not do is create a GameCanvas; that is shared with the main application.
 * <br><br>
 * You will notice that asset loading is not done with static methods this time.  
 * Instance asset loading makes it easier to process our game modes in a loop, which 
 * is much more scalable. However, we still want the assets themselves to be static.
 * This is the purpose of our AssetState variable; it ensures that multiple instances
 * place nicely with the static assets.
 * <br><br>
 * Adapted from Walker M. White's WorldController.java in Cornell CS 3152, Spring 2023.
 */
public class WorldController implements Screen {

	/** The Box2D world */
	protected World world;
	/** Exit code for quitting the game */
	public static final int EXIT_QUIT = 0;
	/** Default drawscale */
	protected static final float DEFAULT_SCALE = 32;
	/** The default value of gravity (going down) */
	protected static final float DEFAULT_GRAVITY = -4.9f;

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

	/** Level number **/
	private int levelNum;
	/** Number of levels in this world */
	private final int numLevels;
	/** JSON for the previous level */
	private JsonValue prevJSON;
	/** LevelController for the current level */
	private LevelController levelController;
	/** JSON for the next level */
	private JsonValue nextJSON;
	/** The AssetDirectory */
	private AssetDirectory directory;
	private Level nextLevel;
	private Level prevLevel;

	/**
	 * Returns the canvas associated with the current LevelController
	 * <br>
	 * The canvas is shared across all controllers
	 *
	 * @return the canvas associated with this controller
	 */
	public GameCanvas getCanvas() {
		return levelController.getCanvas();
	}

	/**
	 * Sets the canvas associated with this controller
	 * <br><br>
	 * The canvas is shared across all controllers.  Setting this value will compute
	 * the drawing scale from the canvas size.
	 *
	 * @param canvas the canvas associated with this controller
	 */
	public void setCanvas(GameCanvas canvas) {
		levelController.setCanvas(canvas);
	}

	/**
	 * Sets the ScreenListener for this mode
	 * <br><br>
	 * The ScreenListener will respond to requests to quit.
	 */
	public void setScreenListener(ScreenListener listener) {
		this.listener = listener;
	}

	/**
	 * Creates a new game world with the default values.
	 * <br><br>
	 * The game world is scaled so that the screen coordinates do not agree
	 * with the Box2D coordinates.  The bounds are in terms of the Box2d
	 * world, not the screen.
	 */
	protected WorldController(int numLevels) {
		this(new Vector2(0,DEFAULT_GRAVITY), numLevels);
	}

	/**
	 * Creates a new controller for the game world
	 *
	 * @param gravity	The gravitational force on this Box2D world
	 */
	protected WorldController(Vector2 gravity, int numLevels) {
		world = new World(gravity, false);
		levelController = new LevelController(new Vector2(DEFAULT_SCALE, DEFAULT_SCALE), world);
		this.numLevels = numLevels;
		levelNum = 1;
	}

	/**
	 * Returns the current LevelController
	 *
	 * @return The current LevelController for the current level
	 */
	public LevelController getLevelController() { return levelController; }

	/**
	 * Steps the level
	 * <br><br>
	 * The previous level is set to the current level<br>
	 * The current level is set to the next level<br>
	 * The next level is loaded in<br>
	 *
	 * @param resetSpawn whether the player spawns at the respawn point of the level or at the edge
	 *                    (from previous level)
	 */
	public void nextLevel(boolean resetSpawn){
		if (levelNum < numLevels) {
			levelNum++;

			prevJSON = levelController.getJSON();
			levelController.setJSON(nextJSON);
			levelController.setRet(false);
			reset();
			if (levelNum < numLevels) {
				nextJSON = tiledJSON(levelNum + 1);
			}
		}
	}

	/**
	 * Steps the level
	 * <br><br>
	 * The next level is set to the current level<br>
	 * The current level is set to the previous level<br>
	 * The previous level is loaded in<br>
	 *
	 * @param resetSpawn whether the player spawns at the respawn point of the level or at the edge
	 *                    (from previous level)
	 */
	public void prevLevel(boolean resetSpawn){
		if (levelNum > 1) {
			levelNum--;
			nextJSON = levelController.getJSON();
			levelController.setJSON(prevJSON);
			levelController.setRet(!resetSpawn);
			reset();
			if (levelNum > 1) {
				prevJSON = tiledJSON(levelNum - 1);
			}
		}
	}
	public void setLevelController(int level) {
		if (level < numLevels) {
			levelController.setJSON(tiledJSON(level+1));
		}
	}


	/**
	 * Loads in the JSON of a level
	 *
	 * @param levelNum the number associated with the level to be loaded in
	 * @return JSON of the level
	 */
	private JsonValue tiledJSON(int levelNum){ return directory.getEntry("tiledLevel" + levelNum, JsonValue.class); }


	/**
	 * Dispose of all (non-static) resources allocated to this mode.
	 */
	public void dispose() {
		levelController.dispose();
	}

	/**
	 * Gather the assets for this controller.
	 * <br><br>
	 * This method extracts the asset variables from the given asset directory. It
	 * should only be called after the asset directory is completed.
	 *
	 * @param directory	Reference to global asset manager.
	 */
	public void gatherAssets(AssetDirectory directory) {
		// Allocate the tiles
		// Creating the hashmaps
		textureRegionAssetMap = new HashMap<>();
		soundAssetMap = new HashMap<>();
		fontAssetMap = new HashMap<>();

		String[] names = {"cat", "sit", "deadCat", "jumpingCat", "jump_anim", "walk", "button_anim",
				"spikes", "button", "flamethrower", "flame", "laser", "checkpoint", "checkpointActive",
				"checkpoint_anim", "checkpoint_active_anim", "checkpoint_base", "checkpoint_base_active",
				"background", "flame_anim", "roboMob",
				"spirit_anim", "spirit_photon", "spirit_photon_cat", "spirit_region",
				"meow_anim", "idle_anim", "idle_anim_stand",
				"metal_tileset", "steel","burnCat"};

		for (String n : names){
			textureRegionAssetMap.put(n, new TextureRegion(directory.getEntry(n, Texture.class)));
		}

		names = new String[]{"jump", "dash", "metalLanding", "pew", "plop", "meow"};
		for (String n : names){
			soundAssetMap.put(n, directory.getEntry(n, Sound.class));
		}

		names = new String[]{"retro"};
		for (String n : names){
			fontAssetMap.put(n, directory.getEntry(n, BitmapFont.class));
		}

		constants = directory.getEntry("constants", JsonValue.class);
		this.directory = directory;

		// Giving assets to levelController
		levelController.setAssets(textureRegionAssetMap, fontAssetMap, soundAssetMap, constants);
		levelController.setJSON(tiledJSON(1));
		nextJSON = tiledJSON(2);

		//Set controls
		InputController.getInstance().setControls(directory.getEntry("controls", JsonValue.class));

//		InputController.getInstance().writeTo("inputLogs/alphademo.txt");
//		InputController.getInstance().readFrom("inputLogs/alphademo.txt");
	}

	/**
	 * Returns whether to process the update loop
	 * <br><br>
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
		if (levelController.preUpdate(dt)) {
			pause();
			listener.exitScreen(this, EXIT_QUIT);
			return false;
		}

		InputController input = InputController.getInstance();

		if (levelController.getLevel().isFailure() || input.didReset()) {
			reset();
		} else if (levelController.getLevel().isComplete() || input.didNext()) {
			pause();
			nextLevel(input.didNext());
			return false;
		} else if (levelController.isRet() || input.didPrev()) {
			pause();
			prevLevel(input.didPrev());
			return false;
		}
		return true;
	}

	public void reset(){
		Vector2 gravity = new Vector2( world.getGravity() );
		world = new World(gravity, false);

		levelController.reset(world);
	}

	/**
	 * Called when the Screen is resized. 
	 * <br><br>
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
	 * <br><br>
	 * We defer to the other methods update() and draw().  However, it is VERY important
	 * that we only quit AFTER a draw.
	 *
	 * @param delta Number of seconds since last animation frame
	 */
	public void render(float delta){
		//FOR DEBUGGING
//		delta = 1/60f;
//		if (Gdx.input.isKeyPressed(Input.Keys.F)){
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				Thread.currentThread().interrupt();
//			}
//		}

		if (preUpdate(delta)) {
			levelController.update(delta); // This is the one that must be defined.
			levelController.postUpdate(delta);
		}
		levelController.draw(delta);
	}

	/**
	 * Called when the Screen is paused.
	 * <br><br>
	 * This is usually when it's not active or visible on screen. An Application is 
	 * also paused before it is destroyed.
	 */
	public void pause() {
//		currLevel.pause();
	}

	/**
	 * Called when the Screen is resumed from a paused state.
	 * <br><br>
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
}