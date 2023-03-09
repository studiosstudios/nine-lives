package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.game.*;
import edu.cornell.gdiac.game.obstacle.*;

public class Spikes extends BoxObstacle {

    private PolygonShape sensorShape;

    private PolygonShape solidShape;

    /** The initializing data (to avoid magic numbers) */
    private final JsonValue data;

    /** if the spikes are active */
    private boolean active;

    /** if the spikes are initially active */
    private final boolean initialActive;

    private Fixture sensorFixture;

    private Fixture solidFixture;

    private ObjectSet<Joint> joints = new ObjectSet<Joint>();

    public Spikes(float x, float y, float angle, boolean active,
                  TextureRegion texture, Vector2 scale, JsonValue data){
        super( x+data.get("offset").getFloat(0),
                y+data.get("offset").getFloat(1),
                texture.getRegionWidth()/scale.x,
                texture.getRegionHeight()/scale.y);
        assert angle % 90 == 0;
        this.data = data;
        this.active = active;
        initialActive = active;

        setAngle((float) (angle * Math.PI/180));
        setBodyType(BodyDef.BodyType.StaticBody);
        setSensor(true);
        setFixedRotation(true);
        setName("spikes");
        setDrawScale(scale);
        setTexture(texture);
    }


    /**
     * Sets the active status of the spikes.
     * @param activatorActive  whether the corresponding activators are active
     */
    public void setActive(boolean activatorActive, World world){

        boolean next = initialActive ^ activatorActive;
        if (next && !active) {
            //state switch from inactive to active
            active = true;
            createFixtures();
        } else if (!next && active){
            //state switch from active to inactive
            active = false;
            releaseFixtures();
            destroyJoints(world);
        }
    }


    public boolean activatePhysics(World world){
        Vector2 sensorCenter = new Vector2(data.get("sensor_offset").getFloat(0),
                data.get("sensor_offset").getFloat(1));
        sensorShape = new PolygonShape();
        sensorShape.setAsBox(getWidth() / 2 * data.getFloat("sensor_width_scale"),
                getHeight() / 2 * data.getFloat("sensor_height_scale"),
                sensorCenter, 0.0f);

        Vector2 solidCenter = new Vector2(data.get("solid_offset").getFloat(0),
                data.get("solid_offset").getFloat(1));
        solidShape = new PolygonShape();
        solidShape.setAsBox(getWidth() / 2 * data.getFloat("solid_width_scale"),
                getHeight() / 2 * data.getFloat("solid_height_scale"),
                solidCenter, 0.0f);

        if (!super.activatePhysics(world)) {
            return false;
        }
        if (!active) {
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
        if (active) {
            canvas.drawPhysics(solidShape, Color.YELLOW, getX(), getY(), getAngle(), drawScale.x, drawScale.y);
            canvas.drawPhysics(sensorShape, Color.RED, getX(), getY(), getAngle(), drawScale.x, drawScale.y);
        }
    }

    @Override
    public void draw(GameCanvas canvas) {
        if (active) {
            super.draw(canvas);
        }
    }



}
