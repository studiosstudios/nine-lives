package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;
import com.badlogic.gdx.math.Rectangle;

/**
 * The CameraRegion is composed of two rectangles, a collision rectangle and a non-collision rectangle.
 *
 * When you collide with a collision rectangle, the camera will snap to the non-collision rectangle (indicated by
 * snapBounds). You can toggle whether you want the camera to snap at all (shouldSnap), and if so, if it should snap to
 * the collision region or another specified rectangle of your choice.
 */

public class CameraRegion extends BoxObstacle {
    /** Zoom percentage of camera after collision with this camera tile/Zoom percentage of camera relative to the camera region size **/
    private float zoom;
    /** Whether zoom is relative to viewport or to camera region size*/
    private boolean relativeZoom;
    /** Constants that are shared between all instances of this class */
    private static JsonValue objectConstants;
    /** Number of fixture colliding with this camera region */
    private int fixtureCount;
    /** Whether camera should snap to snapBounds */
    private boolean shouldSnap;
    /** Bounds the camera will snap to if shouldSnap is true */
    private Rectangle snapBounds;

    /**
     * @param properties     String-Object map of properties for this object
     * @param scale World scale
     */
    public CameraRegion(ObjectMap<String, Object> properties, Vector2 scale, Rectangle bounds){
        super((float)properties.get("width"), (float)properties.get("height"));
        zoom = (float) properties.get("zoom", 0.6f);
        setBodyType(BodyDef.BodyType.StaticBody); //lmao
        setSensor(true);
        setDrawScale(scale);
        setX((float) properties.get("x") + getDimension().x/2);
        setY((float) properties.get("y") - getDimension().y/2);
        setName((String) properties.get("name"));
        fixtureCount = 0;
        shouldSnap = (boolean) properties.get("shouldSnap", false);
        if((boolean) properties.get("snapCollisionArea", true)){
            snapBounds = this.getBounds();
        }
        else{
            snapBounds = new Rectangle((float) properties.get("bX", 0f) + bounds.x, (float) properties.get("bY", 0f) + bounds.y, (float) properties.get("bWidth", 18f), (float) properties.get("bHeight", 32f));
        }
        float expectedWidth = snapBounds.getWidth() * zoom;
        float expectedHeight = snapBounds.getHeight() * zoom;
        relativeZoom = (boolean) properties.get("isZoomRelative", false);
        if(relativeZoom)
            zoom = Math.min(expectedWidth*scale.x/GameCanvas.STANDARD_WIDTH, expectedHeight*scale.y/GameCanvas.STANDARD_HEIGHT);
        zoom = Math.round(zoom*100)/100f;
    }

    /**
     * @return number of fixtures colliding with this camera region
     */
    public int getFixtureCount(){
        return fixtureCount;
    }

    /**
     * Add one to number of fixtures colliding with this camera region
     */
    public void addFixture(){
        fixtureCount += 1;
    }

    /**
     * Subtract one from number of fixtures colliding with this camera region
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
     * Gets bounds of CameraRegion (the collision part)
     * @return bounds of CameraRegion as a Rectangle (the collision part)
     */
    public Rectangle getBounds(){
        return new Rectangle(getX() - getDimension().x/2,getY()-getDimension().y/2,getWidth(),getHeight());
    }

    /**
     * Gets bounds the camera will snap to (the non-collision part)
     * @return bounds the camera will snap to (the non-collision part)
     */
    public Rectangle getSnapBounds(){
        return snapBounds;
    }

    /**
     * Returns if the camera should snap to this camera region
     * @return snapRegion
     */
    public boolean shouldSnap(){
        return shouldSnap;
    }

    @Override
    public void drawDebug(GameCanvas canvas){
//        float xTranslate = (canvas.getCamera().getX()-canvas.getWidth()/2)/drawScale.x;
//        float yTranslate = (canvas.getCamera().getY()-canvas.getHeight()/2)/drawScale.y;
        canvas.drawPhysics(shape, Color.TEAL,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
    }
}
