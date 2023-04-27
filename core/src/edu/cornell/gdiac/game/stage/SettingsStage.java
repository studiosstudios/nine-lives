package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import edu.cornell.gdiac.assets.AssetDirectory;

public class SettingsStage extends StageWrapper {
    private Actor backButtonActor;
    private Actor backCatpawActor;
    private Actor catpaw1Actor;
    private Actor catpaw2Actor;
    /** State to keep track of whether the main menu button has been clicked */
    private int backButtonState;
    private boolean catpaw1State;
    private boolean catpaw2State;
//    private CheckBox vSyncCheckbox = new CheckBox(null, new Skin());

    public boolean isBack() { return backButtonState == 2; }
    public int getBackButtonState() { return backButtonState; }
    public void setBackButtonState(int state) { backButtonState = state; }

    public SettingsStage(AssetDirectory internal, boolean createActors) {
        super(internal, createActors);
    }

    /**
     *
     */
    @Override
    public void createActors() {
//        addActor(new Image(internal.getEntry("settingsBackground", Texture.class)));
        Actor background = addActor(internal.getEntry("settingsBackground", Texture.class), 0, 0);
        background.setScale(0.5f);
        backButtonActor = addActor(internal.getEntry("back", Texture.class),32,buttonY-225);
        backButtonActor.setScale(0.5f);

        backCatpawActor = addActor(internal.getEntry("catpaw", Texture.class), 332, buttonY-225);
        backCatpawActor.setScale(0.5f);
        backCatpawActor.setVisible(false);

        backButtonActor.addListener(createCatpawListener(backButtonActor, backCatpawActor));

        catpaw1Actor = addActor(internal.getEntry("catpaw", Texture.class), 332, 370);
        catpaw1Actor.setScale(0.5f);
        catpaw2Actor = addActor(internal.getEntry("catpaw", Texture.class), 332, 370-60);
        catpaw2Actor.setScale(0.5f);
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
        Actor actor = event.getListenerActor();
        if (actor == backButtonActor) {
            backButtonState = 1;
            backButtonActor.setColor(Color.LIGHT_GRAY);
        } else if (actor == catpaw1Actor) {
            catpaw1State = !catpaw1State;
            if (catpaw1State) {
                catpaw1Actor.setColor(113/255f, 67/255f, 24/255f, 1f);
            } else {
                catpaw1Actor.setColor(Color.WHITE);
            }
        } else if (actor == catpaw2Actor) {
            catpaw2State = !catpaw2State;
            if (catpaw2State) {
                catpaw2Actor.setColor(113/255f, 67/255f, 24/255f, 1f);
            } else {
                catpaw2Actor.setColor(Color.WHITE);
            }
        }
        return true;
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
        if (backButtonState == 1) {
            backButtonState = 2;
            backButtonActor.setColor(Color.WHITE);
        }
    }
}
