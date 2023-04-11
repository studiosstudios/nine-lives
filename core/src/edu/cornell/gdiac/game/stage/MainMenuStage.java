package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import edu.cornell.gdiac.assets.AssetDirectory;

public class MainMenuStage extends StageWrapper{
    private Actor playButtonActor;
    private Actor levelSelectActor;
    private Actor settingsActor;
    private Actor exitButtonActor;
    private int playButtonState;
    private int levelSelectState;
    private int settingsState;
    private int exitButtonState;

    public int getPlayButtonState() { return playButtonState; }
    public void setPlayButtonState(int state) { playButtonState = state; }
    public int getLevelSelectState() { return levelSelectState; }
    public void setLevelSelectState(int state) { levelSelectState = state; }
    public int getSettingsState() { return settingsState; }
    public void setSettingsState(int state) { settingsState = state; }
    public int getExitButtonState() { return exitButtonState; }
    public void setExitButtonState(int state) { exitButtonState = state; }

    public boolean isPlay() { return playButtonState == 2; }
    public boolean isLevelSelect() { return levelSelectState == 2; }
    public boolean isSettings() { return settingsState == 2; }
    public boolean isExit() { return exitButtonState == 2; }

    public MainMenuStage(AssetDirectory internal, boolean createActors) {
        super(internal, createActors);
    }

    /**
     *
     */
    @Override
    public void createActors() {
        addActor(new Image(internal.getEntry( "background", Texture.class )));
        addActor(internal.getEntry("mainMenuCat", Texture.class),-50,-65);
        playButtonActor = addActor(internal.getEntry("playGame", Texture.class),buttonX, buttonY);
        levelSelectActor = addActor(internal.getEntry("levelSelect", Texture.class),buttonX, buttonY-75);
        settingsActor = addActor(internal.getEntry("settings", Texture.class),buttonX, buttonY-150);
        exitButtonActor = addActor(internal.getEntry("exit", Texture.class),buttonX, buttonY-225);
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
        if (actor == playButtonActor) {
            playButtonState = 1;
            playButtonActor.setColor(Color.LIGHT_GRAY);
        } else if (actor == levelSelectActor) {
            levelSelectState = 1;
            levelSelectActor.setColor(Color.LIGHT_GRAY);
        } else if (actor == settingsActor) {
            settingsState = 1;
            settingsActor.setColor(Color.LIGHT_GRAY);
        } else if (actor == exitButtonActor) {
            exitButtonState = 1;
            exitButtonActor.setColor(Color.LIGHT_GRAY);
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
        if (playButtonState == 1) {
            playButtonState = 2;
            playButtonActor.setColor(Color.WHITE);
        } else if (levelSelectState == 1) {
            levelSelectState = 2;
            levelSelectActor.setColor(Color.WHITE);
        } else if (settingsState == 1) {
            settingsState = 2;
            settingsActor.setColor(Color.WHITE);
        } else if (exitButtonState == 1) {
            exitButtonState = 2;
            exitButtonActor.setColor(Color.WHITE);
        }
    }
}
