package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;
import edu.cornell.gdiac.game.obstacle.ComplexObstacle;

public class Flamethrower extends ComplexObstacle implements Activatable {
    protected static JsonValue objectConstants;

    protected Flame flame;

    protected BoxObstacle flameBase;

    /** Whether the flamethrower object is on and shooting a flame */
    private boolean isShooting;

    private boolean activated;

    private boolean initialActivation;

    /**
     * Returns true if the flamethrower if shooting
     *
     * @return true if the flamethrower is shooting
     */
    public boolean getShooting() {
        return isShooting;
    }
    /**
     * Sets whether the flamethrower object is firing
     *
     * @param shooting whether the flamethrower is firing
     */
    public void setShooting(boolean shooting) {
        isShooting = shooting;
    }

    public Flamethrower(TextureRegion flamethrowerTexture, TextureRegion flameTexture, Vector2 scale, JsonValue data) {
        super();
//        setName("flamethrower");

        this.setFixedRotation(false);
        flameBase = new BoxObstacle(flamethrowerTexture.getRegionWidth()/scale.x, flamethrowerTexture.getRegionHeight()/scale.y);
        flameBase.setDrawScale(scale);
        flameBase.setTexture(flamethrowerTexture);
        flameBase.setBodyType(BodyDef.BodyType.StaticBody);
//        flameBase.setAngle((float) (angle * Math.PI/180));
        flameBase.setFriction(0f);
        flameBase.setRestitution(0f);
        flameBase.setName("flamethrower");
        flameBase.setDensity(0f);

        flame = new Flame(flameTexture, scale, data);
        bodies.add(flameBase);
        bodies.add(flame);

        flameBase.setX(data.get("pos").getFloat(0));
        flameBase.setY(data.get("pos").getFloat(1) +
                (flame.getHeight()*objectConstants.getFloat("base_y_offset_scale")));
        initActivations(data);
//        this.setAngle((float) (angle * Math.PI/180));
    }

    /**
     * Creates the joints for this object.
     * <p>
     * This method is executed as part of activePhysics. This is the primary method to
     * override for custom physics objects.
     *
     * @param world Box2D world to store joints
     * @return true if object allocation succeeded
     */
    @Override
    protected boolean createJoints(World world) {
        assert bodies.size > 0;

        WeldJointDef jointDef = new WeldJointDef();

        jointDef.bodyA = flameBase.getBody();
        jointDef.bodyB = flame.getBody();
        jointDef.localAnchorA.set(new Vector2());
        jointDef.localAnchorB.set(new Vector2(0f, -flame.getHeight()*0.5f));
        jointDef.collideConnected = false;
        Joint joint = world.createJoint(jointDef);
        joints.add(joint);

        return true;
    }

    /**
     * Updates the object's physics state (NOT GAME LOGIC).
     *
     * We use this method to reset cooldowns.
     *
     * @param dt	Number of seconds since last animation frame
     */
    public void update(float dt) {

    }

    /** TODO: turn on flames */
    @Override
    public void activated(World world){}

    /** TODO: turn off flames */
    @Override
    public void deactivated(World world){}

    @Override
    public void draw(GameCanvas canvas){
        if (activated){
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
