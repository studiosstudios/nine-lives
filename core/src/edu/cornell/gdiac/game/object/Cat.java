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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.*;
import edu.cornell.gdiac.game.obstacle.*;

/**
 * Player avatar for the plaform game.
 *
 * Note that this class returns to static loading.  That is because there are
 * no other subclasses that we might loop through.
 */
public class Cat extends CapsuleObstacle {
    /** The initializing data (to avoid magic numbers) */
    private final JsonValue data;

    /** The factor to multiply by the input */
    private final float force;
    private final float dash_force;
    /** Whether we are actively dashing */
    private boolean isDashing;
    public boolean canDash;
    private Animation<TextureRegion> animation;
    private TextureRegion[][] spriteFrames;
    private float animationTime;
    private Animation<TextureRegion> animation2;
    private TextureRegion[][] spriteFrames2;
    private float animationTime2;
    /** The amount to slow the character down */
    private final float damping;
    /** The maximum character speed */
    private final float maxspeed;
    /** Identifier to allow us to track the sensor in ContactListener */
    private final String groundSensorName;
    /** Identifier to allow us to track side sensor in ContactListener */
    private final String sideSensorName;
    /** The impulse for the character jump */
    private final float jump_force;
    /** Damping multiplier to slow down jump */
    private final float jumpDamping;

    /** The current horizontal movement of the character */
    private float   movement;
    /** The current vertical movement of the character */
    private float   verticalMovement;
    /** Current jump movement of the character */
    private float horizontalMovement;
    private float   jumpMovement;
    /** Which direction is the character facing */
    private boolean faceRight;
    /** Whether we are actively jumping */
    private boolean isMeowing;
    private boolean isJumping;
    /** Whether we stopped jumping in air */
    private boolean stoppedJumping;
    /** Whether our feet are on the ground */
    private boolean isGrounded;

    /** Whether we are in contact with a wall */
    private int wallCount;
    /** Whether we are climbing on a wall */
    private boolean isClimbing;
    private Texture normal_texture;
    private Texture jumping_texture;
    private Texture sit_texture;
    private boolean jump_animated;

    /** List of shapes corresponding to the sensors attached to this body */
    private Array<PolygonShape> sensorShapes;

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

    /**
     * Returns up/down movement of this character.
     *
     * This is the result of input times cat force.
     *
     * @return up/down movement of this character.
     */
    public float getVerticalMovement() {
        return verticalMovement;
    }
    public float getHorizontalMovement() {
        return horizontalMovement;
    }
    /**
     * Sets up/down movement of this character.
     *
     * This is the result of input times cat force.
     *
     * @param value up/down movement of this character.
     */
    public void setVerticalMovement(float value) {
        verticalMovement = value;
    }
    public void setHorizontalMovement(float value) {
        horizontalMovement = value;
    }
    public void setDashing(boolean value){ isDashing = value; }
    public void setMeowing(boolean value){ isMeowing = value;}

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
     * Sets whether the cat is in contact with a wall
     */
    public void incrementWalled() {
//        System.out.println("Walled: " + value);
        wallCount++;
    }

    /**
     * Sets whether the cat is in contact with a wall
     */
    public void decrementWalled() {
        wallCount--;
    }

    /**
     * Whether the cat is in contact with a wall
     * @return whether the cat is in contact with a wall
     */
    public boolean isWalled() { return wallCount > 0; }

    /**
     * Sets whether the cat is actively climbing
     *
     * @param value whether the cat is actively climbing
     */
    public void setClimbing(boolean value) {
        if (value && !isClimbing) {
            System.out.println("Started climbing");
            body.setGravityScale(0);
        }
        else if (!value && isClimbing) {
            System.out.println("Stopped climbing");
            body.setGravityScale(1);
        }
        isClimbing = value;
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
            jump_animated = false;
            animationTime = 0;
            animationTime2 = 0;
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
    public String getGroundSensorName() {
        return groundSensorName;
    }

    /**
     * Returns the name of the side sensor
     *
     * This is used by ContactListener
     *
     * @return the name of the side sensor
     */
    public String getSideSensorName() {
        return sideSensorName;
    }

    /**
     * Whether the cat is currently climbing
     * @return Whether the cat is currently climbing
     */
    public boolean getIsClimbing() { return isClimbing; }

    /**
     * Returns true if this character is facing right
     *
     * @return true if this character is facing right
     */
    public boolean isFacingRight() {
        return faceRight;
    }

    /**
     * Manually force cat to face right
     * @param faceRight true if we want the cat to face right
     */
    public void setFacingRight(boolean faceRight) { this.faceRight = faceRight; }

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
    public Cat(JsonValue data, float width, float height, boolean ret, Vector2 prev_pos,
               com.badlogic.gdx.graphics.Texture[] arr) {
        // The shrink factors fit the image to a tigher hitbox
        super(data.get(ret?"ret_pos":"pos").getFloat(0),
                prev_pos == null ? data.get(ret?"ret_pos":"pos").getFloat(1) : prev_pos.y,
                width*data.get("shrink").getFloat( 0 ),
                height*data.get("shrink").getFloat( 1 ),
                Orientation.TOP);
        setDensity(data.getFloat("density", 0));
        setFriction(data.getFloat("friction", 0));  /// HE WILL STICK TO WALLS IF YOU FORGET
        setFixedRotation(true);
        normal_texture = arr[0];
        jumping_texture = arr[1];
        sit_texture = arr[4];
        maxspeed = data.getFloat("maxspeed", 0);
        damping = data.getFloat("damping", 0);
        force = data.getFloat("force", 0);
        jump_force = data.getFloat( "jump_force", 0 );
        dash_force = data.getFloat( "dash_force", 0 );;
        jumpDamping = data.getFloat("jump_damping", 0);
        groundSensorName = "catGroundSensor";
        sideSensorName = "catSideSensor";
        sensorShapes = new Array<>();
        this.data = data;
        jump_animated = false;
        int spriteWidth = 50;
        int spriteHeight = 50;
        spriteFrames = TextureRegion.split(arr[2], spriteWidth, spriteHeight);
        spriteFrames2 = TextureRegion.split(arr[3], 47, 32);
        float frameDuration = 0.025f;
        animation = new Animation<>(frameDuration, spriteFrames[0]);
        animation2 = new Animation<>(0.05f, spriteFrames2[0]);
        animationTime2 = 0f;
        animation.setPlayMode(Animation.PlayMode.REVERSED);
        animationTime = 0f;

        // Gameplay attributes
        isGrounded = false;
        canDash = true;
        isJumping = false;
        isMeowing = false;
        if(ret)
            faceRight = false;
        else
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
        JsonValue groundSensorJV = data.get("ground_sensor");
        Fixture a = generateSensor( new Vector2(0, -getHeight() / 2),
                        groundSensorJV.getFloat("shrink",0)*getWidth()/2.0f,
                        groundSensorJV.getFloat("height",0),
                        getGroundSensorName() );

        // Side sensors to help detect for wall climbing
        JsonValue sideSensorJV = data.get("side_sensor");
        Fixture b= generateSensor( new Vector2(-getWidth() / 2, 0),
                        sideSensorJV.getFloat("width", 0),
                        sideSensorJV.getFloat("shrink") * getHeight() / 2.0f,
                        getSideSensorName() );

        generateSensor( new Vector2(getWidth() / 2, 0),
                        sideSensorJV.getFloat("width", 0),
                        sideSensorJV.getFloat("shrink") * getHeight() / 2.0f,
                        getSideSensorName() );

        return true;
    }

    /**
     * Generates a sensor fixture to be used on the Cat.
     *
     * We set friction to 0 to ensure fixture has no physical effects.
     *
     * @param location relative location of the sensor fixture
     * @param hx half-width used for PolygonShape
     * @param hy half-height used for PolygonShape
     * @param name name for the sensor UserData
     * @return
     */
    private Fixture generateSensor(Vector2 location, float hx, float hy, String name) {
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.friction = 0;
        sensorDef.isSensor = true;
        PolygonShape sensorShape = new PolygonShape();
        sensorShape.setAsBox(hx, hy, location, 0.0f);
        sensorDef.shape = sensorShape;

        Fixture sensorFixture = body.createFixture( sensorDef );
        sensorFixture.setUserData(name);
        sensorShapes.add(sensorShape);
        return sensorFixture;
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
        if (getVerticalMovement() == 0f && getIsClimbing()) {
            forceCache.set(0,-getDamping()*getVY());
            body.applyForce(forceCache,getPosition(),true);
        }

        // Velocity too high, clamp it
        if (Math.abs(getVX()) >= getMaxSpeed()) {
            setVX(Math.signum(getMovement())*getMaxSpeed());
        } else {
            forceCache.set(getMovement() * 0.5f,0);
            body.applyForce(forceCache,getPosition(),true);
        }
        // Jump!
        if (isJumping() && !stoppedJumping) {
            forceCache.set(0, jumpMovement);
            body.applyLinearImpulse(forceCache,getPosition(),true);
        }
        if (isDashing() && canDash){
            float jump = 0;
            if (isJumping()){
                jump = (dash_force);
            }
            if(movement > 0){
                forceCache.set((-dash_force),jump);
            }
            else if(movement < 0){
                forceCache.set((dash_force),jump);
            }
            else{
                forceCache.set(0,jump);
            }
            setVY(Math.signum(getVerticalMovement())*(getMaxSpeed()*1.2f));
            setVX(Math.signum(getHorizontalMovement())*(getMaxSpeed()*1.2f));
            body.applyForce(forceCache,getPosition(),true);
            canDash = false;
        }
        if (getIsClimbing()) {
            if (Math.abs(getVY()) >= getMaxSpeed()) {
                setVY(Math.signum(getVerticalMovement())*getMaxSpeed());
            } else {
                forceCache.set(0, getVerticalMovement());
                body.applyForce(forceCache,getPosition(),true);
            }
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
        float x;
        if (faceRight) {
            x = getX() * drawScale.x - 20;
        } else {
            x = getX() * drawScale.x + 30;
        }
        if(isJumping && !jump_animated){
            animation.setPlayMode(Animation.PlayMode.REVERSED);
            animationTime += Gdx.graphics.getDeltaTime();
            TextureRegion currentFrame = animation.getKeyFrame(animationTime);
            canvas.draw(currentFrame,Color.WHITE, origin.x, origin.y,x,getY()*drawScale.y-25, getAngle(),effect,1.0f);
        }
        else if((isMeowing && !isJumping) || animationTime2 != 0){
            animation2.setPlayMode(Animation.PlayMode.NORMAL);
            animationTime2 += Gdx.graphics.getDeltaTime();
            TextureRegion currentFrame2 = animation2.getKeyFrame(animationTime);
            canvas.draw(currentFrame2,Color.WHITE, origin.x, origin.y,x-10,getY()*drawScale.y-15, getAngle(),effect,1.0f);
            if (animationTime2 >= (0.05*5)){
                animationTime2 = 0;
            }
        }
        else {
            if (isJumping) {
                canvas.draw(jumping_texture, Color.WHITE, origin.x, origin.y, x, getY() * drawScale.y - 15, getAngle(), effect, 1.0f);
            } else if (horizontalMovement != 0 || verticalMovement != 0){
                canvas.draw(normal_texture, Color.WHITE, origin.x, origin.y, x, getY() * drawScale.y - 15, getAngle(), effect, 1.0f);
            }
            else{
                canvas.draw(sit_texture, Color.WHITE, origin.x, origin.y, x, getY() * drawScale.y - 15, getAngle(), effect, 1.0f);
            }
        }
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
        for (PolygonShape shape : sensorShapes) {
            canvas.drawPhysics(shape,Color.RED,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
        }

    }
}