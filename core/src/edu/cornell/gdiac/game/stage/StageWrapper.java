package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import edu.cornell.gdiac.assets.AssetDirectory;

abstract class StageWrapper extends Stage {

    /** Internal assets for this loading screen */
    public AssetDirectory internal;
    /** Standard window size (for scaling) */
    static int STANDARD_WIDTH  = 1024;
    /** Standard window height (for scaling) */
    static int STANDARD_HEIGHT = 576;
    /** x-coordinate for center of button list */
    public int buttonX;
    /** y-coordinate for top button */
    public int buttonY;

    public StageWrapper() {
        super(new ExtendViewport(STANDARD_WIDTH, STANDARD_HEIGHT, STANDARD_WIDTH, STANDARD_HEIGHT));
        internal = new AssetDirectory( "loading.json" );
        internal.loadAssets();
        internal.finishLoading();
        buttonX = (int)(3f/5 * STANDARD_WIDTH);
        buttonY = (int)(1f/2 * STANDARD_HEIGHT);
        createActors();
    }

    public void draw() {
        super.getViewport().apply();
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

    abstract public void createActors();

    abstract public boolean listenerTouchDown(InputEvent event, float x, float y, int pointer, int button);

    abstract public void listenerTouchUp(InputEvent event, float x, float y, int pointer, int button);

    class Listener extends InputListener {
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) { return listenerTouchDown(event,x,y,pointer,button); }
        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, int button) { listenerTouchUp(event,x,y,pointer,button); }
    }
}
