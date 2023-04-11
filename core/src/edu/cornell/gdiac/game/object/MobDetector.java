package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;

public class MobDetector {

    /** points of the beam */
    private Array<Vector2> points;
    /** End of the beam */
    private Vector2 endPointCache;
    /** Beam thickness */
    private float thickness;
    /** The color of the beam */
    private Color color;
    /** Whether the beam is pointing right or not */
    private Boolean pointingRight;
    /** The mob associated with this beam */
    private Mob mob;

    /**
     * Creates a new MobDetector model
     *
     * @param mob the mob associated with this detector
     */
    public MobDetector(Mob mob) {
        this.mob = mob;
        // Detector Beam
        pointingRight = mob.isFacingRight();
        thickness = 5f;
        color = Color.BLUE;
        points = new Array<>();
    }

    /**
     * Returns whether the mob is pointing right or not
     *
     * @return pointingRight
     */
    public Boolean isPointingRight(){ return pointingRight; }

    /**
     * Sets whether the mob is pointing right or not
     *
     * @param val the value to set pointing right
     */
    public void setPointingRight(Boolean val){
        pointingRight = val;
    }

    /**
     * Adds a point to the detector beam
     *
     * @param point to add to the beam
     */
    public void addBeamPoint(Vector2 point){ points.add(point);}

    /**
     * Begins casting the ray.
     * The points are cleared and the first point is added.
     * The first point is the position of the mob associated with this detector.
     *
     */
    public void beginRayCast(){
        points.clear();
        points.add(mob.getPosition());
    }

    /**
     * Returns the start position of the detector ray
     *
     * @return associated mob's current position
     */
    public Vector2 getRayCastStart(){
        return mob.getPosition();
    }

    /**
     * Returns the points of the detector ray
     *
     * @return points
     */
    public Array<Vector2> getPoints() { return points; }

    /**
     * Returns the thickness of the ray
     *
     * @return thickness
     */
    public float getThickness() { return thickness; }

    /**
     * Sets the end point of the mob detector.
     * nEd point of mob detector is position of first object it hits,
     * (but under assumption that the laser is guaranteed to hit something)
     * @param endPoint
     */
    public void setEndPoint(Vector2 endPoint) {endPointCache = endPoint;}

    /**
     * Returns the end point cache of the ray
     *
     * @return endPointCache
     */
    public Vector2 getEndPointCache() {return endPointCache;}

    /**
     * Draws the detector ray
     *
     * @param canvas
     */
    public void drawDebug(GameCanvas canvas) {
        if (points.size > 1) {
            canvas.drawFactoryPath(points, thickness, Color.BLUE, mob.getDrawScale().x, mob.getDrawScale().y);
        }
    }
}
