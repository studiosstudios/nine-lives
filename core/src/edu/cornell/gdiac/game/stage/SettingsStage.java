package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.Input;
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
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.game.AudioController;
import edu.cornell.gdiac.game.Save;

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

    public boolean isBack() { return backButtonState == 2; }
    public int getBackButtonState() { return backButtonState; }
    public void setBackButtonState(int state) { backButtonState = state; }
    public void setAudioController(AudioController audioController) { this.audioController = audioController; }

    public SettingsStage(AssetDirectory internal, boolean createActors) {
        super(internal, createActors);
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

        TextButton[] buttons = new TextButton[]{upButton, downButton, rightButton, leftButton, jumpButton, dashButton,
                climbButton, switchButton, cancelButton, undoButton, panButton};

        int[] bindings = Save.getControls();

        for (int i = 0; i < bindings.length; i++) {
            buttons[i].setText(Input.Keys.toString(bindings[i]).toUpperCase());
        }

        for (int i = 0; i < bindings.length; i++) {
            TextButton button = buttons[i];
            table.row();
            table.add(labels[i]).pad(0, 175, 0, 0);
            table.add(button).pad(0, 15, 0, 0);
        }

        for (Cell cell: table.getCells()) {
            cell.align(Align.left);
        }
        table.columnDefaults(1).setActorWidth(400);
        table.columnDefaults(1).fillX();

        backButtonActor.setTouchable(Touchable.disabled);
        backButtonActor.setVisible(false);

        fakeBackActor.setTouchable(Touchable.enabled);
        fakeBackActor.setVisible(true);
    }

    private void resetControls() {

    }
}
