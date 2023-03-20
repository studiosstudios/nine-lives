package edu.cornell.gdiac.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Texture.TextureFilter;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import edu.cornell.gdiac.assets.*;
import edu.cornell.gdiac.util.*;

/**
 * Class that provides a loading screen for the state of the game.
 *
 * Adapted from CS 3152 Lab 4, this class now uses LibGDX's scene2d library to handle
 * the UI of the main and accompanying menus for the game. This is done to dynamically handle
 * resizing of the game window, along with streamlining the input handling for when buttons are clicked.
 *
 * TODO: Develop the rest of the game's menus using separate classes for each "Stage", such as a
 * MainMenuStage, SettingsStage, and etc.
 */
public class LoadingMode implements Screen {
	// There are TWO asset managers.  One to load the loading screen.  The other to load the assets
	/** Internal assets for this loading screen */
	private AssetDirectory internal;
	/** The actual assets to be loaded */
	private AssetDirectory assets;

	/** Standard window size (for scaling) */
	private static int STANDARD_WIDTH  = 1024;
	/** Standard window height (for scaling) */
	private static int STANDARD_HEIGHT = 576;
	/** Default budget for asset loader (do nothing but load 60 fps) */
	private static int DEFAULT_BUDGET = 15;
	/** The amount of time to devote to loading assets (as opposed to on screen hints, etc.) */
	private int   budget;

	/** Reference to GameCanvas created by the root */
	private GameCanvas canvas;
	/** Listener that will update the player mode when we are done */
	private ScreenListener listener;
	/** Whether or not this player mode is still active */
	private boolean active;

	/** x-coordinate for center of button list */
	private int buttonX;
	/** y-coordinate for top button */
	private int buttonY;

	/** The current stage being rendered on the screen */
	private Stage stage;

	/** The stage for the settings menu */
	private Stage settingsStage;

	/** The stage for the main menu */
	private Stage mainMenuStage;
	/** Background texture for start-up */
	private Texture background;
	/** Play button to display when done */
	private Texture playButton;
	/** Level Select button to display when done */
	private Texture levelSelectButton;
	/** Settings button to display when done */
	private Texture settingsButton;
	/** Exit game button to display when done */
	private Texture exitButton;
	/** The current state of the play button */
	private int playButtonState;
	/** The current state of the level select button */
	private int levelSelectButtonState;
	/** The current state of the settings button */
	private int settingsButtonState;
	/** The current state of the exit game button */
	private int exitButtonState;

	/** The actor (Image) for the play button, which helps to handle the input listening for clicks */
	private Actor playButtonActor;
	/** The actor (Image) for the level select button, which helps to handle the input listening for clicks */
	private Actor levelSelectButtonActor;
	/** The actor (Image) for the settings button, which helps to handle the input listening for clicks */
	private Actor settingsButtonActor;
	/** The actor (Image) for the exit button, which helps to handle the input listening for clicks */
	private Actor exitButtonActor;

	/** Texture for the Main Menu Button FROM the settings menu. */
	private Texture mainMenuButton;
	/** State to keep track of whether the main menu button has been clicked */
	private int mainMenuButtonState;
	/** The actor (Image) for the main menu button, which helps to handle the input listening for clicks */
	private Actor mainMenuButtonActor;

	/**
	 * Returns the budget for the asset loader.
	 *
	 * The budget is the number of milliseconds to spend loading assets each animation
	 * frame.  This allows you to do something other than load assets.  An animation
	 * frame is ~16 milliseconds. So if the budget is 10, you have 6 milliseconds to
	 * do something else.  This is how game companies animate their loading screens.
	 *
	 * @return the budget in milliseconds
	 */
	public int getBudget() {
		return budget;
	}

	/**
	 * Sets the budget for the asset loader.
	 *
	 * The budget is the number of milliseconds to spend loading assets each animation
	 * frame.  This allows you to do something other than load assets.  An animation 
	 * frame is ~16 milliseconds. So if the budget is 10, you have 6 milliseconds to 
	 * do something else.  This is how game companies animate their loading screens.
	 *
	 * @param millis the budget in milliseconds
	 */
	public void setBudget(int millis) {
		budget = millis;
	}
	
	/**
	 * Returns true if all assets are loaded and the player is ready to go.
	 *
	 * @return true if the player is ready to go
	 */
	public boolean isReady() {
		return playButtonState == 2;
	}

	/**
	 * Return true if all assets are loaded and the player wants to go to the settings screen.
	 *
	 * @return true if the player wants to go to the settings screen
	 */
	public boolean isSettings() { return settingsButtonState == 2;}

	public boolean isExit() { return exitButtonState == 2; }

	public boolean isBack() { return mainMenuButtonState == 2; }

	/**
	 * Returns the asset directory produced by this loading screen
	 *
	 * This asset loader is NOT owned by this loading scene, so it persists even
	 * after the scene is disposed.  It is your responsbility to unload the
	 * assets in this directory.
	 *
	 * @return the asset directory produced by this loading screen
	 */
	public AssetDirectory getAssets() {
		return assets;
	}

	/**
	 * Creates a LoadingMode with the default budget, size and position.
	 *
	 * @param file  	The asset directory to load in the background
	 * @param canvas 	The game canvas to draw to
	 */
	public LoadingMode(String file, GameCanvas canvas) {
		this(file, canvas, DEFAULT_BUDGET);
	}

	/**
	 * Creates a LoadingMode with the default size and position.
	 *
	 * The budget is the number of milliseconds to spend loading assets each animation
	 * frame.  This allows you to do something other than load assets.  An animation 
	 * frame is ~16 milliseconds. So if the budget is 10, you have 6 milliseconds to 
	 * do something else.  This is how game companies animate their loading screens.
	 *
	 * @param file  	The asset directory to load in the background
	 * @param canvas 	The game canvas to draw to
	 * @param millis 	The loading budget in milliseconds
	 */
	public LoadingMode(String file, GameCanvas canvas, int millis) {
		this.canvas  = canvas;
		budget = millis;
		
		buttonX = (int)(3f/5 * STANDARD_WIDTH);
		buttonY = (int)(1f/2 * STANDARD_HEIGHT);

		// We need these files loaded immediately
		internal = new AssetDirectory( "loading.json" );
		internal.loadAssets();
		internal.finishLoading();

		// Load the next two images immediately.
		background = internal.getEntry( "background", Texture.class );
		background.setFilter( TextureFilter.Linear, TextureFilter.Linear );

		// No progress so far.
		playButtonState = 0;
		levelSelectButtonState = 0;
		settingsButtonState = 0;
		exitButtonState = 0;
		playButton = null;
		levelSelectButton = null;
		settingsButton = null;
		exitButton = null;

		mainMenuStage = new Stage(new ExtendViewport(STANDARD_WIDTH, STANDARD_HEIGHT, STANDARD_WIDTH, STANDARD_HEIGHT));
		Image backgroundImage = new Image(background);
		mainMenuStage.addActor(backgroundImage);

		mainMenuButtonState = 0;
		mainMenuButtonActor = null;

		settingsStage = new Stage(new ExtendViewport(STANDARD_WIDTH, STANDARD_HEIGHT, STANDARD_WIDTH, STANDARD_HEIGHT));
		settingsStage.addActor(new Image(internal.getEntry("settingsBackground", Texture.class)));
		createSettingsStageActors();

		stage = mainMenuStage;

		Gdx.input.setInputProcessor( stage );

		// Start loading the real assets
		assets = new AssetDirectory( file );
		assets.loadAssets();
		active = true;
	}
	
	/**
	 * Called when this screen should release all resources.
	 */
	public void dispose() {
		internal.unloadAssets();
		internal.dispose();
	}
	
	/**
	 * Update the status of this player mode.
	 *
	 * We prefer to separate update and draw from one another as separate methods, instead
	 * of using the single render() method that LibGDX does.  We will talk about why we
	 * prefer this in lecture.
	 *
	 * @param delta Number of seconds since last animation frame
	 */
	private void update(float delta) {
		if (playButton == null) {
			assets.update(budget);
			if (assets.getProgress() >= 1.0f) {
				createMainMenuStageActors();
			}
		}
	}

	/**
	 * Draw the status of this player mode.
	 *
	 * We prefer to separate update and draw from one another as separate methods, instead
	 * of using the single render() method that LibGDX does.  We will talk about why we
	 * prefer this in lecture.
	 */
	private void draw() {
		Gdx.gl.glClearColor(0, 0, 0, 1.0f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		canvas.begin();
		stage.getViewport().apply();
		stage.act();
		stage.draw();
		canvas.end();
	}

	// ADDITIONAL SCREEN METHODS
	/**
	 * Called when the Screen should render itself.
	 *
	 * We defer to the other methods update() and draw().  However, it is VERY important
	 * that we only quit AFTER a draw.
	 *
	 * @param delta Number of seconds since last animation frame
	 */
	public void render(float delta) {
		if (active) {
			update(delta);
			draw();

			// We are are ready, notify our listener
			if (isReady() && listener != null) {
				listener.exitScreen(this, 0);
			} else if (isSettings()) {
//				listener.exitScreen(this, 1);
				settingsButtonState = 0;
				changeStage(settingsStage);
			} else if (isBack()) {
				mainMenuButtonState = 0;
				changeStage(mainMenuStage);
			}
			else if (isExit() && listener != null) {
				listener.exitScreen(this, 99);
			}
		}
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
		stage.getViewport().update(width, height, true);
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
		active = true;
	}

	/**
	 * Called when this screen is no longer the current screen for a Game.
	 */
	public void hide() {
		// Useless if called in outside animation loop
		active = false;
	}

	/**
	 * Sets the ScreenListener for this mode
	 *
	 * The ScreenListener will respond to requests to quit.
	 */
	public void setScreenListener(ScreenListener listener) {
		this.listener = listener;
	}

	// HELPER FUNCTIONS / CLASSES

	/**
	 * Creates the actors for the main menu stage.
	 *
	 * TODO: Factor this out into an external `MainMenuStage` class.
	 */
	private void createMainMenuStageActors() {
		playButton = internal.getEntry("playGame", Texture.class);
		EventListener mainMenuButtonListener = new MainMenuButtonListener();
		playButtonActor = new Image(playButton);
		playButtonActor.setPosition(buttonX, buttonY);
		playButtonActor.addListener(mainMenuButtonListener);
		mainMenuStage.addActor(playButtonActor);

		settingsButton = internal.getEntry("settings", Texture.class);
		settingsButtonActor = new Image(settingsButton);
		settingsButtonActor.setPosition(buttonX, buttonY-75);
		settingsButtonActor.addListener(mainMenuButtonListener);
		mainMenuStage.addActor(settingsButtonActor);

		levelSelectButton = internal.getEntry("levelSelect", Texture.class);
		levelSelectButtonActor = new Image(levelSelectButton);
		levelSelectButtonActor.setPosition(buttonX, buttonY-150);
		levelSelectButtonActor.addListener(mainMenuButtonListener);
		mainMenuStage.addActor(levelSelectButtonActor);

		exitButton = internal.getEntry("exit", Texture.class);
		exitButtonActor = new Image(exitButton);
		exitButtonActor.setPosition(buttonX, buttonY-225);
		exitButtonActor.addListener(mainMenuButtonListener);
		mainMenuStage.addActor(exitButtonActor);
	}

	/**
	 * Creates the actors for the settings stage.
	 * TODO: Factor this out into an external `SettingsStage` class
	 */
	private void createSettingsStageActors() {
		mainMenuButton = internal.getEntry("back", Texture.class);
		mainMenuButtonActor = new Image(mainMenuButton);
		mainMenuButtonActor.setPosition(buttonX, buttonY);
		mainMenuButtonActor.addListener(new MainMenuButtonListener());
		settingsStage.addActor(mainMenuButtonActor);
	}

	/**
	 * Changes the currently active stage.
	 *
	 * Not only does this change the stage, but it also updates the InputProcessor to handle
	 * that stage's actors, along with appropriately resizing to ensure the aspect ratio of the
	 * new stage is correct.
	 * @param s
	 */
	private void changeStage(Stage s) {
		stage = s;
		Gdx.input.setInputProcessor(s);
		resize(canvas.getWidth(), canvas.getHeight());
	}

	/**
	 * TEMPORARY Listener class to handle button listening.
	 *
	 * TODO: Factor this out with a separate `MainMenuScreen`
	 * TODO: Separate these listeners with the `mainMenuButtonActor` listener, which is supposed to be with the SettingsScreen
	 */

	private class MainMenuButtonListener extends InputListener {
		@Override
		public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
			Actor actor = event.getListenerActor();
			if (actor == playButtonActor) {
				playButtonState = 1;
				playButtonActor.setColor(Color.LIGHT_GRAY);
			}
			else if (actor == levelSelectButtonActor) {

			}
			else if (actor == settingsButtonActor) {
				settingsButtonState = 1;
				settingsButtonActor.setColor(Color.LIGHT_GRAY);
			}
			else if (actor == exitButtonActor) {
				exitButtonState = 1;
				exitButtonActor.setColor(Color.LIGHT_GRAY);
			}
			//temp
			else if (actor == mainMenuButtonActor) {
				//TODO: factor this out (CJ)
				mainMenuButtonState = 1;
				mainMenuButtonActor.setColor(Color.LIGHT_GRAY);
			}
			return true;
		}

		@Override
		public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
			if (playButtonState == 1) {
				playButtonState = 2;
				playButtonActor.setColor(Color.WHITE);
			}
			else if (levelSelectButtonState == 1) {

			}
			else if (settingsButtonState == 1) {
				settingsButtonState = 2;
				settingsButtonActor.setColor(Color.WHITE);
			}
			else if (exitButtonState == 1) {
				exitButtonState = 2;
				exitButtonActor.setColor(Color.WHITE);
			}
			//temp
			else if (mainMenuButtonState == 1) {
				//TODO: factor this out (CJ)
				mainMenuButtonState = 2;
				mainMenuButtonActor.setColor(Color.WHITE);
			}
		}
	}

}