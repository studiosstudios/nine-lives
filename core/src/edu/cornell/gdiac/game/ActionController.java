package edu.cornell.gdiac.game;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.game.object.*;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;
import edu.cornell.gdiac.game.obstacle.Obstacle;
import edu.cornell.gdiac.game.obstacle.WheelObstacle;
import edu.cornell.gdiac.util.PooledList;

import java.util.HashMap;

public class ActionController {
    /** The Box2D world */
    protected World world;
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

    private long meowId = -1;

    /** The level */
    private Level level;





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

    public ActionController(Rectangle bounds, Vector2 scale, float volume) {
        this.bounds = bounds;
        this.scale = scale;
        this.volume = volume;
    }

    public void setLevel(Level level){
        this.level = level;
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

    public void update(float dt){
        Cat avatar = level.getAvatar();
        avatar.setMovement(InputController.getInstance().getHorizontal() *avatar.getForce() * (avatar.getIsClimbing() ? 0 : 1));
        avatar.setVerticalMovement(InputController.getInstance().getVertical() * avatar.getForce());
        avatar.setJumping(InputController.getInstance().didPrimary());
        avatar.setDashing(InputController.getInstance().didDash());
        avatar.setClimbing(InputController.getInstance().didClimb() && avatar.isWalled());

        avatar.applyForce();
        if (avatar.isJumping()) {
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
                    s.updateActivated(a.isActive(), world);
                }
            }
        }
    }

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
        Cat avatar = level.getAvatar();
        if (!level.getDied()) {
            level.getAvatar().setJumping(false);
            level.setDied(true);
//            died = true;
            // decrement lives
            level.setNumLives(level.getNumLives()-1);
//            numLives--;
            // 0 lives
            if (level.getNumLives() <= 0) {
                level.resetLives();
//                numLives = MAX_NUM_LIVES;
                level.setFailure(true);
            } else {
                // create dead body
                DeadBody deadBody = new DeadBody(levelJV.get("cat"), level.getDwidth(), level.getDheight());
                deadBody.setDrawScale(scale);
                deadBody.setTexture(textureRegionAssetMap.get("deadcat"));
                deadBody.setSensor(false);
                deadBody.setLinearVelocity(avatar.getLinearVelocity());
                deadBody.setLinearDamping(2f);
                deadBody.setPosition(avatar.getPosition());
                level.setNewDeadBody(deadBody);
//                newDeadBody = deadBody;
                level.getAddQueue().add(deadBody);
                return deadBody;
            }
        }
        return null;
    }

    public void died() {
        level.setDied(false);
        level.getNewDeadBody().setFacingRight(level.getAvatar().isFacingRight());
        level.getAvatar().setPosition(level.getRespawnPos());
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
