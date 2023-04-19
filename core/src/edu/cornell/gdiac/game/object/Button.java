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
 * The simplest activator, active when pressed and inactive otherwise.
 */
public class Button extends Activator {

    /**
     * Creates a new Button object.
     *
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param tileSize       Tile size of the Tiled map for loading positions
     * @param levelHeight    Height of level (in grid cell units) for loading y position
     * @param textureScale   Texture scale for rescaling texture
     */
    public Button(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, int tileSize, int levelHeight, Vector2 textureScale){
        super(properties, tMap, scale, tileSize, levelHeight, textureScale);
    }

    /** For a button, active = isPressed() */
    public void updateActivated(){
        active = isPressed();
    }

}
