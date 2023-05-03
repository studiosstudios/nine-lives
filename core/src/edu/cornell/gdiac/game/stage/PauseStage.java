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
    /** State to keep track of whether the main menu button has been clicked */
    private int resumeButtonState;
    private int mainMenuState;

    public boolean isResume() { return resumeButtonState == 2; }
    public int getResumeButtonState() { return resumeButtonState; }
    public void setResumeButtonState(int state) { resumeButtonState = state; }
    public boolean isMainMenu() { return mainMenuState == 2; }
    public int getMainMenuState() { return mainMenuState; }
    public void setMainMenuState(int state) { mainMenuState = state; }

    public GameController currLevel;
    public PauseStage(AssetDirectory internal, boolean createActors) {
        super(internal, createActors);
    }

    /**
     *
     */
    @Override
    public void createActors() {
        Actor background = addActor(internal.getEntry("bg-pause", Texture.class),0, 0);
        background.setScale(0.5f);
        resumeButtonActor = addActor(internal.getEntry("resume", Texture.class), 0,buttonY-50);
        resumeButtonActor.setScale(0.5f);
        resumeButtonActor.setX(xHalf-resumeButtonActor.getWidth()/4);
        mainMenuActor = addActor(internal.getEntry("main-menu", Texture.class), 0, buttonY-100);
        mainMenuActor.setScale(0.5f);
        mainMenuActor.setX(xHalf-mainMenuActor.getWidth()/4);

        resumeButtonActor.addListener(createHoverListener(resumeButtonActor));
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
        }
    }
}
