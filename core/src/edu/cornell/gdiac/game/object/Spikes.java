package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.ObjectMap;

import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.game.*;
import edu.cornell.gdiac.game.obstacle.*;

import java.util.HashMap;

//TODO: make this a ComplexObstacle
public class Spikes extends BoxObstacle implements Activatable {
    /** Constants that are shared between all instances of this class */
    private static JsonValue objectConstants;
    /** Shape of the sensor that kills the player */
    private PolygonShape sensorShape;
    /** Shape of the solid part of the spikes */
    private PolygonShape solidShape;
    /** Fixture of the hitbox that kills the player */
    private Fixture sensorFixture;
    /** Fixture of the solid part of the spikes */
    private Fixture solidFixture;
    /** Set of joints that are attached to this object */
    private ObjectSet<Joint> joints = new ObjectSet<Joint>();
    /** Current activation state */
    private boolean activated;
    /** Starting activation state */
    private boolean initialActivation;

    /**
     * Creates a new spikes object.
     * @param texture        TextureRegion for drawing.
     * @param drawScale      Draw scale for drawing.
     * @param textureScale   Texture scale to resize texture.
     * @param data           JSON for loading.
     */
    public Spikes(TextureRegion texture, Vector2 drawScale, Vector2 textureScale, JsonValue data){
        super(texture.getRegionWidth()/drawScale.x*textureScale.x,
                texture.getRegionHeight()/drawScale.y*textureScale.y);

        setBodyType(BodyDef.BodyType.StaticBody);
        setSensor(true);
        setFixedRotation(true);
        setName("spikes");
        setDrawScale(drawScale);
        setTextureScale(textureScale);
        setTexture(texture);

        Vector2 sensorCenter = new Vector2(objectConstants.get("sensor_offset").getFloat(0),
                objectConstants.get("sensor_offset").getFloat(1));
        sensorShape = new PolygonShape();
        sensorShape.setAsBox(getWidth() / 2 * objectConstants.getFloat("sensor_width_scale"),
                getHeight() / 2 * objectConstants.getFloat("sensor_height_scale"),
                sensorCenter, 0.0f);

        Vector2 solidCenter = new Vector2(objectConstants.get("solid_offset").getFloat(0),
                objectConstants.get("solid_offset").getFloat(1));
        solidShape = new PolygonShape();
        solidShape.setAsBox(getWidth() / 2 * objectConstants.getFloat("solid_width_scale"),
                getHeight() / 2 * objectConstants.getFloat("solid_height_scale"),
                solidCenter, 0.0f);

        setX(data.get("pos").getFloat(0)+objectConstants.get("offset").getFloat(0));
        setY(data.get("pos").getFloat(1)+objectConstants.get("offset").getFloat(1));
        setAngle((float) (data.getFloat("angle") * Math.PI/180));

        initActivations(data);
    }

    public Spikes(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale,
                  int tileSize, int levelHeight, Vector2 textureScale){
        super(tMap.get("spikes").getRegionWidth()/scale.x*textureScale.x,
                tMap.get("spikes").getRegionHeight()/scale.y*textureScale.y);

        setBodyType(BodyDef.BodyType.StaticBody);
        setSensor(true);
        setFixedRotation(true);
        setName("spikes");
        setDrawScale(scale);
        setTextureScale(textureScale);
        setTexture(tMap.get("spikes"));

        Vector2 sensorCenter = new Vector2(objectConstants.get("sensor_offset").getFloat(0),
                objectConstants.get("sensor_offset").getFloat(1));
        sensorShape = new PolygonShape();
        sensorShape.setAsBox(getWidth() / 2 * objectConstants.getFloat("sensor_width_scale"),
                getHeight() / 2 * objectConstants.getFloat("sensor_height_scale"),
                sensorCenter, 0.0f);

        Vector2 solidCenter = new Vector2(objectConstants.get("solid_offset").getFloat(0),
                objectConstants.get("solid_offset").getFloat(1));
        solidShape = new PolygonShape();
        solidShape.setAsBox(getWidth() / 2 * objectConstants.getFloat("solid_width_scale"),
                getHeight() / 2 * objectConstants.getFloat("solid_height_scale"),
                solidCenter, 0.0f);

        setX((float) properties.get("x")/tileSize+objectConstants.get("offset").getFloat(0));
        setY(levelHeight - (float) properties.get("y")/tileSize+objectConstants.get("offset").getFloat(1));
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

        FixtureDef solidDef = new FixtureDef();
        solidDef.density = 0;
        solidDef.shape = solidShape;
        solidFixture = body.createFixture( solidDef );

        //create sensor
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.density = 0;
        sensorDef.isSensor = true;
        sensorDef.shape = sensorShape;
        sensorFixture = body.createFixture( sensorDef );
        sensorFixture.setUserData(this);
    }

    /**
     * Release solid fixture and sensor fixture.
     */
    protected void releaseFixtures(){
        super.releaseFixtures();
        if (sensorFixture != null) {
            body.destroyFixture(sensorFixture);
            sensorFixture = null;
        }
        if (solidFixture != null) {
            body.destroyFixture(solidFixture);
            solidFixture = null;
        }
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
            canvas.drawPhysics(solidShape, Color.YELLOW, getX()-xTranslate, getY()-yTranslate, getAngle(), drawScale.x, drawScale.y);
            canvas.drawPhysics(sensorShape, Color.RED, getX()-xTranslate, getY()-yTranslate, getAngle(), drawScale.x, drawScale.y);
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

}
