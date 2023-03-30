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
 * An activator that stays active for a defined period of time after release.
 */
public class TimedButton extends Activator {
    /** The number of remaining ticks until the timed button deactivates. */
    private int pressedTicks;
    /** The total number of ticks a timed button stays active for after release. */
    private int totalDurationTicks;

    /**
     * Creates a new timed button object.
     * @param texture   Animation filmstrip.
     * @param texture2  Static texture.
     * @param scale     Draw scale for drawing.
     * @param data      JSON for loading.
     */
    public TimedButton(TextureRegion texture,TextureRegion texture2, Vector2 scale, JsonValue data){
        super(texture, texture2, scale, data);
        totalDurationTicks = data.getInt("duration");
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

}
