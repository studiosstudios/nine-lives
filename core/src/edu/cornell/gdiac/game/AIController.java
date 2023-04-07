package edu.cornell.gdiac.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.game.object.*;
import edu.cornell.gdiac.game.obstacle.*;

public class AIController {
    /** The bounds of the world */
    private Rectangle bounds;
    /**
     * Enumeration to encode the finite state machine.
     */
    private enum FSMState {
        /** The mob just spawned */
        SPAWN,
        /** The mob is patrolling around without a target */
        WANDER,
        /** The mob has a target, but must get closer */
        CHASE
    }

    // Instance Attributes
    /** The mob's current state in the FSM */
    private FSMState state;
    /** The mob being controlled by this AIController */
    private Mob mob;
    /** The level; used for pathfinding */
    private Level level;
    /** The target Cat (to chase). */
    private Cat target;
    /** How much did we move horizontally? */
    private float horizontal;
    /** The constant to move the mob by when not chasing */
    private static final float MOVE_CONSTANT = 0.02f;
    /** The detector ray associated with the mob of the controller */
    private MobDetector detectorRay;

    /** fields needed for raycasting */
    private Vector2 rayCastPoint = new Vector2();
    private Fixture rayCastFixture;
    private float closestFraction;
    private Vector2 startPointCache = new Vector2();
    private Vector2 endPointCache = new Vector2();


    /**
     * Creates an AIController for the ship with the given id.
     *
     * @param bounds The bounds of the Box2D world
     * @param level The level model (for pathfinding)
     * @param mob The mob model associated with this controller
     */
    public AIController(Rectangle bounds, Level level, Mob mob) {
        this.bounds = bounds;
        this.level = level;
        this.mob = mob;

        this.state = FSMState.SPAWN;

        // Select an initial target
        target = null;

        // Mob Detector Ray
        detectorRay = mob.getDetectorRay();
    }

    /**
     * Returns the mob of this AI controller
     *
     * @return mob
     */
    public Mob getMob() {
        return mob;
    }

    /**
     * Returns the next action for them mob.
     * First check if a target was detected in the detector ray.
     * Then change the mob's state if applicable.
     *
     * @return the horizontal distance to move by
     */
    public float getAction() {
        detectRayCast(detectorRay);
        changeStateifApplicable();
        return getHorizontal();
    }

    /**
     * Returns the detector ray associated with the mob
     *
     * @return detectorRay
     */
    public MobDetector getDetectorRay() { return detectorRay; }

    /**
     * Changes the state of this mob.
     *
     */
    public void changeStateifApplicable() {

        // Goes straight to WANDER after the mob spawns
        if (state == FSMState.SPAWN) {
            this.state = FSMState.WANDER;
        }
        else if (state == FSMState.WANDER) {
            // If the mob has a target, then switch to CHASE
            if (target != null) {
                if (mob.isAggressive()) {
                    this.state = FSMState.CHASE;
                }
            }

            // doesn't go into CHASE state, continues walking in same dir
            // check if there's anything blocking it in collision controller
            if (mob.isFacingRight()) {
                horizontal = MOVE_CONSTANT;
            } else {
                horizontal = -MOVE_CONSTANT;
            }
        }
        else if (state == FSMState.CHASE) {
            if (target == null) {
                this.state = FSMState.WANDER;
                return;
            }
            // Target is in the detector ray
            if (mob.getX() <= target.getX()) {
                // target is right of mob, mob moves right a little faster
                horizontal = MOVE_CONSTANT*3;
            } else {
                // target is left of mob, mob moves lefts a little faster
                horizontal = -MOVE_CONSTANT*3;
            }
        } else {
            state = FSMState.WANDER;
        }
    }

    /**
     * Returns the amount of sideways movement.
     *
     * -1 = left, 1 = right, 0 = still
     *
     * @return the amount of sideways movement.
     */
    public float getHorizontal() {
        return horizontal;
    }

    /**
     * Sets the end point cache of the detector
     * @param start the start position for the detector
     * @param isRight if the mob is facing right
     */
    private void getRayCastEnd(Vector2 start, Boolean isRight){
        if (isRight) {
            endPointCache.set(bounds.width, start.y);
        } else {
            endPointCache.set(0, start.y);
        }
    }

    /**
     * The main ray cast callback
     *
     */
    private RayCastCallback DetectorRayCastCallback = new RayCastCallback() {
        @Override
        /**
         * Gets closest raycasted fixture and stores collision point and the fixture itself
         */
        public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
            if ( fraction < closestFraction && fixture.getBody() != mob.getBody()) {
                closestFraction = fraction;
                rayCastPoint.set(point);
                rayCastFixture = fixture;
            }
            return 1;
        }
    };

    /**
     * Sees if a target has been detected
     *
     * @param detector the detector ray of the mob
     */
    public void detectRayCast(MobDetector detector) {
        detector.beginRayCast();

        //initial beam
        closestFraction = 1;
        startPointCache.set(detector.getRayCastStart());
        detector.setPointingRight(mob.isFacingRight());
        getRayCastEnd(startPointCache, mob.isFacingRight());
        level.world.rayCast(DetectorRayCastCallback, startPointCache, endPointCache);

        if (closestFraction == 1) {
            rayCastPoint = endPointCache;
            rayCastFixture = null;
        } else {
            Boolean detected = rayCastFixture.getBody().getUserData() instanceof Cat;
            if (detected) {
                target = level.getCat();
            } else {
                target = null;
            }
            detector.setEndPoint(rayCastFixture.getBody().getPosition());

        }
        detector.addBeamPoint(new Vector2(rayCastPoint));
    }
}
