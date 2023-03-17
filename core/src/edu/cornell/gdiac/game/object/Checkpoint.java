package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;
import edu.cornell.gdiac.game.obstacle.ComplexObstacle;

public class Checkpoint extends BoxObstacle
{
    /** The initializing data (to avoid magic numbers) */
    private final JsonValue data;

    /** Whether the laser object is on and firing a beam */

    protected TextureRegion checkpoint;
    protected TextureRegion active_checkpoint;

    protected Vector2 origin;

    private boolean active;
    private PolygonShape sensorShape;
    private Fixture sensorFixture;
    protected static JsonValue objectConstants;
    /**
     * Creates a new LaserBase
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
        return true;
    }

    public void setActive(boolean b){
        active = b;
        setTexture(active?active_checkpoint:checkpoint);
    }

    public boolean getActive(){
        return active;
    }

    @Override
    public void draw(GameCanvas canvas){
        super.draw(canvas);
//        System.out.println("drawing checkpoint");
    }

    public static void setConstants(JsonValue constants) { objectConstants = constants; }
    protected void createFixtures(){
        super.createFixtures();

        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = 0;
        sensorDef.isSensor = true;
        sensorDef.shape = sensorShape;
        sensorFixture = body.createFixture( sensorDef );
        sensorFixture.setUserData(this);
    }
    protected void releaseFixtures(){
        super.releaseFixtures();
        if (sensorFixture != null) {
            body.destroyFixture(sensorFixture);
            sensorFixture = null;
        }
    }
}
