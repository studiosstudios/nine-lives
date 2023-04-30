package edu.cornell.gdiac.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import edu.cornell.gdiac.assets.*;
import edu.cornell.gdiac.game.stage.*;
import edu.cornell.gdiac.util.*;

/**
 * Class that provides a loading screen for the state of the game.
 *
 * Adapted from CS 3152 Lab 4, this class now uses LibGDX's scene2d library to handle
 * the UI of the main and accompanying menus for the game. This is done to dynamically handle
 * resizing of the game window, along with streamlining the input handling for when buttons are clicked.
 *
 * TODO: Develop the rest of the game's menus using separate classes for each "Stage", such as a MainMenuStage, SettingsStage, and etc.
 * Adapted from Walker M. White's LoadingMode.java in Cornell CS 3152, Spring 2023.
 */
public class StageController implements Screen {
	// There are TWO asset managers.  One to load the loading screen.  The other to load the assets
	/** Internal assets for this loading screen */
	private final AssetDirectory internal;
	/** The actual assets to be loaded */
	private AssetDirectory assets;
	/** Standard window size (for scaling) */
	private static int STANDARD_WIDTH  = 1024;
	/** Standard window height (for scaling) */
	private static int STANDARD_HEIGHT = 576;
	/** Default budget for asset loader (do nothing but load 60 fps) */
	private static int DEFAULT_BUDGET = 15;
	/** The amount of time to devote to loading assets (as opposed to onscreen hints, etc.) */
	private int   budget;

	/** Reference to GameCanvas created by the root */
	private GameCanvas canvas;
	/** Listener that will update the player mode when we are done */
	private ScreenListener listener;
	/** Whether this player mode is still active */
	private boolean active;
	public boolean pause = false;
	public boolean loading;
	public boolean starting;
	public boolean fromSelect;

	/** The current stage being rendered on the screen */
	private StageWrapper stage;

	/** The stage for the settings menu */
	private SettingsStage settingsStage;

	/** The stage for the main menu */
	private MainMenuStage mainMenuStage;
	/** The stage for the pause menu */
	private PauseStage pauseStage;
	/** The stage for the level select menu */
	private LevelSelectStage levelSelectStage;
	private LoadingStage loadingStage;
	private StartStage startStage;

	public GameController currLevel;
	private int selectedLevel;

	public int getSelectedLevel() { return selectedLevel; }
	public void setSelectedLevel(int level) { selectedLevel = level; }

	/**
	 * Returns the budget for the asset loader.
	 * <br><br>
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
	 * <br><br>
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
	 * Returns the asset directory produced by this loading screen
	 *
	 * @return the asset directory produced by this loading screen
	 */
	public AssetDirectory getAssets() {
		return assets;
	}

	public StageWrapper getStage() { return stage; }
	public StageWrapper getPauseStage() { return pauseStage; }

	/**
	 * Creates a LoadingMode with the default budget, size and position.
	 *
	 * @param file  	The asset directory to load in the background
	 * @param canvas 	The game canvas to draw to
	 */
	public StageController(String file, GameCanvas canvas) {
		this(file, canvas, DEFAULT_BUDGET, false, false);
	}

	/**
	 * Creates a LoadingMode with the default size and position.
	 * <br><br>
	 * The budget is the number of milliseconds to spend loading assets each animation
	 * frame.  This allows you to do something other than load assets.  An animation 
	 * frame is ~16 milliseconds. So if the budget is 10, you have 6 milliseconds to 
	 * do something else.  This is how game companies animate their loading screens.
	 *
	 * @param file  	The asset directory to load in the background
	 * @param canvas 	The game canvas to draw to
	 * @param millis 	The loading budget in milliseconds
	 */
	public StageController(String file, GameCanvas canvas, int millis, boolean start, boolean paused) {
		this.canvas  = canvas;
		budget = millis;

		// We need these files loaded immediately
		internal = new AssetDirectory( "loading.json" );
		internal.loadAssets();
		internal.finishLoading();

		if(!Save.exists()) {
			Save.create();
		}

		mainMenuStage = new MainMenuStage(internal, true);
		settingsStage = new SettingsStage(internal, true);
		pauseStage = new PauseStage(internal, true);
		levelSelectStage = new LevelSelectStage(internal, true);
		loadingStage = new LoadingStage(internal, true);

		if (start) {
			starting = true;
			startStage = new StartStage(internal, true);
			stage = startStage;
		} else if (paused) {
			stage = pauseStage;
		} else {
			stage = mainMenuStage;
		}

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

	public void loadAssets() {
		assets = new AssetDirectory("assets.json");
		assets.loadAssets();
		assets.finishLoading();
	}
	
	/**
	 * Update the status of this player mode.
	 * <br><br>
	 * We prefer to separate update and draw from one another as separate methods, instead
	 * of using the single render() method that LibGDX does.  We will talk about why we
	 * prefer this in lecture.
	 *
	 * @param delta Number of seconds since last animation frame
	 */
	private void update(float delta) {
		if (starting) {
			if(!assets.isFinished()) {
				assets.update(budget);
				if (assets.getProgress() >= 1.0f) {
					starting = false;
					changeStage(mainMenuStage);
					startStage = null;
				}
			}
		}
	}

	/**
	 * Draw the status of this player mode.
	 * <br><br>
	 * We prefer to separate update and draw from one another as separate methods, instead
	 * of using the single render() method that LibGDX does.  We will talk about why we
	 * prefer this in lecture.
	 */
	private void draw() {
		if (!pause) {
			Gdx.gl.glClearColor(0, 0, 0, 1.0f);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		}
		canvas.begin();
		stage.getViewport().apply();
		stage.act();
		stage.draw();
		canvas.end();
	}

	// ADDITIONAL SCREEN METHODS
	/**
	 * Called when the Screen should render itself.
	 * <br><br>
	 * We defer to the other methods update() and draw().  However, it is VERY important
	 * that we only quit AFTER a draw.
	 *
	 * @param delta Number of seconds since last animation frame
	 */
	public void render(float delta) {
		if (active) {
			if (!pause && startStage != null) { update(delta); }
			draw();
			if (pause) {
				pauseStage.currLevel = this.currLevel;
//				changeStage(pauseStage);
			}
			if (loading) {
				loading = false;
//				try {
//					Thread.sleep(1000);
//				} catch (InterruptedException e) {
//					Thread.currentThread().interrupt();
//				}
				if (fromSelect) {
					fromSelect = false;
					listener.exitScreen(this, 69);
				} else {
					fromSelect = false;
					listener.exitScreen(this,0);
				}
			}

			// We are ready, notify our listener
			if (mainMenuStage.isPlay() && listener != null) {
				loading = true;
				fromSelect = false;
				changeStage(loadingStage);
				getStage().act();
				getStage().draw();
//				listener.exitScreen(this, 0);
			} else if (mainMenuStage.isSettings()) {
				mainMenuStage.setSettingsState(0);
				changeStage(settingsStage);
			} else if (mainMenuStage.isLevelSelect()) {
				mainMenuStage.setLevelSelectState(0);
				changeStage(levelSelectStage);
			} else if (settingsStage.isBack() || levelSelectStage.isBack()) {
				if (settingsStage.isBack()) {
					settingsStage.exit();
				}
				settingsStage.setBackButtonState(0);
				levelSelectStage.setBackButtonState(0);
				changeStage(mainMenuStage);
			} else if (levelSelectStage.isPlay() && listener != null) {
				loading = true;
				fromSelect = true;
				changeStage(loadingStage);
				getStage().act();
				getStage().draw();
				levelSelectStage.setPlayButtonState(0);
				selectedLevel = levelSelectStage.getSelectedLevel();
//				listener.exitScreen(this, 69);
			} else if (pauseStage.isResume() && listener != null) {
				pause = false;
				pauseStage.setResumeButtonState(0);
				pauseStage.currLevel = null;
				listener.exitScreen(this, 25);
			} else if (pauseStage.isMainMenu() && listener != null) {
				pause = false;
				pauseStage.setMainMenuState(0);
				pauseStage.currLevel = null;
				mainMenuStage.createActors();
				changeStage(mainMenuStage);
				listener.exitScreen(this, 79);
			} else if (mainMenuStage.isExit() && listener != null) {
				listener.exitScreen(this, 99);
			}
		}
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
		stage.getViewport().update(width, height, true);
	}

	/**
	 * Called when the Screen is paused.
	 *  <br><br>
	 * This is usually when it's not active or visible on screen. An Application is 
	 * also paused before it is destroyed.
	 */
	public void pause() {
		// TODO Auto-generated method stub

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
	 * <br><br>
	 * The ScreenListener will respond to requests to quit.
	 */
	public void setScreenListener(ScreenListener listener) {
		this.listener = listener;
	}

	// HELPER FUNCTIONS / CLASSES

	/**
	 * Changes the currently active stage.
	 * <br><br>
	 * Not only does this change the stage, but it also updates the InputProcessor to handle
	 * that stage's actors, along with appropriately resizing to ensure the aspect ratio of the
	 * new stage is correct.
	 *
	 * @param s The new stage
	 */
	private void changeStage(StageWrapper s) {
		stage = s;
		Gdx.input.setInputProcessor(s);
		resize(canvas.getWidth(), canvas.getHeight());
	}
}