package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.game.AudioController;
import edu.cornell.gdiac.game.Save;

import java.util.HashMap;
import java.util.Map;

public class SettingsStage extends StageWrapper {
    private Table table;
    private Slider effectsSlider;
    private Slider musicSlider;
    private TextButton controls;
    private Actor backButtonActor;
    private Actor fakeBackActor;
    /** State to keep track of whether the main menu button has been clicked */
    private int backButtonState;
    private AudioController audioController;
//    key0: up - up
//    key1: down - down
//    key2: right - right
//    key3: left - left
//    key4: jump - c
//    key5: dash - x
//    key6: climb - z
//    key7: switch - shift_left
//    key8: cancel - control_left
//    key9: undo - u
//    key10: pan - tab
    private TextButton upButton;
    private TextButton downButton;
    private TextButton rightButton;
    private TextButton leftButton;
    private TextButton jumpButton;
    private TextButton dashButton;
    private TextButton climbButton;
    private TextButton switchButton;
    private TextButton cancelButton;
    private TextButton undoButton;
    private TextButton panButton;
    private TextButton setDefault;
    public static final int[] defaultControls = new int[] {
        Input.Keys.UP,
        Input.Keys.DOWN,
        Input.Keys.RIGHT,
        Input.Keys.LEFT,
        Input.Keys.C,
        Input.Keys.X,
        Input.Keys.Z,
        Input.Keys.SHIFT_LEFT,
        Input.Keys.CONTROL_LEFT,
        Input.Keys.U,
        Input.Keys.TAB
    };

    private static HashMap<TextButton, Integer> buttonToIndex = new HashMap<>();

    private int[] bindings;
    private TextButton[] buttons;
    private TextButton changeButton;
    private ControlsInputProcessor controlsInputProcessor;
    public InputMultiplexer inputMultiplexer;

    public boolean isBack() { return backButtonState == 2; }
    public int getBackButtonState() { return backButtonState; }
    public void setBackButtonState(int state) { backButtonState = state; }
    public void setAudioController(AudioController audioController) { this.audioController = audioController; }

    public SettingsStage(String internal, boolean createActors) {
        super(internal, createActors, true);
        bindings = Save.getControls();
        controlsInputProcessor = new ControlsInputProcessor();
        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(this);
        inputMultiplexer.addProcessor(controlsInputProcessor);
//        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    /**
     *
     */
    @Override
    public void createActors() {
        Actor background = addActor(internal.getEntry("bg-settings", Texture.class), 0, 0);
        background.setScale(0.5f);
        backButtonActor = addActor(internal.getEntry("back", Texture.class),32,40);
        backButtonActor.setScale(0.5f);
        backButtonActor.addListener(createHoverListener(backButtonActor));

        fakeBackActor = addActor(internal.getEntry("back", Texture.class),32,40);
        fakeBackActor.setScale(0.5f);
        fakeBackActor.addListener(createHoverListener(fakeBackActor));
        fakeBackActor.setTouchable(Touchable.disabled);
        fakeBackActor.setVisible(false);

        table = new Table();

        settings();

        addActor(table);

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
        } if (actor == fakeBackActor) {
            fakeBackActor.setColor(Color.LIGHT_GRAY);
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
        Actor actor = event.getListenerActor();
        if (backButtonState == 1) {
            backButtonState = 2;
            backButtonActor.setColor(Color.WHITE);
        } else if (actor == fakeBackActor) {
            fakeBackActor.setColor(Color.WHITE);
            Save.setControls(bindings);
            // TODO: INPUT CONTROLLER UPDATE CONTROLS
            table.clear();
            settings();
            table.pack();
        }
    }

    public void exit() {
        Save.setVolume(effectsSlider.getValue());
        Save.setMusic(musicSlider.getValue());
    }

    private void settings() {
        table.align(Align.topLeft);
        table.setFillParent(true);

        effectsSlider = new Slider(0, 1, 0.05f, false, sliderStyle);
//        effectsSlider.sizeBy(1.5f,1);
        effectsSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (audioController != null) {
                    audioController.setSfxVolume(effectsSlider.getValue());
                }
                //TODO: play sound effect on release for player to hear adjusted SFX volume
            }
        });
        effectsSlider.setValue(Save.getVolume());

        musicSlider = new Slider(0, 1, 0.05f, false, sliderStyle);
//        musicSlider.sizeBy(1,1);
//        musicSlider.scaleBy(0.75f);
        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (audioController != null) {
                    audioController.setVolume(musicSlider.getValue());
                }
            }
        });
        musicSlider.setValue(Save.getMusic());

        controls = new TextButton("controls", textButtonStyle);
        controls.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                audioController.playSoundEffect("menu-select");
                Save.setVolume(effectsSlider.getValue());
                Save.setMusic(musicSlider.getValue());
                table.clear();
//                table = null;
                controls();
                table.pack();
            }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                controls.getLabel().setColor(Color.LIGHT_GRAY);
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                controls.getLabel().setColor(Color.WHITE);
            }
        });

        Label volumeLabel = new Label("effects volume", labelStyle);
        Label playGame = new Label("music volume", labelStyle);

        table.row();
        table.add(volumeLabel).pad(20, 20, 0, 0);
        table.row();
        table.add(effectsSlider).width(350).pad(0,20,0,0);
        table.row();
        table.add(playGame).pad(0,20,0,0);
        table.row();
        table.add(musicSlider).width(350).pad(0,20,0,0);
        table.row();
        table.add(controls).pad(0,20,0,0);
        for (Cell cell: table.getCells()) {
            cell.align(Align.left);
        }
        table.columnDefaults(1).setActorWidth(400);
        table.columnDefaults(1).fillX();

        backButtonActor.setTouchable(Touchable.enabled);
        backButtonActor.setVisible(true);

        fakeBackActor.setTouchable(Touchable.disabled);
        fakeBackActor.setVisible(false);
    }

    private void controls() {
        table.align(Align.topLeft);
        table.setFillParent(true);
        table.row().pad(100, 0, 0, 0);
        Table leftTable = new Table();
        Table rightTable = new Table();

        Label up = new Label("up", controlStyle);
        Label down = new Label("down", controlStyle);
        Label right = new Label("right", controlStyle);
        Label left = new Label("left", controlStyle);
        Label jump = new Label("jump", controlStyle);
        Label dash = new Label("dash", controlStyle);
        Label climb = new Label("climb", controlStyle);
        Label switchBody = new Label("switch", controlStyle);
        Label cancel = new Label("cancel", controlStyle);
        Label undo = new Label("undo", controlStyle);
        Label pan = new Label("pan", controlStyle);

        Label[] labels = new Label[]{up, down, right, left, jump, dash, climb, switchBody, cancel, undo, pan};

        ClickListener buttonListener = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                TextButton b = (TextButton) event.getListenerActor();
//                changeButton = b;
//                changeButton.setChecked(true);
//                changeButton.getLabel().setColor(Color.LIGHT_GRAY);
                if (changeButton != null) {
                    changeButton.setChecked(false);
                }
                if (changeButton == b) {
//                    Array<Integer> bindingsArray = new Array<>();
//
//                    for (int i = 0; i < bindings.length; i++) {
//                        bindingsArray.add(Integer.valueOf(bindings[i]));
//                    }
//
//                    if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.M || bindingsArray.contains(Integer.valueOf(keycode), false)) {
//                        // TODO: PUT ERROR SOUND FOR BAD KEY BIND
//                        return true;
//                    }
//
//                    bindings[buttonToIndex.get(changeButton)] = keycode;
//                    changeButton.setText(Input.Keys.toString(keycode).toUpperCase());
                    changeButton.setChecked(false);
                    changeButton = null;
                } else {
                    changeButton = b;
//                    System.out.println(changeButton);
                    changeButton.getLabel().setColor(new Color(226/255, 149/255, 73/255, 1));
                }
            }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                TextButton b = (TextButton) event.getListenerActor();
                b.getLabel().setColor(Color.LIGHT_GRAY);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                TextButton b = (TextButton) event.getListenerActor();
                b.getLabel().setColor(Color.WHITE);
            }
        };

        upButton = new TextButton("", controlButtonStyle);
        downButton = new TextButton("", controlButtonStyle);
        rightButton = new TextButton("", controlButtonStyle);
        leftButton = new TextButton("", controlButtonStyle);
        jumpButton = new TextButton("", controlButtonStyle);
        dashButton = new TextButton("", controlButtonStyle);
        climbButton = new TextButton("", controlButtonStyle);
        switchButton = new TextButton("", controlButtonStyle);
        cancelButton = new TextButton("", controlButtonStyle);
        undoButton = new TextButton("", controlButtonStyle);
        panButton = new TextButton("", controlButtonStyle);
        setDefault = new TextButton("reset controls", controlButtonStyle);

        upButton.addListener(buttonListener);
        downButton.addListener(buttonListener);
        rightButton.addListener(buttonListener);
        leftButton.addListener(buttonListener);
        jumpButton.addListener(buttonListener);
        dashButton.addListener(buttonListener);
        climbButton.addListener(buttonListener);
        switchButton.addListener(buttonListener);
        cancelButton.addListener(buttonListener);
        undoButton.addListener(buttonListener);
        panButton.addListener(buttonListener);
        setDefault.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                audioController.playSoundEffect("menu-select");
                resetControls();
            }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                TextButton b = (TextButton) event.getListenerActor();
                b.getLabel().setColor(Color.LIGHT_GRAY);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                TextButton b = (TextButton) event.getListenerActor();
                b.getLabel().setColor(Color.WHITE);
            }
        });

        buttons = new TextButton[]{upButton, downButton, rightButton, leftButton, jumpButton, dashButton,
                climbButton, switchButton, cancelButton, undoButton, panButton};

        buttonToIndex.put(upButton, 0);
        buttonToIndex.put(downButton, 1);
        buttonToIndex.put(rightButton, 2);
        buttonToIndex.put(leftButton, 3);
        buttonToIndex.put(jumpButton, 4);
        buttonToIndex.put(dashButton, 5);
        buttonToIndex.put(climbButton, 6);
        buttonToIndex.put(switchButton, 7);
        buttonToIndex.put(cancelButton, 8);
        buttonToIndex.put(undoButton, 9);
        buttonToIndex.put(panButton, 10);

        int[] bindings = Save.getControls();

        for (int i = 0; i < bindings.length; i++) {
            buttons[i].setText(Input.Keys.toString(bindings[i]).toUpperCase());
        }

        for (int i = 0; i < bindings.length/2 + 1; i++) {
            TextButton button = buttons[i];
            leftTable.row();
            leftTable.add(labels[i]).pad(0, 20, 0, 0).align(Align.left);
            leftTable.add(button).pad(0, 15, 0, 0).align(Align.left);
        }
        for (int i = bindings.length/2 + 1; i < bindings.length; i++) {
            TextButton button = buttons[i];
            rightTable.row();
            rightTable.add(labels[i]).pad(0, 20, 0, 0).align(Align.left);
            rightTable.add(button).pad(0, 15, 0, 0).align(Align.left);
        }


        for (Cell cell: table.getCells()) {
            cell.align(Align.left);
        }
//        table.columnDefaults(1).setActorWidth(400);
//        table.columnDefaults(1).fillX();
        table.add(leftTable);
        table.add(rightTable);
        table.row();
        table.add(setDefault).pad(0, 20, 0, 0).align(Align.center);

        backButtonActor.setTouchable(Touchable.disabled);
        backButtonActor.setVisible(false);

        fakeBackActor.setTouchable(Touchable.enabled);
        fakeBackActor.setVisible(true);
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        if (changeButton != null) {
            changeButton.getLabel().setColor(new Color(226/255f, 149/255f, 73/255f, 1));
        }
    }

    private void resetControls() {
        for (int i = 0; i < defaultControls.length; i++) {
            bindings[i] = defaultControls[i];
            buttons[i].setText(Input.Keys.toString(defaultControls[i]).toUpperCase());
        }
    }

    public class ControlsInputProcessor extends InputAdapter {
        @Override
        public boolean keyUp(int keycode) {
            if (changeButton != null) {
                Array<Integer> bindingsArray = new Array<>();

                for (int i = 0; i < bindings.length; i++) {
                    bindingsArray.add(Integer.valueOf(bindings[i]));
                }

                if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.M || bindingsArray.contains(Integer.valueOf(keycode), false)) {
                    // TODO: PUT ERROR SOUND FOR BAD KEY BIND
                    return true;
                }

                bindings[buttonToIndex.get(changeButton)] = keycode;
                changeButton.setText(Input.Keys.toString(keycode).toUpperCase());
                changeButton.setChecked(false);
                changeButton = null;
//                System.out.println(bindings);
            }
            return true;
        }
    }
}
