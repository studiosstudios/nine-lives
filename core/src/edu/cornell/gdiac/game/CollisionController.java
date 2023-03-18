package edu.cornell.gdiac.game;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.game.object.*;
import edu.cornell.gdiac.game.obstacle.Obstacle;

public class CollisionController implements ContactListener, ContactFilter {


    /** The Level model */
    private Level level;
    /** The ActionController */
    private ActionController actionController;
    /** Mark set to handle more sophisticated collision callbacks */
    protected ObjectSet<Fixture> sensorFixtures;
    /** Whether should return to previous level */
    boolean shouldReturn;

    /**
     * Creates and initialize a new instance of a CollisionController
     *
     * @param actionController The ActionController associated with the level
     */
    public CollisionController(ActionController actionController){
        this.actionController = actionController;
        sensorFixtures = new ObjectSet<>();
        shouldReturn = false;
    }

    /**
     * Sets the level model
     *
     * @param level The Level model to be set to level
     */
    public void setLevel(Level level){
        this.level = level;
    }

    /**
     * Returns whether to return to previous level
     *
     * @return shouldReturn
     */
    public boolean getReturn() { return shouldReturn; }

    /**
     * Sets whether to return to previous level
     *
     * @param value given to shouldReturn
     */
    public void setReturn(boolean value) { shouldReturn = value; }

    /**
     * Callback method for the start of a collision
     *
     * This method is called when we first get a collision between two objects.  We use
     * this method to test if it is the "right" kind of collision.  In particular, we
     * use it to test if we made it to the win door.
     *
     * @param contact The two bodies that collided
     */
    public void beginContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        try {
            Obstacle bd1 = (Obstacle) body1.getUserData();
            Obstacle bd2 = (Obstacle) body2.getUserData();

            //cat collisions
            if (bd1 == level.getCat() || bd2 == level.getCat()) {

                //ensure bd1 and fd1 are cat body and fixtures
                if (bd2 == level.getCat()) {
                    //don't need to swap bd1 and bd2 because we are assuming bd1 == cat
                    bd2 = bd1;

                    Object temp = fd1;
                    fd1 = fd2;
                    fd2 = temp;
                }

                // See if we have landed on the ground.
                if (level.getCat().getGroundSensorName().equals(fd1)) {
                    level.getCat().setGrounded(true);
                    sensorFixtures.add(fix2); // Could have more than one ground
                }

                // See if we are touching a wall
                if (level.getCat().getSideSensorName().equals(fd1) && level.getCat() != bd2) {
                    level.getCat().incrementWalled();
                }

                // Check for win condition
                if (bd2 == level.getGoalDoor()) {
                    level.setComplete(true);
                }
                if (bd2 == level.getRetDoor()) {
                    setReturn(true);
                }

                if (fd2 instanceof Spikes) {
                    actionController.die();
                }
                if (fd2 == Flamethrower.getSensorName()){
                    actionController.die();
                }
                if (fd2 == LaserBeam.getSensorName()) {
                    actionController.die();
                }
            }

            //Check for body
            if (fd1 instanceof DeadBody) {
                if (fd2 instanceof Spikes) {
                    actionController.fixBodyToSpikes((DeadBody) fd1, (Spikes) fd2, contact.getWorldManifold().getPoints());
                } else if (fd2 == Flamethrower.getSensorName()) {
                    ((DeadBody) fd1).setBurning(true);
                }

            } else if (fd2 instanceof DeadBody) {
                if (fd1 instanceof Spikes) {
                    actionController.fixBodyToSpikes((DeadBody) fd2, (Spikes) fd1, contact.getWorldManifold().getPoints());
                } else if (fd1 == Flamethrower.getSensorName()) {
                    ((DeadBody) fd2).setBurning(true);
                }
            }

            // Check for activator
            if (fd2 instanceof Activator) {
                ((Activator) fd2).addPress();
            } else if (fd1 instanceof Activator) {
                ((Activator) fd1).addPress();
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Callback method for the start of a collision
     *
     * This method is called when two objects cease to touch.  The main use of this method
     * is to determine when the characer is NOT on the ground.  This is how we prevent
     * double jumping.
     */
    public void endContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        Object bd1 = body1.getUserData();
        Object bd2 = body2.getUserData();

        if ((level.getCat().getGroundSensorName().equals(fd2) && level.getCat() != bd1) ||
                (level.getCat().getGroundSensorName().equals(fd1) && level.getCat() != bd2)) {
            sensorFixtures.remove(level.getCat() == bd1 ? fix2 : fix1);
            if (sensorFixtures.size == 0) {
                level.getCat().setGrounded(false);
            }
        }

        // Not handling case where there may be multiple walls at once
        if ((level.getCat().getSideSensorName().equals(fd2) && level.getCat() != bd1) ||
                (level.getCat().getSideSensorName().equals(fd1) && level.getCat() != bd2)) {
            level.getCat().decrementWalled();
        }

        //Check for body
        if (fd1 instanceof DeadBody) {
            if (fd2 == Flamethrower.getSensorName()) {
                ((DeadBody) fd1).setBurning(false);
            }

        } else if (fd2 instanceof DeadBody) {
            if (fd1 == Flamethrower.getSensorName()) {
                ((DeadBody) fd2).setBurning(false);
            }
        }

        // Check for button
        if (fd2 instanceof Activator) {
            ((Activator) fd2).removePress();
        } else if (fd1 instanceof Activator) {
            ((Activator) fd1).removePress();
        }
    }

    /** Unused ContactListener method */
    public void postSolve(Contact contact, ContactImpulse impulse) {}
    /** Unused ContactListener method */
    public void preSolve(Contact contact, Manifold oldManifold) {}

    /**Contact Filter method */
    public boolean shouldCollide(Fixture fixtureA, Fixture fixtureB) {
        return true;
    }
}
