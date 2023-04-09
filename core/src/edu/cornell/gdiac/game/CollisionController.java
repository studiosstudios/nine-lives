package edu.cornell.gdiac.game;

import com.badlogic.gdx.physics.box2d.*;
import edu.cornell.gdiac.game.object.*;
import edu.cornell.gdiac.game.obstacle.Obstacle;

public class CollisionController implements ContactListener, ContactFilter {


    /** The Level model */
    private Level level;
    /** The ActionController */
    private ActionController actionController;
    /** Whether should return to previous level */
    boolean shouldReturn;

    /**
     * Creates and initialize a new instance of a CollisionController
     *
     * @param actionController The ActionController associated with the level
     */
    public CollisionController(ActionController actionController){
        this.actionController = actionController;
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

            for (int i = 0; i < 2; i++){

                //cat collisions
                Cat cat = level.getCat();
                if (bd1 == cat) {

                    // See if we have landed on the ground.
                    if (!fix2.isSensor() && cat.getGroundSensorName().equals(fd1)) {
                        cat.setGrounded(true);
                    }

                    // See if we are touching a wall
                    if (cat.getSideSensorName().equals(fd1) && bd2 instanceof Wall) {
                        if (((Wall) bd2).isClimbable()){
                            cat.incrementWalled();
                        }
                    }

                    // Check for win condition
                    if (bd2 instanceof Exit) {
                        switch (((Exit) bd2).exitType()) {
                            case GOAL:
                                level.setComplete(true);
                                break;
                            case RETURN:
                                setReturn(true);
                                break;
                        }
                    }
                    if (fd2 instanceof Spikes) {
                        actionController.die();
                    }
                    if (fd2 instanceof Flamethrower.Flame){
                        actionController.die();
                    }
                    if (fd2 instanceof Checkpoint){
                        level.updateCheckpoints(((Checkpoint) fd2));
                    }
                    if (bd2 instanceof Mob){
//                    System.out.println("hit a mob");
                        actionController.die();
                    }
                }

                //dead body collisions
                if (fd1 instanceof DeadBody) {
                    DeadBody db = (DeadBody) fd1;
                    if (fd2 instanceof Spikes) {
                        actionController.fixBodyToSpikes(db, (Spikes) fd2, contact.getWorldManifold().getPoints());
                        db.addHazard();
                    } else if (fd2 instanceof Flamethrower.Flame) {
                        db.setBurning(true);
                        db.addHazard();
                    }
                }

                //Add ground fixture to moveable
                if (bd1 instanceof Movable && !fix2.isSensor() && bd1 != bd2 && ((Movable) bd1).getGroundSensorName().equals(fd1)){
                    ((Movable) bd1).getGroundFixtures().add(fix2);
                }

                // TODO: fix collisions when obstacles collide with top and bottom
                // Mob changes direction when hits a wall
                if (bd1 instanceof Mob && !(fd2 instanceof Activator) && !(fd2 instanceof Checkpoint)) {
                    ((Mob) bd1).setFacingRight(!((Mob) bd1).isFacingRight());
                }

                // Activator
                if (fd1 instanceof Activator) {
                    ((Activator) fd1).addPress();
                }

                //swap everything
                Body bodyTemp = body1;
                body1 = body2;
                body2 = bodyTemp;

                Obstacle bdTemp = bd1;
                bd1 = bd2;
                bd2 = bdTemp;

                Object fdTemp = fd1;
                fd1 = fd2;
                fd2 = fdTemp;

                Fixture fixTemp = fix1;
                fix1 = fix2;
                fix2 = fixTemp;
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Callback method for the start of a collision
     *
     * This method is called when two objects cease to touch.  The main use of this method
     * is to determine when the character is NOT on the ground.  This is how we prevent
     * double jumping.
     */
    public void endContact(Contact contact) {
        Fixture fix1 = contact.getFixtureA();
        Fixture fix2 = contact.getFixtureB();

        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        try {
            Obstacle bd1 = (Obstacle) body1.getUserData();
            Obstacle bd2 = (Obstacle) body2.getUserData();

            for (int i = 0; i < 2; i++) {
                //cat collisions
                Cat cat = level.getCat();
                if (bd1 == cat) {

                    if (cat.getGroundSensorName().equals(fd1) && cat != bd2) {
//                cat.getGroundFixtures().remove(fix2);
                        if (cat.getGroundFixtures().size == 0) {
                            cat.setGrounded(false);
                        }
                    }

                    // Not handling case where there may be multiple walls at once
                    if ((cat.getSideSensorName().equals(fd1) && cat != bd2) && (bd2 instanceof Wall) && ((Wall) bd2).isClimbable()) {
                        cat.decrementWalled();
                    }
                }

                //dead body collisions
                if (fd1 instanceof DeadBody) {
                    DeadBody db = (DeadBody) fd1;
                    if (fd2 instanceof Spikes) {
                        db.removeHazard();
                    } else if (fd2 instanceof Flamethrower.Flame) {
                        db.setBurning(false);
                        db.removeHazard();
                    }
                }

                // Check mobs
//        if (fd1 instanceof Mob) {
//            ((Mob) fd1).setFacingRight(!((Mob) fd1).isFacingRight());
//        } else if (fd2 instanceof Mob) {
//            ((Mob) fd1).setFacingRight(!((Mob) fd1).isFacingRight());
//        }

                if (bd1 instanceof Movable && !fix2.isSensor() && bd1 != bd2 && ((Movable) bd1).getGroundSensorName().equals(fd1)) {
                    ((Movable) bd1).getGroundFixtures().remove(fix2);
                }


                // Check for button
                if (fd2 instanceof Activator) {
                    ((Activator) fd2).removePress();
                }

                //swap everything
                Body bodyTemp = body1;
                body1 = body2;
                body2 = bodyTemp;

                Obstacle bdTemp = bd1;
                bd1 = bd2;
                bd2 = bdTemp;

                Object fdTemp = fd1;
                fd1 = fd2;
                fd2 = fdTemp;

                Fixture fixTemp = fix1;
                fix1 = fix2;
                fix2 = fixTemp;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Unused ContactListener method */
    public void postSolve(Contact contact, ContactImpulse impulse) {}
    /** Unused ContactListener method */
    public void preSolve(Contact contact, Manifold oldManifold) {}

    /**Contact Filter method */
    public boolean shouldCollide(Fixture fix1, Fixture fix2) {
        Body body1 = fix1.getBody();
        Body body2 = fix2.getBody();

        Object fd1 = fix1.getUserData();
        Object fd2 = fix2.getUserData();

        Object bd1 = body1.getUserData();
        Object bd2 = body2.getUserData();

        //flame does not turn on activators
        if (fd1 instanceof Activator && bd2 instanceof Flamethrower.Flame ||
                fd2 instanceof Activator && bd1 instanceof Flamethrower.Flame){
            return false;
        }
        return true;
    }
}
