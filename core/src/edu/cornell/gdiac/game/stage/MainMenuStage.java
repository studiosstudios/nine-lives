package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
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
    private Actor playButtonActor;
    private Actor levelSelectActor;
    private Actor settingsActor;
    private Actor exitButtonActor;
    private int playButtonState;
    private AnimationDrawable animation;
    private int levelSelectState;

    private int settingsState;
    private int exitButtonState;

    public int getPlayButtonState() { return playButtonState; }
    public void setPlayButtonState(int state) { playButtonState = state; }
    public int getLevelSelectState() { return levelSelectState; }
    public void setLevelSelectState(int state) { levelSelectState = state; }
    public int getSettingsState() { return settingsState; }
    public void setSettingsState(int state) { settingsState = state; }
    public int getExitButtonState() { return exitButtonState; }
    public void setExitButtonState(int state) { exitButtonState = state; }

    public boolean isPlay() { return playButtonState == 2; }
    public boolean isLevelSelect() { return levelSelectState == 2; }
    public boolean isSettings() { return settingsState == 2; }
    public boolean isExit() { return exitButtonState == 2; }

    public MainMenuStage(String internal, boolean createActors) {
        super(internal, createActors, false);
    }
    /**
     *
     */
    @Override
    public void createActors() {
        animations = new AssetDirectory("jsons/ui-animations.json");
        animations.loadAssets();
        animations.finishLoading();
        Animation<TextureRegion> anim = new Animation<>(0.65f, TextureRegion.split(animations.getEntry("main-menu-cat-anim", Texture.class),1024,1024)[0]);
        anim.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);
        animation = new AnimationDrawable(anim);
        Actor backgroundActor = addActor(internal.getEntry("background", Texture.class), 0,0);
        backgroundActor.setScale(0.5f);
        catActor = new AnimatedActor(animation);
        addActor(catActor);
        catActor.setScale(0.5f);
        catActor.setPosition(15,15);
        if (Save.getStarted()) {
            playButtonActor = addActor(internal.getEntry("continue-game", Texture.class),buttonX+215-19-50, buttonY);
            playButtonActor.setScale(0.5f);
        } else {
            playButtonActor = addActor(internal.getEntry("play-game", Texture.class),buttonX+215, buttonY);
            playButtonActor.setScale(0.5f);
        }
        levelSelectActor = addActor(internal.getEntry("level-select", Texture.class),buttonX+215-19, buttonY-50-10);
        levelSelectActor.setScale(0.5f);
        settingsActor = addActor(internal.getEntry("settings", Texture.class),buttonX+215+28, buttonY-100-20);
        settingsActor.setScale(0.5f);
        exitButtonActor = addActor(internal.getEntry("exit", Texture.class),buttonX+215+6.5f, buttonY-150-30);
        exitButtonActor.setScale(0.5f);

        playButtonActor.addListener(createHoverListener(playButtonActor));
        levelSelectActor.addListener(createHoverListener(levelSelectActor));
        settingsActor.addListener(createHoverListener(settingsActor));
        exitButtonActor.addListener(createHoverListener(exitButtonActor));
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        catActor.act(delta);
//        addActor(animation.getKeyFrame(time).getTexture(),15,15);
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
        if (actor == playButtonActor) {
            playButtonState = 1;
            playButtonActor.setColor(Color.LIGHT_GRAY);
        } else if (actor == levelSelectActor) {
            levelSelectState = 1;
            levelSelectActor.setColor(Color.LIGHT_GRAY);
        } else if (actor == settingsActor) {
            settingsState = 1;
            settingsActor.setColor(Color.LIGHT_GRAY);
        } else if (actor == exitButtonActor) {
            exitButtonState = 1;
            exitButtonActor.setColor(Color.LIGHT_GRAY);
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
        if (playButtonState == 1) {
            playButtonState = 2;
            playButtonActor.setColor(Color.WHITE);
        } else if (levelSelectState == 1) {
            levelSelectState = 2;
            levelSelectActor.setColor(Color.WHITE);
        } else if (settingsState == 1) {
            settingsState = 2;
            settingsActor.setColor(Color.WHITE);
        } else if (exitButtonState == 1) {
            exitButtonState = 2;
            exitButtonActor.setColor(Color.WHITE);
        }
    }
}

class AnimatedActor extends Image {
    private final AnimationDrawable drawable;

    public AnimatedActor(AnimationDrawable drawable) {
        super(drawable);
        this.drawable = drawable;
    }

    @Override
    public void act(float delta) {
        drawable.act(delta);
        super.act(delta);
    }
}

class AnimationDrawable extends BaseDrawable {
    public final Animation<TextureRegion> animation;
    private float stateTime = 0;

    public AnimationDrawable(Animation<TextureRegion> animation) {
        this.animation = animation;
        setMinWidth(animation.getKeyFrame(0).getRegionWidth());
        setMinHeight(animation.getKeyFrame(0).getRegionHeight());
    }

    public void act(float delta) {
        stateTime += delta;
    }

    public void reset() {
        stateTime = 0;
    }

    @Override
    public void draw(Batch batch, float x, float y, float width, float height) {
        batch.draw(animation.getKeyFrame(stateTime), x, y, width, height);
    }
}