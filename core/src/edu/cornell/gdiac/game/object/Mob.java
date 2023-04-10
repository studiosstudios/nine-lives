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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
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

    /** walking animation */
    private Animation<TextureRegion> walkAnimation;

    private float walkTime;
    private TextureRegion[][] spriteFrames;
    /** The factor to multiply by the input */
    private final float force;
    /** The amount to slow the character down */
    private final float damping;
    /** The maximum character speed */
    private final float maxspeed;
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
    /** The detector ray attached to this mob */
    public MobDetector detectorRay;


    /**
     * Returns left/right movement of this character.
     *
     * This is the result of input times mob force.
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
     * Creates a new mob  with the given physics data
     *
     * The size is expressed in physics units NOT pixels.  In order for
     * drawing to work properly, you MUST set the drawScale. The drawScale
     * converts the physics units to pixels.
     *
     * Note that the animation consists of multiple textures so we pass in the width/height
     * for scaling and constructing the capsule.
     *
     * @param animationTexture the mob animation texture
     * @param textureWidth the mob's animation texture's width
     * @param textureHeight the mob's animation texture's height
     * @param drawScale the draw scale for the mob
     * @param textureScale the texture scale for the mob
     * @param data  The JSON data for this mob
     *
     */
    public Mob(Texture animationTexture, float textureWidth, float textureHeight, Vector2 drawScale, Vector2 textureScale, JsonValue data) {
        super(textureWidth/drawScale.x*textureScale.x/2f,
                textureHeight/drawScale.y*textureScale.y);
        setFixedRotation(true);
        setName("mob");
        setX(data.get("pos").getFloat(0));
        setY(data.get("pos").getFloat(1));
        setDrawScale(drawScale);
        setTextureScale(textureScale);
        walkTime = 0f;
        spriteFrames = TextureRegion.split(animationTexture, 2048, 2048);
        walkAnimation = new Animation<>(0.15f, spriteFrames[0]);

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

    /**
     * Returns the sensor name of the mob
     *
     * @return sensorName
     */
    public static String getSensorName() {
        return sensorName;
    }

    /**
     * Returns the detector ray of the mob
     *
     * @return detectorRay
     */
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
            System.out.println("activate physics");
            return false;
        }
        System.out.println("activate physics");
        body.setUserData(this);
        return true;
    }

    /**
     * Applies the force to the body of this mob
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
        float y = getY()*drawScale.y-24f;
        walkAnimation.setPlayMode(Animation.PlayMode.LOOP_REVERSED);
        walkTime += Gdx.graphics.getDeltaTime();
        TextureRegion currentFrame = walkAnimation.getKeyFrame(walkTime);
        canvas.draw(currentFrame,Color.WHITE, origin.x, origin.y,x,y, getAngle(),effect * textureScale.x,textureScale.y);
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