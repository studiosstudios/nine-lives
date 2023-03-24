package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;
import com.badlogic.gdx.physics.box2d.World;

public class Laser extends BoxObstacle implements Activatable{

    /** points of the beam */
    private Array<Vector2> points;

    private Vector2 endPointCache = new Vector2();

    private Vector2 beamOffset;
    private static float thickness;
    protected static JsonValue objectConstants;
    private boolean activated;
    private boolean initialActivation;
    private Color color;

    /** makes laser beams change color with time*/
    private float totalTime;

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

        dir = Direction.angleToDir(data.getInt("angle"));
        switch (dir){
            case UP:
                beamOffset = new Vector2(objectConstants.get("beamOffset").getFloat(0), objectConstants.get("beamOffset").getFloat(1));
                break;
            case DOWN:
                beamOffset = new Vector2(objectConstants.get("beamOffset").getFloat(0), -objectConstants.get("beamOffset").getFloat(1));
                break;
            case LEFT:
                beamOffset = new Vector2(-objectConstants.get("beamOffset").getFloat(1), objectConstants.get("beamOffset").getFloat(0));
                break;
            case RIGHT:
                beamOffset = new Vector2(objectConstants.get("beamOffset").getFloat(1), -objectConstants.get("beamOffset").getFloat(0));
                break;
        }
        totalTime = 0;
        color = Color.RED;
        points = new Array<>();
        initActivations(data);
    }

    public Direction getDirection(){ return dir; }

    public void addBeamPoint(Vector2 point){ points.add(point);}

    public void beginRayCast(){
        points.clear();
        points.add(getRayCastStart());
    }


    public Vector2 getRayCastStart(){
        return getPosition().add(beamOffset);
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
    public boolean isActivated() { return activated; }

    @Override
    public void setInitialActivation(boolean initialActivation){ this.initialActivation = initialActivation; }

    @Override
    public boolean getInitialActivation() { return initialActivation; }
    public static void setConstants(JsonValue constants) {
        objectConstants = constants;
        thickness = constants.getFloat("thickness");
    }
}
