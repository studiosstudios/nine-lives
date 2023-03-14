package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.game.*;
import edu.cornell.gdiac.game.obstacle.*;

public class Spikes extends BoxObstacle implements Activatable {

    protected static JsonValue objectConstants;
    private PolygonShape sensorShape;

    private PolygonShape solidShape;

    private Fixture sensorFixture;

    private Fixture solidFixture;

    private ObjectSet<Joint> joints = new ObjectSet<Joint>();

    private boolean activated;

    private boolean initialActivation;

    public Spikes(TextureRegion texture, Vector2 scale, JsonValue data){
        super(texture.getRegionWidth()/scale.x,
                texture.getRegionHeight()/scale.y);

        setBodyType(BodyDef.BodyType.StaticBody);
        setSensor(true);
        setFixedRotation(true);
        setName("spikes");
        setDrawScale(scale);
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


    @Override
    public void activated(World world){
        createFixtures();
    }

    @Override
    public void deactivated(World world){
        releaseFixtures();
        destroyJoints(world);
    }

    public boolean activatePhysics(World world){
        if (!super.activatePhysics(world)) {
            return false;
        }
        if (!activated) {
            releaseFixtures();
        }
        return true;
    }

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

    /** destroy all joints connected to this spike
     * is it weird that the spikes can access the world like this?
     * potentially can be fixed with setActive returning a list of joints to destroy
     * that the controller then destroys */
    public void destroyJoints(World world){
        for (Joint j : joints) {
            world.destroyJoint(j);
        }
        joints.clear();
    }

    public void addJoint(Joint joint){ joints.add(joint); }


    /**
     * Draws the outline of the physics body.d
     *
     * This method can be helpful for understanding issues with collisions.
     *
     * @param canvas Drawing context
     */
    public void drawDebug(GameCanvas canvas) {
        super.drawDebug(canvas);
        if (activated) {
            canvas.drawPhysics(solidShape, Color.YELLOW, getX(), getY(), getAngle(), drawScale.x, drawScale.y);
            canvas.drawPhysics(sensorShape, Color.RED, getX(), getY(), getAngle(), drawScale.x, drawScale.y);
        }
    }

    @Override
    public void draw(GameCanvas canvas) {
        if (activated) {
            super.draw(canvas);
        }
    }

    @Override
    public void setActivated(boolean activated){ this.activated = activated; }

    @Override
    public boolean getActivated() { return activated; }

    @Override
    public void setInitialActivation(boolean initialActivation){ this.initialActivation = initialActivation; }

    @Override
    public boolean getInitialActivation() { return initialActivation; }

    public static void setConstants(JsonValue constants) { objectConstants = constants; }

}
