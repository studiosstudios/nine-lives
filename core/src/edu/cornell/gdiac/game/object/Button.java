package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import com.badlogic.gdx.utils.ObjectMap;

import java.util.HashMap;

/**
 * The simplest activator, active when pressed and inactive otherwise.
 */
public class Button extends Activator {

    /**
     * Creates a new Button object.
     *
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param textureScale   Texture scale for rescaling texture
     */
    public Button(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, Vector2 textureScale){
        super(properties, "button-top", "button-base",tMap,scale, textureScale);
    }

    /** For a button, active = isPressed() */
    public void updateActivated(){
        activating = isPressed();
    }

}
