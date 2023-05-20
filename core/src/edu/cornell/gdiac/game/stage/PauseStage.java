package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.game.GameController;

public class PauseStage extends StageWrapper {
    private Actor resumeButtonActor;
    private Actor mainMenuActor;
    private Actor settingsActor;
    private Actor restartActor;
    /** State to keep track of whether the main menu button has been clicked */
    private int resumeButtonState;
    private int mainMenuState;
    private int settingsState;
    private int restartState;

    public boolean isResume() { return resumeButtonState == 2; }
    public int getResumeButtonState() { return resumeButtonState; }
    public void setResumeButtonState(int state) { resumeButtonState = state; }
    public boolean isMainMenu() { return mainMenuState == 2; }
    public int getMainMenuState() { return mainMenuState; }
    public void setMainMenuState(int state) { mainMenuState = state; }
    public boolean isSettings() { return settingsState == 2; }
    public int getSettingsState() { return settingsState; }
    public void setSettingsState(int state) { settingsState = state; }
    public boolean isRestart() { return restartState == 2; }
    public int getRestartState() { return restartState; }
    public void setRestartState(int state) { restartState = state; }

    public GameController currLevel;
    public PauseStage(AssetDirectory internal, boolean createActors) {
        super(internal, createActors, false);
    }

    /**
     *
     */
    @Override
    public void createActors() {
        Actor background = addActor(internal.getEntry("bg-pause", Texture.class),0, 0);
        background.setScale(0.5f);
        resumeButtonActor = addActor(internal.getEntry("resume", Texture.class), 0,buttonY-150+23+20+23+20+23+20);
        resumeButtonActor.setScale(0.5f);
        resumeButtonActor.setX(xHalf-resumeButtonActor.getWidth()/4);
        restartActor = addActor(internal.getEntry("restart", Texture.class), 0, buttonY-150+23+20+23+20);
        restartActor.setScale(0.5f);
        restartActor.setX(xHalf-restartActor.getWidth()/4);
        settingsActor = addActor(internal.getEntry("pause-settings", Texture.class), 0, buttonY-150+15+20);
        settingsActor.setScale(0.5f);
        settingsActor.setX(xHalf-settingsActor.getWidth()/4);
        mainMenuActor = addActor(internal.getEntry("main-menu", Texture.class), 0, buttonY-150);
        mainMenuActor.setScale(0.5f);
        mainMenuActor.setX(xHalf-mainMenuActor.getWidth()/4);

        resumeButtonActor.addListener(createHoverListener(resumeButtonActor));
        restartActor.addListener(createHoverListener(restartActor));
        settingsActor.addListener(createHoverListener(settingsActor));
        mainMenuActor.addListener(createHoverListener(mainMenuActor));
    }

    /**
     * @param event
     * @param x
     * @param y
     * @param pointer
     * @param button
     * @return
     */
    @Override
    public boolean listenerTouchDown(InputEvent event, float x, float y, int pointer, int button) {
        Actor actor = event.getListenerActor();
        if (actor == resumeButtonActor) {
            resumeButtonState = 1;
            resumeButtonActor.setColor(Color.LIGHT_GRAY);
        } else if (actor == mainMenuActor) {
            mainMenuState = 1;
            mainMenuActor.setColor(Color.LIGHT_GRAY);
        } else if (actor == settingsActor) {
            settingsState = 1;
            settingsActor.setColor(Color.LIGHT_GRAY);
        } else if (actor == restartActor) {
            restartState = 1;
            restartActor.setColor(Color.LIGHT_GRAY);
        }
        return true;
    }

    /**
     * @param event
     * @param x
     * @param y
     * @param pointer
     * @param button
     */
    @Override
    public void listenerTouchUp(InputEvent event, float x, float y, int pointer, int button) {
        if (resumeButtonState == 1) {
            resumeButtonState = 2;
            resumeButtonActor.setColor(Color.WHITE);
        } else if (mainMenuState == 1) {
            mainMenuState = 2;
            mainMenuActor.setColor(Color.WHITE);
        } else if (settingsState == 1) {
            settingsState = 2;
            settingsActor.setColor(Color.WHITE);
        } else if (restartState == 1) {
            restartState = 2;
            restartActor.setColor(Color.WHITE);
        }
    }
}
