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
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import edu.cornell.gdiac.game.*;
import edu.cornell.gdiac.game.obstacle.*;

import java.util.HashMap;

/**
 * Player avatar for the plaform game.
 *
 * Note that this class returns to static loading.  That is because there are
 * no other subclasses that we might loop through.
 */
public class Mob extends CapsuleObstacle {
    /** The initializing data (to avoid magic numbers) */
//    private final JsonValue data;

    /** walking animation */
    private Animation<TextureRegion> walkAnimation;

    private float walkTime;
    private static TextureRegion[][] labSpriteFrames;
    private static TextureRegion[][] forestSpriteFrames;
    /** The factor to multiply by the input */
//    private final float force;
    /** The amount to slow the character down */
    private final float damping;
    /** The maximum character speed */
    private final float maxspeed;
    /** The current horizontal movement of the character */
    private float   movement;
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
    /** Height scale for mob */
    public static final float H_SCALE = 0.9f;


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
     * Returns ow hard the brakes are applied to get a mob to stop moving
     *
     * @return ow hard the brakes are applied to get a mob to stop moving
     */
    public float getDamping() {
        return damping;
    }

    /**
     * Returns the upper limit on mob left-right movement.
     *
     * This does NOT apply to vertical movement.
     *
     * @return the upper limit on mob left-right movement.
     */
    public float getMaxSpeed() {
        return maxspeed;
    }

    /**
     * Returns true if this mob is facing right
     *
     * @return true if this mob is facing right
     */
    public boolean isFacingRight() {
        return faceRight;
    }

    /**
     * Manually force cat to face right
     * @param faceRight true if we want the mob to face right
     */
    public void setFacingRight(boolean faceRight) { this.faceRight = faceRight; }

    /**
     * Returns true if this mob is aggressive
     *
     * @return true if this mob is aggressive
     */
    public boolean isAggressive() {
        return isAggressive;
    }

    public static JsonValue objectConstants;

    /**
     * Creates a new Mob object.
     *
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param textureScale   Texture scale for rescaling texture
     */

    public Mob(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, Vector2 textureScale, String biome){
        super(objectConstants.get("scale").getFloat(0), 2*objectConstants.get("scale").getFloat(1));

        setFixedRotation(true);
        setName("mob");
        setX((float) properties.get("x") + objectConstants.get("offset").getFloat(0));
        setY((float) properties.get("y") + objectConstants.get("offset").getFloat(1)-getDimension().y/2);
        setDrawScale(scale);
        setTextureScale(textureScale);
        walkTime = 0f;
        if (labSpriteFrames == null) labSpriteFrames = TextureRegion.split(tMap.get("robot-anim").getTexture(), 256, 256);
        if (forestSpriteFrames == null) forestSpriteFrames = TextureRegion.split(tMap.get("forest-mob-anim").getTexture(), 208, 256);
        if (biome.equals("metal")) {
            walkAnimation = new Animation<>(0.1f, labSpriteFrames[0]);
        } else {
            walkAnimation = new Animation<>(0.1f, forestSpriteFrames[0]);
        }
        setTexture(walkAnimation.getKeyFrames()[0]);

        setDensity(objectConstants.getFloat("density", 0));
        setFriction(objectConstants.getFloat("friction", 0));  /// HE WILL STICK TO WALLS IF YOU FORGET
        setFixedRotation(true);

        sensorShapes = new Array<>();

        // Gameplay attributes
        isGrounded = false;
        setFacingRight((boolean) properties.get("facingRight", true));
        isAggressive = (boolean) properties.get("aggressive", false);
        maxspeed = (float) properties.get("maxspeed", 0f);
        damping = (float) properties.get("damping", 0f);
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
            return false;
        }
        body.setUserData(this);
        return true;
    }

    /**
     * Creates PointLight for with soft and xray true
     * @param rayHandler Ray Handler associated with the currently active box2d world
     */
    public void createLight(RayHandler rayHandler) {
        createPointLight(objectConstants.get("light"), rayHandler);
        getLight().attachToBody(getBody());
        getLight().setSoft(true);
        getLight().setXray(true);
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
        float x = getX() * drawScale.x;
        float y = getY()*drawScale.y;
        walkAnimation.setPlayMode(Animation.PlayMode.LOOP_REVERSED);
        walkTime += Gdx.graphics.getDeltaTime();
        TextureRegion currentFrame = walkAnimation.getKeyFrame(walkTime);
        canvas.draw(currentFrame,Color.WHITE, origin.x, origin.y,x,y, getAngle(),effect * textureScale.x,textureScale.y * 0.9f);
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
//        float xTranslate = (canvas.getCamera().getX()-canvas.getWidth()/2)/drawScale.x;
//        float yTranslate = (canvas.getCamera().getY()-canvas.getHeight()/2)/drawScale.y;
        // Draw detectorRay
        if (detectorRay.getPoints().size > 1) {
            canvas.drawLineDebug(detectorRay.getPoints().get(0).sub(0,0), detectorRay.getPoints().get(detectorRay.getPoints().size-1).sub(0,0), Color.BLUE, getDrawScale().x, getDrawScale().y);
        }

        for (PolygonShape shape : sensorShapes) {
            canvas.drawPhysics(shape,Color.RED,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
        }
    }

    public ObjectMap<String, Object> storeState(){
        ObjectMap<String, Object> stateMap = super.storeState();
        stateMap.put("faceRight", faceRight);
        return stateMap;
    }

    public void loadState(ObjectMap<String, Object> stateMap){
        super.loadState(stateMap);
        faceRight = (boolean) stateMap.get("faceRight");
    }


    /**
     * Sets the shared constants for all instances of this class.
     * @param constants JSON storing the shared constants.
     */
    public static void setConstants(JsonValue constants) {objectConstants = constants;}
}