package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;
import edu.cornell.gdiac.util.Direction;

import java.util.HashMap;

public class Goal extends BoxObstacle
{
    /** The origin position of the goal */
    protected Vector2 origin;
    /** Whether this goal is active or not */
    private boolean active;
    /** width of the door when fully closed */
    private final float width;
    /** height of the door when fully closed */
    private final float height;
    /** x position of the door when fully closed */
    private final float x;
    /** y position of the door when fully closed */
    private final float y;
    private int textureSize;
    private TextureRegion top;

    private TextureRegion bottom;

    private TextureRegion middle;

    private static float shrink;
    /** The sensor shape for this goal */
    private PolygonShape sensorShape;
    /** The constants for the goal */
    protected static JsonValue objectConstants;
    private static final String sensorName = "goalobjsensor";
    /** The texture for the non-active goal */
    private TextureRegion texture;
    /** The texture for the active goal */
    private TextureRegion activeTexture;
    /** The frames of the non-active goal animation */
    private TextureRegion[][] spriteFrames;
    /** The frames of the active goal animation */
    private TextureRegion[][] activeSpriteFrames;
    /** How long the goal has been animating */
    private float animationTime;
    /** Filmstrip of non-active goal animation */
    private Animation<TextureRegion> animation;
    /** Filmstrip of active goal animation */
    private Animation<TextureRegion> active_animation;

    /**
     * Creates a new Door object.
     *
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param textureSize    Size of texture in pixels
     */
    public Goal(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, int textureSize){
        this((float) properties.get("width"), (float) properties.get("height"),
                properties, tMap, scale, textureSize);
    }

    /**
     * Creates a new Door object.
     *
     * @param width          Width of the door
     * @param height         Height of the door
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param textureSize    Size of texture in pixels
     */
    public Goal(float width, float height, ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, int textureSize){
        super(width, height);
//        super(32/scale.x, 64/scale.y);

        // Split the texture
        TextureRegion[][] tiles = tMap.get("goal-active").split(tMap.get("goal-active").getTexture(), textureSize, textureSize);
        System.out.println(tiles);
        top = tiles[0][2];
        middle = tiles[0][1];
        bottom = tiles[0][0];

//        this.textureSize = textureSize;
//        setDrawScale(scale);

        active = false;
        setTextureScale(textureScale);
        int spriteWidth = 1024;
        int spriteHeight = 2048;
//        this.texture = tMap.get("goal");
//        this.activeTexture = tMap.get("goal");
//        spriteFrames = TextureRegion.split(tMap.get("checkpoint-anim").getTexture(), spriteWidth, spriteHeight);
//        activeSpriteFrames = TextureRegion.split(tMap.get("checkpoint-active-anim").getTexture(), spriteWidth, spriteHeight);
//        float frameDuration = 0.1f;
//
//        animation = new Animation<>(frameDuration, spriteFrames[0]);
//        active_animation = new Animation<>(frameDuration, activeSpriteFrames[0]);

//        animation.setPlayMode(Animation.PlayMode.LOOP);
//        animationTime = 0f;
        setMass(0);
        setName("goal");
        setDrawScale(scale);
        setSensor(true);
        x =(float) properties.get("x") + objectConstants.get("offset").getFloat(0) + width/2f;
        y = (float) properties.get("y") + objectConstants.get("offset").getFloat(1) - height;

        this.width = width;
        this.height = height;
        this.textureSize = textureSize;  // <------------------------  lmao

        setDimension(width, height);
        setX(x);
        setY(y);
        System.out.println("x: " + x);
        System.out.println("y: " + y);



//        setX((float) properties.get("x") + objectConstants.get("offset").getFloat(0));
//        setY((float) properties.get("y") + objectConstants.get("offset").getFloat(1));

//        setSensor(true);
        setBodyType(BodyDef.BodyType.StaticBody);
        Vector2 solidCenter = new Vector2(0,0);
        sensorShape = new PolygonShape();
        sensorShape.setAsBox(width, height, solidCenter, 0.0f);
    }

    @Override
    /**
     * @return position of checkpoint base rather than goal origin
     */
    public Vector2 getPosition(){
        return new Vector2(getX()-objectConstants.get("base_offset").getFloat(0),getY()-objectConstants.get("base_offset").getFloat(1));
    }

    /**
     * Creates the physics Body(s) for this object, adding them to the world.
     * <p>
     *
     * @param world Box2D world to store body
     * @return true if object allocation succeeded
     */
    public boolean activatePhysics(World world) {
        if (!super.activatePhysics(world)) {
            return false;
        }

        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = 0;
        sensorDef.isSensor = true;
        sensorDef.shape = sensorShape;

        Fixture sensorFixture = body.createFixture(sensorDef);
        sensorFixture.setUserData(sensorName);

        return true;
    }

//    /**
//     * @param b  whether we want the goal to be active
//     */
//    public void setCurrent(boolean b){
//        active = b;
//        int currFrame = animation.getKeyFrameIndex(animation.getFrameDuration());
//
//        if (b) {
//            animation.setPlayMode(Animation.PlayMode.LOOP);
//        } else {
//            active_animation.setPlayMode(Animation.PlayMode.LOOP);
//        }
//    }

    @Override
    public void draw(GameCanvas canvas){
        float scale = 32f/textureSize;

//        System.out.println(y*drawScale.y);
        canvas.draw(bottom, Color.WHITE, 0, 0, x*drawScale.x, y*drawScale.y, 0, scale, scale);
        for (float dy = 1; dy < height-1; dy+= 1){
//            System.out.println("drawing middle");
//            if (isActive()) {}
            canvas.draw(middle, Color.WHITE, 0, 0, x*drawScale.x, (y + dy) * drawScale.y, 0, scale, scale);
        }
        canvas.draw(top, Color.WHITE, 0, 0, x*drawScale.x, (y + height - 1) * drawScale.y, 0, scale, scale);

    }
//    public void draw(GameCanvas canvas) {
//        if (active) {
////           TextureRegion singleFrame = spriteFrames[0][0];
////           TextureRegion[][] splitTexture = TextureRegion.split(singleFrame.getTexture(), singleFrame.getRegionWidth(), singleFrame.getRegionHeight()/2);
////           setTexture(splitTexture[1][1]);
//            setTexture(texture);
//        } else {
////           TextureRegion singleFrame = activeSpriteFrames[0][0];
////           TextureRegion[][] splitTexture = TextureRegion.split(singleFrame.getTexture(), singleFrame.getRegionWidth(), singleFrame.getRegionHeight()/2);
////           setTexture(splitTexture[1][1]);
//            setTexture(activeTexture);
//        }
//        super.draw(canvas);
//    }

//    @Override
//    public void draw(GameCanvas canvas){
//        animationTime += Gdx.graphics.getDeltaTime();
//        if (current) {
//            setTexture(animation.getKeyFrame(animationTime));
//            animation.setPlayMode(Animation.PlayMode.LOOP);
//        } else {
//            setTexture(active_animation.getKeyFrame(animationTime));
//            active_animation.setPlayMode(Animation.PlayMode.LOOP);
//        }
//        super.draw(canvas);
//    }

    /**
     * Loads json values that specify object properties that remain the same across all levels
     * @param constants Json field corresponding to this object
     */
    public static void setConstants(JsonValue constants) {
        objectConstants = constants;
        shrink = constants.getFloat("shrink");
    }

    public void drawDebug(GameCanvas canvas){
        super.drawDebug(canvas);
//        float xTranslate = (canvas.getCamera().getX()-canvas.getWidth()/2)/drawScale.x;
//        float yTranslate = (canvas.getCamera().getY()-canvas.getHeight()/2)/drawScale.y;
        canvas.drawPhysics(sensorShape, Color.RED, getX(), getY(), getAngle(), drawScale.x, drawScale.y);
    }

    public String getSensorName(){ return sensorName; }

    @Override
    public void loadState(ObjectMap<String, Object> state){
        super.loadState(state);
        Vector2 pos = (Vector2) state.get("position");
        setX(pos.x + objectConstants.get("base_offset").getFloat(0));
        setY(pos.y + objectConstants.get("base_offset").getFloat(1));
    }
}
