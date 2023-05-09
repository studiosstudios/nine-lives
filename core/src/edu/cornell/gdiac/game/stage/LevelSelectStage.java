package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import edu.cornell.gdiac.assets.AssetDirectory;

import java.util.ArrayList;
import java.util.List;

public class LevelSelectStage extends StageWrapper {
    private Actor selectedActor;
    private Actor backButtonActor;
    private Actor playButtonActor;
//    private Actor oneActor;
//    private Actor twoActor;
//    private List<Actor> actorList = new ArrayList<>(2);
//    actorList.add(oneActor);
    /** State to keep track of whether the main menu button has been clicked */
    private int backButtonState;
    private int playButtonState;
//    private int oneState;
//    private int twoState;

    public boolean isBack() { return backButtonState == 2; }
    public int getBackButtonState() { return backButtonState; }
    public void setBackButtonState(int state) { backButtonState = state; }
    public boolean isPlay() { return playButtonState == 2; }
    public int getPlayButtonState() { return playButtonState; }
    public void setPlayButtonState(int state) { playButtonState = state; }
    public LevelSelectStage(AssetDirectory internal, boolean createActors) {
        super(internal, createActors);
    }
//    public int getSelectedLevel() {
//        int l = (oneState == 1 && twoState == 0) ? 1 : 2;
//        return l;
//    }

    /**
     *
     */
    @Override
    public void createActors() {
//        addActor(new Image(internal.getEntry("bg-lab", Texture.class)));
        Actor background = addActor(internal.getEntry("bg-level-select", Texture.class), 0, 0);
        background.setScale(0.5f);
        backButtonActor = addActor(internal.getEntry("back", Texture.class),940,buttonY-225);
        backButtonActor.setScale(0.5f);
        playButtonActor = addActor(internal.getEntry("play", Texture.class), 940, buttonY-175);
        playButtonActor.setScale(0.5f);

        backButtonActor.addListener(createHoverListener(backButtonActor));
        playButtonActor.addListener(createHoverListener(playButtonActor));

//        oneActor = addActor(internal.getEntry("one", Texture.class), xHalf-(18.5f*18)-5f, yHalf+(29*3));
//        twoActor = addActor(internal.getEntry("two", Texture.class), xHalf-(22.5f*11), yHalf+(29*3));
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
        } else if (actor == playButtonActor) {
            playButtonState = 1;
            playButtonActor.setColor(Color.LIGHT_GRAY);
        }
//        else if (actor == oneActor) {
//            oneState = 1;
//            oneActor.setColor(Color.LIGHT_GRAY);
//            twoState = 0;
//            twoActor.setColor(Color.WHITE);
//        } else if (actor == twoActor) {
//            twoState = 1;
//            twoActor.setColor(Color.LIGHT_GRAY);
//            oneState = 0;
//            oneActor.setColor(Color.WHITE);
//        }
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
        } else if (playButtonState == 1) {
            playButtonState = 2;
            playButtonActor.setColor(Color.WHITE);
        }
    }
}
