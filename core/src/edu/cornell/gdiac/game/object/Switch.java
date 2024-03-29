package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import com.badlogic.gdx.utils.ObjectMap;

import java.util.HashMap;

/**
 * An activator that toggles activation on press.
 */
public class Switch extends Activator {
    /** True if pressed in previous frame */
    private boolean prevPressed;

    /**
     * Creates a new switch object.
     *
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param textureScale   Texture scale for rescaling texture
     */
    public Switch(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, Vector2 textureScale, String biome){
        super(properties, biome.equals("metal") ? "switch-top" : "forest-switch-top", "switch-base",tMap, scale, textureScale, biome, false);
        prevPressed = false;
        setName("switch");
    }

    /** For a switch, active is toggled every time button is pressed */
    public void updateActivated(){
        if (isPressed() && !prevPressed) {
            activating = !activating;
        }
        prevPressed = isPressed();
    }

    public ObjectMap<String, Object> storeState(){
        ObjectMap<String, Object> stateMap = super.storeState();
        stateMap.put("prevPressed", prevPressed);
        return stateMap;
    }

    public void loadState(ObjectMap<String, Object> stateMap){
        super.loadState(stateMap);
        prevPressed = (boolean) stateMap.get("prevPressed");
    }

}
