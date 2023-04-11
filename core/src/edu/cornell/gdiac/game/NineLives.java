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
	private WorldController controller;

	/**
	 * Creates a new game from the configuration settings.
	 *
	 * This method configures the asset manager, but does not load any assets
	 * or assign any screen.
	 */
	public NineLives() { }

	/**
	 * Called when the Application is first created.
	 *
	 * This is method immediately loads assets for the loading screen, and prepares
	 * the asynchronous loader for all other assets.
	 */
	public void create() {
		canvas  = new GameCanvas();
		menu = new StageController("assets.json", canvas, 1);

		controller = new WorldController(5);
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

		controller.dispose();
		controller = null;

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
		canvas.resize();
		super.resize(width,height);
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
		if (exitCode == 77) {
//			setScreen(menu);
			menu.loadAssets(); // THIS IS A GOOD PLACE TO PUT A CONTROLS LOADING SCREEN
			directory = menu.getAssets();
			controller.gatherAssets(directory);
			controller.setScreenListener(this);
			controller.setCanvas(canvas);
			controller.resume();
			controller.getCurrLevel().reset(null);
			setScreen(controller);
			menu.dispose();
			menu = null;
		} else if (exitCode == 69) {
//			setScreen(menu);
			directory = menu.getAssets();
			controller.gatherAssets(directory);
			controller.setScreenListener(this);
			controller.setCanvas(canvas);
			controller.resume();
			controller.setCurrLevel(menu.getSelectedLevel());
			controller.getCurrLevel().reset(null);
			setScreen(controller);
			menu.dispose();
			menu = null;
		} else if (exitCode == 25) {
//			setScreen(menu);
			controller.pauseStage = null;
			controller.resume();
			setScreen(controller);
			menu.dispose();
			menu = null;
		} else if (exitCode == WorldController.EXIT_QUIT && screen == controller) {
			// pause stage
			menu = new StageController("assets.json", canvas, 1);
			menu.setScreenListener(this);
			menu.pause = true;
			menu.currLevel = controller.getCurrLevel();
			controller.pauseStage = menu;
			controller.pause();
//			setScreen(menu);
//			controller.getCurrLevel().getLevel().draw(canvas,false);
		} else if (exitCode == 88) {
			setScreen(menu);
		} else if (exitCode == 99) {
			Gdx.app.exit();
		}
	}

}
