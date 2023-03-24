package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.PolygonObstacle;

public class Mirror extends PolygonObstacle {

    protected static JsonValue objectConstants;

    private Direction dir;

    private static float alpha;

    private Color color;

    private Vector2 endPointCache = new Vector2();
    public static void setConstants(JsonValue constants) {
        objectConstants = constants;
        alpha = constants.getFloat("alpha");
    }

    public Mirror(TextureRegion texture, Vector2 scale, JsonValue data){
        super(objectConstants.get("shape").asFloatArray());

        setBodyType(data.getBoolean("pushable", false) ? BodyDef.BodyType.DynamicBody : BodyDef.BodyType.StaticBody);
        setFixedRotation(true);
        setName("mirror");
        setDrawScale(scale);
        setTexture(texture);
        setAngle((float) (data.getInt("angle") * Math.PI/180));
        dir = Direction.angleToDir(data.getInt("angle"));

        setRestitution(objectConstants.getFloat("restitution", 0));
        setFriction(objectConstants.getFloat("friction", 0));
        setDensity(objectConstants.getFloat("density", 0));
        setMass(objectConstants.getFloat("mass", 0));
        color = new Color(1, 1, 1, alpha);

        //this is ugly but it's easy
        float xOffset, yOffset;
        switch (dir) {
            case UP:
                xOffset = 0;
                yOffset = 0;
                break;
            case LEFT:
                xOffset = 1;
                yOffset = 0;
                break;
            case DOWN:
                xOffset = 1;
                yOffset = 1;
                break;
            case RIGHT:
                xOffset = 0;
                yOffset = 1;
                break;
            default:
                throw new RuntimeException("undefined angle");
        }
        setX(data.get("pos").getFloat(0)+xOffset);
        setY(data.get("pos").getFloat(1)+yOffset);
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
