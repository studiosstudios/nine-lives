package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import edu.cornell.gdiac.assets.AssetDirectory;

public class ControlsStage extends StageWrapper {
    public ControlsStage(AssetDirectory internal, boolean createActors) {
        super(internal, createActors);
    }

    /**
     *
     */
    @Override
    public void createActors() {

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
        return false;
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

    }
}
