/*
 * DeadBodyModel.java
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * Updated asset version, 2/6/2021
 */
package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.game.*;
import edu.cornell.gdiac.game.obstacle.*;

/**
 * Player avatar for the plaform game.
 *
 * Note that this class returns to static loading.  That is because there are
 * no other subclasses that we might loop through.
 */
public class DeadBody extends BoxObstacle implements Movable {
    /** Constants that are shared between all instances of this class */
    private static JsonValue objectConstants;
    /** How long the body has been burning */
    private int burnTicks;
    /** If the body is currently burning */
    private boolean burning;
    /** The total number ticks a body burns for */
    private static int totalBurnTicks;

    /**
     * The amount to slow the model down
     */
    private final float damping;
    /**
     * Which direction is the model facing
     */
    private boolean faceRight;
    /**
     * The physics shape of this object
     */
    private CircleShape sensorShape;
    /** The number of hazards that the body is touching */
    private int hazardsTouching;
    /** If dead body is currently being hit by a laser.
     * This is necessary because laser collisions are done with raycasting.*/
    private boolean touchingLaser;

    private ObjectSet<Fixture> groundFixtures = new ObjectSet<>();
    private PolygonShape groundSensorShape;
    private final String groundSensorName;

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
     * Creates a new dead body model with the given physics data
     * <p>
     * The size is expressed in physics units NOT pixels.  In order for
     * drawing to work properly, you MUST set the drawScale. The drawScale
     * converts the physics units to pixels.
     *
     * @param texture      the texture
     * @param scale        the draw scale
     * @param position     position
     */
    public DeadBody(TextureRegion texture, Vector2 scale, Vector2 position) {
        super(texture.getRegionWidth()/scale.x*objectConstants.get("shrink").getFloat(0),
                texture.getRegionHeight()/scale.y*objectConstants.get("shrink").getFloat(1));

        setTexture(texture);
        setDrawScale(scale);
        setDensity(objectConstants.getFloat("density", 0));
        setMass(objectConstants.getFloat("mass", 0));
        setFriction(objectConstants.getFloat("friction", 0));  /// HE WILL STICK TO WALLS IF YOU FORGET
        setFixedRotation(true);

//        setLinearDamping(objectConstants.getFloat("damping", 2f)); this messes with moving platforms

        damping = objectConstants.getFloat("damping", 0);

        // Gameplay attributes
        setX(position.x+objectConstants.get("offset").getFloat(0));
        setY(position.y+objectConstants.get("offset").getFloat(1));
        burnTicks = 0;
        burning = false;
        faceRight = true;

        groundSensorName = "deadBodyGroundSensor";
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

        //center sensor
        Vector2 sensorCenter = new Vector2();
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = 0;
        sensorDef.isSensor = true;
        sensorShape = new CircleShape();
        sensorShape.setRadius(objectConstants.getFloat("sensorRadius"));
        sensorShape.setPosition(sensorCenter);
        sensorDef.shape = sensorShape;

        Fixture sensorFixture = body.createFixture(sensorDef);
        sensorFixture.setUserData(this);

        //ground sensor
        sensorDef = new FixtureDef();
        sensorDef.friction = 0;
        sensorDef.isSensor = true;
        Vector2 location = new Vector2(0, -getDimension().y/2f);
        groundSensorShape = new PolygonShape();
        groundSensorShape.setAsBox(getDimension().x/2.5f, 0.1f, location, 0.0f);
        sensorDef.shape = groundSensorShape;

        Fixture groundSensorFixture = body.createFixture( sensorDef );
        groundSensorFixture.setUserData(groundSensorName);
        return true;
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
    }

    /**
     * @param burning the new burning state of this dead body
     */
    public void setBurning(boolean burning){
        this.burning = burning;
    }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        float effect = faceRight ? 1.0f : -1.0f;
        Color color = new Color(1, 1, 1, 1f - ((float)burnTicks)/((float)totalBurnTicks));
        canvas.draw(texture, color, origin.x, origin.y, getX() * drawScale.x, getY() * drawScale.y, getAngle(), effect, 1.0f);
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
        canvas.drawPhysics(sensorShape, Color.RED, getX(), getY(), drawScale.x, drawScale.y);
        canvas.drawPhysics(groundSensorShape, Color.RED, getX(), getY(), getAngle(), drawScale.x, drawScale.y);
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

    public String getGroundSensorName(){ return groundSensorName; }
}