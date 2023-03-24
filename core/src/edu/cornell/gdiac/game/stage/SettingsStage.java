package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import edu.cornell.gdiac.assets.AssetDirectory;

public class SettingsStage extends StageWrapper {
    private Actor BackButtonActor;
    /** State to keep track of whether the main menu button has been clicked */
    private int backButtonState;

    public boolean isBack() { return backButtonState == 2; }
    public int getBackButtonState() { return backButtonState; }
    public void setBackButtonState(int state) { backButtonState = state; }

    public SettingsStage(AssetDirectory internal, boolean createActors) {
        super(internal, createActors);
    }

    /**
     *
     */
    @Override
    public void createActors() {
        addActor(new Image(internal.getEntry("settingsBackground", Texture.class)));
        BackButtonActor = addActor(internal.getEntry("back", Texture.class),buttonX,buttonY);
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
        if (actor == BackButtonActor) {
            backButtonState = 1;
            BackButtonActor.setColor(Color.LIGHT_GRAY);
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
        if (backButtonState == 1) {
            backButtonState = 2;
            BackButtonActor.setColor(Color.WHITE);
        }
    }
}
