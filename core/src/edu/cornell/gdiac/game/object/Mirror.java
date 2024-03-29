package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.PolygonObstacle;
import edu.cornell.gdiac.util.Direction;

import java.util.HashMap;

public class Mirror extends PolygonObstacle {
    /** Constants that are shared between all instances of this class*/
    private static JsonValue objectConstants;
    /** Direction of the mirror.
     *  Up is defined as the sloped side being normal to the vector (1, 1). */
    private Direction dir;
    /** The color of all mirrors */
    private static Color color;

    /**
     * Sets the shared constants for all instances of this class/
     * @param constants JSON storing the shared constants.
     */
    public static void setConstants(JsonValue constants) {
        objectConstants = constants;
        color = new Color(1, 1, 1, constants.getFloat("alpha"));
    }

    /**
     * Creates a new Mirror object.
     *
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param textureScale   Texture scale for rescaling texture
     */
    public Mirror(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, Vector2 textureScale){
        super(objectConstants.get("shape").asFloatArray());

        setBodyType((boolean) properties.get("pushable", false) ? BodyDef.BodyType.DynamicBody : BodyDef.BodyType.StaticBody);
        setFixedRotation(true);
        setName("mirror");
        setDrawScale(scale);
        setTextureScale(textureScale);
        setTexture(tMap.get("steel"));
        setAngle((float) ((float) properties.get("rotation") * Math.PI/180));
        dir = Direction.angleToDir((int) ((float) properties.get("rotation")));

        setRestitution(objectConstants.getFloat("restitution", 0));
        setFriction(objectConstants.getFloat("friction", 0));
        setDensity(objectConstants.getFloat("density", 0));
        setMass(objectConstants.getFloat("mass", 0));

        //this is ugly but it's easy
        float xOffset, yOffset;
        switch (dir) {
            case UP:
                xOffset = 0;
                yOffset = 0;
                break;
            case LEFT:
                xOffset = 0;
                yOffset = -1;
                break;
            case DOWN:
                xOffset = 1;
                yOffset = -1;
                break;
            case RIGHT:
                xOffset = 1;
                yOffset = 0;
                break;
            default:
                throw new IllegalArgumentException("undefined angle");
        }
        setX((float) properties.get("x")+objectConstants.get("offset").getFloat(0) + xOffset);
        setY((float) properties.get("y")+objectConstants.get("offset").getFloat(1) + yOffset);
    }

    /**
     * Returns the direction a laser beam should reflect if it hits this mirror.
     * @param beamDir the direction of the incident beam
     * @return <code>null</code> if the beam does not reflect, otherwise the direction
     *         of the reflection
     */
    public Direction reflect(Direction beamDir){
        //do not attempt to understand any logic here, this was done case-by-case
        switch (beamDir) {
            case UP:
                if (dir == Direction.DOWN) {
                    return Direction.LEFT;
                } else if (dir == Direction.RIGHT) {
                    return Direction.RIGHT;
                }
                return null;
            case DOWN:
                if (dir == Direction.UP) {
                    return Direction.RIGHT;
                } else if (dir == Direction.LEFT) {
                    return Direction.LEFT;
                }
                return null;
            case LEFT:
                if (dir == Direction.RIGHT) {
                    return Direction.DOWN;
                } else if (dir == Direction.UP) {
                    return Direction.UP;
                }
                return null;
            case RIGHT:
                if (dir == Direction.LEFT) {
                    return Direction.UP;
                } else if (dir == Direction.DOWN) {
                    return Direction.DOWN;
                }
                return null;
        }
        return null;
    }

    @Override
    public void draw(GameCanvas canvas){
        if (region != null) {
            canvas.draw(region, color,0,0,getX()*drawScale.x,getY()*drawScale.y,getAngle(),1,1);
        }
    }

}
