package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction;
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;

public class CreditsStage extends StageWrapper {
    private Actor background;
    public boolean finished;
    DelayAction delay;
    RunnableAction quit;
    public CreditsStage(String directory, boolean createActors, boolean settings) {
        super(directory, createActors, settings);
        delay = new DelayAction(2);
        quit = new RunnableAction();
        quit.setRunnable(() -> {
            finished = true;
            background.clearActions();;
        });
        SequenceAction sequence = new SequenceAction(delay, quit);
        background.addAction(sequence);
    }

    /**
     *
     */
    @Override
    public void createActors() {
        background = addActor(internal.getEntry("bg-credits", Texture.class), 0, 0);
        background.setScale(0.5f);
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
