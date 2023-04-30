package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.game.Save;

public class SettingsStage extends StageWrapper {
    private Table table;
    private TextureRegionDrawable sliderTexture;
    private TextureRegionDrawable sliderKnobTexture;
    private TextureRegionDrawable sliderTextureBefore;
    private Slider volumeSlider;
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
        table = new Table();
        BitmapFont font = internal.getEntry("chewy", BitmapFont.class);
        Texture knob = internal.getEntry("catpaw", Texture.class);
        Texture slider = internal.getEntry("progressbar", Texture.class);
        Texture before = internal.getEntry("progressbarbefore", Texture.class);
        sliderTexture = new TextureRegionDrawable(new TextureRegion(slider));
        sliderKnobTexture = new TextureRegionDrawable(new TextureRegion(knob));
        sliderTextureBefore = new TextureRegionDrawable(new TextureRegion(before));
//        addActor(new Image(internal.getEntry("settingsBackground", Texture.class)));
        Actor background = addActor(internal.getEntry("settingsBackground", Texture.class), 0, 0);
        background.setScale(0.5f);
        backButtonActor = addActor(internal.getEntry("back", Texture.class),32,buttonY-225);
        backButtonActor.setScale(0.5f);

        backCatpawActor = addActor(internal.getEntry("catpaw", Texture.class), 332, buttonY-225);
        backCatpawActor.setScale(0.5f);
        backCatpawActor.setVisible(false);

        backButtonActor.addListener(createCatpawListener(backButtonActor, backCatpawActor));

//        catpaw1Actor = addActor(internal.getEntry("catpaw", Texture.class), 332, 370);
//        catpaw1Actor.setScale(0.5f);
//        catpaw2Actor = addActor(internal.getEntry("catpaw", Texture.class), 332, 370-60);
//        catpaw2Actor.setScale(0.5f);

        table.align(Align.topLeft);
        table.setFillParent(true);
        addActor(table);

        Slider.SliderStyle sliderStyle = new Slider.SliderStyle(sliderTexture, sliderKnobTexture);
        sliderStyle.knobBefore = sliderTextureBefore;

        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);

        volumeSlider = new Slider(0, 1, 0.05f, false, sliderStyle);
        volumeSlider.sizeBy(1.5f,1);
        volumeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {

            }
        });
        volumeSlider.setValue(Save.getVolume());

//        volumeSlider.setPosition(100, 100);
//        volumeSlider.setWidth(460);
//        volumeSlider.setHeight(50);

        Label volumeLabel = new Label("Volume", labelStyle);

        table.row();
        table.add(volumeLabel).pad(50, 20, 10, 0);
        table.row();
        table.add(volumeSlider).width(350).pad(0,20,35,0);

        for (Cell cell: table.getCells()) {
            cell.align(Align.left);
        }

        table.columnDefaults(1).setActorWidth(400);
        table.columnDefaults(1).fillX();
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

    public void exit() {
        Save.setVolume(volumeSlider.getValue());
    }
}
