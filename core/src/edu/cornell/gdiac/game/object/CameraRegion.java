package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;
import com.badlogic.gdx.math.Rectangle;

public class CameraRegion extends BoxObstacle {
    /** Zoom percentage of camera after collision with this camera tile/Zoom percentage of camera relative to the camera region size **/
    private float zoom;
    /** Whether zoom is relative to viewport or to camera region size*/
    private boolean relativeZoom;
    /** Constants that are shared between all instances of this class */
    private static JsonValue objectConstants;
    /** Number of fixture colliding with this camera region */
    private int fixtureCount;
    /** Whether camera should snap to this camera region */
    private boolean snapRegion;

    /**
     * @param properties     String-Object map of properties for this object
     * @param scale World scale
     */
    public CameraRegion(ObjectMap<String, Object> properties, Vector2 scale){
        super((float)properties.get("width"), (float)properties.get("height"));
        zoom = (float) properties.get("zoom");
//        this.shouldSnap = (boolean) properties.get("shouldSnap"); //TODO: LOOK INTO WHY THIS IS NULL
        setBodyType(BodyDef.BodyType.StaticBody); //lmao
        setSensor(true);
        setDrawScale(scale);
        setX((float) properties.get("x") + getDimension().x/2);
        setY((float) properties.get("y") - getDimension().y/2);
        setName((String) properties.get("name"));
        fixtureCount = 0;
        float expectedWidth = getWidth() * zoom;
        float expectedHeight = getHeight() * zoom;
        relativeZoom = (boolean) properties.get("isZoomRelative");
        snapRegion = (boolean) properties.get("shouldSnapRegion");
        if(relativeZoom)
            zoom = Math.min(expectedWidth*scale.x/GameCanvas.STANDARD_WIDTH, expectedHeight*scale.y/GameCanvas.STANDARD_HEIGHT);
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
        return new Rectangle(getX() - getDimension().x/2,getY()-getDimension().y/2,getWidth(),getHeight());
    }

    /**
     * Returns if the camera should snap to this camera region
     * @return snapRegion
     */
    public boolean shouldSnap(){
        return snapRegion;
    }

    @Override
    public void drawDebug(GameCanvas canvas){
//        float xTranslate = (canvas.getCamera().getX()-canvas.getWidth()/2)/drawScale.x;
//        float yTranslate = (canvas.getCamera().getY()-canvas.getHeight()/2)/drawScale.y;
        canvas.drawPhysics(shape, Color.TEAL,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
    }
}
