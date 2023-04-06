package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;
import edu.cornell.gdiac.game.obstacle.ComplexObstacle;

public class Checkpoint extends BoxObstacle
{
    /** The initializing data (to avoid magic numbers) */
    private final JsonValue data;

    protected TextureRegion checkpoint;
    protected TextureRegion active_checkpoint;

    private boolean active;
    private PolygonShape sensorShape;
    protected static JsonValue objectConstants;
    /**
     * Creates a new Checkpoint
     *
     * The size is expressed in physics units NOT pixels.  In order for
     * drawing to work properly, you MUST set the drawScale. The drawScale
     * converts the physics units to pixels.
     *
     */
    public Checkpoint(JsonValue data, Vector2 scale, TextureRegion checkpointTexture, TextureRegion activeCheckpointTexture) {
        super(checkpointTexture.getRegionWidth()/scale.x,
                checkpointTexture.getRegionHeight()/scale.y);
        this.data = data;
        this.checkpoint = checkpointTexture;
        this.active_checkpoint = activeCheckpointTexture;
        active = false;
        setTexture(checkpointTexture);
        setName("checkpoint");
        setDrawScale(scale);
        setX(data.get("pos").getFloat(0)+objectConstants.get("offset").getFloat(0));
        setY(data.get("pos").getFloat(1)+objectConstants.get("offset").getFloat(1));
        setSensor(true);
        setBodyType(BodyDef.BodyType.StaticBody);
        Vector2 solidCenter = new Vector2(0,0);
        sensorShape = new PolygonShape();
        sensorShape.setAsBox(getWidth() / 2 * objectConstants.getFloat("solid_width_scale"),
                getHeight() / 2 * objectConstants.getFloat("solid_height_scale"),
                solidCenter, 0.0f);
//        System.out.println(getWidth());
//        System.out.println(getHeight());
    }

    @Override
    /**
     * @return position of checkpoint base rather than checkpoint origin
     */
    public Vector2 getPosition(){
        return new Vector2(getX()-objectConstants.get("offset").getFloat(0),getY()-objectConstants.get("offset").getFloat(1));
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
        setTexture(active?active_checkpoint:checkpoint);
    }

    /**
     * @return true if the checkpoint is active
     */
    public boolean getActive(){
        return active;
    }

    @Override
    public void draw(GameCanvas canvas){
        super.draw(canvas);
    }

    /**
     * Loads json values that specify object properties that remain the same across all levels
     * @param constants Json field corresponding to this object
     */
    public static void setConstants(JsonValue constants) { objectConstants = constants; }

    @Override
    public void loadState(ObjectMap<String, Object> state){
        super.loadState(state);
        Vector2 pos = (Vector2) state.get("position");
        setX(pos.x + objectConstants.get("offset").getFloat(0));
        setY(pos.y + objectConstants.get("offset").getFloat(1));
    }
}
