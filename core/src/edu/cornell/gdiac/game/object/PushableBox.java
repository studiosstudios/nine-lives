package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;

import java.util.HashMap;


public class PushableBox extends BoxObstacle implements Movable {
    /** Constants that are shared between all instances of this class */
    private static JsonValue objectConstants;
    private ObjectSet<Fixture> groundFixtures = new ObjectSet<>();
    private PolygonShape sensorShape;
    private final String groundSensorName;
    private float damping;

    /**
     * Sets the shared constants for all instances of this class.
     * @param constants JSON storing the shared constants.
     */
    public static void setConstants(JsonValue constants) { objectConstants = constants; }


    /**
     * Creates a new Box object.
     *
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param textureScale   Texture scale for rescaling texture
     */
    public PushableBox(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, Vector2 textureScale){
        super(tMap.get("box").getRegionWidth()/scale.x*textureScale.x * objectConstants.getFloat("shrink"),
                tMap.get("box").getRegionHeight()/scale.y*textureScale.y * objectConstants.getFloat("shrink"));

        setBodyType(BodyDef.BodyType.DynamicBody);
        setFixedRotation(true);
        setName("box");
        setDrawScale(scale);
        setTexture(tMap.get("box"));
        setTextureScale(textureScale);

        setRestitution(objectConstants.getFloat("restitution", 0));
        setFriction(objectConstants.getFloat("friction", 0));
        setDensity(objectConstants.getFloat("density", 0));
        setMass(objectConstants.getFloat("mass", 0));
        setX((float) properties.get("x")+objectConstants.get("offset").getFloat(0));
        setY((float) properties.get("y")+objectConstants.get("offset").getFloat(1));
        damping = objectConstants.getFloat("damping", 0);

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

    public void update(float dt){ setRelativeVX(getRelativeVelocity().x/damping); }

    public ObjectSet<Fixture> getGroundFixtures() { return groundFixtures; }
    public String getGroundSensorName(){ return groundSensorName; }
    @Override
    public void drawDebug(GameCanvas canvas){
        super.drawDebug(canvas);
//        float xTranslate = (canvas.getCamera().getX()-canvas.getWidth()/2)/drawScale.x;
//        float yTranslate = (canvas.getCamera().getY()-canvas.getHeight()/2)/drawScale.y;
        canvas.drawPhysics(sensorShape, Color.RED,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
    }

    public ObjectMap<String, Object> storeState(){
        ObjectMap<String, Object> stateMap = super.storeState();
        stateMap.put("groundFixtures", copyObjectSet(groundFixtures));
        return stateMap;
    }

    private <T> ObjectSet<T> copyObjectSet(ObjectSet<T> base) {
        ObjectSet<T> copy = new ObjectSet<>();
        for (T el : base){
            copy.add(el);
        }
        return copy;
    }
    public void loadState(ObjectMap<String, Object> stateMap){
        super.loadState(stateMap);
        groundFixtures = (ObjectSet<Fixture>) stateMap.get("groundFixtures");
    }
}
