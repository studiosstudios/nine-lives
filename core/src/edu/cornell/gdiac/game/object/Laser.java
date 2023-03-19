package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;
import com.badlogic.gdx.physics.box2d.World;

public class Laser extends BoxObstacle implements Activatable{

    /** points of the beam */
    private Array<Vector2> points;

    private Vector2 endPointCache = new Vector2();

    private static float thickness;
    protected static JsonValue objectConstants;
    private boolean activated;
    private boolean initialActivation;

    /** storing the angle in degrees to prevent comparison errors*/
    private int angle;

    public Laser(TextureRegion texture, Vector2 scale, JsonValue data){
        super(texture.getRegionWidth()/scale.x,
                texture.getRegionHeight()/scale.y);

        setBodyType(BodyDef.BodyType.StaticBody);
        setName("laser");
        setDrawScale(scale);
        setTexture(texture);;

        setRestitution(objectConstants.getFloat("restitution", 0));
        setFriction(objectConstants.getFloat("friction", 0));
        setDensity(objectConstants.getFloat("density", 0));
        setMass(objectConstants.getFloat("mass", 0));
        setX(data.get("pos").getFloat(0)+objectConstants.get("offset").getFloat(0));
        setY(data.get("pos").getFloat(1)+objectConstants.get("offset").getFloat(1));
        angle = data.getInt("angle");
        setAngle((float) (angle * Math.PI/180));
        setFixedRotation(true);

        points = new Array<>();
        initActivations(data);
    }

    public void addBeamPoint(Vector2 point){ points.add(point);}

    public void beginRayCast(){
        points.clear();
        points.add(getPosition());
    }

    public Vector2 getRayCastStart(){
        return getPosition();
    }

    public Vector2 getRayCastEnd(Rectangle bounds){
        switch (angle) {
            case 0:
                endPointCache.set(getX(),bounds.height);
                break;
            case 90:
                endPointCache.set(0, getY());
                break;
            case 180:
                endPointCache.set(getX(), 0);
                break;
            case 270:
                endPointCache.set(bounds.width, getY());
                break;
            default:
                throw new RuntimeException("undefined angle");
        }
        return endPointCache;
    }
    @Override
    public void draw(GameCanvas canvas){
        if (activated) {
            if (points.size > 1) {
                canvas.drawFactoryPath(points, thickness, Color.RED, drawScale.x, drawScale.y);
            }
        }
        super.draw(canvas);
    }


    @Override
    public void activated(World world){
    }

    @Override
    public void deactivated(World world){
        points.clear();
        points.add(getPosition());
    }

    @Override
    public void setActivated(boolean activated) {this.activated = activated;}

    @Override
    public boolean getActivated() { return activated; }

    @Override
    public void setInitialActivation(boolean initialActivation){ this.initialActivation = initialActivation; }

    @Override
    public boolean getInitialActivation() { return initialActivation; }
    public static void setConstants(JsonValue constants) {
        objectConstants = constants;
        thickness = constants.getFloat("thickness");
    }
}
