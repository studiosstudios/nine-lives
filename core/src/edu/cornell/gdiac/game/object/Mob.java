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
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
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

    public static JsonValue objectConstants;

    /**
     * Creates a new mob  with the given physics data
     *
     * The size is expressed in physics units NOT pixels.  In order for
     * drawing to work properly, you MUST set the drawScale. The drawScale
     * converts the physics units to pixels.
     *
     * @param texture the mob texture
     * @param drawScale the draw scale for the mob
     * @param  textureScale the texture scale for the mob
     * @param data  The JSON data for this mob
     *
     */
    public Mob(TextureRegion texture, Vector2 drawScale, Vector2 textureScale, JsonValue data) {
        super(texture.getRegionWidth()/drawScale.x*textureScale.x/2f,
                texture.getRegionHeight()/drawScale.y*textureScale.y);

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
        sensorShapes = new Array<>();


        // Gameplay attributes
        isGrounded = false;
        setFacingRight(!data.getBoolean("facingRight"));
        isAggressive = data.getBoolean("aggressive");
        // setName("mob");

        detectorRay = new MobDetector(this);
    }

    public Mob(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, int tileSize, int levelHeight, Vector2 textureScale){
        super(tMap.get("roboMob").getRegionWidth()/scale.x*textureScale.x/2f,
                tMap.get("roboMob").getRegionHeight()/scale.y*textureScale.y);


        setFixedRotation(true);
        setName("mob");
        setX((float) properties.get("x")/tileSize + objectConstants.get("offset").getFloat(0));
        setY(levelHeight - (float) properties.get("y")/tileSize + objectConstants.get("offset").getFloat(1)-getDimension().y/2);
        setDrawScale(scale);
        setTextureScale(textureScale);
        setTexture(tMap.get("roboMob"));

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
        float xTranslate = (canvas.getCamera().getX()-canvas.getWidth()/2)/drawScale.x;
        float yTranslate = (canvas.getCamera().getY()-canvas.getHeight()/2)/drawScale.y;
        // Draw detectorRay
        if (detectorRay.getPoints().size > 1) {
            canvas.drawLineDebug(detectorRay.getPoints().get(0).sub(xTranslate,yTranslate), detectorRay.getPoints().get(detectorRay.getPoints().size-1).sub(xTranslate,yTranslate), Color.BLUE, getDrawScale().x, getDrawScale().y);
        }

        for (PolygonShape shape : sensorShapes) {
            canvas.drawPhysics(shape,Color.RED,getX()-xTranslate,getY()-yTranslate,getAngle(),drawScale.x,drawScale.y);
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