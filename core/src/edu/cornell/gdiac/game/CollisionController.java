package edu.cornell.gdiac.game;

import com.badlogic.gdx.physics.box2d.*;
import edu.cornell.gdiac.game.object.*;
import edu.cornell.gdiac.game.obstacle.Obstacle;

public class CollisionController implements ContactListener, ContactFilter {
    //TODO: consider storing a set of current contacts, to ensure no issues with undoing level states

    /** The Level model */
    private Level level;
    /** The ActionController */
    private ActionController actionController;
    /** Whether should return to previous level */
    private boolean shouldReturn;
    /** Whether the player just progressed to a new level */
    private boolean didChange;

    /** Camera to set zoom for CameraRegions*/
    private Camera camera;

    /**
     * Creates and initialize a new instance of a CollisionController
     *
     * @param actionController The ActionController associated with the level
     */
    public CollisionController(ActionController actionController){
        this.actionController = actionController;
        shouldReturn = false;
        didChange = false;
    }

    /**
     * Sets the level model
     *
     * @param level The Level model to be set to level
     */
    public void setLevel(Level level){
        this.level = level;
        shouldReturn = false;
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
     * Sets if the player just progressed to the next level. This makes this controller ignore the first collision of a
     * cat and a return exit.
     *
     * @param value given to didNext
     */
    public void setDidChange(boolean value) { didChange = value; }

    /**
     * Set camera for current instance of collisionController
     * @param camera camera for current canvas
     */
    public void setCamera(Camera camera){
        this.camera = camera;
    }

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

                    // See if right side is touching a wall
                    if (cat.getRightSensorName().equals(fd1) && bd2 instanceof Wall) {
                        if (((Wall) bd2).isClimbable()){
                            cat.incrementRightWalled();
                        }
                    }

                    // See if left side is touching a wall
                    if (cat.getLeftSensorName().equals(fd1) && bd2 instanceof Wall) {
                        if (((Wall) bd2).isClimbable()){
                            cat.incrementLeftWalled();
                        }
                    }

                    // Check for win condition
                    if (bd2 == level.getGoalExit() && !didChange) level.setComplete(true);
                    if (bd2 == level.getReturnExit() && !didChange) setReturn(true);

                    if (bd2 instanceof Spikes && fd2.equals(Spikes.pointyName) && fd1.equals(Cat.bodyName)) {
                        actionController.die();
                    }
                    if (bd2 instanceof Flamethrower.Flame && fd2.equals(Flamethrower.flameSensorName)){
                        actionController.die();
                    }
                    if (bd2 instanceof Checkpoint && ((Checkpoint) bd2).getSensorName().equals(fd2)){
                        level.updateCheckpoints(((Checkpoint) bd2), true);
                    }
                    if (bd2 instanceof Mob){
//                    System.out.println("hit a mob");
                        actionController.die();
                    }
                    if (bd2 instanceof SpiritRegion){
                        cat.getSpiritRegions().add((SpiritRegion) bd2);
                    }
                    if (bd2 instanceof CameraTile){
                        camera.setZoom(true, ((CameraTile) bd2).getZoom());
                    }
                }

                //dead body collisions
                if (bd1 instanceof DeadBody) {
                    DeadBody db = (DeadBody) bd1;
                    if (bd2 instanceof Spikes) {
                        if (fd1.equals(DeadBody.centerSensorName) && fd2.equals(Spikes.centerName)) {
                            actionController.fixBodyToSpikes(db, (Spikes) bd2, contact.getWorldManifold().getPoints());
                        }
                        if (fd1.equals(DeadBody.catBodyName) && fd2.equals(Spikes.pointyName)){
                            db.addHazard();
                        }
                    } else if (bd2 instanceof Flamethrower.Flame) {
                        db.setBurning(true);
                        db.addHazard();
                    } else if (bd2 instanceof SpiritRegion){
                        db.getSpiritRegions().add((SpiritRegion) bd2);
                    }
                }

                //Add ground fixture to moveable
                if (bd1 instanceof Movable && !fix2.isSensor() && bd1 != bd2 && bd2 != cat && ((Movable) bd1).getGroundSensorName().equals(fd1)){
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
                        cat.getGroundFixtures().remove(fix2);
                        if (cat.getGroundFixtures().size == 0) {
                            cat.setGrounded(false);
                        }
                    }

                    // Right sensor walled
                    if (((cat.getRightSensorName().equals(fd1)) && cat != bd2) && (bd2 instanceof Wall) && ((Wall) bd2).isClimbable()) {
                        cat.decrementRightWalled();
                    }
                    // Left sensor walled
                    if (((cat.getLeftSensorName().equals(fd1)) && cat != bd2) && (bd2 instanceof Wall) && ((Wall) bd2).isClimbable()) {
                        cat.decrementLeftWalled();
                    }

                    if (bd2 instanceof SpiritRegion){
                        cat.getSpiritRegions().remove((SpiritRegion) bd2);
                    }

                    if (bd2 instanceof Exit) {
                        didChange = false;
                    }
                }

                //dead body collisions
                if (bd1 instanceof DeadBody) {
                    DeadBody db = (DeadBody) bd1;
                    if (bd2 instanceof Spikes) {
                        if (fd1.equals(DeadBody.catBodyName) && fd2.equals(Spikes.pointyName)){
                            db.removeHazard();
                        }
                    } else if (bd2 instanceof Flamethrower.Flame) {
                        db.setBurning(false);
                        db.removeHazard();
                    }
                    if (bd2 instanceof SpiritRegion){
                        db.getSpiritRegions().remove((SpiritRegion) bd2);
                    }
                }

                // Check mobs
//        if (fd1 instanceof Mob) {
//            ((Mob) fd1).setFacingRight(!((Mob) fd1).isFacingRight());
//        } else if (fd2 instanceof Mob) {
//            ((Mob) fd1).setFacingRight(!((Mob) fd1).isFacingRight());
//        }

                if (bd1 instanceof Movable && !fix2.isSensor() && bd1 != bd2 && bd2 != cat && ((Movable) bd1).getGroundSensorName().equals(fd1)) {
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

        try {
            Obstacle bd1 = (Obstacle) body1.getUserData();
            Obstacle bd2 = (Obstacle) body2.getUserData();

            for (int i = 0; i < 2; i++) {

                //flame does not turn on activators
                if (fd1 instanceof Activator && bd2 instanceof Flamethrower.Flame) {
                    return false;
                }

                //spikes and dead bodies
                if (bd1 instanceof Spikes && bd2 instanceof DeadBody) {
//                    System.out.println(fd1 + " and " + fd2 + ": " + (fd2.equals(DeadBody.centerSensorName) && fd1.equals(Spikes.centerName)));
                    return !fd1.equals(Spikes.solidName) && !fd2.equals(DeadBody.hitboxSensorName);
                }

                //cat and spikes
                if (bd1 instanceof Spikes && bd2 instanceof Cat) {
                    return !fd1.equals(Spikes.solidName);
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
        return true;
    }
}
