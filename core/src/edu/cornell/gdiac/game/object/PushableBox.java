package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;



public class PushableBox extends BoxObstacle implements Movable {
    /** Constants that are shared between all instances of this class */
    private static JsonValue objectConstants;
    private ObjectSet<Fixture> groundFixtures = new ObjectSet<>();
    private PolygonShape sensorShape;
    private final String groundSensorName;

    /**
     * Sets the shared constants for all instances of this class.
     * @param constants JSON storing the shared constants.
     */
    public static void setConstants(JsonValue constants) { objectConstants = constants; }

    /**
     * Creates a new box object.
     * @param texture  TextureRegion for drawing.
     * @param scale    Draw scale for drawing.
     * @param data     JSON data for loading.
     */
    public PushableBox(TextureRegion texture, Vector2 scale, JsonValue data){
        super(texture.getRegionWidth()/scale.x,
                texture.getRegionHeight()/scale.y);

        setBodyType(BodyDef.BodyType.DynamicBody);
        setFixedRotation(true);
        setName("box");
        setDrawScale(scale);
        setTexture(texture);

        setRestitution(objectConstants.getFloat("restitution", 0));
        setFriction(objectConstants.getFloat("friction", 0));
        setDensity(objectConstants.getFloat("density", 0));
        setMass(objectConstants.getFloat("mass", 0));
        setX(data.get("pos").getFloat(0)+objectConstants.get("offset").getFloat(0));
        setY(data.get("pos").getFloat(1)+objectConstants.get("offset").getFloat(1));

        groundSensorName = "boxGroundSensor";
    }

    /**
     * Creates ground sensor for this movable.
     * @param world Box2D world to store body
     *
     * @return      true if success
     */
    public boolean activatePhysics(World world){
        if (!super.activatePhysics(world)) {
            return false;
        }

        FixtureDef sensorDef = new FixtureDef();
        sensorDef.friction = 0;
        sensorDef.isSensor = true;
        Vector2 location = new Vector2(0, -getDimension().y/2f);
        sensorShape = new PolygonShape();
        sensorShape.setAsBox(getDimension().x/2.5f, 0.1f, location, 0.0f);
        sensorDef.shape = sensorShape;

        Fixture sensorFixture = body.createFixture( sensorDef );
        sensorFixture.setUserData(groundSensorName);

        return true;
    }

    public boolean isMovable() {return true;}

    public ObjectSet<Fixture> getGroundFixtures() { return groundFixtures; }

    public String getGroundSensorName(){ return groundSensorName; }
    @Override
    public void drawDebug(GameCanvas canvas){
        super.drawDebug(canvas);
        canvas.drawPhysics(sensorShape, Color.RED,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
    }

}
