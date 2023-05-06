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

import java.util.HashMap;

public class Goal extends BoxObstacle
{
    /** The origin position of the goal */
    protected Vector2 origin;
    /** Whether this goal is active or not */
    private boolean active;
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
     * Creates a new Goal object.
     *
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param textureScale   Texture scale for rescaling texture
     */
    public Goal(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, Vector2 textureScale){

        super(32/scale.x, 64/scale.y);
        active = false;
        setTextureScale(textureScale);
        int spriteWidth = 1024;
        int spriteHeight = 2048;
        this.texture = tMap.get("goal");
        this.activeTexture = tMap.get("goal");
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
        setX((float) properties.get("x") + objectConstants.get("offset").getFloat(0));
        setY((float) properties.get("y") + objectConstants.get("offset").getFloat(1));
        setSensor(true);
        setBodyType(BodyDef.BodyType.StaticBody);
        Vector2 solidCenter = new Vector2(0,0);
        sensorShape = new PolygonShape();
        sensorShape.setAsBox(getWidth() / 2 * objectConstants.getFloat("solid_width_scale"),
                getHeight() / 2 * objectConstants.getFloat("solid_height_scale"),
                solidCenter, 0.0f);
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
    public void draw(GameCanvas canvas) {
        if (active) {
//           TextureRegion singleFrame = spriteFrames[0][0];
//           TextureRegion[][] splitTexture = TextureRegion.split(singleFrame.getTexture(), singleFrame.getRegionWidth(), singleFrame.getRegionHeight()/2);
//           setTexture(splitTexture[1][1]);
            setTexture(texture);
        } else {
//           TextureRegion singleFrame = activeSpriteFrames[0][0];
//           TextureRegion[][] splitTexture = TextureRegion.split(singleFrame.getTexture(), singleFrame.getRegionWidth(), singleFrame.getRegionHeight()/2);
//           setTexture(splitTexture[1][1]);
            setTexture(activeTexture);
        }
        super.draw(canvas);
    }

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
    public static void setConstants(JsonValue constants) { objectConstants = constants; }

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
