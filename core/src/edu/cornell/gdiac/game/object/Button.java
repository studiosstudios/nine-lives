package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.*;
import edu.cornell.gdiac.game.obstacle.*;

/**
 * The simplest activator, active when pressed and inactive otherwise.
 */
public class Button extends Activator {

    /**
     * Creates a new button object.
     * @param texture   Animation filmstrip.
     * @param texture2  Static texture.
     * @param scale     Draw scale for drawing.
     * @param data      JSON for loading.
     */
    public Button(TextureRegion texture, TextureRegion texture2, Vector2 scale, JsonValue data){
        super(texture, texture2, scale, data);
        setName("button");
    }

    /** For a button, active = isPressed() */
    public void updateActivated(){
        active = isPressed();
    }

}
