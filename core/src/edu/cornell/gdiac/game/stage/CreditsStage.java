package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.*;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.assets.AssetDirectory;

public class CreditsStage extends StageWrapper {
    private Actor background;
    private AnimatedActor caliActor;
    private AnimationDrawable caliAnimation;
    private Actor creditsRoll;
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
        background = addActor(internal.getEntry("bg-credits", Texture.class), 0, 0);
        background.setScale(0.5f);

        Array<TextureRegion> sleepingFrames = flattenFilmStrip(internal.getEntry("credits-cat-sleeping-anim", Texture.class),2048,576);

        Array<TextureRegion> caliKeyFrames = new Array<>();
        caliKeyFrames.addAll(flattenFilmStrip(internal.getEntry("credits-cat-walking-anim", Texture.class),2048,576));
        caliKeyFrames.addAll(flattenFilmStrip(internal.getEntry("credits-cat-rolling-anim", Texture.class),2048,576));
        caliKeyFrames.addAll(sleepingFrames);
        caliKeyFrames.addAll(sleepingFrames);
        caliKeyFrames.addAll(sleepingFrames);
        caliKeyFrames.addAll(sleepingFrames);
        caliKeyFrames.addAll(sleepingFrames);
        caliKeyFrames.addAll(sleepingFrames);
        caliKeyFrames.addAll(sleepingFrames);
        caliKeyFrames.addAll(sleepingFrames);
        caliKeyFrames.addAll(sleepingFrames);
        caliKeyFrames.addAll(sleepingFrames);
        caliKeyFrames.addAll(sleepingFrames);
        caliKeyFrames.addAll(sleepingFrames);
        caliKeyFrames.addAll(sleepingFrames);
        Animation<TextureRegion> cali = new Animation<>(0.25f, caliKeyFrames);
        cali.setPlayMode(Animation.PlayMode.NORMAL);

        caliAnimation = new AnimationDrawable(cali);
        caliActor = new AnimatedActor(caliAnimation);
        addActor(caliActor);
        caliActor.setScale(0.5f);
        caliActor.setPosition(0,0);

        creditsRoll = addActor(internal.getEntry("credits-roll", Texture.class), -42, -2000);
        creditsRoll.setScale(0.5f);
        creditsRoll.addAction(Actions.moveBy(0,2500,25));

        delay = new DelayAction(25);
        quit = new RunnableAction();
        quit.setRunnable(() -> {
            finished = true;
            background.clearActions();;
        });
        SequenceAction sequence = new SequenceAction(delay, quit);

        background.addAction(sequence);
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        caliActor.act(delta);
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

    private Array<TextureRegion> flattenFilmStrip(Texture filmstrip, int w, int h) {
        Array<TextureRegion> flattened = new Array<>();
        TextureRegion[][] tempFrames = TextureRegion.split(filmstrip,w,h);
        for (int i = 0; i < tempFrames.length; i++) {
            for (int j = 0; j < tempFrames[i].length; j++) {
                flattened.add(tempFrames[i][j]);
            }
        }
        return flattened;
    }
 }
