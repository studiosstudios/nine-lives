package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.utils.ObjectMap;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.*;
import com.badlogic.gdx.Gdx;

import java.util.HashMap;

/**
 * An abstract class that represents objects that can be pressed and activate other objects. The exact behaviour of
 * how an activator becomes active must be implemented by the inheritor.
 */
public abstract class Activator extends PolygonObstacle {
    /** Constants that are shared between all instances of this class */
    protected static JsonValue objectConstants;
    /** Filmstrip */
    protected Animation<TextureRegion> animation;
    /** If the activator is activating objects */
    protected boolean active;
    /** The unique string id of this Activator */
    protected String id;
    /** Array of animation frames */
    private TextureRegion[][] spriteFrames;
    /** How long the activator has been animating */
    private float animationTime;
    /** Shape of the sensor that presses this activator */
    private PolygonShape sensorShape;
    /** The number of objects pressing on this activator */
    public int numPressing;
    /** Whether the camera will pan on next activation */
    private boolean pan;
    /** If pressing this activator for the first time should pan the camera */
    private boolean shouldPan;

    /**
     * @return true if the activator is currently activating
     */
    public boolean isActivating(){ return active; }

    /**
     * @return true if an object is pressing this activator
     */
    public boolean isPressed(){ return numPressing > 0; }

    /**
     * @return ID of this activator
     */
    public String getID(){ return id; }

    /** A new object is pressing the activator */
    public void addPress() { numPressing++; }

    /** An object has stopped pressing the activator */
    public void removePress() { numPressing--; }

    /**
     * Updates the active state of this activator. This is called every game loop, and is the
     * primary method to specify for inheritors.
     */
    public abstract void updateActivated();

    /**
     * Creates a new Activator object.
     *
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param tileSize       Tile size of the Tiled map for loading positions
     * @param levelHeight    Height of level (in grid cell units) for loading y position
     * @param textureScale   Texture scale for rescaling texture
     */

    public Activator(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, int tileSize, int levelHeight, Vector2 textureScale){
        super(objectConstants.get("body_shape").asFloatArray());
        setDrawScale(scale);
        int spriteWidth = 32;
        int spriteHeight = 32;
        setTextureScale(textureScale);
        spriteFrames = TextureRegion.split(tMap.get("button_anim").getTexture(), spriteWidth, spriteHeight);
        float frameDuration = 0.2f;
        animation = new Animation<>(frameDuration, spriteFrames[0]);
        setBodyType(BodyDef.BodyType.StaticBody);
        animation.setPlayMode(Animation.PlayMode.REVERSED);
        animationTime = 0f;

        setDrawScale(scale);
        setTexture(tMap.get("button"));
        setFixedRotation(true);

        id = (String) properties.get("id");
        setX((float) properties.get("x")/tileSize+objectConstants.get("offset").getFloat(0));
        setY(levelHeight - (float) properties.get("y")/tileSize+objectConstants.get("offset").getFloat(1));
        pan = (boolean) properties.get("shouldPan", false);
        active = false;
    }

    @Override
    public void draw(GameCanvas canvas){
        if(isPressed()){
            animation.setPlayMode(Animation.PlayMode.REVERSED);
            animationTime += Gdx.graphics.getDeltaTime();
            TextureRegion currentFrame = animation.getKeyFrame(animationTime);
            canvas.draw(currentFrame, getX()*drawScale.x,getY()*drawScale.x);
        }
        else {
            animation.setPlayMode(Animation.PlayMode.NORMAL);
            animationTime += Gdx.graphics.getDeltaTime();
            TextureRegion currentFrame = animation.getKeyFrame(animationTime);
            canvas.draw(currentFrame, getX()*drawScale.x,getY()*drawScale.x);
        }
    }

    /**
     * Creates the physics Body(s) for this object, adding them to the world.
     * Creates activator sensor.
     * @param world Box2D world to store body
     *
     * @return true if object allocation succeeded
     */
    public boolean activatePhysics(World world){
        if (!super.activatePhysics(world)) {
            return false;
        }
        for (Fixture fd : body.getFixtureList()){
            fd.setUserData(this);
        }
        //create top sensor
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = 0;
        sensorDef.isSensor = true;
        sensorShape = new PolygonShape();
        sensorShape.set(objectConstants.get("sensor_shape").asFloatArray());
        sensorDef.shape = sensorShape;

        Fixture sensorFixture = body.createFixture( sensorDef );
        sensorFixture.setUserData(this);

        return true;
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
        canvas.drawPhysics(sensorShape,Color.RED,getX()-xTranslate,getY()-yTranslate,getAngle(),drawScale.x,drawScale.y);
    }

    /**
     * Sets the shared constants for all instances of this class.
     * @param constants JSON storing the shared constants.
     */
    public static void setConstants(JsonValue constants) { objectConstants = constants; }

    public boolean getPan(){
        return pan;
    }
    public void setPan(boolean p){
        pan = p;
    }
}
