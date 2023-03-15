package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.*;
import edu.cornell.gdiac.game.obstacle.*;

public class Switch extends Activator {

    private boolean prevPressed;
    public Switch(TextureRegion texture, Vector2 scale, JsonValue data){
        super(texture, scale, data);
        prevPressed = false;
        setName("switch");
    }

    /** for a switch, active is toggled every time button is pressed */
    public void updateActivated(){
        if (isPressed() && !prevPressed) {
            active = !active;
        }
        prevPressed = isPressed();
    }

}
