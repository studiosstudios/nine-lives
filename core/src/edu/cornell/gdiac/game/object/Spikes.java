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

import java.util.HashMap;

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

    /**
     * Creates a new Spikes object.
     *
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param textureScale   Texture scale for rescaling texture
     */
    public Spikes(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, Vector2 textureScale){
        super(tMap.get("spikes").getRegionWidth()/scale.x*textureScale.x,
                tMap.get("spikes").getRegionHeight()/scale.y*textureScale.y);

        setBodyType(BodyDef.BodyType.StaticBody);
        setSensor(true);
        setFixedRotation(true);
        setName("spikes");
        setDrawScale(scale);
        setTextureScale(textureScale);
        setTexture(tMap.get("spikes"));
        setFriction(objectConstants.getFloat("friction"));
        fixtureShapes = new Array<>();

        setX((float) properties.get("x")+objectConstants.get("offset").getFloat(0));
        setY((float) properties.get("y")+objectConstants.get("offset").getFloat(1));
        setAngle((float) ((float) properties.get("rotation") * Math.PI/180));
//        System.out.println(getPosition());
        initTiledActivations(properties);
    }

    /**
     * Turn on spikes.
     * @param world the box2D world
     */
    @Override
    public void activated(World world){
        setActive(true);
    }

    /**
     * Turn off spikes.
     * @param world the box2D world
     */
    @Override
    public void deactivated(World world){
        destroyJoints(world);
        setActive(false);
    }

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

    /**
     *
     */
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

    public void addJoint(Joint joint){ joints.add(joint); }


    /**
     * Draws the outline of the physics body.
     *
     * This method can be helpful for understanding issues with collisions.
     *
     * @param canvas Drawing context
     */
    public void drawDebug(GameCanvas canvas) {
        super.drawDebug(canvas);
        float xTranslate = (canvas.getCamera().getX()-canvas.getWidth()/2)/drawScale.x;
        float yTranslate = (canvas.getCamera().getY()-canvas.getHeight()/2)/drawScale.y;
        if (activated) {
//            System.out.println(drawScale);
            for (PolygonShape shape : fixtureShapes) {
                canvas.drawPhysics(shape, Color.RED, getX() - xTranslate, getY() - yTranslate,
                        getAngle(), drawScale.x, drawScale.y);
            }
        }
    }

    @Override
    public void draw(GameCanvas canvas) {
        if (activated) {
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
    public static void setConstants(JsonValue constants) { objectConstants = constants; }
    public float getXPos(){
        return getX();
    }
    public float getYPos(){
        return getY();
    }
}
