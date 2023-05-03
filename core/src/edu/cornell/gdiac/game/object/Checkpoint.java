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

public class Checkpoint extends BoxObstacle
{
    /** The origin position of the checkpoint */
    protected Vector2 origin;
    /** Whether this checkpoint is active or not */
    private boolean current;
    /** The sensor shape for this checkpoint */
    private PolygonShape sensorShape;
    /** The constants for the checkpoint */
    protected static JsonValue objectConstants;
    private static final String sensorName = "checkpointSensor";
    /** The texture for the non-active checkpoint base */
    private TextureRegion baseTexture;
    /** The texture for the active checkpoint base */
    private TextureRegion activeBaseTexture;
    /** The frames of the non-active checkpoint animation */
    private TextureRegion[][] spriteFrames;
    /** The frames of the active checkpoint animation */
    private TextureRegion[][] activeSpriteFrames;
    /** How long the checkpoint has been animating */
    private float animationTime;
    /** Filmstrip of non-active checkpoint animation */
    private Animation<TextureRegion> animation;
    /** Filmstrip of active checkpoint animation */
    private Animation<TextureRegion> activeAnimation;
    /** if the player was facing right when getting this checkpoint */
    private boolean facingRight;
    /** direction of this checkpoint */
    private Direction dir;
    private Vector2 baseOffset;
    private Vector2 respawnOffset;

    /**
     * Creates a new Checkpoint object.
     *
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param textureScale   Texture scale for rescaling texture
     */
    public Checkpoint(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, Vector2 textureScale){

        super(32/scale.x, 64/scale.y);
        current = false;
        setTextureScale(textureScale);
        int spriteWidth = 1024;
        int spriteHeight = 2048;
        this.baseTexture = tMap.get("checkpoint-base");
        this.activeBaseTexture = tMap.get("checkpoint-base-active");
        spriteFrames = TextureRegion.split(tMap.get("checkpoint-anim").getTexture(), spriteWidth, spriteHeight);
        activeSpriteFrames = TextureRegion.split(tMap.get("checkpoint-active-anim").getTexture(), spriteWidth, spriteHeight);
        float frameDuration = 0.1f;

        animation = new Animation<>(frameDuration, spriteFrames[0]);
        activeAnimation = new Animation<>(frameDuration, activeSpriteFrames[0]);

        animation.setPlayMode(Animation.PlayMode.LOOP);
        animationTime = 0f;
        setAngle((float) ((float) properties.get("rotation") * Math.PI/180));
        dir = Direction.angleToDir((int) ((float) properties.get("rotation")));
        setMass(0);
        setName("checkpoint");
        setDrawScale(scale);
        setSensor(true);

        Vector2 offset = new Vector2(objectConstants.get("offset").getFloat(0), objectConstants.get("offset").getFloat(1));
        Direction.rotateVector(offset, dir);
        setX((float) properties.get("x") + offset.x);
        setY((float) properties.get("y") + offset.y);
        setSensor(true);
        setBodyType(properties.containsKey("attachName") ? BodyDef.BodyType.DynamicBody : BodyDef.BodyType.StaticBody);
        Vector2 solidCenter = new Vector2(0,0);
        sensorShape = new PolygonShape();
        sensorShape.setAsBox(getWidth() / 2 * objectConstants.getFloat("solid_width_scale"),
                getHeight() / 2 * objectConstants.getFloat("solid_height_scale"),
                solidCenter, 0.0f);


        baseOffset = new Vector2(objectConstants.get("base_offset").getFloat(0), objectConstants.get("base_offset").getFloat(1));
        Direction.rotateVector(baseOffset, dir);

        respawnOffset = new Vector2(objectConstants.get("respawn_offset").getFloat(0),objectConstants.get("respawn_offset").getFloat(1));
        Direction.rotateVector(respawnOffset, dir);
    }

    @Override
    /**
     * @return position of checkpoint base rather than checkpoint origin
     */
    public Vector2 getPosition(){
        return super.getPosition().sub(baseOffset);
    }

    /**
     * @return respawn position of the cat for this checkpoint
     */
    public Vector2 getRespawnPosition(){
        return getPosition().add(respawnOffset);
    }

    public boolean facingRight() { return facingRight; }

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

    /**
     * @param b               whether we want the checkpoint to be active
     * @param facingRight     if the player was facing right when getting this checkpoint
     */
    public void setCurrent(boolean b, boolean facingRight){
        current = b;
        this.facingRight = facingRight;
        if (b) {
            animation.setPlayMode(Animation.PlayMode.LOOP);
        } else {
            activeAnimation.setPlayMode(Animation.PlayMode.LOOP);
        }
    }

    /**
     * @return true if the checkpoint is active
     */
    public boolean getCurrent(){
        return current;
    }

    public void drawBase(GameCanvas canvas) {
       if (current) {
//           TextureRegion singleFrame = spriteFrames[0][0];
//           TextureRegion[][] splitTexture = TextureRegion.split(singleFrame.getTexture(), singleFrame.getRegionWidth(), singleFrame.getRegionHeight()/2);
//           setTexture(splitTexture[1][1]);
           setTexture(baseTexture);
        } else {
//           TextureRegion singleFrame = activeSpriteFrames[0][0];
//           TextureRegion[][] splitTexture = TextureRegion.split(singleFrame.getTexture(), singleFrame.getRegionWidth(), singleFrame.getRegionHeight()/2);
//           setTexture(splitTexture[1][1]);
           setTexture(activeBaseTexture);
       }
        super.draw(canvas);
    }

    @Override
    public void draw(GameCanvas canvas){
        animationTime += Gdx.graphics.getDeltaTime();
        if (current) {
            setTexture(animation.getKeyFrame(animationTime));
            animation.setPlayMode(Animation.PlayMode.LOOP);
        } else {
            setTexture(activeAnimation.getKeyFrame(animationTime));
            activeAnimation.setPlayMode(Animation.PlayMode.LOOP);
        }
        super.draw(canvas);
    }

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
        setX(pos.x + baseOffset.x);
        setY(pos.y + baseOffset.y);
    }
}
