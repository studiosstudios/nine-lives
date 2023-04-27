package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;

/** NOT USED YET **/
public class CameraTile extends BoxObstacle {
    /** Zoom percentage of camera after collision with this camera tile **/
    private float zoom;
    /** Constants that are shared between all instances of this class */
    private static JsonValue objectConstants;

    /**
     * @param zoom  Zoom percentage of camera after collision with this camera tile
     * @param scale World scale
     */
    public CameraTile(ObjectMap<String, Object> properties, float zoom, Vector2 scale){
        super(32/scale.x, 32/scale.y);
        this.zoom = zoom;
        setSensor(true);
        setX((float) properties.get("x") + getDimension().x/2);
        setY((float) properties.get("y") - getDimension().y/2);
    }

    /**
     * @return the zoom percentage of camera after collision with this camera tile
     */
    public float getZoom(){
        return zoom;
    }

    @Override
    public void drawDebug(GameCanvas canvas){
        float xTranslate = (canvas.getCamera().getX()-canvas.getWidth()/2)/drawScale.x;
        float yTranslate = (canvas.getCamera().getY()-canvas.getHeight()/2)/drawScale.y;
        canvas.drawPhysics(shape, Color.PURPLE,getX()-xTranslate,getY()-yTranslate,getAngle(),drawScale.x,drawScale.y);
    }
}
