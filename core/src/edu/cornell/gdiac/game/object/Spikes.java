package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.game.*;
import edu.cornell.gdiac.game.obstacle.*;
import edu.cornell.gdiac.util.Direction;

import java.util.HashMap;

/**
 * Represents a spikes object. Note that their must be something behind the base of the spikes, due to how they close.
 */
public class Spikes extends BoxObstacle implements Activatable {
    /** Constants that are shared between all instances of this class */
    private static JsonValue objectConstants;
    /** Array of sensor shapes used for debugging */
    private Array<PolygonShape> fixtureShapes;
    /** Set of joints that are attached to this object */
    private ObjectSet<Joint> joints = new ObjectSet<>();
    /** Current activation state */
    private boolean activated;
    /** Starting activation state */
    private boolean initialActivation;
    public static final String solidName = "spikesSolid";
    public static final String pointyName = "spikesPointy";
    public static final String centerName = "spikesCenter";
    public static final String textureShapeName = "spikesTexture";
    /** The total number of ticks for the spikes to open/close */
    private static final int totalTicks = 7;
    /** ticks/totalTicks represents the fraction of the spikes showing */
    private float ticks;
    /** 1 if closing, -1 if opening, 0 if static */
    private float closing;
    private Direction dir;
    /** initial x position */
    private final float x;
    /** initial y position */
    private final float y;
    private static HashMap<Integer, Integer> gidMap = new HashMap<>();
    private static TextureRegion[][] tileset;

    /**
     * Creates a new Spikes object.
     *
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param textureScale   Texture scale for rescaling texture
     */
    public Spikes(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, Vector2 textureScale){
        super(1, 1);
        setBodyType(properties.containsKey("attachName") ? BodyDef.BodyType.DynamicBody : BodyDef.BodyType.StaticBody);
        setSensor(true);
        setFixedRotation(true);
        setName("spikes");
        setDrawScale(scale);
        setTextureScale(textureScale);
        if (tileset == null) tileset = tMap.get("spikes").split(tMap.get("spikes").getTexture(),(int) (scale.x/textureScale.x), (int) (scale.y/textureScale.y));
        setTexture(tileset[0][gidMap.get(properties.get("gid"))]);
        setFriction(objectConstants.getFloat("friction"));
        fixtureShapes = new Array<>();

        dir = Direction.angleToDir((int) ((float) properties.get("rotation", 0f)));
        Vector2 offset = new Vector2(objectConstants.get("offset").getFloat(0), objectConstants.get("offset").getFloat(1));
        Direction.rotateVector(offset, dir);
        setX((float) properties.get("x") + offset.x);
        setY((float) properties.get("y") + offset.y);
        x = getX();
        y = getY();
        ticks = totalTicks;
        closing = 0;
        setAngle((float) ((float) properties.get("rotation") * Math.PI/180));
//        System.out.println(getPosition());
        initTiledActivations(properties);
    }

    /**
     * Moves up/down if currently activating/deactivating.
     *
     * @param dt Timing values from parent loop
     */
    public void update(float dt){
        super.update(dt);
        ticks += closing;
        if (ticks <= 0){
            setActive(false);
            closing = 0;
            ticks = 0;
            return;
        }
        if (ticks >= totalTicks){
            ticks = totalTicks;
            closing = 0;
        }
        switch (dir) {
            case LEFT:
                setX(x + (1-ticks / totalTicks));
                break;
            case UP:
                setY(y - (1-ticks / totalTicks));
                break;
            case DOWN:
                setY(y + (1-ticks / totalTicks));
                break;
            case RIGHT:
                setX(x - (1-ticks / totalTicks));
                break;
        }

    }

    /**
     * Turn on spikes.
     * @param world the box2D world
     */
    @Override
    public void activated(World world){
        setActive(true);
        closing = 1;
    }

    /**
     * Turn off spikes.
     * @param world the box2D world
     */
    @Override
    public void deactivated(World world){ closing = -1; }

    /**
     * Creates the physics body for this object, adding them to the world. Immediately deactivates
     * self if necessary.
     * @param world Box2D world to store body
     *
     * @return      true if object allocation succeeded
     */
    public boolean activatePhysics(World world){
        if (!super.activatePhysics(world)) {
            return false;
        }
        if (!activated) {
            deactivated(world);
            setActive(false);
        }
        return true;
    }

    /**
     * Create solid fixture and sensor fixture.
     */
    protected void createFixtures(){
        super.createFixtures();

        body.getFixtureList().get(0).setUserData(textureShapeName);

        //solid fixture
        float hx = getWidth()*objectConstants.get("solid_scale").getFloat(0)/2f;
        float hy = getHeight()*objectConstants.get("solid_scale").getFloat(1)/2f;
        Fixture fix = generateFixture(new Vector2(0, hy - getHeight()/2f), hx, hy, solidName, false);
        fix.setFriction(objectConstants.getFloat("friction"));

        //pointy fixture - the part that actually kills the player
        hx = getWidth()*objectConstants.get("pointy_scale").getFloat(0)/2f;
        hy = getHeight()*objectConstants.get("pointy_scale").getFloat(1)/2f;
        generateFixture(Vector2.Zero, hx, hy, pointyName, true);

        //center fixture - the part that dead bodies weld to
        hx = getWidth()*objectConstants.get("center_scale").getFloat(0)/2f;
        hy = getHeight()*objectConstants.get("center_scale").getFloat(1)/2f;
        generateFixture(new Vector2(0, hy - getHeight()/2f), hx, hy, centerName, false);
    }


    private Fixture generateFixture(Vector2 location, float hx, float hy, String name, boolean sensor){
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.friction = 0;
        fixtureDef.density = 0;
        fixtureDef.isSensor = sensor;
        PolygonShape fixtureShape = new PolygonShape();
        fixtureShape.setAsBox(hx, hy, location, 0.0f);
        fixtureDef.shape = fixtureShape;

        Fixture fixture = body.createFixture(fixtureDef);
        fixture.setUserData(name);
        fixtureShapes.add(fixtureShape);
        return fixture;
    }

    /** Destroy all joints connected to this spike */
    public void destroyJoints(World world){
        for (Joint j : joints) {
            world.destroyJoint(j);
        }
        joints.clear();
    }

    /** Clears the joints array */
    public void clearJoints(){ joints.clear(); }

    public void addJoint(Joint joint){ joints.add(joint); }

    public ObjectSet<Joint> getJoints() {return joints;}
    /**
     * Draws the outline of the physics body.
     *
     * This method can be helpful for understanding issues with collisions.
     *
     * @param canvas Drawing context
     */
    public void drawDebug(GameCanvas canvas) {
        super.drawDebug(canvas);
//        float xTranslate = (canvas.getCamera().getX()-canvas.getWidth()/2)/drawScale.x;
//        float yTranslate = (canvas.getCamera().getY()-canvas.getHeight()/2)/drawScale.y;
        if (activated) {
//            System.out.println(drawScale);
            for (PolygonShape shape : fixtureShapes) {
                canvas.drawPhysics(shape, Color.RED, getX(), getY(),
                        getAngle(), drawScale.x, drawScale.y);
            }
        }
    }

    @Override
    public void draw(GameCanvas canvas) {
        if (isActive()) {
            super.draw(canvas);
        }
    }

    //region ACTIVATABLE METHODS
    @Override
    public void setActivated(boolean activated){ this.activated = activated; }

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
        gidMap.put(7, 0);
        gidMap.put(10, 1);
        gidMap.put(11, 3);
        gidMap.put(12, 2);
    }
    public float getXPos(){
        return getX();
    }
    public float getYPos(){
        return getY();
    }

    public ObjectMap<String, Object> storeState(){
        ObjectMap<String, Object> stateMap = super.storeState();
        stateMap.put("ticks", ticks);
        stateMap.put("closing", closing);
        stateMap.put("activated", activated);
        return stateMap;
    }

    public void loadState(ObjectMap<String, Object> stateMap){
        super.loadState(stateMap);
        ticks = (float) stateMap.get("ticks");
        closing = (float) stateMap.get("closing");
        joints.clear();
    }
}
