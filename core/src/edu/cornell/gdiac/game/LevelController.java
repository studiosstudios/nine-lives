package edu.cornell.gdiac.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
    /** The boundary of the world */
    protected Rectangle bounds;
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
     * @param bounds	The game bounds in Box2D coordinates
     * @param gravity	The gravitational force on this Box2D world
     */
    public LevelController(Rectangle bounds, Vector2 gravity) {
        world = new World(gravity, false);
        this.bounds = new Rectangle(bounds);
        scale = new Vector2(1,1);
        debug = false;
        setRet(false);
        sensorFixtures = new ObjectSet<>();

        level = new Level(world, bounds, scale, MAX_NUM_LIVES);
        actionController = new ActionController(bounds, scale, volume);
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
        this.scale.x = 1024f/bounds.getWidth();
        this.scale.y = 576f/bounds.getHeight();
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
                          HashMap<String, Sound> sMap, JsonValue constants, JsonValue levelJV){
        //for now levelcontroller will have access to these assets, but in the future we may see that it is unnecessary
        textureRegionAssetMap = tMap;
        fontAssetMap = fMap;
        soundAssetMap = sMap;
        constantsJSON = constants;
        Level.setConstants(constants);
        this.levelJV = levelJV;
        displayFont = fMap.get("retro");

        //send the relevant assets to classes that need them
        actionController.setVolume(constantsJSON.get("defaults").getFloat("volume"));
        actionController.setAssets(sMap);
        level.setAssets(tMap);
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
    public void reset(Cat prevCat) {

        Vector2 gravity = new Vector2( world.getGravity() );
        justRespawned = true;
        level.dispose();
        world = new World(gravity,false);
        level.setWorld(world);
        world.setContactListener(collisionController);
        world.setContactFilter(collisionController);

        collisionController.setReturn(false);
        actionController.setMobControllers(level);

        boolean tempRet = isRet();
        setRet(false);
        populateLevel(tempRet, prevCat);
        prevLivesState = new LevelState[9];
        canvas.getCamera().setLevelSize(level.bounds.width, level.bounds.height);
        canvas.getCamera().updateCamera(level.getCat().getPosition().x*scale.x, level.getCat().getPosition().y*scale.y, false);
    }

    /**
     * Lays out the game geography.
     */
    public void populateLevel(boolean ret, Cat prevCat) {
        level.populateLevel(textureRegionAssetMap, fontAssetMap, soundAssetMap, constantsJSON, levelJV, ret, prevCat);
        actionController.setMobControllers(level);
    }

    /**
     * Dispose of all (non-static) resources allocated to this mode.
     */
    public void dispose() {
        level.dispose();
        bounds = null;
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
        input.readInput(bounds, scale);

        // Toggle debug
        if (input.didDebug()) {
            debug = !debug;
//            canvas.getCamera().debugCamera(debug);
        }

        // Handle resets
        if (input.didReset()) {
//            flashColor.set(0, 0, 0, 1);
            reset(null);
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

//        if (InputController.getInstance().didUndo()) {
//            if (level.getNumLives() < 9) {
//                loadLevelState(prevLivesState[8 - level.getNumLives()]);
//                flashColor.set(1, 1, 1, 1);
//            }
//        }
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
        canvas.drawRectangle(0, 0, bounds.width,bounds.height, flashColor, scale.x, scale.y);
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