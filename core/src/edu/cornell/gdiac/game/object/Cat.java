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

import box2dLight.RayHandler;
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
import java.util.HashSet;
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
        MOVING, JUMPING, CLIMBING, DASHING, WALL_JUMPING
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
    private int wallJumpTimer = 0;
    private int coyoteTimer = 6;


    /**
     * Identifier to allow us to track the sensor in ContactListener
     */
    private final String groundSensorName;

    /**
     * Identifier to allow us to track right side sensor in ContactListener
     */
    private final String rightSensorName;

    /**
     * User data of body fixtures for this cat. Used in Contact Listener
     */
    public static final String bodyName = "catBody";

    /**
     * Whether we are in contact with a wall
     *
     * Identifier to allow us to track left side sensor in ContactListener
     */
    private final String leftSensorName;

    private Fixture rightFixture;
    private Fixture leftFixture;

    /**
     * The number of walls in contact with the right side sensor
     */
    private int rightWallCount;

    /**
     * The number of walls in contact with the left side sensor
     */
    private int leftWallCount;

    /**
     * Which direction is the character facing
     */
    private boolean facingRight;

    /**
     * Whether we are actively meowing
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
    private ObjectMap<String, Integer> spiritRegions;
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
    private float jumpTime;
    private float meowTime;
    private float walkTime;
    private TextureRegion normalTexture;
    private TextureRegion jumpTexture;
    private TextureRegion sitTexture;
    private float stationaryTime;

    /** List of shapes corresponding to the sensors attached to this body */
    private Array<PolygonShape> sensorShapes;
    private float failedSwitchTicks;
    private static final float FAILED_SWITCH_TICKS = 30f;
    private Color failColor = new Color(1, 0, 0, 1);
    private Queue<DashShadow> dashShadowQueue = new Queue<>();
    private Color dashColor = new Color(0.68f, 0.85f, 0.9f, 1f);
    private TextureRegion currentFrame;
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
        if (horizontalMovement != 0) {
            setFacingRight(horizontalMovement > 0);
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
     * Whether the player is currently pressing the climb input.
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
            soundBuffer.add("metal-landing");
        }
        else if (!value) {
            setOrientation(Orientation.VERTICAL);
            if (state == State.MOVING) {
                coyoteTimer--;
            }
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
        jumpTime = 0;
        meowTime = 0;
        jumpMovement = jumpForce;
        setOrientation(Orientation.TOP);
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
     * Returns the name of the right side sensor
     * <p>
     * This is used by ContactListener
     *
     * @return the name of the right side sensor
     */
    public String getRightSensorName() {
        return rightSensorName;
    }

    /**
     * Returns the name of the left side sensor
     * <p>
     * This is used by ContactListener
     *
     * @return the name of the right side sensor
     */
    public String getLeftSensorName() {
        return leftSensorName;
    }

    /**
     * Increments when the right side sensor is in contact with a wall
     */
    public void incrementRightWalled() {
        rightWallCount++;
    }

    /**
     * Decrements when the right side sensor lose contact with a wall
     */
    public void decrementRightWalled() {
        rightWallCount--;
    }

    /**
     * Increments when the right side sensor is in contact with a wall
     */
    public void incrementLeftWalled() {
        leftWallCount++;
    }

    /**
     * Decrements when the right side sensor lose contact with a wall
     */
    public void decrementLeftWalled() {
        leftWallCount--;
    }

    /**
     * Whether the cat is in contact with a wall
     *
     * @return whether the cat is in contact with a wall
     */
    public boolean isWalled() {
        return (isFacingRight() ? rightWallCount : leftWallCount) > 0;
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
        if (state != State.CLIMBING) {
            this.facingRight = facingRight;
        }
    }

    public ObjectMap<String, Integer> getSpiritRegions() {
        return spiritRegions;
    }

    public void addSpiritRegion(SpiritRegion sr){
        String color = sr.getColorString();
        spiritRegions.put(color, spiritRegions.get(color, 0) + 1);
    }

    public void removeSpiritRegion(SpiritRegion sr){
        String color = sr.getColorString();
        if (!spiritRegions.containsKey(color) || spiritRegions.get(color) <= 1) {
            spiritRegions.remove(color);
        } else {
            spiritRegions.put(color, spiritRegions.get(color) - 1);
        }
    }

    //endregion
    /*/////*/

    /*/////*/
    //region GETTERS AND SETTERS: Animation

    /**
     * For flipping texture appropriately
     * @return -1 if facing right, 1 if facing left
     */
    public int getDirectionFactor() {
        return isFacingRight() ? -1 : 1;
    }

    /**
     * To draw the cat texture, this method returns the x-coordinate of the center of its image, scaled
     * appropriately and facing the correct orientation.
     * @return The centered x-coordinate of the current cat frame.
     */
    public float getTextureCenterX() {
        return getDrawX() - getDirectionFactor() * currentFrame.getRegionWidth()/drawScale.x/2;
    }

    /**
     * To draw the cat texture, this method returns the y-coordinate of the center of its image, scaled
     * appropriately and facing the correct orientation.
     * @return The centered x-coordinate of the current cat frame.
     */
    public float getTextureCenterY() {
        return getDrawY() - currentFrame.getRegionHeight()/drawScale.y/2;
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
                tMap.get("cat").getRegionHeight()/scale.y*objectConstants.get("shrink").getFloat( 1 ),
                Orientation.TOP);
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
        rightSensorName = "catRightSensor";
        leftSensorName = "catLeftSensor";
        sensorShapes = new Array<>();
        groundFixtures = new ObjectSet<>();
        spiritRegions = new ObjectMap<>();
        soundBuffer = new HashSet<>();

        normalTexture = tMap.get("cat");
        jumpTexture = tMap.get("jump");
        sitTexture = tMap.get("sit");
        currentFrame = sitTexture;

        walkAnimation = new Animation<>(0.15f, TextureRegion.split(tMap.get("walk-anim").getTexture(),2048,2048)[0]);

        jumpAnimation = new Animation<>(0.025f, TextureRegion.split(tMap.get("jump-anim").getTexture(),2048,2048)[0]);
        meowAnimation = new Animation<>(0.05f, TextureRegion.split(tMap.get("meow-anim").getTexture(),2048,2048)[0]);
        meowAnimation.setPlayMode(Animation.PlayMode.REVERSED);
        idleStandAnimation = new Animation<>(0.15f, TextureRegion.split(tMap.get("idle-stand-anim").getTexture(),2048,2048)[0]);
        idleStandAnimation.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);
        idleAnimation = new Animation<>(0.15f, TextureRegion.split(tMap.get("idle-sit-anim").getTexture(),2048,2048)[0]);
        idleAnimation.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);
        walkAnimation.setPlayMode(Animation.PlayMode.LOOP);

        jumpTime = 0f;
        meowTime = 0f;
        walkTime = 0f;
        stationaryTime = 0f;
        failedSwitchTicks = FAILED_SWITCH_TICKS;

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
        // create the box from our xsuperclass
        if (!super.activatePhysics(world)) {
            return false;
        }

        for (Fixture f : body.getFixtureList()) f.setUserData(bodyName);
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
        rightFixture = generateSensor(new Vector2(getWidth() / 2, 0),
                sideSensorJV.getFloat("width", 0),
                sideSensorJV.getFloat("shrink") * getHeight() / 2.0f,
                getRightSensorName());
        leftFixture = generateSensor(new Vector2(-getWidth() / 2, 0),
                sideSensorJV.getFloat("width", 0),
                sideSensorJV.getFloat("shrink") * getHeight() / 2.0f,
                getLeftSensorName());

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

        Fixture sensorFixture = body.createFixture(sensorDef);
        sensorFixture.setUserData(name);
        sensorShapes.add(sensorShape);
        return sensorFixture;
    }

    public void createLight(RayHandler rayHandler) {
        createPointLight(objectConstants.get("light"), rayHandler);
        getLight().attachToBody(getBody());
        getLight().setSoft(true);
        getLight().setXray(true);
    }

    /**
     * Handles STATE of the cat All STATE transitions should be contained here
     */
    public void updateState() {
        failedSwitchTicks = Math.min(FAILED_SWITCH_TICKS, failedSwitchTicks + 1);
        if (coyoteTimer < 6) {
            coyoteTimer--;
            if (coyoteTimer <= 0) coyoteTimer = 6;
        }
        switch (state) {
            case MOVING:
                // MOVING -> JUMPING
                if (jumpPressed && (isGrounded || coyoteTimer < 4)) {
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
                    soundBuffer.add("dash");
                    return;
                }
                if (jumpPressed && !isGrounded && isWalled()) {
                    forceCache.set(8 * getDirectionFactor(), 10);
                    state = State.WALL_JUMPING;
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
                else if (jumpPressed) {
                    // Kicking off from wall
                    if (Math.signum(horizontalMovement) == getDirectionFactor()) {
                        forceCache.set(13 * Math.signum(horizontalMovement), 6);
                    }
                    // Kicking higher onto wall
                    else {
                        forceCache.set(8 * getDirectionFactor(), 10);
                    }
                    state = State.WALL_JUMPING;
                    setGravityScale(2f);
                }
                break;
            case DASHING:
                // DASHING -> MOVING
                dashTimer++;
                if (dashTimer >= 7) {
                    state = State.MOVING;
//                    setVX(0);
                    if (getRelativeVelocity().y > 0) {
                        setRelativeVY(maxSpeed);
                    }
                    setGravityScale(2f);
                    if (isGrounded) onGroundedReset();
                    return;
                }
                if (getOrientation() != Orientation.VERTICAL) {
                    setOrientation(Orientation.VERTICAL);
                }
                break;
            case WALL_JUMPING:
                wallJumpTimer++;
                if (wallJumpTimer > 5) {
                    state = State.MOVING;
                    wallJumpTimer = 0;
                    return;
                }
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
                float speed;
                if (horizontalMovement == 0){
                    speed = getRelativeVelocity().x * 0.5f;
                } else {
                    speed = 0.84f * (getRelativeVelocity().x + horizontalMovement * 0.06f);
                }
                setRelativeVX(speed);
                break;
            case CLIMBING:
                setRelativeVX(0);
                setRelativeVY(verticalMovement / 5f);
                break;
            case DASHING:
                setRelativeVX(dashCache.x);
                setRelativeVY(dashCache.y);
                addDashShadow();
                break;
            case WALL_JUMPING:
                setRelativeVX(forceCache.x);
                setRelativeVY(forceCache.y);
                break;
        }
    }

    /**
     * Calculates the dash vector - the vector describing the dash force according to the current
     * combination of keys held down - and stores it in <code>dashCache</code>.
     */
    private void calculateDashVector() {
        float horizontalForce = horizontalMovement / 1.8f;
        float verticalForce = verticalMovement / 1.8f;
        if (horizontalMovement == 0 && verticalMovement == 0) {
            // If the player dashes without holding any keys, we increase the force of the dash.
            // Otherwise, the dash itself 'feels' too short.
            horizontalForce = (isFacingRight() ? 1 : -1) * getForce() / 1.6f;
        } else if (horizontalMovement != 0 && verticalMovement != 0) {
            horizontalForce = horizontalForce / (float)Math.sqrt(2);
            verticalForce = verticalForce / (float)Math.sqrt(2);
        }

        dashCache.set(horizontalForce, verticalForce);
    }

    /**
     * Creates a new Dash Shadow according to the cat's current state and adds it to <code>dashShadowQueue</code>
     * to be drawn.
     */
    private void addDashShadow() {
        if (dashTimer % 3 == 0) {
            dashShadowQueue.addLast(new DashShadow(getTextureCenterX(), getTextureCenterY(), getDirectionFactor(), currentFrame));
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
        updateAnimation();
        drawDashShadows(canvas);

        float directionFactor = getDirectionFactor();
        float x = getTextureCenterX();
        float y = getTextureCenterY();

        if (failedSwitchTicks < FAILED_SWITCH_TICKS){
            float xOffset = ((float) (Math.sin(-failedSwitchTicks /2) * Math.exp(-failedSwitchTicks
                    /30)))*drawScale.x/2;
            failColor.a = 0.5f - Math.abs(failedSwitchTicks-FAILED_SWITCH_TICKS/2)/ FAILED_SWITCH_TICKS;
            canvas.draw(currentFrame, failColor, origin.x, origin.y, x + xOffset, y, 0, directionFactor/drawScale.x, 1f/drawScale.y);
        }

        canvas.draw(currentFrame, Color.WHITE, origin.x, origin.y, x, y, 0, directionFactor/drawScale.x, 1f/drawScale.y);
    }

    /**
     * This method updates the animation of the cat by a step, along with handling changes
     * to the cat's active movement (such as going from walking to jumping)
     */
    private void updateAnimation() {
        float delta = Gdx.graphics.getDeltaTime();
        // WALKING
        if (state == State.MOVING && horizontalMovement != 0 && isGrounded()) {
            walkTime += delta;
            currentFrame = walkAnimation.getKeyFrame(walkTime);

            stationaryTime = 0;
        }
        // JUMPING (or in the air, such as falling from a platform)
        else if (!isGrounded()) {
            jumpTime += delta;
            if (!jumpAnimation.isAnimationFinished(jumpTime)) {
                currentFrame = jumpAnimation.getKeyFrame(jumpTime);
            }
            else {
                currentFrame = jumpTexture;
            }

            // Ideally, we don't set these to 0 all the time in the update methods, but otherwise it will grow unbounded
            // An easy optimization will be to set them in the state changes for the movement system
            // But that slightly couples animation logic with movement logic, so we can push that off for now -CJ
            walkTime = 0;
            stationaryTime = 0;
        }
        // MEOWING
        else if ((isMeowing && state == State.MOVING) || meowTime != 0) {
            meowTime += delta;
            currentFrame = meowAnimation.getKeyFrame(meowTime);
            if (meowTime >= (0.6)){
                meowTime = 0;
            }
        }
        // CLIMBING
        else if (state == State.CLIMBING) {

        }
        // SITTING
        else if (state == State.MOVING && horizontalMovement == 0 && verticalMovement == 0) {
            stationaryTime += delta;
            if (stationaryTime < 5) {
                currentFrame = idleStandAnimation.getKeyFrame(stationaryTime);
            }
            else {
                currentFrame = idleAnimation.getKeyFrame(stationaryTime);
            }
        }
    }

    /**
     * Loops through <code>dashShadowQueue</code> and draws the dash shadows. Discards any dash shadow
     * whose timer has expired.
     *
     * @param canvas Drawing context
     */
    private void drawDashShadows(GameCanvas canvas) {
        for (int i = 0; i < dashShadowQueue.size; i++) {
            DashShadow shadow = dashShadowQueue.removeFirst();
            dashColor.a = shadow.timer / 10f;
            canvas.draw(currentFrame, dashColor, origin.x, origin.y, shadow.x, shadow.y, 0, shadow.directionFactor/drawScale.x, 1f/drawScale.y);
            if (shadow.timer - 1 > 0) {
                shadow.timer--;
                dashShadowQueue.addLast(shadow);
            }
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
        failedSwitchTicks = 0f;
    }

    /**
     * Inner data class to hold relevant information for dash shadows
     */
    private class DashShadow {
        public float x;
        public float y;
        public float directionFactor;
        public TextureRegion shadowTexture;
        public int timer = 10;

        /**
         * Creates a new DashShadow object. We use the cat's current state to populate the shadow data,
         * namely the cat's current position, heading, and texture.
         * @param x Current drawing x position of the cat
         * @param y Current drawing y position of the cat
         * @param dir Current directionFactor of the cat (isFacingRight() ? -1 : 1)
         * @param shadowTexture Current texture of the cat
         */
        public DashShadow(float x, float y, float dir, TextureRegion shadowTexture) {
            this.x = x;
            this.y = y;
            this.directionFactor = dir;
            this.shadowTexture = shadowTexture;
        }
    }

}