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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Queue;
import edu.cornell.gdiac.game.*;
import edu.cornell.gdiac.game.obstacle.*;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import java.util.HashMap;

/**
 * Player avatar for the plaform game.
 *
 * Note that this class returns to static loading.  That is because there are
 * no other subclasses that we might loop through.
 */
public class Cat extends CapsuleObstacle implements Movable {

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //region FIELDS

    /*/////*/
    //region FIELDS: General Data
    /**
     * The initializing data (to avoid magic numbers)
     */
    private static JsonValue objectConstants;

    /**
     * Buffer used to store sound strings to be played by the ActionController
     *
     * All strings entered into this buffer should be one present as a key in the soundMap
     *
     * We use a Set here to prevent duplicate sounds from entering, which cannot be reliably prevented
     * if we used a Queue.
     */
    private Set<String> soundBuffer;
    //endregion
    /*/////*/

    /*/////*/
    //region FIELDS: Movement and Physics

    /**
     * States used to handle the logic of the cat's movement
     */
    private enum State {
        MOVING, JUMPING, CLIMBING, DASHING
    }

    /**
     * State variable representing the current state of movement the cat is in
     */
    private State state;

    /**
     * The maximum character speed
     */
    private final float maxSpeed;

    /**
     * The factor to multiply by the input
     */
    private final float force;

    private final Vector2 forceCache = new Vector2();

    /**
     * The current horizontal movement of the character
     */
    private float horizontalMovement;

    /**
     * The amount to slow the character down
     */
    private final float horizontalDamping;

    /**
     * Whether the jump key is pressed
     */
    private boolean jumpPressed;

    /**
     * Current jump movement of the character
     */
    private float jumpMovement;

    /**
     * The impulse for the character jump
     */
    private final float jumpForce;

    /**
     * Damping multiplier to slow down jump
     */
    private final float jumpDamping;

    private boolean dashPressed;
    private final float dashForce;

    /**
     * Whether we are actively dashing
     */
    private boolean isDashing;

    public boolean canDash;

    /**
     * The current vertical movement of the character
     */
    private float verticalMovement;

    private boolean climbingPressed;

    /**
     * Whether we are climbing on a wall
     */
    private boolean isClimbing;

    private int dashTimer = 0;
    private final Vector2 dashCache = new Vector2();


    /**
     * Identifier to allow us to track the sensor in ContactListener
     */
    private final String groundSensorName;

    /**
     * Identifier to allow us to track side sensor in ContactListener
     */
    private final String sideSensorName;

    /**
     * Whether we are in contact with a wall
     */
    private int wallCount;

    /**
     * Which direction is the character facing
     */
    private boolean facingRight;

    /**
     * Whether we are actively jumping
     */
    private boolean isMeowing;
    /**
     * Whether our feet are on the ground
     */
    private boolean isGrounded;

    /**
     * Cache for internal force calculations
     */
    private ObjectSet<Fixture> groundFixtures;
    /**
     * The current spirit regions that the cat is inside
     */
    private ObjectSet<SpiritRegion> spiritRegions;
    //endregion
    /*/////*/

    /*/////*/
    //region FIELDS: Animation
    /**
     * Counter for failing to switch animation
     */
    private Animation<TextureRegion> jumpAnimation;
    private Animation<TextureRegion> meowAnimation;
    private Animation<TextureRegion> walkAnimation;
    private Animation<TextureRegion> idleAnimation;
    private Animation<TextureRegion> idleStandAnimation;
    private TextureRegion[][] spriteFrames;
    private TextureRegion[][] spriteFrames2;
    private TextureRegion[][] spriteFrames3;
    private TextureRegion[][] spriteFrames4;
    private TextureRegion[][] spriteFrames5;
    private float jumpTime;
    private float meowTime;
    private float walkTime;
    private TextureRegion normal_texture;
    private TextureRegion jumping_texture;
    private TextureRegion sit_texture;
    private float idleTime;
    private float nonMoveTime;
    private float standTime;
    private int time;
    private boolean jump_animated;

    /** List of shapes corresponding to the sensors attached to this body */
    private Array<PolygonShape> sensorShapes;
    private float failedTicks;
    private static final float FAIL_ANIM_TICKS = 30f;
    private Queue<Map.Entry<Vector3, Integer>> dashShadowQueue = new Queue<>();
    private boolean dashShadowFacingRight = true;
    //endregion
    /*/////*/

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //region GETTERS AND SETTERS

    /*/////*/
    //region GETTERS AND SETTERS: General Data
    public static void setConstants(JsonValue constants) {
        objectConstants = constants;
    }

    public Set<String> getSoundBuffer() {
        return soundBuffer;
    }
    //endregion
    /*/////*/

    /*/////*/
    //region GETTERS AND SETTERS: Movement and Physics

    /**
     * Returns how much force to apply to get the cat moving
     * <p>
     * Multiply this by the input to get the movement value.
     *
     * @return how much force to apply to get the cat moving
     */
    public float getForce() {
        return force;
    }

    public void setHorizontalMovement(float value) {
        horizontalMovement = value * getForce();
        if (horizontalMovement < 0) {
            facingRight = false;
        } else if (horizontalMovement > 0) {
            facingRight = true;
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
    }

    /**
     * Returns true if the cat is actively jumping.
     *
     * @return true if the cat is actively jumping.
     */
    public boolean isJumping() {
        return state == State.JUMPING;
    }

    public void setDashPressed(boolean value) {
        dashPressed = value;
    }


    /**
     * Sets up/down movement of this character.
     * <p>
     * This is the result of input times cat force.
     *
     * @param value up/down movement of this character.
     */
    public void setVerticalMovement(float value) {
        verticalMovement = value * getForce();
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
        // Cat has touched the ground. Reset relevant abilities.
        if (value && !isGrounded) {
            setVY(0); // TODO: Cat bounces. Cat should not bounce (duplicates sounds and stuff). Setting restitutions to 0 does not seem to help; this temporary fixes it.
            onGroundedReset();
            soundBuffer.add("metalLanding");
        }
        isGrounded = value;
    }

    /**
     * Utility function that "resets" field relating to when the cat becomes grounded, such as
     * regaining the ability to dash. This has been factored out to decouple the cases where we
     * might want to reset some abilities even if the cat has not strictly become grounded.
     */
    public void onGroundedReset() {
        canDash = true;
        dashTimer = 0;
        jump_animated = false;
        jumpTime = 0;
        meowTime = 0;
        jumpMovement = jumpForce;
    }

    /**
     * Whether the cat is currently climbing
     *
     * @return Whether the cat is currently climbing
     */
    public boolean getIsClimbing() {
        return isClimbing;
    }

    /*
        PHYSICS GETTERS
     */

    /**
     * Returns the name of the ground sensor
     * <p>
     * This is used by ContactListener
     *
     * @return the name of the ground sensor
     */
    public String getGroundSensorName() {
        return groundSensorName;
    }

    public ObjectSet<Fixture> getGroundFixtures() {
        return groundFixtures;
    }

    /**
     * Returns the name of the side sensor
     * <p>
     * This is used by ContactListener
     *
     * @return the name of the side sensor
     */
    public String getSideSensorName() {
        return sideSensorName;
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
     *
     * @return whether the cat is in contact with a wall
     */
    public boolean isWalled() {
        return wallCount > 0;
    }

    public void setMeowing(boolean value) {
        if (value && !isMeowing) {
            soundBuffer.add("meow");
        }
        isMeowing = value;
    }

    /**
     * Returns true if this character is facing right
     *
     * @return true if this character is facing right
     */
    public boolean isFacingRight() {
        return facingRight;
    }

    /**
     * Manually force cat to face right
     *
     * @param facingRight true if we want the cat to face right
     */
    public void setFacingRight(boolean facingRight) {
        this.facingRight = facingRight;
    }

    public ObjectSet<SpiritRegion> getSpiritRegions() {
        return spiritRegions;
    }
    //endregion
    /*/////*/

    //endregion
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new cat avatar with the given physics data
     * <p>
     * The size is expressed in physics units NOT pixels.  In order for drawing to work properly,
     * you MUST set the drawScale. The drawScale converts the physics units to pixels.
     *
     * The size is expressed in physics units NOT pixels.  In order for
     * drawing to work properly, you MUST set the drawScale. The drawScale
     * converts the physics units to pixels.
     *
     */
    public Cat(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale){
        super((float) properties.get("x") + objectConstants.get("offset").getFloat(0),
                (float) properties.get("y") + objectConstants.get("offset").getFloat(1),
                tMap.get("cat").getRegionWidth()/scale.x*objectConstants.get("shrink").getFloat( 0 ),
                tMap.get("cat").getRegionHeight()/scale.y*objectConstants.get("shrink").getFloat( 1 ), Orientation.TOP);
        System.out.println(getDimension());
        setDrawScale(scale);
        setDensity(objectConstants.getFloat("density", 0));
        setFriction(
                objectConstants.getFloat("friction", 0));  /// HE WILL STICK TO WALLS IF YOU FORGET
        setRestitution(objectConstants.getFloat("restitution", 0));
        setFixedRotation(true);
        maxSpeed = objectConstants.getFloat("maxSpeed", 0);
        horizontalDamping = objectConstants.getFloat("horizontalDamping", 0);
        force = objectConstants.getFloat("force", 0);
        jumpForce = objectConstants.getFloat("jumpForce", 0);
        dashForce = objectConstants.getFloat("dashForce", 0);
        jumpDamping = objectConstants.getFloat("jumpDamping", 0);
        groundSensorName = "catGroundSensor";
        sideSensorName = "catSideSensor";
        sensorShapes = new Array<>();
        groundFixtures = new ObjectSet<>();
        spiritRegions = new ObjectSet<>();
        soundBuffer = new HashSet<>();

        jump_animated = false;
        normal_texture = tMap.get("cat");
        jumping_texture = tMap.get("jumpingCat");
        sit_texture = tMap.get("sit");

        spriteFrames = TextureRegion.split(tMap.get("jump_anim").getTexture(), 2048,2048);
        spriteFrames2 = TextureRegion.split(tMap.get("meow_anim").getTexture(), 2048, 2048);
        spriteFrames3 = TextureRegion.split(tMap.get("walk").getTexture(), 2048, 2048);
        spriteFrames4 = TextureRegion.split(tMap.get("idle_anim").getTexture(),2048,2048);
        spriteFrames5 = TextureRegion.split(tMap.get("idle_anim_stand").getTexture(),2048,2048);

        jumpAnimation = new Animation<>(0.025f, spriteFrames[0]);
        meowAnimation = new Animation<>(0.05f, spriteFrames2[0]);
        walkAnimation = new Animation<>(0.15f, spriteFrames3[0]);
        idleAnimation = new Animation<>(0.15f, spriteFrames4[0]);
        idleStandAnimation = new Animation<>(0.15f, spriteFrames5[0]);

        jumpTime = 0f;
        meowTime = 0f;
        walkTime = 0f;
        failedTicks = FAIL_ANIM_TICKS;

        idleTime = 0f;
        nonMoveTime = 0f;
        standTime = 0f;
        time = 0;

        // Gameplay attributes
        state = State.MOVING;
        setGravityScale(2f);
        onGroundedReset();
        isGrounded = true; // We set this to true in constructor to prevent the grounded sound from playing on spawn
        canDash = true;
        jumpPressed = false;
        isMeowing = false;
        facingRight = true;
        setName("cat");
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
        // We only allow the cat to jump when he's on the ground.
        // Double jumping is not allowed.
        //
        // To determine whether or not the cat is on the ground,
        // we create a thin sensor under his feet, which reports
        // collisions with the world but has no collision response.
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
                getSideSensorName());

        generateSensor(new Vector2(getWidth() / 2, 0),
                sideSensorJV.getFloat("width", 0),
                sideSensorJV.getFloat("shrink") * getHeight() / 2.0f,
                getSideSensorName());

        return true;
    }

    /**
     * Generates a sensor fixture to be used on the Cat.
     * <p>
     * We set friction to 0 to ensure fixture has no physical effects.
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
        sensorDef.isSensor = true;
        PolygonShape sensorShape = new PolygonShape();
        sensorShape.setAsBox(hx, hy, location, 0.0f);
        sensorDef.shape = sensorShape;
        System.out.println(name + ": " + location+ "\twidth: " + hx + "\theight: " + hy) ;

        Fixture sensorFixture = body.createFixture(sensorDef);
        sensorFixture.setUserData(name);
        sensorShapes.add(sensorShape);
        return sensorFixture;
    }

    /**
     * Handles STATE of the cat All STATE transitions should be contained here
     */
    public void updateState() {
        failedTicks = Math.min(FAIL_ANIM_TICKS, failedTicks + 1);
        switch (state) {
            case MOVING:
                // MOVING -> JUMPING
                if (isGrounded && jumpPressed) {
                    state = State.JUMPING;
                    soundBuffer.add("jump");
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
                    dashShadowFacingRight = isFacingRight();
                    soundBuffer.add("dash");
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
                    dashShadowFacingRight = isFacingRight();
                    soundBuffer.add("dash");
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
                if (dashTimer >= 7) {
                    state = State.MOVING;
//                    setVX(0);
                    if (getRelativeVelocity().y > 0) {
                        setVY(maxSpeed);
                    }
                    setGravityScale(2f);
                    return;
                }
                break;
        }
    }

    /**
     * Applies the force to the body of this cat
     * <p>
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
                body.applyLinearImpulse(forceCache, getPosition(), true);
            case MOVING:
                if (horizontalMovement == 0){
                    setRelativeVX(getRelativeVelocity().x * 0.5f);
                } else {
                    setRelativeVX(0.84f * (getRelativeVelocity().x + horizontalMovement * 0.06f));
                }
                break;
            case CLIMBING:
                setRelativeVX(0);
                setRelativeVY(verticalMovement / 3f);
                break;
            case DASHING:
                setRelativeVX(dashCache.x);
                setRelativeVY(dashCache.y);
                processDashShadowQueue();
                break;
        }
    }

    private void calculateDashVector() {
        float horizontalForce = horizontalMovement / 1.8f;
        float verticalForce = verticalMovement / 1.8f;
        if (horizontalMovement == 0 && verticalMovement == 0) {
            horizontalForce = (isFacingRight() ? 1 : -1) * getForce() / 2f;
        } else if (horizontalMovement != 0 && verticalMovement != 0) {
            horizontalForce = horizontalForce / (float)Math.sqrt(2);
            verticalForce = verticalForce / (float)Math.sqrt(2);
        }

        dashCache.set(horizontalForce, verticalForce);
    }

    private void processDashShadowQueue() {
        if (dashTimer % 3 == 0) {
            // We're basically using 4-tuples here with a hideous SimpleEntry<Vector3<>, Integer> set-up
            // The dash currently uses the cat's current frame, but it'd be better if we had a way to save the cat's drawing frame whenever a new shadow is added
            // That will best be done by modularizing the drawing code more + making DashShadow a static inner class to store the 5-tuple
            dashShadowQueue.addLast(new SimpleEntry<>(new Vector3(getX() * drawScale.x, getY() * drawScale.y, isFacingRight() ? -1 : 1), 10));
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
        super.update(dt);
    }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(GameCanvas canvas) {
        float effect = facingRight ? -1.0f : 1.0f;
        float x = getX() * drawScale.x;
        float y = getY()* drawScale.y;
        //walking animation
        TextureRegion frame = sit_texture;
        float xOffset = 0;
        float yOffset = 0;

        if (state != State.JUMPING && horizontalMovement != 0) {
            walkAnimation.setPlayMode(Animation.PlayMode.LOOP);
            walkTime += Gdx.graphics.getDeltaTime();
            frame = walkAnimation.getKeyFrame(walkTime);

            nonMoveTime = 0;
        }
        //jump animation
        else if (state == State.JUMPING && !jump_animated && jumpTime < 0.025f*6) {
            jumpAnimation.setPlayMode(Animation.PlayMode.REVERSED);
            jumpTime += Gdx.graphics.getDeltaTime();
            frame = jumpAnimation.getKeyFrame(jumpTime);

            nonMoveTime = 0;
        }
        //meow animation
        else if ((isMeowing && state != State.JUMPING) || meowTime != 0) {
            meowAnimation.setPlayMode(Animation.PlayMode.REVERSED);
            meowTime += Gdx.graphics.getDeltaTime();
            frame = meowAnimation.getKeyFrame(meowTime);
            if (meowTime >= (0.6)){
                meowTime = 0;
                isMeowing = false;
            }
        }

        //sit
        else if(horizontalMovement == 0 && verticalMovement == 0 && !(state == State.JUMPING)){
            if(nonMoveTime >= 10){
                idleAnimation.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);
                idleTime += Gdx.graphics.getDeltaTime();
                frame = idleAnimation.getKeyFrame(idleTime);
            }
            else if(nonMoveTime >= 5){
                nonMoveTime += Gdx.graphics.getDeltaTime();
            }
            else{
                nonMoveTime += Gdx.graphics.getDeltaTime();
                idleStandAnimation.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);
                standTime += Gdx.graphics.getDeltaTime();
                frame = idleStandAnimation.getKeyFrame(standTime);
            }
        } else {
            frame = jumping_texture;
            nonMoveTime = 0;
        }

        Color dashColor = (new Color(0.68f, 0.85f, 0.9f, 1f));
        for (int i = 0; i < dashShadowQueue.size; i++) {
            Map.Entry<Vector3, Integer> shadow = dashShadowQueue.removeFirst();
            Vector3 drawData = shadow.getKey();
            float directionFactor = drawData.z;
            dashColor.a = shadow.getValue() / 10f;
            canvas.draw(frame, dashColor, origin.x, origin.y, drawData.x-directionFactor*frame.getRegionWidth()/drawScale.x/2, drawData.y-frame.getRegionHeight()/drawScale.y/2, 0, directionFactor/drawScale.x, 1f/drawScale.y);
            if (shadow.getValue() - 1 > 0) {
                shadow.setValue(shadow.getValue() - 1);
                dashShadowQueue.addLast(shadow);
            }
        }

        if (failedTicks < FAIL_ANIM_TICKS){
            xOffset += ((float) (Math.sin(-failedTicks/2) * Math.exp(-failedTicks/30)))*drawScale.x/2;
            Color c = new Color(1, 0 , 0, 0.5f - Math.abs(failedTicks - FAIL_ANIM_TICKS/2)/FAIL_ANIM_TICKS);
            canvas.draw(frame, c, origin.x, origin.y, x - effect*frame.getRegionWidth()/drawScale.x/2 + xOffset, y-frame.getRegionHeight()/drawScale.y/2, 0, effect/drawScale.x, 1f/drawScale.y);
        }

        canvas.draw(frame, Color.WHITE, origin.x, origin.y, x - effect*frame.getRegionWidth()/drawScale.x/2, y-frame.getRegionHeight()/drawScale.y/2, 0, effect/drawScale.x, 1f/drawScale.y);


//            canvas.draw(frame, c, origin.x, origin.y, x + xOffset, y + yOffset, getAngle());
//            canvas.draw(frame, c, origin.x, origin.y, x + xOffset, y + yOffset, getAngle(), effect * flip, 1.0f);
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
        for (PolygonShape shape : sensorShapes) {
            float xTranslate = (canvas.getCamera().getX() - canvas.getWidth() / 2) / drawScale.x;
            float yTranslate = (canvas.getCamera().getY() - canvas.getHeight() / 2) / drawScale.y;
            canvas.drawPhysics(shape, Color.RED, getX() - xTranslate, getY() - yTranslate,
                    getAngle(), drawScale.x, drawScale.y);
        }
        debugPrint();

    }

    public void debugPrint() {
//        System.out.println("STATE: "+state);
//        System.out.println("GROUNDED: "+isGrounded);
//        System.out.println("DASH TIMER: "+dashTimer);
    }

    public boolean isMovable() {
        return true;
    }

    public void failedSwitch() {
        failedTicks = 0f;
    }

}