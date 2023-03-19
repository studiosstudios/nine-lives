package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;
import edu.cornell.gdiac.game.obstacle.PolygonObstacle;

public class Mirror extends PolygonObstacle {

    protected static JsonValue objectConstants;

    private Laser.Direction dir;

    private Vector2 endPointCache = new Vector2();
    public static void setConstants(JsonValue constants) { objectConstants = constants; }

    public Mirror(TextureRegion texture, Vector2 scale, JsonValue data){
        super(objectConstants.get("shape").asFloatArray());

        setBodyType(BodyDef.BodyType.StaticBody);
        setFixedRotation(true);
        setName("mirror");
        setDrawScale(scale);
        setTexture(texture);
        setAngle((float) (data.getInt("angle") * Math.PI/180));
        dir = Laser.angleToDir(data.getInt("angle"));

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
    public Laser.Direction reflect(Laser.Direction beamDir){
        //do not attempt to understand any logic here, this was done case-by-case
        switch (beamDir) {
            case UP:
                if (dir == Laser.Direction.DOWN) {
                    return Laser.Direction.LEFT;
                } else if (dir == Laser.Direction.RIGHT) {
                    return Laser.Direction.RIGHT;
                }
                return null;
            case DOWN:
                if (dir == Laser.Direction.UP) {
                    return Laser.Direction.LEFT;
                } else if (dir == Laser.Direction.LEFT) {
                    return Laser.Direction.LEFT;
                }
                return null;
            case LEFT:
                if (dir == Laser.Direction.RIGHT) {
                    return Laser.Direction.DOWN;
                } else if (dir == Laser.Direction.UP) {
                    return Laser.Direction.RIGHT;
                }
                return null;
            case RIGHT:
                if (dir == Laser.Direction.LEFT) {
                    return Laser.Direction.UP;
                } else if (dir == Laser.Direction.DOWN) {
                    return Laser.Direction.DOWN;
                }
                return null;
        }
        return null;
    }

}
