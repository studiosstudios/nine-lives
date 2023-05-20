package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;

public class CreditsStage extends StageWrapper {
    private Actor background;
    public boolean finished;
    public CreditsStage(String directory, boolean createActors, boolean settings) {
        super(directory, createActors, settings);
    }

    /**
     *
     */
    @Override
    public void createActors() {
        background = addActor(internal.getEntry("bg-credits", Texture.class), 0, 0);
        background.setScale(0.5f);
    }

    @Override
    public void update(float delta) {
        super.update(delta);
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
