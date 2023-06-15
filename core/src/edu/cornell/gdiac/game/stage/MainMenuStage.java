package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.game.Save;

public class MainMenuStage extends StageWrapper{
    protected AssetDirectory animations;
    private AnimatedActor catActor;
    private UIButton playButtonActor;
    private UIButton levelSelectActor;
    private UIButton settingsActor;
    private UIButton exitButtonActor;
    private UIButton[] buttons;
    private AnimationDrawable animation;
    public boolean isPlay() { return playButtonActor.isClicked(); }
    public boolean isLevelSelect() { return levelSelectActor.isClicked(); }
    public boolean isSettings() { return settingsActor.isClicked(); }
    public boolean isExit() { return exitButtonActor.isClicked(); }

    public MainMenuStage(String internal, boolean createActors) {
        super(internal, createActors, false);
    }

    @Override
    public void createActors() {
        animations = new AssetDirectory("jsons/ui-animations.json");
        animations.loadAssets();
        animations.finishLoading();
        Animation<TextureRegion> anim = new Animation<>(0.75f, TextureRegion.split(animations.getEntry("main-menu-cat-anim", Texture.class),1024,1024)[0]);
        anim.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);
        animation = new AnimationDrawable(anim);
        Actor backgroundActor = addActor(internal.getEntry("background", Texture.class), 0,0);
        backgroundActor.setScale(0.5f);
        catActor = new AnimatedActor(animation);
        addActor(catActor);
        catActor.setScale(0.5f);
        catActor.setPosition(15,15);
        if (!Save.getStarted()) {
            playButtonActor = new UIButton(internal.getEntry("continue-game", Texture.class),buttonX+215-19-50-111, buttonY-27, 0.5f, 0.62f, 0.57f);
        } else {
            playButtonActor = new UIButton(internal.getEntry("play-game", Texture.class),buttonX+215-77, buttonY-27, 0.5f, 0.62f, 0.57f);
        }
        levelSelectActor = new UIButton(internal.getEntry("level-select", Texture.class),buttonX+215-19-86, buttonY-50-10-27, 0.5f, 0.62f, 0.57f);
        settingsActor = new UIButton(internal.getEntry("settings", Texture.class), buttonX+215+28-62, buttonY-100-20-27, 0.5f, 0.62f, 0.57f);
        exitButtonActor = new UIButton(internal.getEntry("exit", Texture.class),buttonX+215+6.5f-73, buttonY-150-30-27, 0.5f, 0.62f, 0.57f);

        buttons = new UIButton[]{playButtonActor, levelSelectActor, settingsActor, exitButtonActor};
        for (UIButton b : buttons) {
            addButton(b);
        }
    }

    public void reset() {
        for (UIButton b : buttons) {
            b.reset();
        }
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        for (UIButton b : buttons){
            b.update(delta);
        }
        catActor.act(delta);
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
        for (UIButton b : buttons) {
            if (actor == b) {
                b.touchDown();
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
        for (UIButton b : buttons) {
            if (b.getState() == UIButton.State.CLICKED) {
                b.touchUp();
            }
        }
    }
}