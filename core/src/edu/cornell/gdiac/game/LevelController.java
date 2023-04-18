package edu.cornell.gdiac.game;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.audio.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.*;
import edu.cornell.gdiac.game.object.*;

import edu.cornell.gdiac.game.obstacle.*;

import java.util.HashMap;

/**
 * Gameplay controller handling the in-game operations of the current level.
 * <br><br>
 * Adapted from Walker M. White's PlatformController.java in Cornell CS 3152, Spring 2023.
 */
public class LevelController {
    /** The Box2D world */
    protected World world;
    /** The world scale */
    protected Vector2 scale;
    /** The amount of time for a physics engine step. */
    public static final float WORLD_STEP = 1/60.0f;
    /** Number of velocity iterations for the constraint solvers */
    public static final int WORLD_VELOC = 6;
    /** Number of position iterations for the constraint solvers */
    public static final int WORLD_POSIT = 2;
    /** Whether debug mode is active */
    private boolean debug;
    /** The default sound volume */
    private float volume;
    /** JSON representing the level */
    private JsonValue levelJV;
    /** Mark set to handle more sophisticated collision callbacks */
    protected ObjectSet<Fixture> sensorFixtures;
    /** Whether to return to previous level */
    private boolean ret;
    /** Reference to the game canvas */
    protected GameCanvas canvas;
    /** The maximum number of lives in the game */
    private static final int MAX_NUM_LIVES = 9;
    /** The hashmap for texture regions */
    private HashMap<String, TextureRegion> textureRegionAssetMap;
    /** The hashmap for sounds */
    private HashMap<String, Sound> soundAssetMap;
    /** The hashmap for fonts */
    private HashMap<String, BitmapFont> fontAssetMap;
    /** The BitmapFont for the displayFont */
    protected BitmapFont displayFont;
    /** The JSON value constants */
    private JsonValue constantsJSON;
    /** The ActionController */
    private ActionController actionController;
    /** The CollisionController */
    private CollisionController collisionController;
    /** The Level model */
    private Level level;
    /** Array storing level states of past lives. The ith element of this array is the state
     * of the level at the instant the player had died i times. */
    private LevelState[] prevLivesState = new LevelState[9];
    /** If we have respawned in preUpdate(). Needed in postUpdate() for saving level state. */
    private boolean justRespawned;
    /** The color of the flash animation after resetting/undoing */
    private Color flashColor = new Color(1, 1, 1, 0);


    /**
     * Creates and initialize a new instance of a LevelController
     * <br><br>
     * The game world is scaled so that the screen coordinates do not agree
     * with the Box2D coordinates.  The bounds are in terms of the Box2D
     * world, not the screen.
     *
     */
    public LevelController(Vector2 scale, World world) {
        this.scale = scale.cpy();
        debug = false;
        setRet(false);
        sensorFixtures = new ObjectSet<>();

        this.world = world;
        level = new Level(world, scale, MAX_NUM_LIVES);
        actionController = new ActionController(scale, volume);
        actionController.setLevel(level);
        collisionController = new CollisionController(actionController);
        collisionController.setLevel(level);
    }

    /**
     * Sets the canvas associated with this controller
     * <br><br>
     * The canvas is shared across all controllers.  Setting this value will compute
     * the drawing scale from the canvas size.
     *
     * @param canvas the canvas associated with this controller
     */
    public void setCanvas(GameCanvas canvas) {
        this.canvas = canvas;
    }

    /**
     * Returns the level model
     *
     * @return level
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Returns the canvas associated with this controller
     * <br><br>
     * The canvas is shared across all controllers
     *
     * @return the canvas associated with this controller
     */
    public GameCanvas getCanvas() {
        return canvas;
    }

    /**
     * Returns true if returning to prev level
     *
     * @return true if returning to previous level
     */
    public boolean isRet() { return ret; }

    /**
     * Sets whether to return to the previous level
     *
     * @param value to set ret to
     */
    public void setRet(boolean value){
        ret = value;
    }

    /**
     * Gets the JSON for the currently active level
     * @return JSON for currently active level
     */
    public JsonValue getJSON() { return levelJV; }

    /**
     * Sets the JSON to be used for the currently active level
     * @param level Sets JSON for currently active level
     */
    public void setJSON(JsonValue level) { levelJV = level; }


    /**
     * Sets the hashmaps for Texture Regions, Sounds, Fonts, and sets JSON value constants
     *
     * @param tMap the hashmap for Texture Regions
     * @param fMap the hashmap for Fonts
     * @param sMap the hashmap for Sounds
     * @param constants the JSON value for constants
     */
    public void setAssets(HashMap<String, TextureRegion> tMap, HashMap<String, BitmapFont> fMap,
                          HashMap<String, Sound> sMap, JsonValue constants){
        //for now levelcontroller will have access to these assets, but in the future we may see that it is unnecessary
        textureRegionAssetMap = tMap;
        fontAssetMap = fMap;
        soundAssetMap = sMap;
        constantsJSON = constants;
        setConstants(constants);
        displayFont = fMap.get("retro");

        //send the relevant assets to classes that need them
        actionController.setVolume(constantsJSON.get("defaults").getFloat("volume"));
        actionController.setAssets(sMap);
        level.setAssets(tMap);
    }

    private static void setConstants(JsonValue constants){
        DeadBody.setConstants(constants.get("deadBody"));
        Flamethrower.setConstants(constants.get("flamethrowers"));
        PushableBox.setConstants(constants.get("boxes"));
        Spikes.setConstants(constants.get("spikes"));
        Activator.setConstants(constants.get("activators"));
        Laser.setConstants(constants.get("lasers"));
        Checkpoint.setConstants(constants.get("checkpoint"));
        Mirror.setConstants(constants.get("mirrors"));
        Wall.setConstants(constants.get("walls"));
        Platform.setConstants(constants.get("platforms"));
        Cat.setConstants(constants.get("cat"));
        Exit.setConstants(constants.get("exits"));
        Door.setConstants(constants.get("doors"));
        Mob.setConstants(constants.get("mobs"));
    }

    /**
     * Handles respawning the cat after their death
     * <br><br>
     * The level model died is set to false<br>
     * The level model cat is set to its respawn position
     */
    public void respawn() {
        level.setDied(false);
        level.getCat().setPosition(level.getRespawnPos());
        level.getCat().setFacingRight(true);
        level.getCat().setJumpPressed(false);
        level.getCat().setGrounded(true);
        justRespawned = true;
    }

    /**
     * Resets the status of the game so that we can play again.
     *
     * Note that this method simply repopulates the existing level. Care needs to be taken to
     * properly dispose the level so that the level reset is clean.
     */
    protected void reset(World world) {

        justRespawned = true;
        level.dispose();
        level.setWorld(world);
        world.setContactListener(collisionController);
        world.setContactFilter(collisionController);
        this.world = world;

        collisionController.setReturn(false);

        boolean tempRet = isRet();
        setRet(false);
        level.populateTiled(levelJV);

        actionController.setMobControllers(level);
        prevLivesState = new LevelState[9];
        canvas.getCamera().setLevelSize(level.bounds.width*scale.x, level.bounds.height*scale.y);
        canvas.getCamera().updateCamera(level.getCat().getPosition().x*scale.x, level.getCat().getPosition().y*scale.y, false);
    }

    /**
     * Dispose of all (non-static) resources allocated to this mode.
     */
    public void dispose() {
        level.dispose();
        scale  = null;
        canvas = null;
    }

    /**
     * Returns whether to process the update loop
     * <br><br>
     * At the start of the update loop, we check if it is time
     * to switch to a new game mode.  If not, the update proceeds
     * normally.
     *
     * @param dt Number of seconds since last animation frame
     *
     * @return whether to process the update loop
     */
    public boolean preUpdate(float dt) {

        InputController input = InputController.getInstance();
        input.readInput();

        // Toggle debug
        if (input.didDebug()) {
            debug = !debug;
//            canvas.getCamera().debugCamera(debug);
        }

        if (!level.isFailure() && level.getDied()) {
            respawn();
        }

        return input.didExit();
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
    public void update(float dt) {
        if (collisionController.getReturn()) {
            setRet(true);
        }
        actionController.update(dt);
        flashColor.a -= flashColor.a/10;
        float x_pos = level.getCat().getPosition().x*scale.x;
        float y_pos = level.getCat().getPosition().y*scale.y;
        canvas.getCamera().updateCamera(x_pos, y_pos, true);
    }

    /**
     * Processes physics
     * <br><br>
     * Once the update phase is over, but before we draw, we are ready to handle
     * physics.  The primary method is the step() method in world.  This implementation
     * works for all applications and should not need to be overwritten.
     *
     * @param dt	Number of seconds since last animation frame
     */
    public void postUpdate(float dt) {
        // Add any objects created by actions
        level.addQueuedObjects();
        level.addQueuedJoints();

        // Turn the physics engine crank.
        world.step(WORLD_STEP,WORLD_VELOC,WORLD_POSIT);

        // Update objects
        actionController.postUpdate(dt);

        //Save level state if necessary. This must be done here so that we can save dead bodies after they are added
        //to the world. Ideally we could do this in respawn(), but that would involve rearranging the order of everything
        //which may be a pain. Also it does seem like saving state after stepping the world has better results - will need
        //testing.

        if (justRespawned) {
            prevLivesState[9 - level.getNumLives()] = new LevelState(level);
            justRespawned = false;
        }


        if (InputController.getInstance().didUndo()) {
            if (level.getNumLives() < 9) {
                loadLevelState(prevLivesState[8 - level.getNumLives()]);
                flashColor.set(1, 1, 1, 1);
            }
        }
    }

    /**
     * Called when the Screen is paused.
     * <br><br>
     * We need this method to stop all sounds when we pause.
     * Pausing happens when we switch game modes.
     */
    public void pause() {
        actionController.pause();
    }

    /**
     * Draw the physics objects to the canvas
     * <br><br>
     * For simple worlds, this method is enough by itself.  It will need
     * to be overridden if the world needs fancy backgrounds or the like.
     * <br><br>
     * The method draws all objects in the order that they were added.
     *
     * @param dt	Number of seconds since last animation frame
     */
    public void draw(float dt) {
        canvas.clear();

        canvas.begin();
        canvas.applyViewport();
        level.draw(canvas);
        canvas.drawRectangle(0, 0, level.bounds.width,level.bounds.height, flashColor, scale.x, scale.y);
        canvas.end();

        if (debug) {
            canvas.beginDebug();
            level.drawDebug(canvas);
            canvas.endDebug();
        }

    }


    /**
     * Stores a snapshot of the state of a level.
     */
    private static class LevelState {
        public ObjectMap<Obstacle, ObjectMap<String, Object>> obstacleData = new ObjectMap<>();
        public int numLives;
        public Checkpoint checkpoint;
        public Array<ObjectMap<String, Object>> deadBodyData = new Array<>();

        public LevelState(Level level){
            this.numLives = level.getNumLives();
            this.checkpoint = level.getCheckpoint();
            for (Obstacle obs : level.getObjects()) {
                if (obs instanceof DeadBody) {
                    deadBodyData.add(obs.storeState());
                } else {
                    obstacleData.put(obs, obs.storeState());
                }
            }
        }
    }

    /**
     * Loads in a previously stored level state. The <code>LevelState</code> that is loaded in must
     * consist of references to the same objects as the current level, i.e. the saved level cannot
     * have been reset or disposed.
     * @param state LevelState to load in
     */
    private void loadLevelState(LevelState state){
        level.setNumLives(state.numLives);
        if (state.checkpoint != null) {
            level.updateCheckpoints(state.checkpoint);
        } else {
            level.resetCheckpoints();
        }
        for (Obstacle obs : level.getObjects()){

            if (obs instanceof DeadBody){
                //need to remove and rebuild dead body array because number of dead bodies can change between saved states
                DeadBody db = (DeadBody) obs;
                level.removeDeadBody(db);
            } else {

                obs.loadState(state.obstacleData.get(obs));

                //TODO: test if spike and dead body weld joints still work after loading
                if (obs instanceof Spikes){
                    ((Spikes) obs).destroyJoints(world);
                }
            }
        }

        // rebuild dead body array
        for (ObjectMap<String, Object> dbState : state.deadBodyData){
            level.loadDeadBodyState(dbState);
        }
    }
}