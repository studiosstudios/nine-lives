package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import edu.cornell.gdiac.assets.AssetDirectory;

public class StartStage extends StageWrapper{
    public Actor paw;
    public StartStage(String internal, boolean createActors) {
        super(internal, createActors, false);
    }

    /**
     *
     */
    @Override
    public void createActors() {
        Actor first = addActor(internal.getEntry("splash-screen", Texture.class), 0,0);
        first.setScale(0.5f);
        paw = addActor(internal.getEntry("paw", Texture.class), 0, 60);
        paw.setX(xHalf-paw.getWidth()/2);
        paw.setScale(0.9f);
        paw.setColor(Color.BLACK);
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

    @Override
    public void update(float delta) {

    }
}
