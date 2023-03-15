package edu.cornell.gdiac.game;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.object.*;

import java.util.HashMap;

public class ActionController {
    /** The boundary of the world */
    protected Rectangle bounds;
    /** The world scale */
    protected Vector2 scale;

    /** The hashmap for texture regions */
    private HashMap<String, TextureRegion> textureRegionAssetMap;
    /** The hashmap for sounds */
    private HashMap<String, Sound> soundAssetMap;
    /** The hashmap for fonts */
    private HashMap<String, BitmapFont> fontAssetMap;
    /** The JSON value constants */
    private JsonValue JSONconstants;
    /** JSON representing the level */
    private JsonValue levelJV;
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
     * Sets the hashmaps for Texture Regions, Sounds, Fonts, and sets JSON value constants
     *
     * @param tMap the hashmap for Texture Regions
     * @param fMap the hashmap for Fonts
     * @param sMap the hashmap for Sounds
     * @param constants the JSON value for constants
     */
    public void setAssets(HashMap<String, TextureRegion> tMap, HashMap<String, BitmapFont> fMap,
                          HashMap<String, Sound> sMap, JsonValue constants, JsonValue levelJV){
        textureRegionAssetMap = tMap;
        fontAssetMap = fMap;
        soundAssetMap = sMap;
        JSONconstants = constants;
        this.levelJV = levelJV;
    }

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
        Cat cat = level.getCat();
        cat.setMovement(InputController.getInstance().getHorizontal() *cat.getForce() * (cat.getIsClimbing() ? 0 : 1));
        cat.setVerticalMovement(InputController.getInstance().getVertical() * cat.getForce());
        cat.setHorizontalMovement(InputController.getInstance().getHorizontal() * cat.getForce());
        cat.setJumping(InputController.getInstance().didPrimary());
        cat.setDashing(InputController.getInstance().didDash());
        cat.setClimbing(InputController.getInstance().didClimb() && cat.isWalled());

        cat.applyForce();
        if (cat.isJumping()) {
            jumpId = playSound(soundAssetMap.get("jump"), jumpId, volume);
        }

        if (InputController.getInstance().didMeow()){
            meowId = playSound(soundAssetMap.get("meow"), meowId, volume);
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
                    wjoint.collideConnected = false;
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
    public DeadBody die(){
        Cat cat = level.getCat();
        if (!level.getDied()) {
            level.getCat().setJumping(false);
            level.setDied(true);
            // decrement lives
            level.setNumLives(level.getNumLives()-1);
            // 0 lives
            if (level.getNumLives() <= 0) {
                level.resetLives();
                level.setFailure(true);
            } else {
                // create dead body
                DeadBody deadBody = new DeadBody(levelJV.get("cat"), level.getDwidth(), level.getDheight());
                deadBody.setDrawScale(scale);
                deadBody.setTexture(textureRegionAssetMap.get("deadcat"));
                deadBody.setSensor(false);
                deadBody.setLinearVelocity(cat.getLinearVelocity());
                deadBody.setLinearDamping(2f);
                deadBody.setPosition(cat.getPosition());
                level.setNewDeadBody(deadBody);
                level.queueObject(deadBody);
                return deadBody;
            }
        }
        return null;
    }

    /**
     * Actions carried out when the player has died
     * The level model died is set to false
     * The level model new dead body is set to face the same direction as the cat
     * The level model new dead body is added to the array of dead bodies
     *
     */
    public void died() {
        level.setDied(false);
        level.getNewDeadBody().setFacingRight(level.getCat().isFacingRight());
        level.getCat().setPosition(level.getRespawnPos());
        level.getdeadBodyArray().add(level.getNewDeadBody());
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
}
