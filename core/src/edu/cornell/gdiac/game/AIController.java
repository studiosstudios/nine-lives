package edu.cornell.gdiac.game;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.sun.org.apache.xpath.internal.operations.Bool;
import edu.cornell.gdiac.game.object.Cat;
import edu.cornell.gdiac.game.object.Mob;
import edu.cornell.gdiac.game.obstacle.*;


public class AIController {
    /**
     * Enumeration to encode the finite state machine.
     */
    private static enum FSMState {
        /** The mob just spawned */
        SPAWN,
        /** The mob is patrolling around without a target */
        WANDER,
        /** The mob has a target, but must get closer */
        CHASE
    }

    // Instance Attributes
    /** The ship's current state in the FSM */
    private FSMState state;
    /** The mob being controlled by this AIController */
    private Mob mob;
    /** The level; used for pathfinding */
    private Level level;
    /** The target Cat (to chase). */
    private Cat target;
    /** How much did we move horizontally? */
    private float horizontal;
    /** The crosshair position (for raddoll) */
    private Vector2 crosshair;
    /** The crosshair cache (for using as a return value) */
    private Vector2 crosscache;

    private static final float MOVE_CONSTANT = 0.02f;



    /**
     * Creates an AIController for the ship with the given id.
     *
     * @param level The level model (for pathfinding)
     */
    public AIController(Level level, Mob mob, Boolean aggressive) {
        this.level = level;
        this.mob = mob;

        this.state = FSMState.SPAWN;

        // Select an initial target
        target = null;
    }

    /* Returns mob of this AI Controller*/
    public Mob getMob() {
        return mob;
    }

    public float getAction() {
        changeStateifApplicable();
//        System.out.println(getHorizontal());
        return getHorizontal();
    }

    public void changeStateifApplicable() {
        // Current mob coords
//        float pos_x = mob.getX();
//        float pos_y = mob.getY();

        if (state == FSMState.SPAWN) {
            this.state = FSMState.WANDER;
        }
        else if (state == FSMState.WANDER) {

            if (mob.isAggressive()) {
                // Get a target
                selectTarget();
                if (target != null) {
                    this.state = FSMState.CHASE;
                }
            }
            // doesn't go into CHASE state, continues walking in same dir
            // check if there's anything blocking it in collision controller
            if (horizontal >= 0) {
                horizontal = MOVE_CONSTANT;
            } else {
                horizontal = -MOVE_CONSTANT;
            }
        }
        else if (state == FSMState.CHASE) {
//            System.out.println("CHASING!!!");

            // Should go back to wander if it killed the cat
            if (mob.getX() <= target.getX()) {
                // target is right of mob, mob moves right a little faster
                horizontal = MOVE_CONSTANT*2;
            } else {
                // target is left of mob, mob moves lefts a little faster
                horizontal = -MOVE_CONSTANT*2;
            }
        } else {
            state = FSMState.WANDER; // If debugging is off
        }

//
//        switch (state) {
//            case SPAWN:
//                // Should switch to wander immediately
//
//
//            case WANDER:
//                // Only switch out of WANDER if the mob is aggressive
//                // Otherwise it just goes back and forth
//
//
//            case CHASE:
//                // Should go back to wander if it killed the cat
//                if (mob.getX() <= target.getX()) {
//                    // target is right of mob, mob moves right a little faster
//                    horizontal = 1.5f;
//                } else {
//                    // target is left of mob, mob moves lefts a little faster
//                    horizontal = -1.5f;
//                }
//                break;
//            default:
//                // Unknown or unhandled state, should never get here
//                assert (false);
//                state = FSMState.WANDER; // If debugging is off
//                break;
//        }
    }

    private void selectTarget() {
        Cat targ = null;
        Cat cat = level.getCat();
        float cat_pos_x = level.getCat().getX();
        float cat_pos_y = level.getCat().getY();
        float mob_pos_x = mob.getX();
        float mob_pos_y = mob.getY();
        float buffer_y = level.getCat().getHeight() / 2f;

        // Check that the mob is on the same plane as the Cat (with some buffer)
        if (cat_pos_y <= mob_pos_y + buffer_y || cat_pos_y >= mob_pos_y - buffer_y) {

            // Check that the object is on the same plane as the Cat and Mob (with some buffer)
            for (Obstacle obstacle : level.getObjects()) {
                // Make sure obj is not Cat
                if (obstacle != cat) {
                    float ob_pos_x = obstacle.getX();
                    float ob_pos_y = obstacle.getY();
                    if (ob_pos_y <= mob_pos_y + buffer_y || ob_pos_y >= mob_pos_y - buffer_y) {
                        target = null;
                        return;
                    } else {
                        // Check direction that mob is facing
                        if (mob.isFacingRight() && mob_pos_x <= cat_pos_x) {
                            // Check if obj is to the right of the Mob and to the left of the Cat
                            if (ob_pos_x >= mob_pos_x && ob_pos_x <= cat_pos_x) {
                                // Object is in line of sight between the cat and the mob
                                target = null;
                                return;
                            } else {
                                // the cat is in the line of sight
                                targ = cat;
                            }
                        } else if (!mob.isFacingRight() && mob_pos_x >= cat_pos_x) {
                            // Facing left
                            if (ob_pos_x <= mob_pos_x && ob_pos_x >= cat_pos_x) {
                                // Object is in line of sight between the cat and the mob
                                target = null;
                                return;
                            } else {
                                // the cat is in the line of sight
                                targ = cat;
                            }
                        }
                        else {
                            target = null;
                            return;
                        }
                    }
                }
            }
        }
        target = targ;
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
     * Returns the current position of the crosshairs on the screen.
     *
     * This value does not return the actual reference to the crosshairs position.
     * That way this method can be called multiple times without any fair that
     * the position has been corrupted.  However, it does return the same object
     * each time.  So if you modify the object, the object will be reset in a
     * subsequent call to this getter.
     *
     * @return the current position of the crosshairs on the screen.
     */
    public Vector2 getCrossHair() {
        return crosscache.set(crosshair);
    }

    /**
     * Clamp the cursor position so that it does not go outside the window
     *
     * While this is not usually a problem with mouse control, this is critical
     * for the gamepad controls.
     */
    private void clampPosition(Rectangle bounds) {
        crosshair.x = Math.max(bounds.x, Math.min(bounds.x+bounds.width, crosshair.x));
        crosshair.y = Math.max(bounds.y, Math.min(bounds.y+bounds.height, crosshair.y));
    }

}
