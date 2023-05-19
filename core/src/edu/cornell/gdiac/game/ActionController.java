package edu.cornell.gdiac.game;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.game.object.*;
import edu.cornell.gdiac.game.obstacle.Obstacle;
import edu.cornell.gdiac.util.Direction;
import edu.cornell.gdiac.util.PooledList;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Controller that processes in-world player actions and interactions.
 * <br><br>
 * This controller handles all logic having to do with player actions, including movement, along
 * with the results of the player interacting with the world, including laser raycasting, button pressing,
 * and more.
 */
public class ActionController {
    /** The boundary of the world */
    protected Rectangle bounds;
    /** The world scale */
    protected Vector2 scale;
    /** The JSON value constants */
    private JsonValue constantsJSON;
//    /** The hashmap for sounds */
//    private HashMap<String, Sound> soundAssetMap;
//    /** The default sound volume */
//    private float volume;
//    /** The jump sound */
//    private long jumpId = -1;
//    /** The plop sound */
//    private long plopId = -1;
//    /** The pew (fire) sound */
//    private long fireId = -1;
//    /** The meow sound */
//    private long meowId = -1;
    /** The level */
    private Level level;
    private Array<AIController> mobControllers;

    /** Fields needed for raycasting */
    private Vector2 rayCastPoint = new Vector2();
    private Fixture rayCastFixture;
    private float closestFraction;
    private Vector2 startPointCache = new Vector2();
    private Vector2 endPointCache = new Vector2();
    private ObjectMap<DeadBody, Float> hitDeadbodies = new ObjectMap<>();
    private AudioController audioController;

    /**
     * Creates and initialize a new instance of a ActionController
     *
     * @param scale	    The game scale Vector2
     * @param audioController    The audio controller of the game
     */
    public ActionController(Vector2 scale, AudioController audioController) {
        this.scale = scale;
//        this.volume = volume;
        this.audioController = audioController;
        mobControllers = new Array<>();
    }
//
//    /**
//     * Sets the volume for all sounds in the game.
//     * */
//    public void setVolume(float volume) { this.volume = volume; }

    /**
     * Sets the level model
     *
     * @param level The Level model to be set to level
     */
    public void setLevel(Level level){
        this.level = level;
        this.bounds = level.bounds;
    }

    /**
     * Sets the mob AI controllers in the level
     *
     * @param level The target level to associate the AI controllers with
     */
    public void setMobControllers(Level level) {

        mobControllers.clear();
        for (Mob mob : level.getMobArray()) {
            mobControllers.add(new AIController(bounds, level, mob));
        }
    }

    /**
     * Returns array of currently active MobControllers
     * @return Array of active MobControllers with the current level
     */
    public Array<AIController> getMobControllers() {
        return mobControllers;
    }

//    /**
//     * Sets the hashmaps for Texture Regions, Sounds, Fonts, and sets JSON value constants
//     *
//     * @param sMap the hashmap for Sounds
//     */
//    public void setAssets(HashMap<String, Sound> sMap){ soundAssetMap = sMap; }

    /**
     * Called when the Screen is paused.
     * <br><br>
     * We need this method to stop all sounds when we pause.
     * Pausing happens when we switch game modes.
     * //TODO: pause sounds
     */
    public void pause() {
//        soundAssetMap.get("jump").stop(jumpId);
//        soundAssetMap.get("plop").stop(plopId);
//        soundAssetMap.get("meow").stop(meowId);
    }

    /**
     * The core gameplay loop of this world.
     * <br><br>
     * This method contains the specific update code for this mini-game. It does
     * not handle collisions, as those are managed by the parent class WorldController.
     * This method is called after input is read, but before collisions are resolved.
     * The very last thing that it should do is apply forces to the appropriate objects.
     *
     * @param dt	Number of seconds since last animation frame
     */
    public void update(float dt){
        InputController ic = InputController.getInstance();
        Cat cat = level.getCat();

        updateSpiritLine(dt, level.canSwitch && ic.holdSwitch() && !ic.didSwitch());

        for (SpiritRegion sr : level.getSpiritRegionArray()) {
            sr.setSpiritRegionColorOpacity(ic.holdSwitch());
            sr.update();
        }

        if (level.getSpiritParticles().size != 0 && level.getdeadBodyArray().size == 0) {
            moveSpirits();
        }

        if (level.canSwitch && ic.didSwitch()) {
            //switch body
            DeadBody body = level.getNextBody();
            if (body != null && body.isSwitchable()){
//                if (body != null && body.isSwitchable() && body.inSameSpiritRegion(cat.getSpiritRegions())){
                level.spawnDeadBody();
                cat.setPosition(body.getSwitchPosition());
                cat.setLinearVelocity(body.getLinearVelocity());
                cat.setFacingRight(body.isFacingRight());
                cat.setDashTimer(body.getDashTimer());
                level.removeDeadBody(body);
            } else {
                cat.failedSwitch();
            }
        } else {
            cat.setHorizontalMovement(ic.getHorizontal());
            cat.setVerticalMovement(ic.getVertical());
            cat.setJumpPressed(ic.didJump());
            cat.setClimbingPressed(ic.didClimb());
            cat.setDashPressed(level.canDash && ic.didDash());
            cat.setMeowing(ic.didMeow());

            cat.updateState();
            cat.applyForce();

            for (String soundName : cat.getSoundBuffer()) {
                audioController.playSoundEffect(soundName);
            }
            cat.getSoundBuffer().clear();
        }

        //Die if off-screen
        if (level.bounds.y - cat.getY() > 10){
            die(false);
        }

        //Prepare dead bodies for raycasting
        for (DeadBody d: level.getdeadBodyArray()){
            d.setTouchingLaser(false);
            if (level.bounds.y - d.getY() > 10) {
                level.removeDeadBody(d);
            }
            if (d.isRemoved()){
                level.getdeadBodyArray().removeValue(d, true);
            }
        }

        //Raycast lasers
        for (Laser l : level.getLasers()){
            if (l.isActivated()) {
                rayCastLaser(l);
            }
        }

        // Process buttons
        for (Activator a : level.getActivators()){
            a.updateActivated();
            if (level.getActivationRelations().containsKey(a.getID())){
                for (Activatable s : level.getActivationRelations().get(a.getID())){
                    int activated = s.updateActivated(a.isActivating(), level.getWorld());

                    //destroy joints if spikes deactivated
                    if (activated == -1 && s instanceof Spikes){
                        Spikes spikes = (Spikes) s;

                        for (Joint j : spikes.getJoints()){
                            ((DeadBody) j.getBodyA().getUserData()).clearJoints();
                        }

                        spikes.destroyJoints(level.world);
                    }
                }
            }
        }

        // Mob control:
        for (AIController mobControl : mobControllers) {
            Mob mob = mobControl.getMob();
            mob.setPosition(mob.getX() + mobControl.getAction(), mob.getY());
            mob.applyForce();
        }

    }

    /**
     * Updates the start target and end target of the spirit line based on current
     * level state, then updates spirit line. Note that this MUST be called before
     * the player switches body, otherwise the targets will not be updated properly
     * after switching.
     *
     * @param dt           Number of seconds since last animation frame
     * @param spiritMode   true if level is in spirit mode
     */
    public void updateSpiritLine(float dt, boolean spiritMode){
        SpiritLine spiritLine = level.getSpiritLine();
        Cat cat = level.getCat();
        if (level.isSpiritMode()){
            if (!spiritMode) {
                //switch out of spirit mode
                spiritLine.setStart(cat.getPosition());
            } else {
                spiritLine.startTarget.set(cat.getPosition());
                spiritLine.setStart(cat.getPosition());
                DeadBody nextDeadBody = level.getNextBody();
                if (nextDeadBody != null) {
                    spiritLine.endTarget.set(nextDeadBody.getPosition());
                } else {
                    spiritLine.endTarget.set(cat.getPosition());
                    if (spiritLine.reachedTargets(1f)) {
                        spiritLine.setEnd(cat.getPosition());
                        spiritLine.resetMidpoints();
                    }
                }
            }
        } else {
            if (spiritMode){
                //switch into spirit mode
                spiritLine.setEnd(cat.getPosition());
                spiritLine.setStart(cat.getPosition());
                spiritLine.resetMidpoints();
            } else {
                spiritLine.endTarget.set(cat.getPosition());
                spiritLine.startTarget.set(cat.getPosition());
            }
        }
        level.setSpiritMode(spiritMode);
        spiritLine.update(dt, spiritMode);
    }

    /**
     * Updates all objects in the level.
     * @param dt  Number of seconds since last animation frame
     */
    public void postUpdate(float dt){
        // Garbage collect the deleted objects.
        // Note how we use the linked list nodes to delete O(1) in place.
        // This is O(n) without copying.
        Iterator<PooledList<Obstacle>.Entry> iterator = level.getObjects().entryIterator();
        ObjectMap<Obstacle, Boolean> grounded = new ObjectMap<>();
        while (iterator.hasNext()) {
            PooledList<Obstacle>.Entry entry = iterator.next();
            Obstacle obj = entry.getValue();
            if (obj.isRemoved()) {
                obj.deactivatePhysics(level.getWorld());
                entry.remove();
            } else {
                // Note that update is called last!
                obj.update(dt);

                // Update base velocity.
                if (obj instanceof Movable && ((Movable) obj).isMovable()) {
                    updateBaseVelocity(obj, grounded);
                }

            }
        }
    }

    /**
     * Updates the base velocity of a movable obstacle. If the obstacle is not grounded, resets its base velocity to
     * zero without modifying its linear velocity. If the obstacle is grounded, its base velocity is set to the average
     * of the linear velocities of its grounds, and its linear velocity is changed accordingly.
     * <br><br>
     * If any of the ground fixtures of this obstacle are also movable, this method first recursively updates their
     * base velocities and stores the result in a hashmap. An obstacle is therefore considered grounded if either it
     * has no ground fixtures, or any of its ground fixtures are grounded.
     *
     * @param obj       The obstacle to update
     * @param grounded  Map where keys are updated obstacles (at this tick), and values are if obstacle is grounded.
     *                  This prev
     * @return          True if obstacle is grounded
     */
    public boolean updateBaseVelocity(Obstacle obj, ObjectMap<Obstacle, Boolean> grounded){
        if (grounded.containsKey(obj)) { return grounded.get(obj); }

        Vector2 baseVel = new Vector2();
        ObjectSet<Fixture> fixtures =  ((Movable) obj).getGroundFixtures();
        float numGrounded = 0;

        if (fixtures.size > 0) {

            for (Fixture f : fixtures) {
                Obstacle groundObs = (Obstacle) f.getBody().getUserData();
                //this can cause a stack overflow error if two objects are each other's grounds, which can happen if objects are moving very very fast
                grounded.put(obj, false);
                if (!(groundObs instanceof Movable) || updateBaseVelocity(groundObs, grounded)){
                    numGrounded++;
                    baseVel.add(groundObs.getLinearVelocity());
                }

                //wake up if on opening door
                if (!obj.isAwake() && groundObs instanceof Door && ((Door) groundObs).isMoving()) {
                    obj.setAwake(true);
                }
            }

            //object is grounded, update base velocity to be average of velocities of grounds
            if (numGrounded > 0 && !baseVel.scl(1f / numGrounded).epsilonEquals(Vector2.Zero, 0.001f)) {
//                obj.setBaseVelocity(baseVel);
                obj.setBaseVX(baseVel.x);
                grounded.put(obj, true);
                return true;
            }

        }

        //object is no longer grounded
        obj.resetBaseVelocity();
        return false;

    }

    /**
     * Finds the points of a laser beam using raycasting. The beam will reflect off of mirrors and stop at any other
     * obstacle (or the edge of the screen). The points are added to the <code>Laser</code> instance which will draw the
     * beam. Also handles any collision logic involving laser beams.
     *
     * @param l The laser to raycast a beam out of
     */
    private void rayCastLaser(Laser l){
        l.beginRayCast();
        hitDeadbodies.clear();

        //initial beam
        closestFraction = 1;
        startPointCache.set(l.getBeamStart());
        Direction dir = l.getDirection();
        getRayCastEnd(startPointCache, dir);
        level.world.rayCast(LaserRayCastCallback, startPointCache, endPointCache);
        boolean reflect = false;
        if (closestFraction == 1) {
            rayCastPoint = endPointCache;
            rayCastFixture = null;
        } else {
            reflect = rayCastFixture.getBody().getUserData() instanceof Mirror;
        }
        l.addBeamPoint(new Vector2(rayCastPoint));

        //reflect off of mirrors
        while(reflect) {
            Mirror mirror = (Mirror) rayCastFixture.getBody().getUserData();
            dir = mirror.reflect(dir);
            if (dir != null) {
                closestFraction = 1;
                startPointCache.set(rayCastPoint);
                getRayCastEnd(startPointCache, dir);
                level.world.rayCast(LaserRayCastCallback, startPointCache, endPointCache);
                if (closestFraction == 1) {
                    rayCastPoint = endPointCache;
                    rayCastFixture = null;
                    reflect = false;
                } else {
                    reflect = rayCastFixture.getBody().getUserData() instanceof Mirror;
                }
                l.addBeamPoint(new Vector2(rayCastPoint));
            } else {
                reflect = false;
            }
        }

    }

    /**
     * Sets the target end point of a raycast in the current level starting at a given point.
     * Stores the result into <code>endPointCache</code> for efficiency.
     *
     * @param start
     * @param dir
     */
    private void getRayCastEnd(Vector2 start, Direction dir){
        switch (dir) {
            case UP:
                endPointCache.set(start.x,bounds.height + bounds.y);
                break;
            case LEFT:
                endPointCache.set(bounds.x, start.y);
                break;
            case DOWN:
                endPointCache.set(start.x, bounds.y);
                break;
            case RIGHT:
                endPointCache.set(bounds.width + bounds.x, start.y);
                break;
        }
    }

    /**
     * Fixes a body to spikes
     *
     * @param deadBody the DeadBody to fix to the spikes
     * @param spikes the spikes
     * @param points the points to fix to
     */
    public void fixBodyToSpikes(DeadBody deadBody, Spikes spikes, Vector2[] points) {
        switch ((int) (spikes.getAngle() * 180/Math.PI)) {
            case 0:
            case 90:
            case 270:
                WeldJointDef joint = new WeldJointDef();
                for (Vector2 contactPoint : points) {
                    joint.bodyA = deadBody.getBody();
                    joint.bodyB = spikes.getBody();
                    joint.localAnchorA.set(deadBody.getBody().getLocalPoint(contactPoint));
                    joint.localAnchorB.set(spikes.getBody().getLocalPoint(contactPoint));
                    joint.collideConnected = true;
                    level.queueJoint(joint);
                }
                break;
            case 180:
                break;
            default:
                throw new RuntimeException("impossible spikes angle");
        }
    }

    /**
     * Called when a player dies. Decrements lives, and fails level/spawns body when necessary.
     *
     * @param spawn If we should spawn a dead body. This should only be false when dying from falling offscreen
     */
    public void die(boolean spawn) {
        if (!level.getDied()) {
            level.setDied(true);
            // decrement lives
            level.setNumLives(level.getNumLives()-1);
            level.getCat().setLightActive(false);
            // 0 lives
            if (level.getNumLives() <= 0) {
                level.resetLives();
                level.setFailure(true);
            } else {
                // create dead body
                if (spawn) level.spawnDeadBody();
            }
        }
    }

    /**
     * Main function called when cat reaches goal object.
     * Spirit particles are spawned at dead bodies and dead bodies are removed.
     */
    public void recombineLives() {
        if (level.getNumLives() != level.getMaxLives() && level.getdeadBodyArray().size == 0) {
            level.resetLives();
            return;
        }

        if (level.getSpiritParticles().size == 0) {
            while (level.getdeadBodyArray().size != 0) {
                for (DeadBody body: level.getdeadBodyArray()) {
                    Particle spirit = new Particle();
                    spirit.setX(body.getX());
                    spirit.setY(body.getY());
                    level.addSpiritParticle(spirit);
                    //TODO: smooth remove of dead bodies
                    level.removeDeadBody(body);
                }
            }
        }
    }

    /**
     * Moves the spirit particles.
     * The particles go towards the Cat.
     *
     */
    public void moveSpirits() {
        if (level.getSpiritParticles().size != 0) {
            for (Particle spirit : level.getSpiritParticles()) {
                float x = level.getCat().getX() - spirit.getX();
                float y = level.getCat().getY() - spirit.getY();
                float angle = (float) Math.atan((double)y/(double)x);
                spirit.setAngle(angle, 0.25f);
                spirit.move();
                if (Math.abs(spirit.getX() - level.getCat().getX()) <= 1f &&
                    Math.abs(spirit.getY() - level.getCat().getY()) <= 1f) {
                    level.getSpiritParticles().removeValue(spirit, true);
                    level.setNumLives(level.getNumLives() + 1);
                }
            }
        }
    }

    /**

     * Method to ensure that a sound asset is only played once.
     * <br><br>
     * Every time you play a sound asset, it makes a new instance of that sound.
     * If you play the sounds to close together, you will have overlapping copies.
     * To prevent that, you must stop the sound before you play it again.  That
     * is the purpose of this method.  It stops the current instance playing (if
     * any) and then returns the id of the new instance for tracking.
     *
     * @param sound	The sound asset to play
     * @param soundId	The previously playing sound instance
     *
     * @return the new sound instance for this asset.
     */
    public long playSound(Sound sound, long soundId) {
        return playSound( sound, soundId, 1.0f );
    }

    /**
     * Method to ensure that a sound asset is only played once.
     * <br><br>
     * Every time you play a sound asset, it makes a new instance of that sound.
     * If you play the sounds to close together, you will have overlapping copies.
     * To prevent that, you must stop the sound before you play it again.  That
     * is the purpose of this method.  It stops the current instance playing (if
     * any) and then returns the id of the new instance for tracking.
     *
     * @param sound	The sound asset to play
     * @param soundId	The previously playing sound instance
     * @param volume	The sound volume
     *
     * @return the new sound instance for this asset.
     */
    public long playSound(Sound sound, long soundId, float volume) {
        if (soundId != -1) {
            sound.stop( soundId );
        }
        return sound.play(volume);
    }


    /**
     * A RayCastCallback for lasers. Stores the closest fixture hit into <code>rayCastFixture</code>, and
     * the fraction between the start and end of the ray for that fixture into <code>closestFraction</code>.
     * A <code>closestFraction</code> of 1 means that the raycast hit the end of the world, thus no
     * fixture was found.
     */
    private RayCastCallback LaserRayCastCallback = new RayCastCallback() {

        /**
         * Gets closest raycasted fixture and stores collision point and the fixture itself.
         */
        @Override
        public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
            Obstacle obs = (Obstacle) fixture.getBody().getUserData();
            if ( fraction < closestFraction && (!fixture.isSensor() || obs instanceof Cat)) {
                closestFraction = fraction;
                rayCastPoint.set(point);
                rayCastFixture = fixture;
            }

            return 1;
        }
    };
}
