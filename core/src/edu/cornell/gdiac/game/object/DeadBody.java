package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.CapsuleObstacle;

import java.util.HashMap;

public class DeadBody extends CapsuleObstacle implements Movable {
    /** Constants that are shared between all instances of this class */
    private static JsonValue objectConstants;
    /** How long the body has been burning */
    private int burnTicks;
    /** If the body is currently burning */
    private boolean burning;
    /** The total number ticks a body burns for */
    private static int totalBurnTicks;
    /** The amount to slow the model down */
    private final float damping;
    /** Which direction is the model facing */
    private boolean faceRight;
    /** The number of hazards that the body is touching */
    private int hazardsTouching;
    /** If dead body is currently being hit by a laser.
     * This is necessary because laser collisions are done with raycasting.*/
    private boolean touchingLaser;
    /** The offset of the solid hitbox of the dead body */
    private Vector2 drawOffset;
    /** The set of spirit regions that this dead body is inside */
    private ObjectSet<SpiritRegion> spiritRegions;
    private TextureRegion[][] spriteFrames;
    private Animation<TextureRegion> animation;
    private float time;
    private ObjectSet<Fixture> groundFixtures = new ObjectSet<>();
    public static final String groundSensorName = "deadBodyGround";
    public static final String centerSensorName = "deadBodyCenter";
    public static final String catHitboxSensorName = "catHitBox";
    public static final String hitboxSensorName = "deadBodyHitBox";
    private PolygonShape hitboxShape;
    /** List of shapes corresponding to the sensors attached to this body */
    private Array<Shape> sensorShapes;
    /** Set of joints that are attached to this object */
    private ObjectSet<Joint> joints = new ObjectSet<>();

    /**
     * Returns ow hard the brakes are applied to get a dead body to stop moving
     *
     * @return ow hard the brakes are applied to get a dead body to stop moving
     */
    public float getDamping() {
        return damping;
    }

    /**
     * sets faceRight to facingRight
     */
    public void setFacingRight(boolean facingRight) {
        faceRight = facingRight;
    }

    /**
     * Returns true if this model is facing right
     *
     * @return true if this model is facing right
     */
    public boolean isFacingRight() {
        return faceRight;
    }

    /**
     * If the dead body is safe to be switched into.
     * @return true if the dead body can be switched into
     */
    public boolean isSwitchable(){
        return hazardsTouching == 0 && !touchingLaser;
    }

    /**
     * If the dead body is in the same spirit region.
     * @return true if the dead body is in the same spirit region
     */
    public boolean inSameSpiritRegion(ObjectSet<SpiritRegion> otherRegions){
        for (SpiritRegion region : otherRegions) {
            if (spiritRegions.contains(region)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets if the dead body is being hit by a laser.
     */
    public void setTouchingLaser(boolean touching){ touchingLaser = touching; }

    /**
     * A new hazard has started touching this dead body.
     */
    public void addHazard(){ hazardsTouching++; }

    /**
     * A hazard has stopped touching this dead body.
     */
    public void removeHazard(){ hazardsTouching--; }

    /**
     * Creates a new dead body. Note that the Box2D body created in this constructor is not the actual solid part of the
     * dead body, it is a sensor body that is identical to the body of the Cat. This is so that we have a reference for
     * how much space the Cat would take if swapped with this dead body. The actual solid fixture is created in
     * <code>activatePhysics().</code>
     *
     * @param texture       Texture for regular dead body.
     * @param burnTexture   Texture for burning dead body.
     * @param scale         Draw scale.
     * @param position      Position
     */
    public DeadBody(TextureRegion texture, TextureRegion burnTexture, Vector2 scale, Vector2 position) {
        super(0, 0, objectConstants.getFloat("capsuleWidth"), objectConstants.getFloat("capsuleHeight"), Orientation.TOP);

        spriteFrames = TextureRegion.split(burnTexture.getTexture(), 2048,2048);
        animation = new Animation<>(0.025f, spriteFrames[0]);
        time = 0f;
        setTexture(texture);
        setDrawScale(scale);
        drawOffset = new Vector2(objectConstants.get("draw_offset").getFloat(0), objectConstants.get("draw_offset").getFloat( 1));
        setDensity(objectConstants.getFloat("density", 0));
        setMass(objectConstants.getFloat("mass", 0));
        setFriction(objectConstants.getFloat("friction", 0));  /// HE WILL STICK TO WALLS IF YOU FORGET
        setFixedRotation(true);
        setSensor(true);
        sensorShapes = new Array<>();

//        setLinearDamping(objectConstants.getFloat("damping", 2f)); this messes with moving platforms

        damping = objectConstants.getFloat("damping", 0);

        // Gameplay attributes
        setX(position.x+objectConstants.get("offset").getFloat(0));
        setY(position.y+objectConstants.get("offset").getFloat(1));
        burnTicks = 0;
        burning = false;
        faceRight = true;
        spiritRegions = new ObjectSet<>();
        //create centre sensor (for fixing to spikes)

        setName("deadBody");
    }

    /**
     * Creates the physics Body(s) for this object, adding them to the world.
     * <p>
     * This method overrides the base method to keep your ship from spinning.
     *
     * @param world Box2D world to store body
     * @return true if object allocation succeeded
     */
    public boolean activatePhysics(World world) {
        // create the box from our superclass
        if (!super.activatePhysics(world)) {
            return false;
        }
        for (Fixture f : body.getFixtureList()) {
            f.setUserData(catHitboxSensorName);
        }

        //the actual physical hitbox
        FixtureDef hitboxDef = new FixtureDef();
        float hx = texture.getRegionWidth()/drawScale.x*objectConstants.get("shrink").getFloat(0)/2f;
        float hy = texture.getRegionHeight()/drawScale.y*objectConstants.get("shrink").getFloat(1)/2f;
        Vector2 solidOffset = new Vector2(0, (hy - getHeight()/2f));
        hitboxShape = new PolygonShape();
        hitboxShape.setAsBox(hx, hy, solidOffset, 0);
        hitboxDef.shape = hitboxShape;
        hitboxDef.density = 0;
        hitboxDef.friction = objectConstants.getFloat("friction");
        sensorShapes.add(hitboxShape);
        Fixture solidFixture = body.createFixture(hitboxDef);
        solidFixture.setUserData(hitboxSensorName);

        //center sensor
        JsonValue centerSensorJV = objectConstants.get("center_sensor");
        Fixture fix = generateSensor(solidOffset,
                centerSensorJV.getFloat("width", 0) * getWidth()/2, hy,
                centerSensorName);
        fix.setSensor(false);

        JsonValue groundSensorJV = objectConstants.get("ground_sensor");
        Fixture a = generateSensor(new Vector2(0, -getHeight() / 2),
                groundSensorJV.getFloat("shrink", 0) * getWidth() / 1.3f,
                groundSensorJV.getFloat("height", 0),
                getGroundSensorName());

        // Side sensors to help detect for wall climbing
        JsonValue sideSensorJV = objectConstants.get("side_sensor");
        Fixture b = generateSensor(new Vector2(-getWidth() / 2, 0),
                sideSensorJV.getFloat("width", 0),
                sideSensorJV.getFloat("shrink") * getHeight() / 2.0f,
                catHitboxSensorName);

        generateSensor(new Vector2(getWidth() / 2, 0),
                sideSensorJV.getFloat("width", 0),
                sideSensorJV.getFloat("shrink") * getHeight() / 2.0f,
                catHitboxSensorName);

        return true;
    }

    /**
     * Generates a sensor fixture to be used on the dead body.
     * <p>
     * We set friction to 0 to ensure fixture has no physical effects. Copied from Cat to ensure consistency.
     *
     * @param location relative location of the sensor fixture
     * @param hx       half-width used for PolygonShape
     * @param hy       half-height used for PolygonShape
     * @param name     name for the sensor UserData
     * @return
     */
    private Fixture generateSensor(Vector2 location, float hx, float hy, String name) {
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.friction = 0;
        sensorDef.density = 0;
        sensorDef.isSensor = true;
        PolygonShape sensorShape = new PolygonShape();
        sensorShape.setAsBox(hx, hy, location, 0.0f);
        sensorDef.shape = sensorShape;

        Fixture sensorFixture = body.createFixture(sensorDef);
        sensorFixture.setUserData(name);
        sensorShapes.add(sensorShape);
        return sensorFixture;
    }

    /**
     * Updates the object's physics state (NOT GAME LOGIC).
     * <p>
     * We use this method to reset cooldowns.
     *
     * @param dt Number of seconds since last animation frame
     */
    public void update(float dt) {
        // Apply cooldowns

        super.update(dt);
        if (burning) {
            burnTicks++;
            if (burnTicks >= totalBurnTicks){
                markRemoved(true);
            }
        }
        if (groundFixtures.size == 0){
            setVX(getVX()/damping);
        }
    }

    /**
     * @param burning the new burning state of this dead body
     */
    public void setBurning(boolean burning){
        this.burning = burning;
    }

    /**
     * @return The set of spirit regions that this dead body is inside
     */
    public ObjectSet<SpiritRegion> getSpiritRegions() { return spiritRegions; }

    /** Destroy all joints connected to this deadbody */
    public void destroyJoints(World world){
        for (Joint j : joints) {
            world.destroyJoint(j);
        }
        joints.clear();
    }

    /** Clears the joints array */
    public void clearJoints(){ joints.clear(); }

    public void addJoint(Joint joint){ joints.add(joint); }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        System.out.println(this + ": " + getLinearVelocity());
        float effect = faceRight ? 1.0f : -1.0f;
        Color color = new Color(1, 1, 1, 1f - ((float)burnTicks)/((float)totalBurnTicks));
        float textureX = getX() + drawOffset.x;
        float textureY = getY() + drawOffset.y;
        if(burning){
            animation.setPlayMode(Animation.PlayMode.LOOP);
            time += Gdx.graphics.getDeltaTime();
            TextureRegion frame = animation.getKeyFrame(time);
            float x = textureX * drawScale.x + effect*frame.getRegionWidth()/drawScale.x/2;
            float y = textureY * drawScale.y-frame.getRegionHeight()/drawScale.y/2+5;
            canvas.draw(frame, color, origin.x, origin.y,  x,y, getAngle(), -effect/drawScale.x, 1.0f/drawScale.y);
        }
        else{
            canvas.draw(texture, color, origin.x, origin.y, textureX * drawScale.x, textureY * drawScale.y, getAngle(), effect, 1.0f);
        }
    }

    /**
     * Draws the outline of the physics body.
     * <p>
     * This method can be helpful for understanding issues with collisions.
     *
     * @param canvas Drawing context
     */
    public void drawDebug(GameCanvas canvas) {
        super.drawDebug(canvas);
        float xTranslate = (canvas.getCamera().getX()-canvas.getWidth()/2)/drawScale.x;
        float yTranslate = (canvas.getCamera().getY()-canvas.getHeight()/2)/drawScale.y;
        for (Shape shape : sensorShapes) {
            if (shape instanceof PolygonShape) {
                canvas.drawPhysics((PolygonShape) shape, Color.RED, getX() - xTranslate, getY() - yTranslate,
                        getAngle(), drawScale.x, drawScale.y);
            } else if (shape instanceof CircleShape) {
                canvas.drawPhysics((CircleShape) shape, Color.RED, getX() - xTranslate, getY() - yTranslate, drawScale.x, drawScale.y);
            }
        }
    }

    /**
     * Sets the shared constants for all instances of this class
     * @param constants JSON storing the shared constants.
     */
    public static void setConstants(JsonValue constants) {
        objectConstants = constants;
        totalBurnTicks = constants.getInt("burnTicks");
    }

    public boolean isMovable() {return true;}

    public ObjectSet<Fixture> getGroundFixtures() { return groundFixtures; }

    @Override
    public String getGroundSensorName() { return groundSensorName; }

    public ObjectMap<String, Object> storeState(){
        ObjectMap<String, Object> stateMap = super.storeState();
        stateMap.put("burnTicks", burnTicks);
        HashMap<Spikes, Vector2> jointInfo = new HashMap<>();
        stateMap.put("faceRight", faceRight);
        for (Joint j : joints) {
            jointInfo.put((Spikes) j.getBodyB().getUserData(), j.getAnchorA());
        }
        stateMap.put("jointInfo", jointInfo);

        System.out.println(stateMap);
        return stateMap;
    }

    public void loadState(ObjectMap<String, Object> stateMap){
        super.loadState(stateMap);
        System.out.println(stateMap);
        System.out.println(getLinearVelocity());
        burnTicks = (int) stateMap.get("burnTicks");
        faceRight = (boolean) stateMap.get("faceRight");
        joints.clear();
    }
}