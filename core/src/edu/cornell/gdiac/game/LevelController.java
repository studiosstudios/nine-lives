/*
 * PlatformController.java
 *
 * You SHOULD NOT need to modify this file.  However, you may learn valuable lessons
 * for the rest of the lab by looking at it.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * Updated asset version, 2/6/2021
 */
package edu.cornell.gdiac.game;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.audio.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.*;
import edu.cornell.gdiac.game.object.*;

import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.game.obstacle.*;

import javax.swing.*;
import java.util.HashMap;

/**
 * Gameplay specific controller for the platformer game.  
 *
 * You will notice that asset loading is not done with static methods this time.  
 * Instance asset loading makes it easier to process our game modes in a loop, which 
 * is much more scalable. However, we still want the assets themselves to be static.
 * This is the purpose of our AssetState variable; it ensures that multiple instances
 * place nicely with the static assets.
 */
public class LevelController extends WorldController implements ContactListener {
    /** Texture asset for character avatar */
    private TextureRegion avatarTexture;
    /** Texture asset for the spinning barrier */
    private TextureRegion barrierTexture;

    /** Texture asset for the spikes */
    private TextureRegion spikesTexture;
    /** Texture asset for button avatar */
    private TextureRegion buttonTexture;
    /** Texture asset for the bridge plank */
    private TextureRegion bridgeTexture;
    /** Texture asset for the flame of the flamethrower */
    private TextureRegion flameTexture;
    /** Texture asset for the base of the flamethrower */
    private TextureRegion flamethrowerTexture;
    /** Texture asset for the dead cat */
    private TextureRegion deadCatTexture;
    /** Texture asset for the dead cat */
    private TextureRegion backgroundTexture;

    /** The jump sound.  We only want to play once. */
    private Sound jumpSound;
    private long jumpId = -1;
    /** The weapon fire sound.  We only want to play once. */
    private Sound fireSound;
    private long fireId = -1;
    /** The weapon pop sound.  We only want to play once. */
    private Sound plopSound;
    private long plopId = -1;

    /** The meow sound.  We only want to play once. */
    private Sound meowSound;
    private long meowId = -1;
    /** The default sound volume */
    private float volume;

    // Physics objects for the game
    /** Physics constants for initialization */
    private JsonValue constants;
    /** Reference to the character avatar */
    private static Cat avatar;
    /** Reference to the goalDoor (for collision detection) */
    private BoxObstacle goalDoor;
    /**Reference to the returnDoor (for collision detection) */
    private BoxObstacle retDoor;

    /** Mark set to handle more sophisticated collision callbacks */
    protected ObjectSet<Fixture> sensorFixtures;

    /** Level number **/
    private int level;

    public boolean isRet;

    /** object lists - in the future this will be one list maybe */
    private Array<Activator> activatorList;
    private Array<Spikes> spikesList;
    private Array<DeadBody> deadBodyList;

    /** hashmap to represent activator-spike relationships:
     *   keys are activator ids specified in JSON*/
    private HashMap<String, Array<Spikes>> activationRelations;

    private int numLives;
    private static final int MAX_NUM_LIVES = 4;
    private float dwidth;
    private float dheight;
    private Vector2 respawnPos;
    private boolean died;
    private DeadBody new_dead_body;

    /**
     * Creates and initialize a new instance of the platformer game
     *
     * The game has default gravity and other settings
     */
    public LevelController(int level) {
        super(DEFAULT_WIDTH,DEFAULT_HEIGHT,DEFAULT_GRAVITY);
        setDebug(false);
        setComplete(false);
//        System.out.println("ret set to false in Level Controller");
        setRet(false);
        setFailure(false);
        activatorList = new Array<Activator>();
        spikesList = new Array<Spikes>();
        deadBodyList = new Array<DeadBody>();
        activationRelations = new HashMap<String, Array<Spikes>>();
        world.setContactListener(this);
        sensorFixtures = new ObjectSet<Fixture>();
        this.level = level;
        numLives = MAX_NUM_LIVES;
        died = false;
    }

    /**
     * Gather the assets for this controller.
     *
     * This method extracts the asset variables from the given asset directory. It
     * should only be called after the asset directory is completed.
     *
     * @param directory	Reference to global asset manager.
     */
    public void gatherAssets(AssetDirectory directory) {
        avatarTexture  = new TextureRegion(directory.getEntry("platform:cat",Texture.class));
        barrierTexture = new TextureRegion(directory.getEntry("platform:barrier",Texture.class));
        bridgeTexture = new TextureRegion(directory.getEntry("platform:rope",Texture.class));
        spikesTexture = new TextureRegion(directory.getEntry("platform:spikes", Texture.class));
        buttonTexture = new TextureRegion(directory.getEntry("platform:button", Texture.class));
        flameTexture = new TextureRegion(directory.getEntry("platform:flame", Texture.class));
        flamethrowerTexture = new TextureRegion(directory.getEntry("platform:flamethrower", Texture.class));
        deadCatTexture = new TextureRegion((directory.getEntry("platform:deadCat", Texture.class)));
        backgroundTexture = new TextureRegion((directory.getEntry("platform:background", Texture.class)));

        jumpSound = directory.getEntry( "platform:jump", Sound.class );
        fireSound = directory.getEntry( "platform:pew", Sound.class );
        plopSound = directory.getEntry( "platform:plop", Sound.class );
        meowSound = directory.getEntry( "platform:meow", Sound.class );

        setBackground(backgroundTexture.getTexture());

        switch(level) {
            case 1:
                constants = directory.getEntry("platform:constants_l1", JsonValue.class);
                break;
            case 2:
                constants = directory.getEntry("platform:constants_l2", JsonValue.class);
                break;
            default:
                throw new RuntimeException("Invalid level");
        }

        super.gatherAssets(directory);
    }

    /**
     * Resets the status of the game so that we can play again.
     *
     * This method disposes of the world and creates a new one.
     */
    public void reset() {
        Vector2 gravity = new Vector2(world.getGravity() );
        for(Obstacle obj : objects) {
            obj.deactivatePhysics(world);
        }
        objects.clear();
        addQueue.clear();
        world.dispose();

        activatorList.clear();
        spikesList.clear();
        deadBodyList.clear();

        numLives = MAX_NUM_LIVES;
        died = false;

        world = new World(gravity,false);
        world.setContactListener(this);
        setComplete(false);
        boolean temp_ret = isRet();
        setRet(false);
        setFailure(false);

        populateLevel(temp_ret);
    }

    /**
     * Lays out the game geography.
     */
    private void populateLevel(boolean ret) {
        // Add level goal
        dwidth  = goalTile.getRegionWidth()/scale.x;
        dheight = goalTile.getRegionHeight()/scale.y;

        activationRelations = new HashMap<String, Array<Spikes>>();

        JsonValue goal = constants.get("goal");
        JsonValue goalpos = goal.get("pos");
        goalDoor = new BoxObstacle(goalpos.getFloat(0),goalpos.getFloat(1),dwidth,dheight);
        goalDoor.setBodyType(BodyDef.BodyType.StaticBody);
        goalDoor.setDensity(goal.getFloat("density", 0));
        goalDoor.setFriction(goal.getFloat("friction", 0));
        goalDoor.setRestitution(goal.getFloat("restitution", 0));
//        goalDoor.setSensor(true);
        goalDoor.setDrawScale(scale);
        goalDoor.setTexture(goalTile);
        goalDoor.setName("goal");
        addObject(goalDoor);

        JsonValue retgoal = constants.get("ret_goal");
        JsonValue retgoalpos = retgoal.get("pos");
        retDoor = new BoxObstacle(retgoalpos.getFloat(0),retgoalpos.getFloat(1),dwidth,dheight);
        retDoor.setBodyType(BodyDef.BodyType.StaticBody);
        retDoor.setDensity(retgoal.getFloat("density", 0));
        retDoor.setFriction(retgoal.getFloat("friction", 0));
        retDoor.setRestitution(retgoal.getFloat("restitution", 0));
//        goalDoor.setSensor(true);
        retDoor.setDrawScale(scale);
        retDoor.setTexture(goalTile);
        retDoor.setName("ret_goal");
        addObject(retDoor);

        String wname = "wall";
        JsonValue walljv = constants.get("walls");
        JsonValue defaults = constants.get("defaults");
        for (int ii = 0; ii < walljv.size; ii++) {
            PolygonObstacle obj;
            obj = new PolygonObstacle(walljv.get(ii).asFloatArray(), 0, 0);
            obj.setBodyType(BodyDef.BodyType.StaticBody);
            obj.setDensity(defaults.getFloat( "density", 0.0f ));
            obj.setFriction(defaults.getFloat( "friction", 0.0f ));
            obj.setRestitution(defaults.getFloat( "restitution", 0.0f ));
            obj.setDrawScale(scale);
            obj.setTexture(steelTile);
            obj.setName(wname+ii);
            addObject(obj);
        }

        String pname = "platform";
        JsonValue platjv = constants.get("platforms");
        for (int ii = 0; ii < platjv.size; ii++) {
            PolygonObstacle obj;
            obj = new PolygonObstacle(platjv.get(ii).asFloatArray(), 0, 0);
            obj.setBodyType(BodyDef.BodyType.StaticBody);
            obj.setDensity(defaults.getFloat( "density", 0.0f ));
            obj.setFriction(defaults.getFloat( "friction", 0.0f ));
            obj.setRestitution(defaults.getFloat( "restitution", 0.0f ));
            obj.setDrawScale(scale);
            obj.setTexture(steelTile);
            obj.setName(pname+ii);
            addObject(obj);
        }

        // This world is heavier
        world.setGravity( new Vector2(0,defaults.getFloat("gravity",0)) );


        //Add buttons
        JsonValue buttonsJV = constants.get("button");
        for (JsonValue buttonJV : buttonsJV.get("buttons")) {
            float x = buttonJV.get("pos").getFloat(0);
            float y = buttonJV.get("pos").getFloat(1);
            String id = buttonJV.getString("id");
            TimedButton button = new TimedButton(x, y, id, 120, buttonTexture, scale, buttonsJV);
            activatorList.add(button);
            addObject(button);
        }

        // Add spikes
        JsonValue spikesJV = constants.get("spike");
        for (JsonValue spikeJV : spikesJV.get("spikes")) {
            float x = spikeJV.get("pos").getFloat(0);
            float y = spikeJV.get("pos").getFloat(1);
            float angle = spikeJV.getFloat("angle");
            boolean active = spikeJV.getBoolean("active");
            String activatorID = spikeJV.getString("activatorID");

            Spikes spikes = new Spikes(x, y, angle, active, spikesTexture, scale, spikesJV);

            //add to dependency map if there is an associated activator
            if (!activatorID.equals("")){
                if (activationRelations.containsKey(activatorID)){
                    activationRelations.get(activatorID).add(spikes);
                } else {
                    activationRelations.put(spikeJV.getString("activatorID"), new Array<Spikes>(new Spikes[]{spikes}));
                }
            }
            spikesList.add(spikes);
            addObject(spikes);
        }

        //Add pushable boxes
        JsonValue boxesJV = constants.get("box");
        dwidth  = steelTile.getRegionWidth()/scale.x;
        dheight = steelTile.getRegionHeight()/scale.y;
        for (JsonValue boxJV : boxesJV.get("boxes")) {
            float x = boxJV.get("pos").getFloat(0);
            float y = boxJV.get("pos").getFloat(1);
            BoxObstacle box = new BoxObstacle(x, y, dwidth, dheight);
            box.setTexture(steelTile);
            box.setDrawScale(scale);
            box.setName("box");
            box.setFriction(boxesJV.getFloat("friction"));
            box.setMass(boxesJV.getFloat("mass"));
            addObject(box);
        }

        volume = constants.getFloat("volume", 1.0f);

        // Create flamethrowers
        JsonValue flamethrowersJV = constants.get("flamethrower");
        for (JsonValue flamethrowerJV : flamethrowersJV.get("flamethrowers")) {
            float x = flamethrowerJV.get("pos").getFloat(0);
            float y = flamethrowerJV.get("pos").getFloat(1);
            float angle = flamethrowerJV.getFloat("angle");
            Flamethrower flamethrower = new Flamethrower(constants.get("flamethrower"),x, y, angle, scale, flamethrowerTexture, flameTexture);
            addObject(flamethrower);
        }

        // Create dude
        dwidth  = avatarTexture.getRegionWidth()/scale.x;
        dheight = avatarTexture.getRegionHeight()/scale.y;
        avatar = new Cat(constants.get("cat"), dwidth, dheight, ret, avatar == null? null : avatar.getPosition());
        avatar.setDrawScale(scale);
        avatar.setTexture(avatarTexture);
        respawnPos = avatar.getPosition();
        addObject(avatar);

    }

    /**
     * Returns whether to process the update loop
     *
     * At the start of the update loop, we check if it is time
     * to switch to a new game mode.  If not, the update proceeds
     * normally.
     *
     * @param dt	Number of seconds since last animation frame
     *
     * @return whether to process the update loop
     */
    public boolean preUpdate(float dt) {
        if (!super.preUpdate(dt)) {
            return false;
        }

        if (!isFailure() && avatar.getY() < -1) {
            setFailure(true);
            return false;
        }

        if (!isFailure() && died) {
            died = false;
            new_dead_body.setFacingRight(avatar.isFacingRight());
            avatar.setPosition(respawnPos);
            addObject(new_dead_body);
            deadBodyList.add(new_dead_body);

        }

        return true;
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
    public void update(float dt) {
        // Process actions in object model
        avatar.setMovement(InputController.getInstance().getHorizontal() *avatar.getForce() * (avatar.getIsClimbing() ? 0 : 1));
        avatar.setVerticalMovement(InputController.getInstance().getVertical() * avatar.getForce());
        avatar.setHorizontalMovement(InputController.getInstance().getHorizontal() * avatar.getForce());
        avatar.setJumping(InputController.getInstance().didPrimary());
        avatar.setDashing(InputController.getInstance().didDash());
        avatar.setClimbing(InputController.getInstance().didClimb() && avatar.isWalled());

        avatar.applyForce();
        if (avatar.isJumping() && avatar.isGrounded()) {
            jumpId = playSound( jumpSound, jumpId, volume );
        }

        if (InputController.getInstance().didMeow()){
            meowId = playSound(meowSound, meowId, volume);
        }

        // Process buttons
        for (Activator a : activatorList){
            a.updateActivated();
            if (activationRelations.containsKey(a.getID())){
                for (Spikes s : activationRelations.get(a.getID())){
                    s.setActive(a.isActive());
                }
            }
        }
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

            // See if we have landed on the ground.
            if ((avatar.getGroundSensorName().equals(fd2) && avatar != bd1) ||
                    (avatar.getGroundSensorName().equals(fd1) && avatar != bd2)) {
                avatar.setGrounded(true);
                sensorFixtures.add(avatar == bd1 ? fix2 : fix1); // Could have more than one ground
            }

            // See if we are touching a wall
            if ((avatar.getSideSensorName().equals(fd2) && avatar != bd1) ||
                    (avatar.getSideSensorName().equals(fd1) && avatar != bd2)) {
                avatar.incrementWalled();
            }

            // Check for win condition
            if ((bd1 == avatar && bd2 == goalDoor) ||
                    (bd1 == goalDoor && bd2 == avatar)) {
                setComplete(true);
            }
            if ((bd1 == avatar && bd2 == retDoor) ||
                    (bd1 == retDoor && bd2 == avatar)) {
                setRet(true);
            }
            // Check for death
            if ((bd1 == avatar && (fd2 == Spikes.getSensorName() || fd2 == Flame.getSensorName())) ||
                    (bd2 == avatar && (fd1 == Spikes.getSensorName() || fd1 == Flame.getSensorName()))) {
                die();
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

    private void spikesDeath(DeadBody deadBody, Spikes spikes){

    }

    private void flameDeath(DeadBody deadBody, Flame flame){

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

        if ((avatar.getGroundSensorName().equals(fd2) && avatar != bd1) ||
                (avatar.getGroundSensorName().equals(fd1) && avatar != bd2)) {
            sensorFixtures.remove(avatar == bd1 ? fix2 : fix1);
            if (sensorFixtures.size == 0) {
                avatar.setGrounded(false);
            }
        }

        // Not handling case where there may be multiple walls at once
        if ((avatar.getSideSensorName().equals(fd2) && avatar != bd1) ||
                (avatar.getSideSensorName().equals(fd1) && avatar != bd2)) {
            avatar.decrementWalled();
        }
        // Check for button
        if (fd2 instanceof Activator) {
            ((Activator) fd2).removePress();
        } else if (fd1 instanceof Activator) {
            ((Activator) fd1).removePress();
        }

    }

    /** Unused ContactListener method */
    public void postSolve(Contact contact, ContactImpulse impulse) {

    }
    /** Unused ContactListener method */
    public void preSolve(Contact contact, Manifold oldManifold) {

    }

    /**
     * Called when the Screen is paused.
     *
     * We need this method to stop all sounds when we pause.
     * Pausing happens when we switch game modes.
     */
    public void pause() {
        jumpSound.stop(jumpId);
        plopSound.stop(plopId);
        fireSound.stop(fireId);
        meowSound.stop(meowId);
    }

    /**
     * Called when a player dies. Removes all input but keeps velocities.
     * TODO: create and return body to be used in preSolve (for fixing to spikes)
     */
    private DeadBody die(){
        if (!died) {
            avatar.setJumping(false);
            died = true;
            // decrement lives
            numLives--;
            // 0 lives
            if (numLives <= 0) {
                numLives = MAX_NUM_LIVES;
                setFailure(true);
            } else {
                // create dead body
                DeadBody dead_body = new DeadBody(constants.get("cat"), dwidth, dheight);
                dead_body.setDrawScale(scale);
                dead_body.setTexture(deadCatTexture);
                dead_body.setSensor(false);
                dead_body.setLinearVelocity(avatar.getLinearVelocity());
                dead_body.setPosition(avatar.getPosition());
                new_dead_body = dead_body;
                return dead_body;
            }
        }
        return null;
    }

}