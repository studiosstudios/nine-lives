package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction;
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import edu.cornell.gdiac.assets.AssetDirectory;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.forever;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.parallel;

public class LoadingStage extends StageWrapper {
    Actor paw;
    Actor loadingTutorial;
    Actor loadingText1;
    Actor loadingText2;
    Actor loadingText3;

    public LoadingStage(AssetDirectory internal, boolean createActors) {
        super(internal, createActors);
        DelayAction delay = new DelayAction(3f);

        SequenceAction show1 = new SequenceAction();
        // 0
        show1.addAction(Actions.show());
        show1.addAction(delay);
        // 3
        show1.addAction(Actions.hide());
        show1.addAction(delay);
        // 6
        show1.addAction(delay);


        SequenceAction show2 = new SequenceAction();
        // 0
        show2.addAction(delay);
        // 3
        show2.addAction(Actions.show());
        show2.addAction(delay);
        // 6
        show2.addAction(Actions.hide());
        show2.addAction(delay);

        SequenceAction show3 = new SequenceAction();
        // 0
        show3.addAction(delay);
        // 3
        show3.addAction(delay);
        // 6
        show3.addAction(Actions.show());
        show3.addAction(delay);
        // 9
        show3.addAction(Actions.hide());

//        loadingText1.addAction(forever(show1));
//        loadingText2.addAction(forever(show2));
//        loadingText3.addAction(forever(show3));
    }

    /**
     *
     */
    @Override
    public void createActors() {
        loadingTutorial = addActor(internal.getEntry("loading-tutorial", Texture.class), 0, 0);
        loadingTutorial.setScale(0.5f);
        loadingText1 = addActor(internal.getEntry("loading-text-1", Texture.class), 725, 20);
        loadingText1.setScale(0.5f);
        loadingText1.setVisible(false);
        loadingText2 = addActor(internal.getEntry("loading-text-2", Texture.class), 725, 20);
        loadingText2.setScale(0.5f);
        loadingText2.setVisible(false);
        loadingText3 = addActor(internal.getEntry("loading-text-3", Texture.class), 730, 20);
        loadingText3.setScale(0.5f);
//        loadingText3.setVisible(false);
        paw = addActor(internal.getEntry("paw", Texture.class), 0, 20);
        paw.setX(xHalf*2-paw.getWidth()-25);
//        paw.setScale(0.9f);
        paw.setColor(Color.WHITE);
        paw.setOrigin(paw.getWidth()/2, paw.getHeight()/2);
        paw.addAction(Actions.rotateBy(1800,30));
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
