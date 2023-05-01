package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;
import com.badlogic.gdx.math.Rectangle;

/** NOT USED YET **/
public class CameraRegion extends BoxObstacle {
    /** Zoom percentage of camera after collision with this camera tile **/
    private float zoom;
    /** Constants that are shared between all instances of this class */
    private static JsonValue objectConstants;
    /** number of fixture colliding with this camera region */
    private int fixtureCount;

    /**
     * @param properties     String-Object map of properties for this object
     * @param scale World scale
     */
    public CameraRegion(ObjectMap<String, Object> properties, Vector2 scale){
        super((float)properties.get("width"), (float)properties.get("height"));
        this.zoom = (float) properties.get("zoom");
        setBodyType(BodyDef.BodyType.StaticBody); //lmao
        setSensor(true);
        setDrawScale(scale);
        setX((float) properties.get("x") + getDimension().x/2);
        setY((float) properties.get("y") - getDimension().y/2);
        setName((String) properties.get("name"));
        fixtureCount = 0;
    }

    /**
     * @return number of fixtures colliding with this camera region
     */
    public int getFixtureCount(){
        return fixtureCount;
    }

    /**
     * Add one to fixture count
     */
    public void addFixture(){
        fixtureCount += 1;
    }

    /**
     * Subtract one from fixture count
     */
    public void removeFixture(){
        fixtureCount -= 1;
    }
    /**
     * @return the zoom percentage of camera after collision with this camera tile
     */
    public float getZoom(){
        return zoom;
    }

    /**
     * gets bounds of CameraRegion
     * @return bounds of CameraRegion as a Rectangle
     */
    public Rectangle getBounds(){
        return new Rectangle(getX(),getY(),getWidth(),getHeight());
    }

    @Override
    public void drawDebug(GameCanvas canvas){
//        float xTranslate = (canvas.getCamera().getX()-canvas.getWidth()/2)/drawScale.x;
//        float yTranslate = (canvas.getCamera().getY()-canvas.getHeight()/2)/drawScale.y;
        canvas.drawPhysics(shape, Color.PURPLE,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
    }
}
