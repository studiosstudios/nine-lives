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
    private Color color;

    /** makes laser beams change color with time*/
    private float totalTime;
    public enum Direction {UP, DOWN, LEFT, RIGHT}

    private Direction dir;

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
        setAngle((float) (data.getInt("angle") * Math.PI/180));
        setSensor(true);
        setFixedRotation(true);

        dir = angleToDir(data.getInt("angle"));
        totalTime = 0;
        color = Color.RED;
        points = new Array<>();
        initActivations(data);
    }

    public Direction getDirection(){ return dir; }
    public static Direction angleToDir(int angle){
        switch (angle){
            case 0:
                return Direction.UP;
            case 90:
                return Direction.LEFT;
            case 180:
                return Direction.DOWN;
            case 270:
                return Direction.RIGHT;
            default:
                throw new RuntimeException("undefined angle");
        }
    }

    public void addBeamPoint(Vector2 point){ points.add(point);}

    public void beginRayCast(){
        points.clear();
        points.add(getPosition());
    }

    public Vector2 getRayCastStart(){
        return getPosition();
    }

    @Override
    public void draw(GameCanvas canvas){
        if (activated) {
            if (points.size > 1) {
                canvas.drawFactoryPath(points, thickness, color, drawScale.x, drawScale.y);
                canvas.drawFactoryPath(points, thickness*0.3f, Color.WHITE, drawScale.x, drawScale.y);
            }
        }
        super.draw(canvas);
    }

    public void update(float dt){
        super.update(dt);
        totalTime += dt;
        color.set(1, 0, 0, ((float) Math.cos((double) totalTime * 2)) * 0.25f + 0.75f);
    }

    @Override
    public void activated(World world){
    }

    @Override
    public void deactivated(World world){
        points.clear();
        points.add(getPosition());
        totalTime = 0;
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
