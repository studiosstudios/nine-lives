package edu.cornell.gdiac.game.object;

import box2dLight.ChainLight;
import box2dLight.Light;
import box2dLight.RayHandler;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
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
    /** Array of ChainLights associated with this laser; should be exactly 2 lights, where
     * the 0-index element is the light stored in the Obstacle field */
    private ChainLight[] lights;

    /**
     * Creates a new Laser object.
     *
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param textureScale   Texture scale for rescaling texture
     */
    public Laser(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, Vector2 textureScale){
        super(tMap.get("laser").getRegionWidth()/scale.x*textureScale.x,
                tMap.get("laser").getRegionHeight()/scale.y*textureScale.y);

        setBodyType(properties.containsKey("attachName") ? BodyDef.BodyType.DynamicBody : BodyDef.BodyType.StaticBody);
        setName("laser");
        setDrawScale(scale);
        setTexture(tMap.get("laser"));
        setTextureScale(textureScale);

        setRestitution(objectConstants.getFloat("restitution", 0));
        setFriction(objectConstants.getFloat("friction", 0));
        setDensity(objectConstants.getFloat("density", 0));
        setMass(objectConstants.getFloat("mass", 0));
        dir = Direction.angleToDir((int) ((float) properties.get("rotation")));
        Vector2 offset = new Vector2(objectConstants.get("offset").getFloat(0), objectConstants.get("offset").getFloat(1));
        Direction.rotateVector(offset, dir);
        setX((float) properties.get("x") + offset.x);
        setY((float) properties.get("y") + offset.y);
        setAngle((float) ((float) properties.get("rotation") * Math.PI/180));
        setSensor(true);
        setFixedRotation(true);
        beamOffset = new Vector2(objectConstants.get("beamOffset").getFloat(0), objectConstants.get("beamOffset").getFloat(1));
        Direction.rotateVector(beamOffset, dir);
        totalTime = 0;
        color = new Color(Color.RED);
        points = new Array<>();
        lights = new ChainLight[2];
        initTiledActivations(properties);
    }

    /**
     * Creates ChainLight for with soft and xray true
     * @param rayHandler Ray Handler associated with the currently active box2d world
     */
    public void createLight(RayHandler rayHandler) {
        createChainLight(objectConstants.get("light"), rayHandler, -1);
        getLight().setSoft(true);
        getLight().setXray(true);
        lights[1] = getLightAsChain();
        createChainLight(objectConstants.get("light"), rayHandler, 1);
        getLight().setSoft(true);
        getLight().setXray(true);
        lights[0] = getLightAsChain();
    }

    /**
     * Applies a greyscale effect to the box2dlights associated with this object.
     *
     * @param greyscale  Amount of greyscale to apply: 0 is none, 1 is full.
     */
    @Override
    public void setLightGreyscale(float greyscale){
        for (Light light : lights) {
            light.setColor(lightColor.r * (1 - greyscale) + greyColor * greyscale,
                    lightColor.g * (1 - greyscale) + greyColor * greyscale,
                    lightColor.b * (1 - greyscale) + greyColor * greyscale, lightColor.a);
        }
    }

    /**
     * @return Direction that laser fires in.
     */
    public Direction getDirection(){ return dir; }

    /**
     * Adds a new point to the laser's beam.
     * @param point The point to add.
     */
    public void addBeamPoint(Vector2 point){
        points.add(point);
        for (ChainLight l : lights) {
            l.chain.add(point.x, point.y);
        }
    }

    /**
     * Resets the state of the laser to prepare for raycasting.
     */
    public void beginRayCast(){
        Vector2 beamStart = getBeamStart();
        points.clear();
        points.add(beamStart);
        for (ChainLight l : lights) {
            l.chain.clear();
            l.chain.add(beamStart.x, beamStart.y);
        }
    }

    /**
     * @return The starting point of the beam.
     */
    public Vector2 getBeamStart(){
        return getPosition().add(beamOffset);
    }


    public void drawLaser(GameCanvas canvas){
        if (activated) {
            if (points.size > 1) {
                for (ChainLight l : lights) {
                    // In levels where a wall "blocks" the laser beam, the light can still show up through
                    // the blocking wall. My fix for it now is to just compare the raycasted points of the
                    // laser, and if their distance is too small (0.8 in this case), we just set the light
                    // color to transparent to avoid drawing it. We don't change light.setActive() here bc
                    // it's different when the laser is actually deactivated vs just blocked.
                    if (points.size == 2 && points.get(0).cpy().sub(points.get(1)).len() < 0.8) l.setColor(Color.CLEAR);
                    else { l.updateChain(); l.setColor(color); }
                }
                canvas.drawFactoryPath(points, thickness, color, drawScale.x, drawScale.y);
                canvas.drawFactoryPath(points, thickness*0.3f, Color.WHITE, drawScale.x, drawScale.y);
            }
        }
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
    public void setActivated(boolean activated) {
        this.activated = activated;
        for (ChainLight l : lights) {
            if (l != null) {
                l.setActive(activated);
            }
        }
    }

    @Override
    public boolean isActivated() { return activated; }

    @Override
    public void setInitialActivation(boolean initialActivation){ this.initialActivation = initialActivation; }

    @Override
    public float getXPos() {
        return getX();
    }

    @Override
    public float getYPos() {
        return getY();
    }

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
