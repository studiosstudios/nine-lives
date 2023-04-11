package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * A Platform is a wall that can be moved.
 */
public class Platform extends Wall implements Activatable {
    /** Current activation state */
    private boolean activated;
    /** Starting activation state */
    private boolean initialActivation;
    /** The location of the platform when deactivated */
    private Vector2 startPoint;
    /** The location of the platform when activated */
    private Vector2 endPoint;
    /** The displacement of the platform when moving */
    private Vector2 disp;
    /** 1 if moving towards end point, -1 if moving towards start point, 0 if static */
    private float moving;
    /** Max speed of the platform */
    private float speed;
    /** Damping factor of the velocity update */
    private float damping;
    /** Target velocity for velocity update */
    private Vector2 targetVel = new Vector2();
    /**
     * Creates a new platform object.
     * @param texture  TextureRegion for drawing.
     * @param scale    Draw scale for drawing.
     * @param data     JSON for loading.
     */
    public Platform(TextureRegion texture, Vector2 scale, JsonValue data) {
        super(texture, scale, data);
        setName("platform");
        setBodyType(BodyDef.BodyType.KinematicBody);
        try {
            disp = new Vector2(data.get("disp").getFloat( 0), data.get("disp").getFloat( 1));
        } catch (NullPointerException e) {
            disp = new Vector2();
        }
        speed = data.getFloat("speed", 5);
        damping = data.getFloat("damping", 0.1f);
        initActivations(data);
    }

    /**
     * Update velocity and target velocity if moving between points.
     * @param dt Timing values from parent loop
     */
    public void update(float dt){
        super.update(dt);
        if (moving == 0) { return; }
        Vector2 target = moving == 1 ? disp : Vector2.Zero;

        //check if should start slowing down to 0
        if (target.dst(getPosition()) - estimateDist(dt) <= 0){
            targetVel.set(0, 0);
        }

        //check if close enough to target pos
        if (getPosition().epsilonEquals(target, 0.01f)){
            moving = 0;
            setPosition(target);
            targetVel.set(0, 0);
            setVX(0);
            setVY(0);
        }

        //check if passing through target pos: velocity is parallel to target velocity and target is between
        //current position and next position
        if (targetVel.dot(getLinearVelocity()) >= 0 && !targetVel.equals(Vector2.Zero) &&
                target.dst(getPosition()) < target.dst(getPosition().add(getLinearVelocity().scl(dt)))) {
            moving = 0;
            setPosition(target);
            setVX(0);
            setVY(0);
            targetVel.set(0, 0);
        }

        //update velocity
        setVX(getVX() + (targetVel.x - getVX()) * damping);
        setVY(getVY() + (targetVel.y - getVY()) * damping);

    }

    /**
     * Estimates the distance this platform will travel if its velocity target is set to 0 at this
     * timestep, i.e. that assuming v_{t+1} = (1-damping) * v_t.
     * @param dt Time between frames
     * @return   Magnitude of distance travelled if velocity target is set to 0 vector.
     */
    private float estimateDist(float dt){ return getLinearVelocity().len()* (1-damping)/damping*dt; }

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
        setPosition(activated ? Vector2.Zero : disp);
        moving = 0;
        return true;
    }

    @Override
    public void activated(World world){
        moving = -1;
        targetVel.set(-disp.x, -disp.y).nor().scl(speed);
    }

    //region ACTIVATBLE METHODS
    public void deactivated(World world){
        moving = 1;
        targetVel.set(disp.x, disp.y).nor().scl(speed);
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

    public ObjectMap<String, Object> storeState(){
        ObjectMap<String, Object> stateMap = super.storeState();
        stateMap.put("moving", moving);
        stateMap.put("targetVel", targetVel.cpy());
        return stateMap;
    }

    public void loadState(ObjectMap<String, Object> stateMap){
        super.loadState(stateMap);
        moving = (float) stateMap.get("moving");
        targetVel.set((Vector2) stateMap.get("targetVel"));
    }

}
