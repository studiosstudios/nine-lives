package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import edu.cornell.gdiac.assets.AssetDirectory;
import org.w3c.dom.Text;

public abstract class StageWrapper extends Stage {

    /** Internal assets for this loading screen */
    protected AssetDirectory internal;
    /** Standard window size (for scaling) */
    static int STANDARD_WIDTH  = 1024;
    /** Standard window height (for scaling) */
    static int STANDARD_HEIGHT = 576;
    /** x-coordinate for center of button list */
    public int buttonX;
    public int xHalf;
    public int yHalf;
    /** y-coordinate for top button */

    public int buttonY;
    static BitmapFont font;
    static BitmapFont controlFont;
    static Label.LabelStyle labelStyle;
    static Label.LabelStyle controlStyle;
    static TextButton.TextButtonStyle textButtonStyle;
    static TextButton.TextButtonStyle controlButtonStyle;
    static Slider.SliderStyle sliderStyle;

    public StageWrapper(AssetDirectory internal, boolean createActors) {
        super(new ExtendViewport(STANDARD_WIDTH, STANDARD_HEIGHT, STANDARD_WIDTH, STANDARD_HEIGHT));
        this.internal = internal;
        FreeTypeFontGenerator.setMaxTextureSize(2048);
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("shared/preahvihear.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 32; //24
        parameter.genMipMaps = true;
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        font = gen.generateFont(parameter);
//        font = internal.getEntry("chewy", BitmapFont.class);
//        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        labelStyle = new Label.LabelStyle(font, Color.WHITE);
        textButtonStyle = new TextButton.TextButtonStyle(null,null,null, font);
        parameter.size = 24;
        controlFont = gen.generateFont(parameter);
        gen.dispose();
        controlStyle = new Label.LabelStyle(controlFont, Color.WHITE);
        controlButtonStyle = new TextButton.TextButtonStyle(null,null,null, controlFont);

        Texture knob = internal.getEntry("paw", Texture.class);
        Texture slider = internal.getEntry("slider-empty", Texture.class);
        Texture before = internal.getEntry("slider-full", Texture.class);
        TextureRegionDrawable sliderTexture = new TextureRegionDrawable(new TextureRegion(slider));
        TextureRegionDrawable sliderKnobTexture = new TextureRegionDrawable(new TextureRegion(knob));
        TextureRegionDrawable sliderTextureBefore = new TextureRegionDrawable(new TextureRegion(before));
        sliderStyle = new Slider.SliderStyle(sliderTexture, sliderKnobTexture);
        sliderStyle.knobBefore = sliderTextureBefore;

        buttonX = (int)(3f/5 * STANDARD_WIDTH);
        buttonY = (int)(1f/2 * STANDARD_HEIGHT);
        xHalf = (int) (1f/2 * STANDARD_WIDTH);
        yHalf = (int) (1f/2 * STANDARD_HEIGHT);
        if (createActors) {
            createActors();
        }
    }
    public void draw() {
        super.getViewport().apply();
//        update();
        super.act();
        super.draw();
    }

    public Actor addActor(Texture texture, float x, float y) {
        Actor actor = createActor(texture, x, y);
        super.addActor(actor);
        return actor;
    }
    public Actor createActor(Texture texture, float x, float y) {
        Actor actor = new Image(texture);
        actor.setPosition(x, y);
        actor.addListener(new Listener());
        return actor;
    }

    public void update(float delta) {}

    abstract public void createActors();

    abstract public boolean listenerTouchDown(InputEvent event, float x, float y, int pointer, int button);

    abstract public void listenerTouchUp(InputEvent event, float x, float y, int pointer, int button);

    class Listener extends InputListener {
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) { return listenerTouchDown(event,x,y,pointer,button); }
        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, int button) { listenerTouchUp(event,x,y,pointer,button); }
    }

    public InputListener createCatpawListener(Actor actor, Actor catpaw) {
        return new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                actor.setColor(Color.LIGHT_GRAY);
                catpaw.setColor(Color.LIGHT_GRAY);
                catpaw.setVisible(true);
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                actor.setColor(Color.WHITE);
                catpaw.setColor(Color.WHITE);
                catpaw.setVisible(false);
            }
        };
    }

    public InputListener createHoverListener(Actor actor) {
        return new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                actor.setColor(Color.LIGHT_GRAY);
            }
            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                actor.setColor(Color.WHITE);
            }
        };
    }
}
