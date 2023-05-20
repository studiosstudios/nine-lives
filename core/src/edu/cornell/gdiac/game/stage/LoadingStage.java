package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import edu.cornell.gdiac.assets.AssetDirectory;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.parallel;

public class LoadingStage extends StageWrapper {
    Actor paw;
    Actor loadingTutorial;
    Actor loadingText;

    public LoadingStage(String internal, boolean createActors) {
        super(internal, createActors, false);
    }

    /**
     *
     */
    @Override
    public void createActors() {
        loadingTutorial = addActor(internal.getEntry("loading-tutorial", Texture.class), 0, 0);
        loadingTutorial.setScale(0.5f);
        loadingText = addActor(internal.getEntry("loading-text", Texture.class), 730, 20);
        loadingText.setScale(0.5f);
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
