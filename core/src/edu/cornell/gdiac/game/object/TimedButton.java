package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import edu.cornell.gdiac.game.*;
import edu.cornell.gdiac.game.obstacle.*;

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
     * @param tileSize       Tile size of the Tiled map for loading positions
     * @param levelHeight    Height of level (in grid cell units) for loading y position
     * @param textureScale   Texture scale for rescaling texture
     */
    public TimedButton(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, int tileSize, int levelHeight, Vector2 textureScale){
        super(properties, tMap, scale, tileSize, levelHeight, textureScale );
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
        active = pressedTicks > 0;
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
