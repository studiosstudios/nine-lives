package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
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
        setTexture(checkpoint);
        setName("checkpoint");
        setDrawScale(scale);
        setX(data.get("pos").getFloat(0)+objectConstants.get("offset").getFloat(0));
        setY(data.get("pos").getFloat(1)+objectConstants.get("offset").getFloat(1));
        setSensor(true);
        setBodyType(BodyDef.BodyType.StaticBody);
//        System.out.println(getWidth());
//        System.out.println(getHeight());
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
    }

    public static void setConstants(JsonValue constants) { objectConstants = constants; }
}
