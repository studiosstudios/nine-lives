package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import edu.cornell.gdiac.assets.AssetDirectory;

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
    static Label.LabelStyle labelStyle;

    public StageWrapper(AssetDirectory internal, boolean createActors) {
        super(new ExtendViewport(STANDARD_WIDTH, STANDARD_HEIGHT, STANDARD_WIDTH, STANDARD_HEIGHT));
        this.internal = internal;
        FreeTypeFontGenerator.setMaxTextureSize(2048);
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("shared/preahvihear.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 40; //24
        parameter.genMipMaps = true;
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        font = gen.generateFont(parameter);
        gen.dispose();
//        font = internal.getEntry("chewy", BitmapFont.class);
//        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        labelStyle = new Label.LabelStyle(font, Color.WHITE);
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
        Actor actor = new Image(texture);
        actor.setPosition(x, y);
        actor.addListener(new Listener());
        super.addActor(actor);
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
