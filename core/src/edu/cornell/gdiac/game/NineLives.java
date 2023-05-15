package edu.cornell.gdiac.game;

import com.badlogic.gdx.*;
import edu.cornell.gdiac.util.*;
import edu.cornell.gdiac.assets.*;

/**
 * Root class for Nine Lives.
 *
 * This class is used by DesktopLauncher as the root application of the GDXApp running the engine.
 * It stores references to the game's assets (directory), drawing (canvas), UIs (menu), and
 * main game controller (controller).
 *
 * Adapted from Walker M. White's GDXRoot.java in Cornell CS 3152, Spring 2023.
 */
public class NineLives extends Game implements ScreenListener {
	/** AssetManager to load game assets (textures, sounds, etc.) */
	AssetDirectory directory;
	/** Drawing context to display graphics (VIEW CLASS) */
	private GameCanvas canvas;
	/** Player mode for the game menus (CONTROLLER CLASS) */
	private StageController menu;
	/** The WorldController that contains all LevelControllers*/
	private GameController controller;
	/** The AudioController to control all sound effects and music */
	private AudioController audioController;

	private final int TOTAL_LEVELS = 14;
	private boolean quickLaunchFromTiled;
	private String filepath;

	/**
	 * Creates a new game from the configuration settings.
	 *
	 * This method configures the asset manager, but does not load any assets
	 * or assign any screen.
	 */
	public NineLives(boolean quickLaunchFromTiled, String filepath) {
		this.quickLaunchFromTiled = quickLaunchFromTiled;
		this.filepath = filepath;
	}

	/**
	 * Called when the Application is first created.
	 *
	 * This is method immediately loads assets for the loading screen, and prepares
	 * the asynchronous loader for all other assets.
	 */
	public void create() {
		canvas  = new GameCanvas();
		audioController = new AudioController();
		menu = new StageController("jsons/assets.json", canvas, 1, true, false, audioController);
		menu.setScreenListener(this);
		setScreen(menu);
	}

	/**
	 * Called when the Application is destroyed.
	 *
	 * This is preceded by a call to pause().
	 */
	public void dispose() {
		setScreen(null);

		canvas.dispose();
		canvas = null;

		if (menu != null) {
			menu.dispose();
			menu = null;
		}

		if (controller != null) {
			controller.dispose();
			controller = null;
		}

		// Unload all of the resources
		if (directory != null) {
			directory.unloadAssets();
			directory.dispose();
			directory = null;
		}
		super.dispose();
	}

	/**
	 * Called when the Application is resized.
	 *
	 * This can happen at any point during a non-paused state but will never happen
	 * before a call to create().
	 *
	 * @param width  The new width in pixels
	 * @param height The new height in pixels
	 */
	public void resize(int width, int height) {
		Gdx.gl.glViewport(0, 0, width, height);
		canvas.resize();
		super.resize(width,height);
	}

	/**
	 * Load the assets for the game, create the GameController, and start the game.
	 * @param numLevels   total number levels
	 * @param startLevel  starting level number
	 */
	private void startGame(int numLevels, int startLevel){
		directory = menu.getAssets();
		if (quickLaunchFromTiled) {
			controller = new GameController(filepath, audioController);
		} else {
			controller = new GameController(numLevels, audioController);
		}
		controller.gatherAssets(directory);
		controller.setCanvas(canvas);
		controller.init(quickLaunchFromTiled ? 1 : startLevel);
		controller.setScreenListener(this);
		setScreen(controller);
		menu.dispose();
		menu = null;
	}

	/**
	 * The given screen has made a request to exit its player mode.
	 *
	 * The value exitCode can be used to implement menu options.
	 *
	 * @param screen   The screen requesting to exit
	 * @param exitCode The state of the screen upon exit
	 */
	public void exitScreen(Screen screen, int exitCode) {
		if (screen == menu && exitCode == 0) {
//			menu.loadAssets();
			startGame(TOTAL_LEVELS, 1);
		} else if (screen == menu && exitCode == 69) {
//			menu.loadAssets();
			startGame(TOTAL_LEVELS, menu.getSelectedLevel());
		} else if (screen == menu && exitCode == 25) {
			controller.resume();
			setScreen(controller);
			menu.dispose();
			menu = null;
		} else if (exitCode == GameController.EXIT_QUIT && screen == controller) {
			// pause stage
			menu = new StageController("jsons/assets.json", canvas, 1, false, true, audioController);
			menu.setScreenListener(this);
			menu.pause = true;
			menu.currLevel = controller;
			controller.stageController = menu;
			controller.pause();
//			setScreen(menu);
//			controller.getCurrLevel().getLevel().draw(canvas,false);
		} else if (exitCode == 79) {
			if (menu != null) {
				setScreen(menu);
			}
		} else if (exitCode == 99) {
			Gdx.app.exit();
		}
	}

}
