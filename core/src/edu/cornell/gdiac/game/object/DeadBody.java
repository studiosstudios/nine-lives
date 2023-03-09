/*
 * DeadBodyModel.java
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * Updated asset version, 2/6/2021
 */
package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.*;
import edu.cornell.gdiac.game.obstacle.*;

/**
 * Player avatar for the plaform game.
 *
 * Note that this class returns to static loading.  That is because there are
 * no other subclasses that we might loop through.
 */
public class DeadBody extends CapsuleObstacle {

    private int burnTicks;
    private boolean burning;
    public static final int TOTAL_BURN_TICKS = 900;
    /**
     * The initializing data (to avoid magic numbers)
     */
    private final JsonValue data;

    /**
     * The factor to multiply by the input
     */
    private final float force;
    private final float dash_force;
    /**
     * Whether we are actively dashing
     */
    private boolean isDashing;
    public boolean canDash;
    /**
     * The amount to slow the model down
     */
    private final float damping;
    /**
     * The maximum model speed
     */
    private final float maxspeed;
    /**
     * Identifier to allow us to track the sensor in ContactListener
     */
    private final String sensorName;

    /**
     * The current horizontal movement of the model
     */
    private float movement;
    /**
     * Which direction is the model facing
     */
    private boolean faceRight;
    /**
     * Whether our feet are on the ground
     */
    private boolean isGrounded;
    /**
     * The physics shape of this object
     */
    private CircleShape sensorShape;

    /**
     * Cache for internal force calculations
     */
    private final Vector2 forceCache = new Vector2();


    /**
     * Returns left/right movement of this model.
     * <p>
     * This is the result of input times dead body force.
     *
     * @return left/right movement of this model.
     */
    public float getMovement() {
        return movement;
    }

    /**
     * Sets left/right movement of this model.
     * <p>
     * This is the result of input times dead body force.
     *
     * @param value left/right movement of this model.
     */
    public void setMovement(float value) {
        movement = value;
        // Change facing if appropriate
        if (movement < 0) {
            faceRight = false;
        } else if (movement > 0) {
            faceRight = true;
        }
    }


    /**
     * Returns true if the dead body is on the ground.
     *
     * @return true if the dead body is on the ground.
     */
    public boolean isGrounded() {
        return isGrounded;
    }

    /**
     * Sets whether the dead body is on the ground.
     *
     * @param value whether the dead body is on the ground.
     */
    public void setGrounded(boolean value) {
        isGrounded = value;
    }

    /**
     * Returns how much force to apply to get the dead body moving
     * <p>
     * Multiply this by the input to get the movement value.
     *
     * @return how much force to apply to get the dead body moving
     */
    public float getForce() {
        return force;
    }

    /**
     * Returns ow hard the brakes are applied to get a dead body to stop moving
     *
     * @return ow hard the brakes are applied to get a dead body to stop moving
     */
    public float getDamping() {
        return damping;
    }

    /**
     * Returns the upper limit on dead body left-right movement.
     * <p>
     * This does NOT apply to vertical movement.
     *
     * @return the upper limit on dead body left-right movement.
     */
    public float getMaxSpeed() {
        return maxspeed;
    }

    /**
     * Returns the name of the ground sensor
     * <p>
     * This is used by ContactListener
     *
     * @return the name of the ground sensor
     */
    public String getSensorName() {
        return sensorName;
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
     * Creates a new dead body model with the given physics data
     * <p>
     * The size is expressed in physics units NOT pixels.  In order for
     * drawing to work properly, you MUST set the drawScale. The drawScale
     * converts the physics units to pixels.
     *
     * @param data   The physics constants for this dead body
     * @param width  The object width in physics units
     * @param height The object width in physics units
     */
    public DeadBody(JsonValue data, float width, float height) {
        // The shrink factors fit the image to a tigher hitbox
        super(data.get("pos").getFloat(0),
                data.get("pos").getFloat(1),
                width * data.get("shrink").getFloat(0),
                height * data.get("shrink").getFloat(1),
                Orientation.TOP);
        setDensity(data.getFloat("density", 0));
        setFriction(data.getFloat("friction", 0));  /// HE WILL STICK TO WALLS IF YOU FORGET
        setFixedRotation(true);

        maxspeed = data.getFloat("maxspeed", 0);
        damping = data.getFloat("damping", 0);
        force = data.getFloat("force", 0);
        dash_force = data.getFloat("dash_force", 0);
        sensorName = "deadBodyGroundSensor";
        this.data = data;

        // Gameplay attributes
        burnTicks = 0;
        burning = false;
        isGrounded = false;
        faceRight = true;


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

        // Ground Sensor
        // -------------
        //
        // To determine whether or not the dead body is on the ground,
        // we create a thin sensor under the feet, which reports
        // collisions with the world but has no collision response.
        //TODO: put this in JSON
        Vector2 sensorCenter = new Vector2();
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = 0;
        sensorDef.isSensor = true;
        sensorShape = new CircleShape();
        sensorShape.setRadius(0.1f);
        sensorShape.setPosition(sensorCenter);
        sensorDef.shape = sensorShape;

        // Ground sensor to represent our feet
        Fixture sensorFixture = body.createFixture(sensorDef);
        sensorFixture.setUserData(this);

        return true;
    }


    /**
     * Applies the force to the body of this dead body
     * <p>
     * This method should be called after the force attribute is set.
     */
    public void applyForce() {
        if (!isActive()) {
            return;
        }
        // Don't want to be moving. Damp out player motion
        if (getMovement() == 0f) {
            forceCache.set(-getDamping() * getVX(), 0);
            body.applyForce(forceCache, getPosition(), true);
        }

        // Velocity too high, clamp it
        if (Math.abs(getVX()) >= getMaxSpeed()) {
            setVX(Math.signum(getMovement()) * getMaxSpeed());
        } else {
            forceCache.set(getMovement(), 0);
            body.applyForce(forceCache, getPosition(), true);
        }
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
            if (burnTicks >= TOTAL_BURN_TICKS){
                markRemoved(true);
            }
        }
    }

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
        Color color = new Color(1, 1, 1, 1f - ((float)burnTicks)/((float)TOTAL_BURN_TICKS));
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
    }
}