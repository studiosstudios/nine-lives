package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;
import com.badlogic.gdx.physics.box2d.World;
import edu.cornell.gdiac.util.Direction;

import java.util.HashMap;

public class Laser extends BoxObstacle implements Activatable{

    /** Points of the beam */
    private Array<Vector2> points;
    /** The offset vector between the center of the laser body and the beginning of the beam */
    private Vector2 beamOffset;
    /** Thickness of beams */
    private static float thickness;
    /** Constants that are shared between all instances of this class*/
    protected static JsonValue objectConstants;
    /** Current activation state */
    private boolean activated;
    /** Starting activation state */
    private boolean initialActivation;
    /** Cache of current color of the beam */
    private Color color;
    /** Makes laser beams change color with time */
    private float totalTime;
    /** The direction that this laser fires in */
    private Direction dir;

    /**
     * Creates a new Laser object.
     * @param texture   TextureRegion for drawing.
     * @param scale     Draw scale for drawing.
     * @param data      JSON data for loading.
     */
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

    public Laser(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, int tileSize, int levelHeight){
        super(tMap.get("laser").getRegionWidth()/scale.x,
                tMap.get("laser").getRegionHeight()/scale.y);

        setBodyType(BodyDef.BodyType.StaticBody);
        setName("laser");
        setDrawScale(scale);
        setTexture(tMap.get("laser"));

        setRestitution(objectConstants.getFloat("restitution", 0));
        setFriction(objectConstants.getFloat("friction", 0));
        setDensity(objectConstants.getFloat("density", 0));
        setMass(objectConstants.getFloat("mass", 0));
        setX((float) properties.get("x")/tileSize+objectConstants.get("offset").getFloat(0));
        setY(levelHeight - (float) properties.get("y")/tileSize+objectConstants.get("offset").getFloat(1));
        setAngle((float) ((float) properties.get("rotation") * Math.PI/180));
        setSensor(true);
        setFixedRotation(true);
        dir = Direction.angleToDir((int) ((float) properties.get("rotation")));
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
        initTiledActivations(properties);
    }

    /**
     * @return Direction that laser fires in.
     */
    public Direction getDirection(){ return dir; }

    /**
     * Adds a new point to the laser's beam.
     * @param point The point to add.
     */
    public void addBeamPoint(Vector2 point){ points.add(point);}

    /**
     * Resets the state of the laser to prepare for raycasting.
     */
    public void beginRayCast(){
        points.clear();
        points.add(getBeamStart());
    }

    /**
     * @return The starting point of the beam.
     */
    public Vector2 getBeamStart(){
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

    /**
     * Updates the object's physics state and the beam's color.
     * @param dt Timing values from parent loop
     */
    public void update(float dt){
        super.update(dt);
        totalTime += dt;
        color.set(1, 0, 0, ((float) Math.cos((double) totalTime * 2)) * 0.25f + 0.75f);
    }

    /**
     * Turns on laser. Does nothing because beam raycasting is done in <code>ActionController</code>.
     * @param world  Box2D world
     */
    @Override
    public void activated(World world){}

    /**
     * Turns off laser.
     * @param world  Box2D world
     */
    @Override
    public void deactivated(World world){
        points.clear();
        totalTime = 0;
    }

    //region ACTIVATABLE METHODS
    @Override
    public void setActivated(boolean activated) {this.activated = activated;}

    @Override
    public boolean isActivated() { return activated; }

    @Override
    public void setInitialActivation(boolean initialActivation){ this.initialActivation = initialActivation; }

    @Override
    public boolean getInitialActivation() { return initialActivation; }
    //endregion

    /**
     * Sets the shared constants for all instances of this class
     * @param constants JSON storing the shared constants.
     */
    public static void setConstants(JsonValue constants) {
        objectConstants = constants;
        thickness = constants.getFloat("thickness");
    }
}
