package edu.cornell.gdiac.game;

import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
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
                    System.out.println("cat hit something");
                    System.out.println(bd2);
                    System.out.println(level.getGoal());

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
                        actionController.die(true);
                    }
                    if (bd2 instanceof Flamethrower.Flame && fd2.equals(Flamethrower.flameSensorName)){
                        actionController.die(true);
                    }
                    if (bd2 instanceof Checkpoint && ((Checkpoint) bd2).getSensorName().equals(fd2)){
                        level.updateCheckpoints(((Checkpoint) bd2), true);
                    }
                    if (bd2 instanceof Mob){
                        actionController.die(true);
                    }
                    if (bd2 instanceof SpiritRegion){
                        cat.addSpiritRegion((SpiritRegion) bd2);
                    }
                    if (bd2 instanceof Goal) {
                        //TODO: if not active then collect dead bodies with action controller
                        System.out.println("at the goal");
                        System.out.println(level.getdeadBodyArray());
//                        bd2.setActive(true);
                        actionController.recombineLives();
                    }
                    if (bd2 instanceof CameraRegion){
                        Array<CameraRegion> cameraRegions = level.getCameraRegions();
                        ((CameraRegion) bd2).addFixture();
                        if(!cameraRegions.contains((CameraRegion) bd2,true)){
                            cameraRegions.add((CameraRegion) bd2);
                        }
                        CameraRegion relevantRegion = maxCollidingCamRegion(cameraRegions);
                        camera.setDefaultZoom(relevantRegion.getZoom());
                        camera.setGlideMode("CHANGE_ZOOM");
                        camera.setZoomRate("CAMERA_REGION");
                        if(relevantRegion.shouldSnap()){
                            camera.setGameplayBounds(relevantRegion.getSnapBounds(), relevantRegion.getDrawScale(), true);
                        }
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
                        db.addSpiritRegion((SpiritRegion) bd2);
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
                        cat.removeSpiritRegion((SpiritRegion) bd2);
                    }

                    if (bd2 instanceof Exit) {
                        didChange = false;
                    }

                    if (bd2 instanceof CameraRegion) {
                        ((CameraRegion) bd2).removeFixture();
                        Array<CameraRegion> cameraRegions = level.getCameraRegions();
                        for(int index = 0; index < cameraRegions.size; index++){
                            if(cameraRegions.get(index) == bd2 && cameraRegions.get(index).getFixtureCount() == 0){
                                cameraRegions.removeIndex(index);
                            }
                            break;
                        }
                        if(level.getCameraRegions().isEmpty()){
                            if(level.getCat().isActive()) {
                                camera.setDefaultZoom(Camera.CAMERA_ZOOM);
                                camera.setGameplayBounds(camera.getLevelBounds(), level.getScale(), false);
                            }
                        }
                        else {
                            CameraRegion relevantRegion = maxCollidingCamRegion(cameraRegions);
                            camera.setDefaultZoom(relevantRegion.getZoom());
                            if(relevantRegion.shouldSnap()){
                                camera.setGameplayBounds(relevantRegion.getSnapBounds(), relevantRegion.getDrawScale(), true);
                            }
                            else{
                                camera.setGameplayBounds(camera.getLevelBounds(), relevantRegion.getDrawScale(), false);
                            }
                        }
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
                        db.removeSpiritRegion((SpiritRegion) bd2);
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

    /**
     * Invariant: cameraRegions cannot be empty
     * @param cameraRegions array of camera regions currently in contact with the cat
     * @return camera region with colliding with most amount of cat fixtures
     */
    public CameraRegion maxCollidingCamRegion(Array<CameraRegion> cameraRegions){
        CameraRegion maxCollisionsRegion = cameraRegions.get(0);
        for(int i = 1; i < cameraRegions.size; i++){
            if(cameraRegions.get(i).getFixtureCount() > maxCollisionsRegion.getFixtureCount()){
                maxCollisionsRegion = cameraRegions.get(i);
            }
        }
        return maxCollisionsRegion;
    }
}
