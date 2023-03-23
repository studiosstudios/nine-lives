package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;

public class MobDetector {

    /** Detector Beam */
    /** points of the beam */
    private Array<Vector2> points;
    private Vector2 endPointCache;
    private float thickness;
    private Color color;
    private Boolean pointingRight;

    private Mob mob;

    public MobDetector(Mob mob) {
        this.mob = mob;
        // Detector Beam
        pointingRight = mob.isFacingRight();
        thickness = 5f;
        color = Color.BLUE;
        points = new Array<>();
    }

    public Boolean isPointingRight(){ return pointingRight; }
    public void setPointingRight(Boolean val){
        pointingRight = val;
    }

    public void addBeamPoint(Vector2 point){ points.add(point);}

    public void beginRayCast(){
        points.clear();
        points.add(mob.getPosition());
    }

    public Vector2 getRayCastStart(){
        return mob.getPosition();
    }

    public Array<Vector2> getPoints() { return points; }

    public float getThickness() { return thickness; }

    /*End point of mob detector is position of first object it hits,
    * (but under assumption that the laser is guaranteed to hit something */
    public void setEndPoint(Vector2 endPoint) {endPointCache = endPoint;}

    public Vector2 getEndPointCache() {return endPointCache;}

//    // Temp Draw for Detector ray so that we can see it
//    public void drawDebug(GameCanvas canvas) {
//        if (points.size > 1) {
//            canvas.drawFactoryPath(points, thickness, Color.BLUE, mob.getDrawScale().x, mob.getDrawScale().y);
//        }
//    }
}
