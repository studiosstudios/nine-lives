package edu.cornell.gdiac.game.object;

import box2dLight.PositionalLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.utils.ObjectMap;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.*;
import com.badlogic.gdx.Gdx;
import edu.cornell.gdiac.util.Direction;

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
    protected boolean activating;
    /** The unique string id of this Activator */
    protected String id;
    /** Array of animation frames */
    private TextureRegion[][] spriteFrames;
    /** How long the activator has been animating */
    private float animationTime;
    /** Shape of the sensor that presses this activator */
    private Texture topTexture;
    private TextureRegion bottomTexture;
    private PolygonShape sensorShape;
    /** The number of objects pressing on this activator */
    public int numPressing;
    /** Whether the camera will pan on next activation */
    private boolean pan;
    /** If pressing this activator for the first time should pan the camera */
    private boolean shouldPan;
    private Color color = new Color();
    /** direction of this button */
    private Direction dir;
    private String biome;
    private boolean prevPressed;

    /**
     * @return true if the activator is currently activating
     */
    public boolean isActivating(){ return activating; }

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
     * @param textureScale   Texture scale for rescaling texture
     */
    public Activator(ObjectMap<String, Object> properties, String texture_name, String base_name, HashMap<String, TextureRegion> tMap, Vector2 scale, Vector2 textureScale, String biome, boolean resize){
        super(objectConstants.get("body_shape").asFloatArray());
        topTexture = tMap.get(texture_name).getTexture();
        spriteFrames = TextureRegion.split(topTexture, 256,256);
        this.biome = biome;
        setTextureScale(textureScale);

        //i am so sorry for how i am doing this
        if (biome.equals("forest")) {
            if (resize) {
                setTextureScale(textureScale.x / 2f, textureScale.y / 2f);
                origin.set(-128, -128);
            } else {
                origin.set(0, -8);
            }
        }
        bottomTexture = tMap.get(base_name);
        setRestitution(0);
        float totalAnimationTime = 0.2f;
        animation = new Animation<>(totalAnimationTime/spriteFrames[0].length, spriteFrames[0]);
        setBodyType(properties.containsKey("attachName") ? BodyDef.BodyType.DynamicBody : BodyDef.BodyType.StaticBody);
        animation.setPlayMode(Animation.PlayMode.REVERSED);
        animationTime = 0f;

        setAngle((float) ((float) properties.get("rotation") * Math.PI/180));
        dir = Direction.angleToDir((int) ((float) properties.get("rotation")));
        Vector2 offset = new Vector2(objectConstants.get("offset").getFloat(0), objectConstants.get("offset").getFloat(1));
        Direction.rotateVector(offset, dir);
        setX((float) properties.get("x") + offset.x);
        setY((float) properties.get("y") + offset.y);

        setDrawScale(scale);
        setFixedRotation(true);

        id = (String) properties.get("id");
//        setX((float) properties.get("x")+objectConstants.get("offset").getFloat(0));
//        setY((float) properties.get("y")+objectConstants.get("offset").getFloat(1));
        color.set((Color) properties.get("color", Color.RED));
        pan = (boolean) properties.get("shouldPan", false);
        activating = false;
        prevPressed = false;
    }

    @Override
    public void createLight(RayHandler rayHandler) {
        createPointLight(objectConstants.get("light"), rayHandler);
        // Slight offset here because activators are weird for some reason... Along with the rotation
        ((PositionalLight) getLight()).attachToBody(getBody(), 0.4f, 0.5f, Direction.dirToDegrees(dir));
        getLight().setSoft(true);
        getLight().setXray(true);
    }

    @Override
    public void draw(GameCanvas canvas){
        TextureRegion currentFrame;
        if (prevPressed ^ isPressed()) animationTime = 0;
        prevPressed = isPressed();
        if(isPressed()){
            animation.setPlayMode(Animation.PlayMode.NORMAL);
            animationTime += Gdx.graphics.getDeltaTime();
            currentFrame = animation.getKeyFrame(animationTime);
        }
        else {
            animation.setPlayMode(Animation.PlayMode.REVERSED);
            animationTime += Gdx.graphics.getDeltaTime();
            currentFrame = animation.getKeyFrame(animationTime);
        }
        float x = (getX() - 0.5f)*drawScale.x;

//        System.out.println();
        if (Math.round(Math.toDegrees(getAngle())) == 180) {
            x = x + drawScale.x;
        }

        canvas.draw(currentFrame, color, origin.x, origin.y, x, getY()*drawScale.y, getAngle(), textureScale.x,textureScale.y);
        if (biome.equals("metal")) canvas.draw(bottomTexture, Color.WHITE, origin.x, origin.y, x, (getY())*drawScale.y, getAngle(), textureScale.x, textureScale.y);
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
        sensorFixture.setRestitution(0);
        sensorFixture.setFriction(0);
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
//        float xTranslate = (canvas.getCamera().getX()-canvas.getWidth()/2)/drawScale.x;
//        float yTranslate = (canvas.getCamera().getY()-canvas.getHeight()/2)/drawScale.y;
        canvas.drawPhysics(sensorShape,Color.RED,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
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

    @Override
    public ObjectMap<String, Object> storeState(){
        ObjectMap<String, Object> stateMap = super.storeState();
        stateMap.put("activating", activating);
        return stateMap;
    }

    public void loadState(ObjectMap<String, Object> stateMap) {
        super.loadState(stateMap);
        activating = (boolean) stateMap.get("activating");
    }
}
