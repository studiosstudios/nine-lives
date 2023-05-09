package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.assets.AssetDirectory;

public class HudStage extends StageWrapper {
    public int lives = 9;

    private float bellHeight;

    Actor bell9;
    Actor bell8;
    Actor bell7;
    Actor bell6;
    Actor bell5;
    Actor bell4;
    Actor bell3;
    Actor bell2;
    Actor bell1;

    Actor cracked9;
    Actor cracked8;
    Actor cracked7;
    Actor cracked6;
    Actor cracked5;
    Actor cracked4;
    Actor cracked3;
    Actor cracked2;
    Actor cracked1;


    Array<Actor> bellArray = new Array<>();
    Array<Actor> crackedArray = new Array<>();

    public HudStage(AssetDirectory internal, boolean createActors) {
        super(internal, createActors);
        bellArray.add(bell9);
        bellArray.add(bell8);
        bellArray.add(bell7);
        bellArray.add(bell6);
        bellArray.add(bell5);
        bellArray.add(bell4);
        bellArray.add(bell3);
        bellArray.add(bell2);
        bellArray.add(bell1);
        crackedArray.add(cracked9);
        crackedArray.add(cracked8);
        crackedArray.add(cracked7);
        crackedArray.add(cracked6);
        crackedArray.add(cracked5);
        crackedArray.add(cracked4);
        crackedArray.add(cracked3);
        crackedArray.add(cracked2);
        crackedArray.add(cracked1);
        updateLives();
    }

    /**
     *
     */
    public void createActors() {
        bellHeight = STANDARD_HEIGHT-26;
        Actor bar = addActor(internal.getEntry("bar", Texture.class), 5, STANDARD_HEIGHT-36);
        bar.setScale(0.5f);

        bell9 = addActor(internal.getEntry("bell", Texture.class), 26, bellHeight);
        bell9.setScale(0.5f);
        bell9.setVisible(false);

        bell8 = addActor(internal.getEntry("bell", Texture.class), 26+36, bellHeight);
        bell8.setScale(0.5f);
        bell8.setVisible(false);

        bell7 = addActor(internal.getEntry("bell", Texture.class), 26+36*2, bellHeight);
        bell7.setScale(0.5f);
        bell7.setVisible(false);

        bell6 = addActor(internal.getEntry("bell", Texture.class), 26+36*3, bellHeight);
        bell6.setScale(0.5f);
        bell6.setVisible(false);

        bell5 = addActor(internal.getEntry("bell", Texture.class), 26+36*4, bellHeight);
        bell5.setScale(0.5f);
        bell5.setVisible(false);

        bell4 = addActor(internal.getEntry("bell", Texture.class), 26+36*5, bellHeight);
        bell4.setScale(0.5f);
        bell4.setVisible(false);

        bell3 = addActor(internal.getEntry("bell", Texture.class), 26+36*6, bellHeight);
        bell3.setScale(0.5f);
        bell3.setVisible(false);

        bell2 = addActor(internal.getEntry("bell", Texture.class), 26+36*7, bellHeight);
        bell2.setScale(0.5f);
        bell2.setVisible(false);

        bell1 = addActor(internal.getEntry("bell", Texture.class), 26+36*8, bellHeight);
        bell1.setScale(0.5f);
        bell1.setVisible(false);

        cracked9 = addActor(internal.getEntry("cracked-bell", Texture.class), 26, bellHeight);
        cracked9.setScale(0.5f);
        cracked9.setVisible(false);

        cracked8 = addActor(internal.getEntry("cracked-bell", Texture.class), 26+36, bellHeight);
        cracked8.setScale(0.5f);
        cracked8.setVisible(false);

        cracked7 = addActor(internal.getEntry("cracked-bell", Texture.class), 26+36*2, bellHeight);
        cracked7.setScale(0.5f);
        cracked7.setVisible(false);

        cracked6 = addActor(internal.getEntry("cracked-bell", Texture.class), 26+36*3, bellHeight);
        cracked6.setScale(0.5f);
        cracked6.setVisible(false);

        cracked5 = addActor(internal.getEntry("cracked-bell", Texture.class), 26+36*4, bellHeight);
        cracked5.setScale(0.5f);
        cracked5.setVisible(false);

        cracked4 = addActor(internal.getEntry("cracked-bell", Texture.class), 26+36*5, bellHeight);
        cracked4.setScale(0.5f);
        cracked4.setVisible(false);

        cracked3 = addActor(internal.getEntry("cracked-bell", Texture.class), 26+36*6, bellHeight);
        cracked3.setScale(0.5f);
        cracked3.setVisible(false);

        cracked2 = addActor(internal.getEntry("cracked-bell", Texture.class), 26+36*7, bellHeight);
        cracked2.setScale(0.5f);
        cracked2.setVisible(false);

        cracked1 = addActor(internal.getEntry("cracked-bell", Texture.class), 26+36*8, bellHeight);
        cracked1.setScale(0.5f);
        cracked1.setVisible(false);

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
                crackedArray.get(i).setVisible(false);
            } else {
                crackedArray.get(i).setVisible(true);
                bellArray.get(i).setVisible(false);
            }
        }
    }

}
