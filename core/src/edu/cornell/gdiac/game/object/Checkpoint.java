package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;
import edu.cornell.gdiac.game.obstacle.ComplexObstacle;

public class Checkpoint extends BoxObstacle
{
    /** The origin position of the checkpoint */
    protected Vector2 origin;
    /** Whether this checkpoint is active or not */
    private boolean active;
    /** The sensor shape for this checkpoint */
    private PolygonShape sensorShape;
    /** The constants for the checkpoint */
    protected static JsonValue objectConstants;
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
    private Animation<TextureRegion> active_animation;


    /**
     * Creates a new Checkpoint
     *
     * The size is expressed in physics units NOT pixels.  In order for
     * drawing to work properly, you MUST set the drawScale. The drawScale
     * converts the physics units to pixels.
     *
     * @param pos the position of the checkpoint
     * @param angle the angle of the checkpoint
     * @param scale the scale for drawing the texture
     * @param checkpointTexture the texture for the non-active checkpoint
     * @param activeCheckpointTexture the texture for the active checkpoint
     *
     */
    public Checkpoint(Vector2 pos, float angle, Vector2 scale, TextureRegion checkpointTexture, TextureRegion activeCheckpointTexture,
                      TextureRegion baseTexture, TextureRegion activeBaseTexture) {

        super(32/scale.x, 64/scale.y);
        active = false;
        int spriteWidth = 32;
        int spriteHeight = 64;
        this.baseTexture = baseTexture;
        this.activeBaseTexture = activeBaseTexture;
        spriteFrames = TextureRegion.split(checkpointTexture.getTexture(), spriteWidth, spriteHeight);
        activeSpriteFrames = TextureRegion.split(activeCheckpointTexture.getTexture(), spriteWidth, spriteHeight);
        float frameDuration = 0.1f;

        animation = new Animation<>(frameDuration, spriteFrames[0]);
        active_animation = new Animation<>(frameDuration, activeSpriteFrames[0]);

        animation.setPlayMode(Animation.PlayMode.LOOP);
        animationTime = 0f;
        setAngle(angle);
        setMass(0);
        setName("checkpoint");
        setDrawScale(scale);
        setSensor(true);
        setX(pos.x + objectConstants.get("offset").getFloat(0));
        setY(pos.y + objectConstants.get("offset").getFloat(1));
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
     * @return position of checkpoint base rather than checkpoint origin
     */
    public Vector2 getPosition(){
        return new Vector2(getX()-objectConstants.get("base_offset").getFloat(0),getY()-objectConstants.get("base_offset").getFloat(1));
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
        if (!super.activatePhysics(world)) {
            return false;
        }
        body.getFixtureList().get(0).setUserData(this);
        return true;
    }

    /**
     * @param b  whether we want the checkpoint to be active
     */
    public void setActive(boolean b){
        active = b;
        int currFrame = animation.getKeyFrameIndex(animation.getFrameDuration());

        if (b) {
            animation.setPlayMode(Animation.PlayMode.LOOP);
        } else {
            active_animation.setPlayMode(Animation.PlayMode.LOOP);
        }
    }

    /**
     * @return true if the checkpoint is active
     */
    public boolean getActive(){
        return active;
    }

    public void drawBase(GameCanvas canvas) {
       if (active) {
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
        if (active) {
            setTexture(animation.getKeyFrame(animationTime));
            animation.setPlayMode(Animation.PlayMode.LOOP);
        } else {
            setTexture(active_animation.getKeyFrame(animationTime));
            active_animation.setPlayMode(Animation.PlayMode.LOOP);
        }
        super.draw(canvas);
    }

    /**
     * Loads json values that specify object properties that remain the same across all levels
     * @param constants Json field corresponding to this object
     */
    public static void setConstants(JsonValue constants) { objectConstants = constants; }
}
