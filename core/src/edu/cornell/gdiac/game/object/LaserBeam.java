package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.*;

public class LaserBeam extends ComplexObstacle {
    /** The initializing data (to avoid magic numbers) */
    private final JsonValue data;

    // Invisible anchor objects
    /** The left side of the bridge */
    private WheelObstacle start = null;
    /** The right side of the bridge */
    private WheelObstacle finish = null;

    // Dimension information
    /** The size of the entire bridge */
    protected Vector2 dimension;
    /** The size of a single plank */
    protected Vector2 planksize;
    /* The length of each link */
    protected float linksize = 1.0f;
    /** The spacing between each link */
    protected float spacing = 0.0f;
    private static final String sensorName = "beamSensor";
    private PolygonShape sensorShape;
    private Vector2 startPos;



    /**
     * Creates a new rope bridge with the given physics data
     *
     * This bridge is straight horizontal. The coordinates given are the
     * position of the leftmost anchor.
     *
     * @param data  	The physics constants for this rope bridge
     * @param lwidth	The plank length
     * @param lheight	The bridge thickness
     */
    public LaserBeam(JsonValue data, float x, float y, float beamlength, float lwidth, float lheight, String name) {
        super(x, y);
//        setName(name);
        this.data = data;

        float x0 = x;
        float y0 = y;
        startPos = new Vector2(x, y);
        planksize = new Vector2(lwidth,lheight);
        setBodyType(BodyDef.BodyType.StaticBody);
        linksize = planksize.x;
        setSensor(true);

        // Compute the bridge length
        dimension = new Vector2(beamlength,0f);
        float length = dimension.len();
        Vector2 norm = new Vector2(dimension);
        norm.nor();

        // If too small, only make one plank.
        int nLinks = (int)(length / linksize);
        if (nLinks <= 1) {
            nLinks = 1;
            linksize = length;
            spacing = 0;
        } else {
            spacing = length - nLinks * linksize;
            spacing /= (nLinks-1);
        }

        // Create the planks
        planksize.x = linksize;
        Vector2 pos = new Vector2();
        for (int ii = 0; ii < nLinks; ii++) {
            float t = ii*(linksize+spacing) + linksize/2.0f;
            pos.set(norm);
            pos.scl(t);
            pos.add(x0,y0);
            BoxObstacle plank = new BoxObstacle(pos.x, pos.y, planksize.x, planksize.y);
            plank.setBodyType(BodyDef.BodyType.StaticBody);
            plank.setFriction(0f);
            plank.setRestitution(0f);
            plank.setName("plank"+ii);
            plank.setDensity(0f);
            plank.setSensor(true);
            bodies.add(plank);
        }
    }

    /**
     * Creates the joints for this object.
     *
     * This method is executed as part of activePhysics. This is the primary method to
     * override for custom physics objects.
     *
     * @param world Box2D world to store joints
     *
     * @return true if object allocation succeeded
     */
    protected boolean createJoints(World world) {
        assert bodies.size > 0;

        Vector2 anchor1 = new Vector2();
        Vector2 anchor2 = new Vector2(-linksize / 2, 0);

        // Create the leftmost anchor
        // Normally, we would do this in constructor, but we have
        // reasons to not add the anchor to the bodies list.
        Vector2 pos = bodies.get(0).getPosition();
        pos.x -= linksize / 2;
        start = new WheelObstacle(pos.x,pos.y,data.getFloat("pin_radius", 1));
        start.setName("pin0");
        start.setDensity(0f);
        start.setFriction(0f);
        start.setRestitution(0f);
        start.setSensor(true);
        start.setBodyType(BodyDef.BodyType.StaticBody);
        start.activatePhysics(world);

        // Definition for a revolute joint
        RevoluteJointDef jointDef = new RevoluteJointDef();

        // Initial joint

        jointDef.bodyA = start.getBody();
        jointDef.bodyB = bodies.get(0).getBody();
        jointDef.localAnchorA.set(anchor1);
        jointDef.localAnchorB.set(anchor2);
        jointDef.collideConnected = false;
        Joint joint = world.createJoint(jointDef);
        joints.add(joint);

        // Link the planks together
        anchor1.x = linksize / 2;
        for (int ii = 0; ii < bodies.size-1; ii++) {
            //#region INSERT CODE HERE
            // Look at what we did above
            jointDef.bodyA = bodies.get(ii).getBody();
            jointDef.bodyB = bodies.get(ii+1).getBody();
            jointDef.localAnchorA.set(anchor1);
            jointDef.localAnchorB.set(anchor2);
            jointDef.collideConnected = false;
            joint = world.createJoint(jointDef);
            joints.add(joint);
            //#endregion
        }

        // Create the rightmost anchor
        edu.cornell.gdiac.game.obstacle.Obstacle last = bodies.get(bodies.size-1);

        pos = last.getPosition();
        pos.x += linksize / 2;
        finish = new WheelObstacle(pos.x,pos.y,data.getFloat("pin_radius", 1));
        finish.setName("pin1");
        finish.setDensity(0f);
        finish.setFriction(0f);
        finish.setRestitution(0f);
        finish.setSensor(true);
        finish.setBodyType(BodyDef.BodyType.StaticBody);
        finish.activatePhysics(world);

        // Final joint
        anchor2.x = 0;
        jointDef.bodyA = last.getBody();
        jointDef.bodyB = finish.getBody();
        jointDef.localAnchorA.set(anchor1);
        jointDef.localAnchorB.set(anchor2);
        joint = world.createJoint(jointDef);
        joints.add(joint);

        return true;
    }

    public static String getSensorName() {
        return sensorName;
    }

    public boolean activatePhysics(World world) {
        if (!super.activatePhysics(world)) {
            return false;
        }
        for (Joint joint : joints) {
            Vector2 sensorCenter = new Vector2(0,0);
            FixtureDef sensorDef = new FixtureDef();
            sensorDef.density = 0;
            sensorDef.isSensor = true;
            sensorShape = new PolygonShape();
            sensorShape.setAsBox(dimension.x*0.2f*0.08f, dimension.y, sensorCenter, 0.0f);
            sensorDef.shape = sensorShape;
            Fixture sensorFixture = joint.getBodyA().createFixture(sensorDef);
            sensorFixture.setUserData(getSensorName());
        }
        return true;
    }


    /**
     * Destroys the physics Body(s) of this object if applicable,
     * removing them from the world.
     *
     * @param world Box2D world that stores body
     */
    public void deactivatePhysics(World world) {
        super.deactivatePhysics(world);
        if (start != null) {
            start.deactivatePhysics(world);
        }
        if (finish != null) {
            finish.deactivatePhysics(world);
        }
    }

    /**
     * Sets the texture for the individual planks
     *
     * @param texture the texture for the individual planks
     */
    public void setTexture(TextureRegion texture) {
        for(Obstacle body : bodies) {
            ((SimpleObstacle)body).setTexture(texture);
        }
    }

    /**
     * Returns the texture for the individual planks
     *
     * @return the texture for the individual planks
     */
    public TextureRegion getTexture() {
        if (bodies.size == 0) {
            return null;
        }
        return ((SimpleObstacle)bodies.get(0)).getTexture();
    }

    /**
     * Draws the outline of the physics body.
     *
     * This method can be helpful for understanding issues with collisions.
     *
     * @param canvas Drawing context
     */
    public void drawDebug(GameCanvas canvas) {
        super.drawDebug(canvas);
        canvas.drawPhysics(sensorShape, Color.RED,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
    }
}
