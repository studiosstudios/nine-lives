package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.*;
import edu.cornell.gdiac.game.obstacle.*;

public class TimedButton extends Activator {

    private int pressedTicks;
    private int totalDurationTicks;
    public TimedButton(TextureRegion texture, Vector2 scale, JsonValue data){
        super(texture, scale, data);
        totalDurationTicks = data.getInt("duration");
        pressedTicks = 0;
        setName("timedButton");
    }

    /** for a timed button, stays active for a set period of ticks after release */
    public void updateActivated(){
        if (isPressed()) {
            pressedTicks = totalDurationTicks;
        } else {
            pressedTicks = Math.max(0, pressedTicks - 1);
        }
        active = pressedTicks > 0;
    }

}
