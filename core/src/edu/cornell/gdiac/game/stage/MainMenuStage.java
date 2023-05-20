package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.game.Save;

public class MainMenuStage extends StageWrapper{
    private Table table;
    private Actor catActor;
    private Actor playButtonActor;
    private Actor levelSelectActor;
    private Actor settingsActor;
    private Actor exitButtonActor;
//    private Array<Actor> buttonArray;
//    private Actor playCatpawActor;
//    private Actor levelCatpawActor;
//    private Actor settingsCatpawActor;
//    private Actor exitCatpawActor;
    private int playButtonState;
    private Animation<TextureRegion> animation;
    private int levelSelectState;
    private float time;

    private int settingsState;
    private int exitButtonState;
//    private Array<Integer> stateArray;
    private int selected;

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

    public MainMenuStage(AssetDirectory internal, boolean createActors) {
        super(internal, createActors);
//        selected = 0;
//        stateArray = new Array<>();
//        stateArray.add(playButtonState);
//        stateArray.add(levelSelectState);
//        stateArray.add(settingsState);
//        stateArray.add(exitButtonState);
    }
    /**
     *
     */
    @Override
    public void createActors() {
        animation = new Animation<>(0.15f, TextureRegion.split(internal.getEntry("main-menu-cat-anim", Texture.class),2048,2048)[0]);
        animation.setPlayMode(Animation.PlayMode.LOOP);
        time = 0.0f;
        Actor backgroundActor = addActor(internal.getEntry("background", Texture.class), 0,0);
        backgroundActor.setScale(0.5f);
//        table = new Table();
//        table.setFillParent(true);
//        table.setPosition(100,100);
//        addActor(table);
//        Label playGame = new Label("play game", labelStyle);
//        Label levelSelect = new Label("level select", labelStyle);
//        Label settings = new Label("settings", labelStyle);
//        Label exitGame = new Label("exit game", labelStyle);
//        table.row();
//        table.add(playGame);
//        table.row();
//        table.add(levelSelect);
//        table.row();
//        table.add(settings);
//        table.row();
//        table.add(exitGame);
//        catActor = addActor(animation.getKeyFrame(time).getTexture(),0,0);
//        catActor.setScale(0.1f);
        addActor(internal.getEntry("main-menu-cat", Texture.class),15,15);
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

//        buttonArray = new Array<>();
//        playCatpawActor = addActor(internal.getEntry("paw", Texture.class), buttonX+30, buttonY+25);
//        playCatpawActor.setScale(0.5f);
//        playCatpawActor.setVisible(false);
//        levelCatpawActor = addActor(internal.getEntry("paw", Texture.class), buttonX+30, buttonY-25-4);
//        levelCatpawActor.setScale(0.5f);
//        levelCatpawActor.setVisible(false);
//        settingsCatpawActor = addActor(internal.getEntry("paw", Texture.class), buttonX+30, buttonY-75-12);
//        settingsCatpawActor.setScale(0.5f);
//        settingsCatpawActor.setVisible(false);
//        exitCatpawActor = addActor(internal.getEntry("paw", Texture.class), buttonX+30, buttonY-75-16-50);
//        exitCatpawActor.setScale(0.5f);
//        exitCatpawActor.setVisible(false);

//        buttonArray.add(playButtonActor);
//        buttonArray.add(levelSelectActor);
//        buttonArray.add(settingsActor);
//        buttonArray.add(exitButtonActor);
//
//        playButtonActor.addListener(createCatpawListener(playButtonActor, playCatpawActor));
//        levelSelectActor.addListener(createCatpawListener(levelSelectActor,levelCatpawActor));
//        settingsActor.addListener(createCatpawListener(settingsActor,settingsCatpawActor));
//        exitButtonActor.addListener(createCatpawListener(exitButtonActor,exitCatpawActor));
        playButtonActor.addListener(createHoverListener(playButtonActor));
        levelSelectActor.addListener(createHoverListener(levelSelectActor));
        settingsActor.addListener(createHoverListener(settingsActor));
        exitButtonActor.addListener(createHoverListener(exitButtonActor));
    }

//    @Override
//    public void update(float delta) {
//        super.update(delta);
//        float d = Gdx.graphics.getDeltaTime();
//        time += d;
//        addActor(animation.getKeyFrame(time).getTexture(),15,15);
//    }

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
//            selected = 0;
            playButtonState = 1;
            playButtonActor.setColor(Color.LIGHT_GRAY);
        } else if (actor == levelSelectActor) {
//            selected = 1;
            levelSelectState = 1;
            levelSelectActor.setColor(Color.LIGHT_GRAY);
        } else if (actor == settingsActor) {
//            selected = 2;
            settingsState = 1;
            settingsActor.setColor(Color.LIGHT_GRAY);
        } else if (actor == exitButtonActor) {
//            selected = 3;
            exitButtonState = 1;
            exitButtonActor.setColor(Color.LIGHT_GRAY);
        }
//        updateSelected(selected);
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

//    @Override
//    public boolean keyDown(int keyCode) {
//        if (keyCode == Input.Keys.UP) {
//            if (selected <= 3 && selected > 0) {
//                selected--;
//            }
//        } else if (keyCode == Input.Keys.DOWN) {
//            if (selected >= 0 && selected < 3) {
//                selected++;
//            }
//        }
//        updateSelected(selected);
//        return super.keyDown(keyCode);
//    }

//    public void updateSelected(int index) {
//        for (int i = 0; i > 4; i++) {
//            stateArray.set(i, 0);
//            buttonArray.get(i).setColor(Color.WHITE);
//        }
//        stateArray.set(index, 1);
//        buttonArray.get(index).setColor(Color.LIGHT_GRAY);
//    }
}
