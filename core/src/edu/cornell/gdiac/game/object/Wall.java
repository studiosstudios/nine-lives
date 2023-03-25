package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.PolygonObstacle;

public class Wall extends PolygonObstacle  {
    /** Constants that are shared between all instances of this class */
    private static JsonValue objectConstants;
    /** If this wall is climbable */
    private boolean isClimbable;

    /**
     * Creates a new wall object.
     * @param texture  TextureRegion for drawing.
     * @param scale    Draw scale for drawing.
     * @param data     JSON for loading.
     */
    public Wall(TextureRegion texture, Vector2 scale, JsonValue data){
        super(data.get("shape").asFloatArray());
        setBodyType(BodyDef.BodyType.StaticBody);
        setDensity(objectConstants.getFloat( "density", 0.0f ));
        setFriction(objectConstants.getFloat( "friction", 0.0f ));
        setRestitution(objectConstants.getFloat( "restitution", 0.0f ));
        isClimbable = data.getBoolean("climbable", false);
        setDrawScale(scale);
        setTexture(texture);
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
