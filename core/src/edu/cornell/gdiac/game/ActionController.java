package edu.cornell.gdiac.game;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.object.*;

import java.util.HashMap;

public class ActionController {
    /** The boundary of the world */
    protected Rectangle bounds;
    /** The world scale */
    protected Vector2 scale;
    /** The hashmap for sounds */
    private HashMap<String, Sound> soundAssetMap;
    /** The JSON value constants */
    private JsonValue JSONconstants;
    /** The default sound volume */
    private float volume;
    /** The jump sound */
    private long jumpId = -1;
    /** The plop sound */
    private long plopId = -1;
    /** The pew (fire) sound */
    private long fireId = -1;
    /** The meow sound */
    private long meowId = -1;
    /** The level */
    private Level level;
    private Array<AIController> mobControllers;

    /** fields needed for raycasting */
    private Vector2 rayCastPoint = new Vector2();
    private Fixture rayCastFixture;
    private float closestFraction;
    private Vector2 startPointCache = new Vector2();
    private Vector2 endPointCache = new Vector2();
    /**
     * Creates and initialize a new instance of a ActionController
     *
     * @param bounds    The game bounds in Box2d coordinates
     * @param scale	    The game scale Vector2
     * @param volume    The volume of the game
     */
    public ActionController(Rectangle bounds, Vector2 scale, float volume) {
        this.bounds = bounds;
        this.scale = scale;
        this.volume = volume;
        mobControllers = new Array<>();
    }

    /** sets the volume */
    public void setVolume(float volume) { this.volume = volume; }

    /**
     * Sets the level model
     *
     * @param level The Level model to be set to level
     */
    public void setLevel(Level level){
        this.level = level;
    }

    public void setControllers(Level level) {
        for (Mob mob : level.getMobArray()) {
            mobControllers.add(new AIController(level, mob, mob.isAggressive()));
        }
    }

    /**
     * Sets the hashmaps for Texture Regions, Sounds, Fonts, and sets JSON value constants
     *
     * @param sMap the hashmap for Sounds
     */
    public void setAssets(HashMap<String, Sound> sMap){ soundAssetMap = sMap; }

    /**
     * Called when the Screen is paused.
     *
     * We need this method to stop all sounds when we pause.
     * Pausing happens when we switch game modes.
     */
    public void pause() {
        soundAssetMap.get("jump").stop(jumpId);
        soundAssetMap.get("plop").stop(plopId);
        soundAssetMap.get("fire").stop(fireId);
        soundAssetMap.get("meow").stop(meowId);
    }

    /**
     * The core gameplay loop of this world.
     *
     * This method contains the specific update code for this mini-game. It does
     * not handle collisions, as those are managed by the parent class WorldController.
     * This method is called after input is read, but before collisions are resolved.
     * The very last thing that it should do is apply forces to the appropriate objects.
     *
     * @param dt	Number of seconds since last animation frame
     */
    public void update(float dt){

        //Raycast lasers
        for (Laser l : level.getLasers()){
            if (l.getActivated()) {
                rayCastLaser(l);
            }
        }

        // Process buttons
        for (Activator a : level.getActivators()){
            a.updateActivated();
            if (level.getActivationRelations().containsKey(a.getID())){
                for (Activatable s : level.getActivationRelations().get(a.getID())){
                    s.updateActivated(a.isActive(), level.getWorld());
                }
            }
        }

        // Mob control:
        for (AIController mobControl : mobControllers) {
            Mob mob = mobControl.getMob();
            mob.setPosition(mob.getX() + mobControl.getAction(), mob.getY());
            mob.applyForce();
        }

        Cat cat = level.getCat();
        level.setSpiritMode(InputController.getInstance().holdSwitch());
        if (InputController.getInstance().didSwitch()){
            //switch body
            DeadBody body = level.getNextBody();
            if (body != null){
                level.spawnDeadBody();
                level.setBodySwitched(true);
                cat.setPosition(body.getPosition());
                cat.setLinearVelocity(body.getLinearVelocity());
                cat.setFacingRight(body.isFacingRight());
                body.markRemoved(true);
                level.removeDeadBody(body);
            }
        } else {
            InputController ic = InputController.getInstance();
            cat.setHorizontalMovement(ic.getHorizontal());
            cat.setVerticalMovement(ic.getVertical());
            cat.setJumpPressed(ic.didJump());
            cat.setClimbingPressed(ic.didClimb());
            cat.setDashPressed(ic.didDash());

            cat.updateState();
            cat.applyForce();
            if (cat.isJumping()) {
                jumpId = playSound(soundAssetMap.get("jump"), jumpId, volume);
            }
        }
        if (InputController.getInstance().didMeow()){
            cat.setMeowing(true);
            meowId = playSound(soundAssetMap.get("meow"), meowId, volume);
        }

    }

    /**
     * Finds the points of a laser beam using raycasting. The beam will reflect off of mirrors and stop at any other
     * obstacle (or the edge of the screen). The points are added to the <code>Laser</code> instance which will draw the
     * beam. Also handles any collision logic involving laser beams.
     * @param l The laser to raycast a beam out of
     */
    private void rayCastLaser(Laser l){
        l.beginRayCast();

        //initial beam
        closestFraction = 1;
        startPointCache.set(l.getRayCastStart());
        Laser.Direction dir = l.getDirection();
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

        if (level.getCat().getBody().getFixtureList().contains(rayCastFixture, true)){
            die();
        }
        if (rayCastFixture != null && rayCastFixture.getBody().getUserData() instanceof DeadBody){
            ((DeadBody) rayCastFixture.getBody().getUserData()).touchingLaser();
        }
    }

    /**
     * Sets the target end point of a raycast in the current level starting at a given point.
     * Stores the result into <code>endPointCache</code> for efficiency.
     * @param start
     * @param dir
     */
    private void getRayCastEnd(Vector2 start, Laser.Direction dir){
        switch (dir) {
            case UP:
                endPointCache.set(start.x,bounds.height);
                break;
            case LEFT:
                endPointCache.set(0, start.y);
                break;
            case DOWN:
                endPointCache.set(start.x, 0);
                break;
            case RIGHT:
                endPointCache.set(bounds.width, start.y);
                break;
        }
    }

    /**
     * Fixes a body to spikes
     *
     * @param deadbody the DeadBody to fix to the spikes
     * @param spikes the spikes
     * @param points the points to fix to
     */
    public void fixBodyToSpikes(DeadBody deadbody, Spikes spikes, Vector2[] points) {
        switch ((int) (spikes.getAngle() * 180/Math.PI)) {
            case 0:
            case 90:
            case 270:
                WeldJointDef wjoint = new WeldJointDef();
                for (Vector2 contactPoint : points) {
                    wjoint.bodyA = deadbody.getBody();
                    wjoint.bodyB = spikes.getBody();
                    wjoint.localAnchorA.set(deadbody.getBody().getLocalPoint(contactPoint));
                    wjoint.localAnchorB.set(spikes.getBody().getLocalPoint(contactPoint));
                    wjoint.collideConnected = true;
                    level.queueJoint(wjoint);
                }
                break;
            case 180:
                break;
            default:
                throw new RuntimeException("impossible spikes angle");
        }
    }

    /**
     * Called when a player dies. Removes all input but keeps velocities.
     */
    public void die(){
        if (!level.getDied()) {
            level.setDied(true);
            // decrement lives
            level.setNumLives(level.getNumLives()-1);
            // 0 lives
            if (level.getNumLives() <= 0) {
                level.resetLives();
                level.setFailure(true);
            } else {
                // create dead body
                level.spawnDeadBody();
            }
        }
    }

    /**
     * Actions carried out when the player has died
     * The level model died is set to false
     * The level model cat is set to its respawn position
     */
    public void died() {
        level.setDied(false);
        level.getCat().setPosition(level.getRespawnPos());
        level.getCat().setFacingRight(true);
        level.getCat().setJumpPressed(false);
        level.getCat().setGrounded(true);
        System.out.println(level.getCat().isGrounded());
//        level.getCat().update
    }

    /**
     * Method to ensure that a sound asset is only played once.
     *
     * Every time you play a sound asset, it makes a new instance of that sound.
     * If you play the sounds to close together, you will have overlapping copies.
     * To prevent that, you must stop the sound before you play it again.  That
     * is the purpose of this method.  It stops the current instance playing (if
     * any) and then returns the id of the new instance for tracking.
     *
     * @param sound		The sound asset to play
     * @param soundId	The previously playing sound instance
     *
     * @return the new sound instance for this asset.
     */
    public long playSound(Sound sound, long soundId) {
        return playSound( sound, soundId, 1.0f );
    }

    /**
     * Method to ensure that a sound asset is only played once.
     *
     * Every time you play a sound asset, it makes a new instance of that sound.
     * If you play the sounds to close together, you will have overlapping copies.
     * To prevent that, you must stop the sound before you play it again.  That
     * is the purpose of this method.  It stops the current instance playing (if
     * any) and then returns the id of the new instance for tracking.
     *
     * @param sound		The sound asset to play
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

    private RayCastCallback LaserRayCastCallback = new RayCastCallback() {
        @Override
        /**
         * Gets closest raycasted fixture and stores collision point and the fixture itself
         */
        public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
            if ( fraction < closestFraction ) {
                closestFraction = fraction;
                rayCastPoint.set(point);
                rayCastFixture = fixture;
            }

            return 1;
        }
    };
}
