/*
 * CatModel.java
 *
 * You SHOULD NOT need to modify this file.  However, you may learn valuable lessons
 * for the rest of the lab by looking at it.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * Updated asset version, 2/6/2021
 */
package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.CapsuleObstacle;

/**
 * Player avatar for the plaform game.
 *
 * Note that this class returns to static loading.  That is because there are
 * no other subclasses that we might loop through.
 */
public class DeadCat extends CapsuleObstacle {
    /** The initializing data (to avoid magic numbers) */
    private final JsonValue data;

    /** The factor to multiply by the input */
    private final float force;
    private final float dash_force;
    /** Whether we are actively dashing */
    private boolean isDashing;
    public boolean canDash;
    /** The amount to slow the character down */
    private final float damping;
    /** The maximum character speed */
    private final float maxspeed;
    /** Identifier to allow us to track the sensor in ContactListener */
    private final String sensorName;
    /** The impulse for the character jump */
    private final float jump_force;
    /** Damping multiplier to slow down jump */
    private final float jumpDamping;

    /** The current horizontal movement of the character */
    private float   movement;
    /** Current jump movement of the character */
    private float   jumpMovement;
    /** Which direction is the character facing */
    private boolean faceRight;
    /** Whether we are actively jumping */
    private boolean isJumping;
    /** Whether we stopped jumping in air */
    private boolean stoppedJumping;
    /** Whether our feet are on the ground */
    private boolean isGrounded;
    /** The physics shape of this object */
    private PolygonShape sensorShape;

    /** Cache for internal force calculations */
    private final Vector2 forceCache = new Vector2();


    /**
     * Returns left/right movement of this character.
     *
     * This is the result of input times cat force.
     *
     * @return left/right movement of this character.
     */
    public float getMovement() {
        return movement;
    }

    /**
     * Sets left/right movement of this character.
     *
     * This is the result of input times cat force.
     *
     * @param value left/right movement of this character.
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
    public void setDashing(boolean value){
        isDashing = value;
    }
    public boolean isDashing(){
        return isDashing;
    }
    /**
     * Returns true if the cat is actively jumping.
     *
     * @return true if the cat is actively jumping.
     */
    public boolean isJumping() {
        return isJumping;
    }

    /**
     * Sets whether the cat is actively jumping.
     *
     * @param value whether the cat is actively jumping.
     */
    public void setJumping(boolean value) {
        isJumping = value;
        if (isJumping) {
            jumpMovement *= jumpDamping;
        }
        if (!isJumping && !isGrounded()){
            stoppedJumping = true;
        }
    }


    /**
     * Returns true if the cat is on the ground.
     *
     * @return true if the cat is on the ground.
     */
    public boolean isGrounded() {
        return isGrounded;
    }

    /**
     * Sets whether the cat is on the ground.
     *
     * @param value whether the cat is on the ground.
     */
    public void setGrounded(boolean value) {
        isGrounded = value;
        if (isGrounded) {
            canDash = true;
            jumpMovement = jump_force;
            stoppedJumping = false;
        }
    }

    /**
     * Returns how much force to apply to get the cat moving
     *
     * Multiply this by the input to get the movement value.
     *
     * @return how much force to apply to get the cat moving
     */
    public float getForce() {
        return force;
    }

    /**
     * Returns ow hard the brakes are applied to get a cat to stop moving
     *
     * @return ow hard the brakes are applied to get a cat to stop moving
     */
    public float getDamping() {
        return damping;
    }

    /**
     * Returns the upper limit on cat left-right movement.
     *
     * This does NOT apply to vertical movement.
     *
     * @return the upper limit on cat left-right movement.
     */
    public float getMaxSpeed() {
        return maxspeed;
    }

    /**
     * Returns the name of the ground sensor
     *
     * This is used by ContactListener
     *
     * @return the name of the ground sensor
     */
    public String getSensorName() {
        return sensorName;
    }

    /**
     * Returns true if this character is facing right
     *
     * @return true if this character is facing right
     */
    public boolean isFacingRight() {
        return faceRight;
    }

    /**
     * Creates a new cat avatar with the given physics data
     *
     * The size is expressed in physics units NOT pixels.  In order for
     * drawing to work properly, you MUST set the drawScale. The drawScale
     * converts the physics units to pixels.
     *
     * @param data  	The physics constants for this cat
     * @param width		The object width in physics units
     * @param height	The object width in physics units
     */
    public DeadCat(JsonValue data, float width, float height) {
        // The shrink factors fit the image to a tigher hitbox
        super(	data.get("pos").getFloat(0),
                data.get("pos").getFloat(1),
                width*data.get("shrink").getFloat( 0 ),
                height*data.get("shrink").getFloat( 1 ),
                Orientation.TOP);
        setDensity(data.getFloat("density", 0));
        setFriction(data.getFloat("friction", 0));  /// HE WILL STICK TO WALLS IF YOU FORGET
        setFixedRotation(true);

        maxspeed = data.getFloat("maxspeed", 0);
        damping = data.getFloat("damping", 0);
        force = data.getFloat("force", 0);
        jump_force = data.getFloat( "jump_force", 0 );
        dash_force = data.getFloat( "dash_force", 0 );;
        jumpDamping = data.getFloat("jump_damping", 0);
        sensorName = "catGroundSensor";
        this.data = data;

        // Gameplay attributes
        isGrounded = false;
        canDash = true;
        isJumping = false;
        faceRight = true;
        stoppedJumping = false;

        setName("cat");
    }

    /**
     * Creates the physics Body(s) for this object, adding them to the world.
     *
     * This method overrides the base method to keep your ship from spinning.
     *
     * @param world Box2D world to store body
     *
     * @return true if object allocation succeeded
     */
    public boolean activatePhysics(World world) {
        // create the box from our superclass
        if (!super.activatePhysics(world)) {
            return false;
        }

        // Ground Sensor
        // -------------
        // We only allow the cat to jump when he's on the ground.
        // Double jumping is not allowed.
        //
        // To determine whether or not the cat is on the ground,
        // we create a thin sensor under his feet, which reports
        // collisions with the world but has no collision response.
        Vector2 sensorCenter = new Vector2(0, -getHeight() / 2);
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = data.getFloat("density",0);
        sensorDef.isSensor = true;
        sensorShape = new PolygonShape();
        JsonValue sensorjv = data.get("sensor");
        sensorShape.setAsBox(sensorjv.getFloat("shrink",0)*getWidth()/2.0f,
                sensorjv.getFloat("height",0), sensorCenter, 0.0f);
        sensorDef.shape = sensorShape;

        // Ground sensor to represent our feet
        Fixture sensorFixture = body.createFixture( sensorDef );
        sensorFixture.setUserData(getSensorName());

        return true;
    }


    /**
     * Applies the force to the body of this cat
     *
     * This method should be called after the force attribute is set.
     */
    public void applyForce() {
        if (!isActive()) {
            return;
        }
        // Don't want to be moving. Damp out player motion
        if (getMovement() == 0f) {
            forceCache.set(-getDamping()*getVX(),0);
            body.applyForce(forceCache,getPosition(),true);
        }

        // Velocity too high, clamp it
        if (Math.abs(getVX()) >= getMaxSpeed()) {
            setVX(Math.signum(getMovement())*getMaxSpeed());
        } else {
            forceCache.set(getMovement(),0);
            body.applyForce(forceCache,getPosition(),true);
        }
        if (isDashing() && canDash && isJumping()){
            if(movement > 0){
                forceCache.set(dash_force,dash_force);
            }
            else if(movement < 0){
                forceCache.set(-dash_force,dash_force);
            }
            else{
                forceCache.set(0,dash_force);
            }
            body.applyLinearImpulse(forceCache,getPosition(),true);
            canDash = false;
        }
        // Jump!
        if (isJumping() && !stoppedJumping) {
            forceCache.set(0, jumpMovement);
            body.applyLinearImpulse(forceCache,getPosition(),true);
        }
    }

    /**
     * Updates the object's physics state (NOT GAME LOGIC).
     *
     * We use this method to reset cooldowns.
     *
     * @param dt	Number of seconds since last animation frame
     */
    public void update(float dt) {
        // Apply cooldowns
//        if (isJumping()) {
//            jumpCooldown = jumpLimit;
//        } else {
//            jumpCooldown = Math.max(0, jumpCooldown - 1);
//        }

        super.update(dt);
    }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        float effect = faceRight ? 1.0f : -1.0f;
        canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y,getAngle(),effect,1.0f);
    }

    /**
     * Draws the outline of the physics body.
     *
     * This method can be helpful for understanding issues with collisions.
     *
     * @param canvas Drawing context
     */
    public void drawDebug(GameCanvas canvas) {
        super.drawDebug(canvas);
        canvas.drawPhysics(sensorShape,Color.RED,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
    }
}