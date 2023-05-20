package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction;
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.assets.AssetDirectory;

public class CreditsStage extends StageWrapper {
    private Actor background;
    private AnimatedActor walkingActor;
    private AnimationDrawable walkingAnimation;
    public boolean finished;
    DelayAction delay;
    RunnableAction quit;
    public CreditsStage(String directory, boolean createActors, boolean settings) {
        super(directory, createActors, settings);
    }

    /**
     *
     */
    @Override
    public void createActors() {
        Array<TextureRegion> flattened = new Array<>();
        TextureRegion[][] tempFrames = TextureRegion.split(internal.getEntry("credits-cat-walking-anim", Texture.class),2048,576);
        for (int i = 0; i < tempFrames.length; i++) {
            for (int j = 0; j < tempFrames[i].length; j++) {
                flattened.add(tempFrames[i][j]);
            }
        }
        Animation<TextureRegion> anim = new Animation<>(0.15f, flattened);
        anim.setPlayMode(Animation.PlayMode.NORMAL);
        walkingAnimation = new AnimationDrawable(anim);
        background = addActor(internal.getEntry("bg-credits", Texture.class), 0, 0);
        background.setScale(0.5f);
        walkingActor = new AnimatedActor(walkingAnimation);
        addActor(walkingActor);
        walkingActor.setScale(0.5f);
        walkingActor.setPosition(0,0);

        delay = new DelayAction(10);
        quit = new RunnableAction();
        quit.setRunnable(() -> {
            finished = true;
            background.clearActions();;
        });
        SequenceAction sequence = new SequenceAction(delay, quit);
//        background.addAction(sequence);
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        walkingActor.act(delta);
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