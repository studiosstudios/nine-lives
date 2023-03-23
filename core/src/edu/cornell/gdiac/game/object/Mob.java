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
public class Mob extends CapsuleObstacle {
    /** The initializing data (to avoid magic numbers) */
    private final JsonValue data;

    /** The factor to multiply by the input */
    private final float force;
    /** The amount to slow the character down */
    private final float damping;
    /** The maximum character speed */
    private final float maxspeed;
//    /** Identifier to allow us to track the sensor in ContactListener */
//    private final String groundSensorName;
//    /** Identifier to allow us to track side sensor in ContactListener */
//    private final String sideSensorName;

    /** The current horizontal movement of the character */
    private float   movement;
    /** The current vertical movement of the character */
    private float   verticalMovement;
    /** Current jump movement of the character */
    private float horizontalMovement;
    /** Which direction is the character facing */
    private boolean faceRight;

    /** Whether our feet are on the ground */
    private boolean isGrounded;

    /** Whether we are in contact with a wall */
    private int wallCount;

    /** List of shapes corresponding to the sensors attached to this body */
    private Array<PolygonShape> sensorShapes;
    private PolygonShape sensorShape;


    /** Cache for internal force calculations */
    private final Vector2 forceCache = new Vector2();

    /** Whether the mob is an aggressive AI */
    private Boolean isAggressive;

    private static final String sensorName = "mobsensor";

    public MobDetector detectorRay;


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


    /**
     * Returns true if the cat is on the ground.
     *
     * @return true if the cat is on the ground.
     */
    public boolean isGrounded() {
        return isGrounded;
    }

//    /**
//     * Sets whether the cat is on the ground.
//     *
//     * @param value whether the cat is on the ground.
//     */
//    public void setGrounded(boolean value) {
//        isGrounded = value;
//        if (isGrounded) {
//            canDash = true;
//            jumpMovement = jump_force;
//            stoppedJumping = false;
//        }
//    }

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

//    /**
//     * Returns the name of the ground sensor
//     *
//     * This is used by ContactListener
//     *
//     * @return the name of the ground sensor
//     */
//    public String getGroundSensorName() {
//        return groundSensorName;
//    }

//    /**
//     * Returns the name of the side sensor
//     *
//     * This is used by ContactListener
//     *
//     * @return the name of the side sensor
//     */
//    public String getSideSensorName() {
//        return sideSensorName;
//    }


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
     * Returns true if this character is aggressive
     *
     * @return true if this character is aggressive
     */
    public boolean isAggressive() {
        return isAggressive;
    }

    /**
     * Creates a new cat avatar with the given physics data
     *
     * The size is expressed in physics units NOT pixels.  In order for
     * drawing to work properly, you MUST set the drawScale. The drawScale
     * converts the physics units to pixels.
     *
     * @param data  	The physics constants for this cat
     */
    public Mob(TextureRegion texture, Vector2 drawScale, Vector2 textureScale, JsonValue data) {
        super(texture.getRegionWidth()/drawScale.x*textureScale.x/2f,
                texture.getRegionHeight()/drawScale.y*textureScale.y);

//        setBodyType(BodyDef.BodyType.DynamicBody);
        setFixedRotation(true);
        setName("mob");
        setX(data.get("pos").getFloat(0));
        setY(data.get("pos").getFloat(1));
        setDrawScale(drawScale);
        setTextureScale(textureScale);
        setTexture(texture);

        setDensity(data.getFloat("density", 0));
        setFriction(data.getFloat("friction", 0));  /// HE WILL STICK TO WALLS IF YOU FORGET
        setFixedRotation(true);

        maxspeed = data.getFloat("maxspeed", 0);
        damping = data.getFloat("damping", 0);
        force = data.getFloat("force", 0);
        sensorShapes = new Array<>();

        this.data = data;

        // Gameplay attributes
        isGrounded = false;
        setFacingRight(!data.getBoolean("facingRight"));
        isAggressive = data.getBoolean("aggressive");
        // setName("mob");

        detectorRay = new MobDetector(this);
    }

    public static String getSensorName() {
        return sensorName;
    }

    public MobDetector getDetectorRay() { return detectorRay; }


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
        body.setUserData(this);
//        FixtureDef sensorDef = new FixtureDef();
//        sensorDef.density = 0;
//        sensorDef.isSensor = true;
//        sensorShape = new PolygonShape();
//        // TODO: sensor shape
////        sensorShape.set(new Vector2[0]);
//        sensorDef.shape = sensorShape;
//
//        Fixture sensorFixture = body.createFixture( sensorDef );
//        sensorFixture.setUserData(getSensorName());

        // Ground Sensor
        // -------------
        // We only allow the cat to jump when he's on the ground.
        // Double jumping is not allowed.
        //
        // To determine whether or not the cat is on the ground,
        // we create a thin sensor under his feet, which reports
        // collisions with the world but has no collision response.
//        JsonValue groundSensorJV = data.get("ground_sensor");
//        Fixture a = generateSensor( new Vector2(0, -getHeight() / 2),
//                groundSensorJV.getFloat("shrink",0)*getWidth()/2.0f,
//                groundSensorJV.getFloat("height",0),
//                getGroundSensorName() );
//
//        // Side sensors to help detect for wall climbing
//        JsonValue sideSensorJV = data.get("side_sensor");
//        Fixture b= generateSensor( new Vector2(-getWidth() / 2, 0),
//                sideSensorJV.getFloat("width", 0),
//                sideSensorJV.getFloat("shrink") * getHeight() / 2.0f,
//                getSideSensorName() );
//
//        generateSensor( new Vector2(getWidth() / 2, 0),
//                sideSensorJV.getFloat("width", 0),
//                sideSensorJV.getFloat("shrink") * getHeight() / 2.0f,
//                getSideSensorName() );

        return true;
    }

//    /**
//     * Generates a sensor fixture to be used on the Cat.
//     *
//     * We set friction to 0 to ensure fixture has no physical effects.
//     *
//     * @param location relative location of the sensor fixture
//     * @param hx half-width used for PolygonShape
//     * @param hy half-height used for PolygonShape
//     * @param name name for the sensor UserData
//     * @return
//     */
//    private Fixture generateSensor(Vector2 location, float hx, float hy, String name) {
//        FixtureDef sensorDef = new FixtureDef();
//        sensorDef.friction = 0;
//        sensorDef.isSensor = true;
//        PolygonShape sensorShape = new PolygonShape();
//        sensorShape.setAsBox(hx, hy, location, 0.0f);
//        sensorDef.shape = sensorShape;
//
//        Fixture sensorFixture = body.createFixture( sensorDef );
//        sensorFixture.setUserData(name);
//        sensorShapes.add(sensorShape);
//        return sensorFixture;
//    }


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
            forceCache.set(getMovement() * 0.5f,0);
            body.applyForce(forceCache,getPosition(),true);
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
        canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y,getAngle(),effect * textureScale.x, textureScale.y);
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
        // Draw detectorRay
        if (detectorRay.getPoints().size > 1) {
            canvas.drawLineDebug(detectorRay.getPoints().get(0), detectorRay.getPoints().get(detectorRay.getPoints().size-1), Color.BLUE, getDrawScale().x, getDrawScale().y);
        }

        for (PolygonShape shape : sensorShapes) {
            canvas.drawPhysics(shape,Color.RED,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
        }
    }
}