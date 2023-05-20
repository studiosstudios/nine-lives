package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
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
    static int numLevels;
    public int buttonX;
    public int xHalf;
    public int yHalf;

    public int buttonY;
    static BitmapFont font;
    static BitmapFont controlFont;
    static Label.LabelStyle labelStyle;
    static Label.LabelStyle controlStyle;
    static TextButton.TextButtonStyle textButtonStyle;
    static TextButton.TextButtonStyle controlButtonStyle;
    static Slider.SliderStyle sliderStyle;

    public StageWrapper(String directory, boolean createActors, boolean settings) {
        super(new ExtendViewport(STANDARD_WIDTH, STANDARD_HEIGHT, STANDARD_WIDTH, STANDARD_HEIGHT));
        internal = new AssetDirectory(directory);
        internal.loadAssets();
        internal.finishLoading();
        if (settings) {
            FreeTypeFontGenerator.setMaxTextureSize(2048);
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("shared/preahvihear.ttf"));
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 32; //24
            parameter.genMipMaps = true;
            parameter.minFilter = Texture.TextureFilter.Linear;
            parameter.magFilter = Texture.TextureFilter.Linear;
            font = gen.generateFont(parameter);
            labelStyle = new Label.LabelStyle(font, Color.WHITE);
            textButtonStyle = new TextButton.TextButtonStyle(null,null,null, font);
            parameter.size = 24;
            controlFont = gen.generateFont(parameter);
            gen.dispose();
            controlStyle = new Label.LabelStyle(controlFont, Color.WHITE);
            controlButtonStyle = new TextButton.TextButtonStyle(null,null,null, controlFont);

            AssetDirectory tableAssets = new AssetDirectory("jsons/table.json");
            tableAssets.loadAssets();
            tableAssets.finishLoading();

            Texture knob = tableAssets.getEntry("paw", Texture.class);
            Texture slider = tableAssets.getEntry("slider-empty", Texture.class);
            Texture before = tableAssets.getEntry("slider-full", Texture.class);
            TextureRegionDrawable sliderTexture = new TextureRegionDrawable(new TextureRegion(slider));
            TextureRegionDrawable sliderKnobTexture = new TextureRegionDrawable(new TextureRegion(knob));
            TextureRegionDrawable sliderTextureBefore = new TextureRegionDrawable(new TextureRegion(before));
            sliderStyle = new Slider.SliderStyle(sliderTexture, sliderKnobTexture);
            sliderStyle.knobBefore = sliderTextureBefore;
        }

        buttonX = (int)(3f/5 * STANDARD_WIDTH);
        buttonY = (int)(1f/2 * STANDARD_HEIGHT);
        xHalf = (int) (1f/2 * STANDARD_WIDTH);
        yHalf = (int) (1f/2 * STANDARD_HEIGHT);
        if (createActors) {
            createActors();
        }
    }
    public StageWrapper(String directory, boolean createActors, boolean settings, int numLevels) {
        super(new ExtendViewport(STANDARD_WIDTH, STANDARD_HEIGHT, STANDARD_WIDTH, STANDARD_HEIGHT));
        this.numLevels = numLevels;
        internal = new AssetDirectory(directory);
        internal.loadAssets();
        internal.finishLoading();
        if (settings) {
            FreeTypeFontGenerator.setMaxTextureSize(2048);
            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("shared/preahvihear.ttf"));
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 32; //24
            parameter.genMipMaps = true;
            parameter.minFilter = Texture.TextureFilter.Linear;
            parameter.magFilter = Texture.TextureFilter.Linear;
            font = gen.generateFont(parameter);
            labelStyle = new Label.LabelStyle(font, Color.WHITE);
            textButtonStyle = new TextButton.TextButtonStyle(null,null,null, font);
            parameter.size = 24;
            controlFont = gen.generateFont(parameter);
            gen.dispose();
            controlStyle = new Label.LabelStyle(controlFont, Color.WHITE);
            controlButtonStyle = new TextButton.TextButtonStyle(null,null,null, controlFont);

            AssetDirectory tableAssets = new AssetDirectory("jsons/table.json");
            tableAssets.loadAssets();
            tableAssets.finishLoading();

            Texture knob = tableAssets.getEntry("paw", Texture.class);
            Texture slider = tableAssets.getEntry("slider-empty", Texture.class);
            Texture before = tableAssets.getEntry("slider-full", Texture.class);
            TextureRegionDrawable sliderTexture = new TextureRegionDrawable(new TextureRegion(slider));
            TextureRegionDrawable sliderKnobTexture = new TextureRegionDrawable(new TextureRegion(knob));
            TextureRegionDrawable sliderTextureBefore = new TextureRegionDrawable(new TextureRegion(before));
            sliderStyle = new Slider.SliderStyle(sliderTexture, sliderKnobTexture);
            sliderStyle.knobBefore = sliderTextureBefore;
        }

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
        super.act();
        super.draw();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        update(delta);
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

        public boolean isAnimationFinished() { return drawable.isAnimationFinished(); }
    }

    class AnimationDrawable extends BaseDrawable {
        public final Animation<TextureRegion> animation;
        private float stateTime = 0;

        public AnimationDrawable(Animation<TextureRegion> animation) {
            this.animation = animation;
            setMinWidth(animation.getKeyFrame(0).getRegionWidth());
            setMinHeight(animation.getKeyFrame(0).getRegionHeight());
        }

        public boolean isAnimationFinished() { return  animation.isAnimationFinished(stateTime); }

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
}
