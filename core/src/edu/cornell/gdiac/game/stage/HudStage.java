package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import edu.cornell.gdiac.assets.AssetDirectory;

public class HudStage extends StageWrapper {
    public int lives = 9;

    Actor nine;
    Actor eight;
    Actor seven;
    Actor six;
    Actor five;
    Actor four;
    Actor three;
    Actor two;
    Actor one;

    Array<Actor> bellArray = new Array<>();

    public HudStage(AssetDirectory internal, boolean createActors) {
        super(internal, createActors);
        bellArray.add(nine);
        bellArray.add(eight);
        bellArray.add(seven);
        bellArray.add(six);
        bellArray.add(five);
        bellArray.add(four);
        bellArray.add(three);
        bellArray.add(two);
        bellArray.add(one);
        updateLives();
    }

    /**
     *
     */
    public void createActors() {
        Actor bar = addActor(internal.getEntry("bar", Texture.class), 0, STANDARD_HEIGHT-36);
        bar.setScale(0.5f);

        nine = addActor(internal.getEntry("bell", Texture.class), 26, STANDARD_HEIGHT-25);
        nine.setScale(0.5f);
        nine.setVisible(false);

        eight = addActor(internal.getEntry("bell", Texture.class), 26+36, STANDARD_HEIGHT-25);
        eight.setScale(0.5f);
        eight.setVisible(false);

        seven = addActor(internal.getEntry("bell", Texture.class), 26+36*2, STANDARD_HEIGHT-25);
        seven.setScale(0.5f);
        seven.setVisible(false);

        six = addActor(internal.getEntry("bell", Texture.class), 26+36*3, STANDARD_HEIGHT-25);
        six.setScale(0.5f);
        six.setVisible(false);

        five = addActor(internal.getEntry("bell", Texture.class), 26+36*4, STANDARD_HEIGHT-25);
        five.setScale(0.5f);
        five.setVisible(false);

        four = addActor(internal.getEntry("bell", Texture.class), 26+36*5, STANDARD_HEIGHT-25);
        four.setScale(0.5f);
        four.setVisible(false);

        three = addActor(internal.getEntry("bell", Texture.class), 26+36*6, STANDARD_HEIGHT-25);
        three.setScale(0.5f);
        three.setVisible(false);

        two = addActor(internal.getEntry("bell", Texture.class), 26+36*7, STANDARD_HEIGHT-25);
        two.setScale(0.5f);
        two.setVisible(false);

        one = addActor(internal.getEntry("bell", Texture.class), 26+36*8, STANDARD_HEIGHT-25);
        one.setScale(0.5f);
        one.setVisible(false);
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

    public Actor addActor(Texture texture, float x, float y) {
        Actor actor = new Image(texture);
        actor.setPosition(x, y);
        actor.addListener(new InputListener());
        super.addActor(actor);
        return actor;
    }

    public void updateLives() {
        for (int i = 0; i < 9; i++) {
            if (i < lives)  {
                bellArray.get(i).setVisible(true);
            } else {
                bellArray.get(i).setVisible(false);
            }
        }
    }

}
