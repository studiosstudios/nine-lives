package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.sun.org.apache.xpath.internal.operations.Bool;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;
import edu.cornell.gdiac.game.obstacle.PolygonObstacle;

import java.util.HashMap;

public class Wall extends PolygonObstacle  {
    /** Constants that are shared between all instances of this class */
    private static JsonValue objectConstants;
    /** If this wall is climbable */
    private boolean isClimbable;

    /**
     * Creates a new wall object.
     * @param texture  TextureRegion for drawing.
     * @param scale    Draw scale for drawing.
     * @param shape    Polygon shape.
     * @param isClimbable Whether wall is climbable or not.
     */
    public Wall(ObjectMap<String, Object> properties, Vector2 scale){
        super((float[]) properties.get("polygon"));
        setBodyType(BodyDef.BodyType.StaticBody);
        setDensity(objectConstants.getFloat( "density", 0.0f ));
        setFriction(objectConstants.getFloat( "friction", 0.0f ));
        setRestitution(objectConstants.getFloat( "restitution", 0.0f ));
        this.isClimbable = (boolean) properties.get("climbable", false);
        setDrawScale(scale);
        setName("wall");
    }

    /**
     * @return If this wall is climbable.
     */
    public boolean isClimbable(){return isClimbable;}

    public void draw(GameCanvas canvas){
        if (isClimbable){
            canvas.draw(region, Color.GREEN,0,0,getX()*drawScale.x,getY()*drawScale.y,getAngle(),1,1);
        } else {
            super.draw(canvas);
        }
    }

    /**
     * Sets the shared constants for all instances of this class.
     * @param constants JSON storing the shared constants.
     */
    public static void setConstants(JsonValue constants) {objectConstants = constants;}


}
