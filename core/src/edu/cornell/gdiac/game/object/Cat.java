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
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.game.*;
import edu.cornell.gdiac.game.obstacle.*;

/**
 * Player avatar for the plaform game.
 *
 * Note that this class returns to static loading.  That is because there are
 * no other subclasses that we might loop through.
 */
public class Cat extends CapsuleObstacle implements Moveable {
    private enum State {
        MOVING, JUMPING, CLIMBING, DASHING
    }
    private State state;

    /** The initializing data (to avoid magic numbers) */
    private final JsonValue data;

    private static JsonValue objectConstants;

    /** The factor to multiply by the input */
    private final float force;
    private final float dash_force;
    /** Whether we are actively dashing */
    private boolean isDashing;
    public boolean canDash;
    private Animation<TextureRegion> jump_animation;
    private Animation<TextureRegion> meow_animation;
    private Animation<TextureRegion> walk_animation;
    private TextureRegion[][] spriteFrames;
    private TextureRegion[][] spriteFrames2;
    private TextureRegion[][] spriteFrames3;
    private float jumpTime;
    private float meowTime;
    private float walkTime;
    private Texture normal_texture;
    private Texture jumping_texture;
    private Texture sit_texture;
    private boolean jump_animated;
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


    /** The current vertical movement of the character */
    private float   verticalMovement;
    /** The current horizontal movement of the character */
    private float horizontalMovement;
    /** Current jump movement of the character */
    private float   jumpMovement;

    /** Whether the jump key is pressed*/
    private boolean jumpPressed;
    /** Which direction is the character facing */
    private boolean faceRight;
    /** Whether we are actively jumping */
    private boolean isMeowing;
    /** Whether our feet are on the ground */
    private boolean isGrounded;

    /** Whether we are in contact with a wall */
    private int wallCount;
    /** Whether we are climbing on a wall */
    private boolean isClimbing;

    private boolean climbingPressed;

    private boolean dashPressed;


    /** List of shapes corresponding to the sensors attached to this body */
    private Array<PolygonShape> sensorShapes;

    /** Cache for internal force calculations */
    private final Vector2 forceCache = new Vector2();

    private int dashTimer = 0;
    private final Vector2 dashCache = new Vector2();

    private ObjectSet<Fixture> groundFixtures;


    /*
        MOVEMENT SETTERS
     */

    // NORMAL MOVEMENT
    public void setHorizontalMovement(float value) {
        horizontalMovement = value * getForce();
        if (horizontalMovement < 0) {
            faceRight = false;
        } else if (horizontalMovement > 0) {
            faceRight = true;
        }
    }

    // JUMPING MOVEMENT
    /**
     * Sets whether the jump key is pressed
     *
     * @param value whether the jump key is pressed
     */
    public void setJumpPressed(boolean value) {
        jumpPressed = value;
//        if (isJumping) {
//            jumpMovement *= jumpDamping;
//        }
//        if (!isJumping && !isGrounded()){
//            stoppedJumping = true;
//        }
    }
    public void setMeowing(boolean value){ isMeowing = value;}

    /**
     * Returns true if the cat is actively jumping.
     *
     * @return true if the cat is actively jumping.
     */
    public boolean isJumping() {
        return state == State.JUMPING;
    }

    /////
    /**
     * Sets up/down movement of this character.
     *
     * This is the result of input times cat force.
     *
     * @param value up/down movement of this character.
     */
    public void setVerticalMovement(float value) {
        verticalMovement = value * getForce();
    }

    public void setDashPressed(boolean value) {
        dashPressed = value;
    }



    /**
     * Sets whether the cat is in contact with a wall
     */
    public void incrementWalled() {
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
            body.setGravityScale(0);
        }
        else if (!value && isClimbing) {
            body.setGravityScale(2f);
        }
        isClimbing = value;
    }

    /**
     *
     */
    public void setClimbingPressed(boolean value) {
        climbingPressed = value;
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
        // Cat has touched the ground. Reset relevant abilities.
        if (isGrounded) {
            canDash = true;
            dashTimer = 0;
            jump_animated = false;
            jumpTime = 0;
            meowTime = 0;
            jumpMovement = jump_force;
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
     * Whether the cat is currently climbing
     * @return Whether the cat is currently climbing
     */
    public boolean getIsClimbing() { return isClimbing; }

    /*
        PHYSICAL GETTERS
     */

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
                width*objectConstants.get("shrink").getFloat( 0 ),
                height*objectConstants.get("shrink").getFloat( 1 ),
                Orientation.TOP);
        System.out.println(width);
        System.out.println(height);
        setDensity(objectConstants.getFloat("density", 0));
        setFriction(objectConstants.getFloat("friction", 0));  /// HE WILL STICK TO WALLS IF YOU FORGET
        setFixedRotation(true);
        normal_texture = arr[0];
        jumping_texture = arr[1];
        sit_texture = arr[4];
        maxspeed = objectConstants.getFloat("maxspeed", 0);
        damping = objectConstants.getFloat("damping", 0);
        force = objectConstants.getFloat("force", 0);
        jump_force = objectConstants.getFloat( "jump_force", 0 );
        dash_force = objectConstants.getFloat( "dash_force", 0 );;
        jumpDamping = objectConstants.getFloat("jump_damping", 0);
        groundSensorName = "catGroundSensor";
        sideSensorName = "catSideSensor";
        sensorShapes = new Array<>();
        groundFixtures = new ObjectSet<>();
        this.data = data;

        jump_animated = false;
        normal_texture = arr[0];
        jumping_texture = arr[1];
        sit_texture = arr[4];

        spriteFrames = TextureRegion.split(arr[2], 65, 65);
        spriteFrames2 = TextureRegion.split(arr[3], 62, 42);
        spriteFrames3 = TextureRegion.split(arr[5], 62, 62);

        jump_animation = new Animation<>(0.025f, spriteFrames[0]);
        meow_animation = new Animation<>(0.05f, spriteFrames2[0]);
        walk_animation = new Animation<>(0.15f, spriteFrames3[0]);

        jumpTime = 0f;
        meowTime = 0f;
        walkTime = 0f;

        // Gameplay attributes
        state = State.MOVING;
        setGravityScale(2f);
        isGrounded = false;
        canDash = true;
        jumpPressed = false;
        isMeowing = false;
        if(ret)
            faceRight = false;
        else
            faceRight = true;
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
        JsonValue groundSensorJV = objectConstants.get("ground_sensor");
        Fixture a = generateSensor( new Vector2(0, -getHeight() / 2),
                        groundSensorJV.getFloat("shrink",0)*getWidth()/2.0f,
                        groundSensorJV.getFloat("height",0),
                        getGroundSensorName() );

        // Side sensors to help detect for wall climbing
        JsonValue sideSensorJV = objectConstants.get("side_sensor");
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
     * Handles STATE of the cat
     * All STATE transitions should be contained here
     */
    public void updateState() {
        switch (state) {
            case MOVING:
                // MOVING -> JUMPING
                if (isGrounded && jumpPressed) {
                    state = State.JUMPING;
                    return;
                }
                // MOVING -> CLIMBING
                if (isWalled() && climbingPressed) {
                    state = State.CLIMBING;
                    setGravityScale(0);
                    return;
                }
                // MOVING -> DASHING
                if (dashTimer == 0 && dashPressed) {
                    state = State.DASHING;
                    setGravityScale(0);
                    calculateDashVector();
                    return;
                }
                break;
            case JUMPING:
                // JUMPING -> MOVING
                if (!jumpPressed) {
                    state = State.MOVING;
                    return;
                }
                // JUMPING -> CLIMBING
                if (isWalled() && climbingPressed) {
                    state = State.CLIMBING;
                    setGravityScale(0);
                    return;
                }
                // JUMPING -> DASHING
                if (dashTimer == 0 && dashPressed) {
                    state = State.DASHING;
                    setGravityScale(0);
                    calculateDashVector();
                    return;
                }
                break;
            case CLIMBING:
                // CLIMBING -> MOVING
                if (!isWalled() || !climbingPressed) {
                    state = State.MOVING;
                    setGravityScale(2f);
                    return;
                }
                break;
            case DASHING:
                // DASHING -> MOVING
                dashTimer++;
                if (dashTimer >= 10) {
                    state = State.MOVING;
                    setVX(0);
                    setVY(0);
                    setGravityScale(2f);
                    return;
                }
                break;
        }
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

        // RUNNING
        switch (state) {
            case JUMPING:
                jumpMovement *= jumpDamping;
                forceCache.set(0, jumpMovement);
                body.applyLinearImpulse(forceCache,getPosition(),true);
            case MOVING:
                setRelativeVX(horizontalMovement * 0.25f);
                break;
            case CLIMBING:
                setRelativeVX(0);
                setRelativeVY(verticalMovement / 3f);
                break;
            case DASHING:
                setRelativeVX(dashCache.x);
                setRelativeVY(dashCache.y);
                break;
        }
//        float speedTarget = getMovement() * getMaxSpeed();
//        float speedDif    = speedTarget - getVX();
//        float accelRate   = 5f;
//        float movement    = speedDif * accelRate;
//        forceCache.set(movement, 0);
//        body.applyForce(forceCache, getPosition(), true);
//        setVX(getMovement() * getMaxSpeed());

        // DAMPENING
//        if (getMovement() == 0f) {
//            forceCache.set(-getDamping()*getVX(),0);
//            body.applyForce(forceCache,getPosition(),true);
//        }
//        if (getVerticalMovement() == 0f && getIsClimbing()) {
//            forceCache.set(0,-getDamping()*getVY());
//            body.applyForce(forceCache,getPosition(),true);
//        }
//
//        // Velocity too high, clamp it
//        if (Math.abs(getVX()) >= getMaxSpeed()) {
//            setVX(Math.signum(getMovement())*getMaxSpeed());
//        } else {
//            forceCache.set(getMovement() * 0.5f,0);
//            body.applyForce(forceCache,getPosition(),true);
//        }
//
//
//        // JUMP
//        if (isJumping() && !stoppedJumping) {
//            forceCache.set(0, jumpMovement);
//            body.applyLinearImpulse(forceCache,getPosition(),true);
//        }
//
//        // DASH
//        if (isDashing() && canDash){
//            float jump = 0;
//            if (isJumping()){
//                jump = (dash_force);
//            }
//            if(movement > 0){
//                forceCache.set((-dash_force),jump);
//            }
//            else if(movement < 0){
//                forceCache.set((dash_force),jump);
//            }
//            else{
//                forceCache.set(0,jump);
//            }
//            setVY(Math.signum(getVerticalMovement())*(getMaxSpeed()));
//            setVX(Math.signum(getHorizontalMovement())*(getMaxSpeed()*4f));
////            body.applyForce(forceCache,getPosition(),true);
//            body.setGravityScale(0);
//            canDash = false;
//        }
//
//        // CLIMB
//        if (getIsClimbing()) {
//            setVY(Math.signum(getVerticalMovement())*getMaxSpeed());
////            if (Math.abs(getVY()) >= getMaxSpeed()) {
////
////            } else {
////                forceCache.set(0, getVerticalMovement());
////                body.applyForce(forceCache,getPosition(),true);
////            }
//        }



    }

    private void calculateDashVector() {
        float hMove = horizontalMovement / 2f;
        float vMove = verticalMovement / 2f;
        if (horizontalMovement != 0 && verticalMovement != 0) {
            dashCache.set(hMove / (float)Math.sqrt(2), vMove / (float)Math.sqrt(2));
        } else {
            dashCache.set(hMove, vMove);
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
        super.update(dt);
    }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        float effect = faceRight ? 1.0f : -1.0f;
        float x = getX() * drawScale.x - effect*25;
        float y = getY()*drawScale.y-20;
        //walking animation
        if(!(state == State.JUMPING)&& horizontalMovement != 0){
            walk_animation.setPlayMode(Animation.PlayMode.LOOP_REVERSED);
            walkTime += Gdx.graphics.getDeltaTime();
            TextureRegion currentFrame3 = walk_animation.getKeyFrame(walkTime);
            canvas.draw(currentFrame3,Color.WHITE, origin.x, origin.y,x,y-10, getAngle(),effect,1.0f);
        }
        //jump animation
        else if(state == State.JUMPING && !jump_animated){
            jump_animation.setPlayMode(Animation.PlayMode.REVERSED);
            jumpTime += Gdx.graphics.getDeltaTime();
            TextureRegion currentFrame = jump_animation.getKeyFrame(jumpTime);
            canvas.draw(currentFrame,Color.WHITE, origin.x, origin.y,x,y-15, getAngle(),effect,1.0f);
        }
        //meow animation
        else if((isMeowing && !(state == State.JUMPING)) || meowTime != 0){
            meow_animation.setPlayMode(Animation.PlayMode.REVERSED);
            meowTime += Gdx.graphics.getDeltaTime();
            TextureRegion currentFrame2 = meow_animation.getKeyFrame(meowTime);
            canvas.draw(currentFrame2,Color.WHITE, origin.x, origin.y,x,y, getAngle(),effect,1.0f);
            if (meowTime >= (0.05*5)){
                meowTime = 0;
                isMeowing = false;
            }
        }
        //sit
        else if(horizontalMovement == 0 && verticalMovement == 0){
            canvas.draw(sit_texture, Color.WHITE, origin.x, origin.y, x,y, getAngle(), effect, 1.0f);
        }
        else{
            if ((state == State.JUMPING)) {
                canvas.draw(jumping_texture, Color.WHITE, origin.x, origin.y, x,y, getAngle(), effect, 1.0f);
            }
            else if (horizontalMovement != 0 || verticalMovement != 0){
                canvas.draw(jumping_texture, Color.WHITE, origin.x, origin.y, x,y, getAngle(), effect, 1.0f);
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
        debugPrint();

    }

    public void debugPrint() {
        System.out.println("STATE: "+state);
        System.out.println("GROUNDED: "+isGrounded);
        System.out.println("DASH TIMER: "+dashTimer);
    }

    public boolean isMoveable(){ return true; }

    public ObjectSet<Fixture> getGroundFixtures(){ return groundFixtures; }

    public static void setConstants(JsonValue constants){objectConstants = constants;}
}