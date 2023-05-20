package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import com.badlogic.gdx.utils.ObjectMap;

import java.util.HashMap;

/**
 * An activator that stays active for a defined period of time after release.
 */
public class TimedButton extends Activator {
    /** The number of remaining ticks until the timed button deactivates. */
    private int pressedTicks;
    /** The total number of ticks a timed button stays active for after release. */
    private int totalDurationTicks;


    /**
     * Creates a new TimedButton object.
     *
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param textureScale   Texture scale for rescaling texture
     */
    public TimedButton(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, Vector2 textureScale, String biome){
        super(properties, "button-top","button-base",tMap, scale, textureScale, biome);
        totalDurationTicks = (int) properties.get("duration");
        pressedTicks = 0;
        setName("timedButton");
    }

    /** For a timed button, stays active for a set period of ticks after release */
    public void updateActivated(){
        if (isPressed()) {
            pressedTicks = totalDurationTicks;
        } else {
            pressedTicks = Math.max(0, pressedTicks - 1);
        }
        activating = pressedTicks > 0;
    }

    public ObjectMap<String, Object> storeState(){
        ObjectMap<String, Object> stateMap = super.storeState();
        stateMap.put("pressedTicks", pressedTicks);
        return stateMap;
    }

    public void loadState(ObjectMap<String, Object> stateMap){
        super.loadState(stateMap);
        pressedTicks = (int) stateMap.get("pressedTicks");
    }

}
