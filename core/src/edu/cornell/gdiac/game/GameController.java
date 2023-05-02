package edu.cornell.gdiac.game;

import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.audio.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.viewport.Viewport;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.game.object.*;

import edu.cornell.gdiac.game.obstacle.*;
import edu.cornell.gdiac.game.stage.HudStage;
import edu.cornell.gdiac.util.ScreenListener;

import java.util.HashMap;

/**
 * Gameplay controller handling the in-game operations of the current level.
 * <br><br>
 * Adapted from Walker M. White's PlatformController.java in Cornell CS 3152, Spring 2023.
 */
public class GameController implements Screen {
    /** Listener that will update the player mode when we are done */
    private ScreenListener listener;
    /** Exit code for quitting the game */
    public static final int EXIT_QUIT = 0;
    /** Default drawscale */
    protected static final float DEFAULT_SCALE = 32;
    /** The default value of gravity (going down) */
    protected static final float DEFAULT_GRAVITY = -4.9f;
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
    /** The JSON value constants */
    private JsonValue constants;
    /** The BitmapFont for the displayFont */
    protected BitmapFont displayFont;
    /** The JSON value constants */
    private JsonValue constantsJSON;
    /** The ActionController */
    private ActionController actionController;
    /** The CollisionController */
    private CollisionController collisionController;
    /** The current level model */
    private Level currLevel;
    /** The next level */
    private Level nextLevel;
    /** The previous level */
    private Level prevLevel;
    /**
     * Array of levels. Will always be of length 3, storing currLevel, nextLevel and prevLevel. We use both an array and
     * three variables to handle level switching and to decrease verbosity and increase readability.
     */
    private Level[] levels;
    /** The index of the current level in levels array.*/
    private int currLevelIndex;
    /** The total number of levels in the game */
    private int numLevels;
    /** The current level index */
    private int levelNum;
    /** JSON for the previous level */
    private JsonValue prevJV;
    /** JSON for the next level */
    private JsonValue nextJV;
    /** The AssetDirectory */
    private AssetDirectory directory;

    /** Temporary list of activatables to pan to */
    private Array<Activatable> panTarget = new Array<>();
    /** Keeps track of time held at activatable upon pan **/
    private float panTime;
    /** Time held at activatable upon pan**/
    final float PAN_HOLD = 50f; //about 17ms per PAN_HOLD unit (holds 0.85 second)
    /** Keeps track of amount of time delayed after respawn **/
    private float respawnDelay;
    /** Whether level was just reset (matters for respawn behavior) **/
    private boolean justReset;
    final float RESPAWN_DELAY = 60f; //about 17ms per RESPAWN_DELAY unit (holds 1 second-0.5s on dead body, 0.5s on respawned cat)
    /** The background texture */
    private Texture background;
    /** Ticks since the player has undone */
    private float undoTime;
    /** The max value of undoTime such that undoing will undo to the previous checkpoint and not the current checkpoint.*/
    private static final float MAX_UNDO_TIME = 120f;
    public StageController stageController = null;
    public boolean paused = false;

    public HudStage hud;

    /**
     * PLAY: User has all controls and is in game
     * PLAYER_PAN: Camera zooms out and player is free to pan around the level (all other gameplay controls stripped from user)
     * PAN: Camera movement not controlled by player (e.g. when activator is pressed or at beginning of level)
     * RESPAWN: Camera focuses on dead body for half of RESPAWN_DELAY and focuses on newly respawned cat for half of RESPAWN_DELAY
     */
    enum GameState {
        PLAY,
        PLAYER_PAN,
        PAN,
        RESPAWN
    }
    /** State of gameplay used for camera */
    public static GameState gameState;
    /** If we have respawned in preUpdate(). Needed in postUpdate() for saving level state. */
    private boolean justRespawned;
    /** The color of the flash animation after resetting/undoing */
    private Color flashColor = new Color(1, 1, 1, 0);

    /** RayHandler that takes care of Box2DLights */
    private RayHandler rayHandler;

    private PointLight tempPointLight;


    /**
     * Creates a new game world with the default values.
     * <br><br>
     * The game world is scaled so that the screen coordinates do not agree
     * with the Box2D coordinates.  The bounds are in terms of the Box2d
     * world, not the screen.
     */
    protected GameController(int numLevels) {
        this(new Vector2(0,DEFAULT_GRAVITY), new Vector2(DEFAULT_SCALE,DEFAULT_SCALE), numLevels);
    }

    /**
     * Creates and initialize a new instance of a GameController
     * <br><br>
     * The game world is scaled so that the screen coordinates do not agree
     * with the Box2D coordinates.  The bounds are in terms of the Box2D
     * world, not the screen.
     */
    protected GameController(Vector2 gravity, Vector2 scale, int numLevels) {
        this.scale = scale;
        debug = false;
        setRet(false);
        world = new World(gravity, true);
//        RayHandler.useDiffuseLight(true);
//        rayHandler = new RayHandler(world);
//        rayHandler.setAmbientLight(0.9f);
//        rayHandler.setAmbientLight(0.35f, 0.35f, 0.35f, 0.01f);

//        tempPointLight = new PointLight(rayHandler, 100, Color.WHITE, 200, 0, 0);

        this.numLevels = numLevels;
        levelNum = 1;
        levels = new Level[3];
        for (int i = 0; i < 3; i++){
            levels[i] = new Level(world, scale, MAX_NUM_LIVES, rayHandler);
        }
        currLevelIndex = 1;

        setLevels();
        actionController = new ActionController(scale, volume);
        actionController.setLevel(levels[currLevelIndex]);
        collisionController = new CollisionController(actionController);
        collisionController.setLevel(levels[currLevelIndex]);

        gameState = GameState.PLAY;
        panTime = 0;
        respawnDelay = 0;

        AssetDirectory internal = new AssetDirectory("jsons/loading.json");
        internal.loadAssets();
        internal.finishLoading();

        hud = new HudStage(internal, true);
        hud.lives = currLevel.getNumLives();
    }

    /**
     * Points <code>currLevel</code>, <code>nextLevel</code> and <code>prevLevel</code> to the correct elements of the
     * <code>levels</code> array, based on <code>currLevelIndex</code>.
     */
    private void setLevels(){
        currLevel = levels[currLevelIndex];
        nextLevel = levels[(currLevelIndex + 1) % 3];
        prevLevel = levels[Math.floorMod(currLevelIndex - 1, 3)];
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
    public Level getCurrLevel() {
        return currLevel;
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
     * Sets the ScreenListener for this mode
     * <br><br>
     * The ScreenListener will respond to requests to quit.
     */
    public void setScreenListener(ScreenListener listener) {
        this.listener = listener;
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
        for (Level l : levels){
            l.setAssets(tMap);
        }
    }

    /**
     * Sets the static constants for all objects.
     *
     * @param constants  Constants JSON
     */
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
        Goal.setConstants(constants.get("goal"));
    }

    /**
     * Steps the level
     * <br><br>
     * The previous level is set to the current level<br>
     * The current level is set to the next level<br>
     * The next level is loaded in<br>
     */
    public void nextLevel(){
        levelNum++;
        prevJV = getJSON();
        setJSON(nextJV);
        setRet(false);


        currLevelIndex = (currLevelIndex + 1) % 3;
        currLevel.setComplete(false);
        setLevels();
//        respawn();
        currLevel.setCat(prevLevel.getCat());
        prevLevel.removeCat();

        nextLevel.dispose();
        if (levelNum < numLevels) {
            nextJV = tiledJSON(levelNum + 1);
            nextLevel.populateTiled(nextJV, currLevel.bounds.x + currLevel.bounds.width, currLevel.bounds.y, levelNum + 1, currLevel.goalY, true);
        }
        initCurrLevel(true);
        collisionController.setDidChange(true);
    }

    /**
     * Steps the level
     * <br><br>
     * The next level is set to the current level<br>
     * The current level is set to the previous level<br>
     * The previous level is loaded in<br>
     */
    public void prevLevel(){
        levelNum--;
        nextJV = getJSON();
        setJSON(prevJV);
        setRet(false);
        if (levelNum > 1) {
            prevJV = tiledJSON(levelNum - 1);
        }
        currLevelIndex = Math.floorMod(currLevelIndex - 1,  3);
        setLevels();

        currLevel.setCat(nextLevel.getCat());
        nextLevel.removeCat();

        prevLevel.dispose();
        if (levelNum > 1) {
            prevJV = tiledJSON(levelNum - 1);
            prevLevel.populateTiled(prevJV, currLevel.bounds.x, currLevel.bounds.y, levelNum - 1, currLevel.returnY, false);
        }

        initCurrLevel(true);
        collisionController.setDidChange(true);
    }

    /**
     * Loads in the JSON of a level
     *
     * @param levelNum the number associated with the level to be loaded in
     * @return JSON of the level
     */
    private JsonValue tiledJSON(int levelNum){ return directory.getEntry("tiledLevel" + levelNum, JsonValue.class); }

    /**
     * Gather the assets for this controller.
     * <br><br>
     * This method extracts the asset variables from the given asset directory. It
     * should only be called after the asset directory is completed.
     * <br><br>
     * Whenever you add an asset to the game, you will need to:<br>
     * 1. Place it in the correct location in the assets/ directory<br>
     * 2. Add it to the assets.json file in the form of "file-name: file-path"<br>
     * 3. Add "file-name" to the correct String array within this method to be accessible from the
     *    corresponding map (For example, if its a sound asset, add it to the array preceding the soundAssetMap<br><br>
     * Note the naming conventions that file names follow:<br>
     * 1. Use hyphens<br>
     * 2. For sprites that are for animations, affix their name with "-anim"<br>
     * 3. For textures that serve as backgrounds, prefix their names with "bg-"<br>
     * 4. Make sure to use the file name (with hyphens) for the assets.json key, and the texture map key as well<br>
     *
     * @param directory	Reference to global asset manager.
     */
    public void gatherAssets(AssetDirectory directory) {
        // Allocate the tiles
        // Creating the hashmaps
        textureRegionAssetMap = new HashMap<>();
        soundAssetMap = new HashMap<>();
        fontAssetMap = new HashMap<>();

        // List of textures we extract. These should be the SAME NAME as the keys in the assets.json.
        // A couple naming conventions: use hyphens, affix animation sprites with "-anim".
        String[] names = {
                // CAT
                "cat", "walk-anim", "jump", "jump-anim", "sit", "idle-sit-anim", "idle-stand-anim", "meow-anim",
                "corpse", "corpse2", "corpse-burnt",
                // SPIKES
                "spikes",
                // BUTTONS & SWITCHES
                "button-base", "button-top", "switch-top",
                // FLAMETHROWERS
                "flamethrower", "flame", "flame-anim",
                // LASERS
                "laser",
                // CHECKPOINTS
                "checkpoint-anim", "checkpoint-active-anim", "checkpoint-base", "checkpoint-base-active",
                // GOAL
                "goal",
                // ROBOT & MOBS
                "robot", "robot-anim",
                // SPIRIT BOUNDARIES
                "spirit-anim", "spirit-photon", "spirit-photon-cat", "spirit-region",
                // TILESETS
                "metal-tileset", "climbable-tileset", "steel",
                // DOORS & PLATFORMS
                "door", "platform",
                // BACKGROUNDS
                "bg-lab",}; // Unsure if this is actually being used
        for (String n : names){
            textureRegionAssetMap.put(n, new TextureRegion(directory.getEntry(n, Texture.class)));
        }

        names = new String[]{"jump", "dash", "metal-landing", "meow"};
        for (String n : names){
            soundAssetMap.put(n, directory.getEntry(n, Sound.class));
        }

        names = new String[]{"retro"};
        for (String n : names){
            fontAssetMap.put(n, directory.getEntry(n, BitmapFont.class));
        }

        constants = directory.getEntry("constants", JsonValue.class);
        this.directory = directory;

        background = textureRegionAssetMap.get("bg-lab").getTexture();

        // Giving assets to levelController
        setAssets(textureRegionAssetMap, fontAssetMap, soundAssetMap, constants);
        setJSON(tiledJSON(1));
        nextJV = tiledJSON(2);

        //Set controls
        InputController.getInstance().setControls(directory.getEntry("controls", JsonValue.class));

//		InputController.getInstance().writeTo("debug-input/recent.txt");
//		InputController.getInstance().readFrom("debug-input/recent.txt");
    }

    /**
     * Handles respawning the cat after their death
     * <br><br>
     * The level model died is set to false<br>
     * The level model cat is set to its respawn position
     * @param  cameraMovement true if we want respawn camera movement, false otherwise
     */
    public void respawn(boolean cameraMovement) {
        currLevel.setDied(false);
        currLevel.getCat().setActive(false);
        currLevel.getCat().setFacingRight(currLevel.getCheckpoint() != null ? currLevel.getCheckpoint().facingRight() : true);
        currLevel.getCat().setJumpPressed(false);
        currLevel.getCat().setGrounded(true);
        currLevel.getCat().setLinearVelocity(Vector2.Zero);
        currLevel.getCat().setPosition(currLevel.getRespawnPos());
        justRespawned = cameraMovement;
    }

    /**
     * Resets the status of the game so that we can play again.
     */
    protected void reset(){ init(levelNum); }

    /**
     * Initializes the game from a given level number.
     *
     * Note that this method simply repopulates the existing levels. Care needs to be taken to
     * properly dispose the level so that the level reset is clean.
     */
    protected void init(int levelNum) {
        System.out.println("Executing init");
        this.levelNum = levelNum;

        prevLevel.dispose();
        currLevel.dispose();
        nextLevel.dispose();
        Vector2 gravity = new Vector2( world.getGravity() );
        world.dispose();
        world = new World(gravity, true);
        rayHandler = new RayHandler(world);
        rayHandler.setAmbientLight(0.8f);
        tempPointLight = new PointLight(rayHandler, 100, Color.WHITE, 100, 48f, 48f);


        justRespawned = true;
        justReset = true;
        currLevelIndex = 1;
        setLevels();
        prevLevel.setWorld(world);
        currLevel.setWorld(world);
        nextLevel.setWorld(world);
        prevLevel.setRayHandler(rayHandler);
        currLevel.setRayHandler(rayHandler);
        nextLevel.setRayHandler(rayHandler);
        world.setContactListener(collisionController);
        world.setContactFilter(collisionController);
        collisionController.setReturn(false);
        setRet(false);

        levelJV = tiledJSON(levelNum);
        currLevel.populateTiled(levelJV, levelNum);
        if (levelNum < numLevels) {
            nextJV = tiledJSON(levelNum + 1);
            nextLevel.populateTiled(nextJV, currLevel.bounds.x + currLevel.bounds.width, currLevel.bounds.y, levelNum + 1, currLevel.goalY, true);
        }
        if (levelNum > 1) {
            prevJV = tiledJSON(levelNum - 1);
            prevLevel.populateTiled(prevJV, currLevel.bounds.x, currLevel.bounds.y, levelNum - 1, currLevel.returnY, false);
        }

        initCurrLevel(false);
    }

    /**
     * Prepares the game for a new level: resets controllers, camera and stored states.
     *
     * @param cameraGlide True if the camera should glide to its new location.
     */
    private void initCurrLevel(boolean cameraGlide){
        collisionController.setLevel(currLevel);
        actionController.setLevel(currLevel);
        actionController.setMobControllers(currLevel);
        if (currLevel.levelStates().size == 0) currLevel.saveState();
        canvas.getCamera().setLevelBounds(currLevel.bounds, scale);
        canvas.getCamera().updateCamera(currLevel.getCat().getPosition().x*scale.x, currLevel.getCat().getPosition().y*scale.y, cameraGlide);
        currLevel.unpause();
        nextLevel.pause();
        prevLevel.pause();
        undoTime = 0;
        resume();
    }

    /**
     * Dispose of all (non-static) resources allocated to this mode.
     */
    public void dispose() {
        currLevel.dispose();
        world.dispose();
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

        if (listener == null) {
            return true;
        }

        InputController input = InputController.getInstance();
        input.readInput();
        // Toggle debug
        if (input.didDebug()) {
            debug = !debug;
//            canvas.getCamera().debugCamera(debug);
        }

        if (!currLevel.isFailure() && currLevel.getDied()) {
            flashColor.set(0, 0, 0, 1);
            respawn(true);
        }

        if (input.didExit()){
            pause();
            listener.exitScreen(this, EXIT_QUIT);
            return false;
        }

        if (currLevel.isFailure() || input.didReset()) {
            if (currLevel.isFailure()) flashColor.set(1, 0, 0, 1);
            reset();
        } else if (currLevel.isComplete() && levelNum < numLevels) {
            pause();
            nextLevel();
            return false;
        } else if (isRet() && levelNum > 1) {
            pause();
            prevLevel();
            return false;
        }  else if (input.didNext() && levelNum < numLevels) {
            pause();
            init(levelNum + 1);
            return false;
        }  else if (input.didPrev() && levelNum > 1) {
            pause();
            init(levelNum - 1);
            return false;
        }
        return true;
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
        updateCamera();

        hud.lives = currLevel.getNumLives();
        hud.updateLives();
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
        currLevel.addQueuedObjects();
        currLevel.addQueuedJoints();

        // Turn the physics engine crank.
        world.step(WORLD_STEP,WORLD_VELOC,WORLD_POSIT);

        // Update objects
        actionController.postUpdate(dt);
        justRespawned = false;
        justReset = false;

        if (InputController.getInstance().didUndo()) {
            Array<Level.LevelState> levelStates = currLevel.levelStates();
            if (undoTime < MAX_UNDO_TIME && levelStates.size > 1) {
                levelStates.pop();
            }
            loadLevelState(levelStates.peek(), false);
            undoTime = 0;
            flashColor.set(1, 1, 1, 1);
        }
    }

    /**
     * Updates GameState and moves camera accordingly
     */
    public void updateCamera(){
        Camera cam = canvas.getCamera();
        InputController input = InputController.getInstance();
        //resetting automatically resets camera to cat
        if(justReset){
            gameState = GameState.PLAY;
        }
        if(input.didPan()){
            gameState = GameState.PLAYER_PAN;
            //move camera
            cam.updateCamera(cam.getX()+input.getCamHorizontal(),cam.getY()+ input.getCamVertical(),false);
        }
        else if(gameState == GameState.PLAYER_PAN){
            gameState = GameState.PLAY;
        }

        for (Activator a : currLevel.getActivators()){
            if (a.isPressed() && a.getPan()){
                a.setPan(false);
                if(currLevel.getActivationRelations().containsKey(a.getID())){
                    panTarget = currLevel.getActivationRelations().get(a.getID());
                }
                gameState = GameState.PAN;
            }
        }
        if(gameState == GameState.PLAY){
            panTime = 0;
            respawnDelay = 0;
            undoTime++;

            input.setDisableAll(false);
            float x_pos = currLevel.getCat().getPosition().x*scale.x;
            float y_pos = currLevel.getCat().getPosition().y*scale.y;
            if(justRespawned && !justReset) {
                gameState = GameState.RESPAWN;
            }
            else {
                //zoom normal when in play state and not panning and not switching bodies
                if (!input.holdSwitch() && !input.didPan()) {
                    cam.zoomOut(false);
                }
                DeadBody nextDeadBody = currLevel.getNextBody();
                if (input.holdSwitch() && nextDeadBody != null) {
                    cam.setGlideMode("SWITCH_BODY");
                    cam.switchBodyCam(nextDeadBody.getX() * scale.x, nextDeadBody.getY() * scale.y);
                } else {
                    cam.setGlideMode("NORMAL");
                    cam.updateCamera(x_pos, y_pos, true);
                }
            }
        }
        if(gameState == GameState.PAN){
            cam.updateCamera(panTarget.get(0).getXPos()*scale.x,panTarget.get(0).getYPos()*scale.y, true);
            if(!cam.isGliding()){
                panTime += 1;
                if(panTime == PAN_HOLD){
                    gameState = GameState.PLAY;
                }
            }
        }
        if(gameState == GameState.PLAYER_PAN){
            cam.zoomOut(true);
        }
        if(gameState == GameState.RESPAWN){
            float xPos = currLevel.getCat().getPosition().x*scale.x;
            float yPos = currLevel.getCat().getPosition().y*scale.y;
            input.setDisableAll(true);
            respawnDelay += 1;
            if(currLevel.getdeadBodyArray().size > 0 && respawnDelay < RESPAWN_DELAY/2){
                xPos = currLevel.getdeadBodyArray().get(currLevel.getdeadBodyArray().size-1).getX()*scale.x;
                yPos = currLevel.getdeadBodyArray().get(currLevel.getdeadBodyArray().size-1).getY()*scale.y;
            }
            cam.updateCamera(xPos, yPos, true);
            if(respawnDelay == RESPAWN_DELAY){
                respawnDelay = 0;
                input.setDisableAll(false);
                gameState = GameState.PLAY;

                currLevel.getCat().setActive(true);
                currLevel.getCat().setPosition(currLevel.getRespawnPos());
            }
        }
    }
    @Override
    public void render(float delta) {
        //FOR DEBUGGING
//		delta = 1/60f;
//		if (Gdx.input.isKeyPressed(Input.Keys.F)){
//			try {
//				Thread.sleep(500);
//			} catch (InterruptedException e) {
//				Thread.currentThread().interrupt();
//			}
//		}
//        System.out.println("before:" + canvas.getCamera().getCamera().position);
        if (!paused) {
            if (preUpdate(delta)) {
                update(delta); // This is the one that must be defined.
                postUpdate(delta);
            }
        }
        if (paused) { updateCamera(); }
        draw(delta);
        if (paused && stageController != null) { stageController.render(delta); }
//        System.out.println("after:" +canvas.getCamera().getCamera().position);
        rayHandler.setCombinedMatrix(canvas.getCamera().getCamera());
//        OrthographicCamera c = canvas.getCamera().getCamera();
//        canvas.getCamera().getCamera().update();
//        System.out.println("Drawing in GameController:" +c.position+" "+c);
//        rayHandler.setCombinedMatrix(c);
//        rayHandler.setCombinedMatrix(
//                c.combined.cpy(),
//                c.position.x,
//                c.position.y,
//                c.viewportWidth,
//                c.viewportHeight
//        );
//        Vector3 projected = c.unproject(new Vector3(400, 400, 0));
//        Vector2 newPos = new Vector2(800, 800).sub(canvas.getCamera().getX(), canvas.getCamera().getY());
//        tempPointLight.setPosition(newPos);

//        System.out.println(c.position+" "+c); this should work idk why it doesnt work
//        System.out.println(canvas.getCamera().getX() + " " + canvas.getCamera().getY());


//        Viewport vp = canvas.getViewport();
//        System.out.println(Math.round(Gdx.graphics.getBackBufferScale()));
//        int backBufferScale = Math.round(Gdx.graphics.getBackBufferScale());
//        rayHandler.useCustomViewport(vp.getScreenX() * backBufferScale, vp.getScreenY() * backBufferScale,
//                                    vp.getScreenWidth() * backBufferScale, vp.getScreenHeight() * backBufferScale);
        rayHandler.updateAndRender();
    }

    /**
     * Called when the Screen is resized.
     * <br><br>
     * This can happen at any point during a non-paused state but will never happen
     * before a call to show().
     *
     * @param width  The new width in pixels
     * @param height The new height in pixels
     */
    @Override
    public void resize(int width, int height) {
        hud.getViewport().update(width, height, true);
    }

    /**
     * Called when the Screen is paused.
     * <br><br>
     * We need this method to stop all sounds when we pause.
     * Pausing happens when we switch game modes.
     */
    public void pause() {
        paused = true;
        actionController.pause();
    }

    /**
     * Called when the Screen is resumed from a paused state.
     * <br><br>
     * This is usually when it regains focus.
     */
    public void resume() {
        paused = false;
        stageController = null;
    }

    /**
     * Called when this screen becomes the current screen for a Game.
     */
    public void show() {
        // Useless if called in outside animation loop
    }

    /**
     * Called when this screen is no longer the current screen for a Game.
     */
    public void hide() {
        // Useless if called in outside animation loop
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
        canvas.applyViewport(false);
        canvas.draw(background, Color.WHITE, canvas.getCamera().getX() - canvas.getWidth()/2, canvas.getCamera().getY()  - canvas.getHeight()/2, canvas.getWidth(), canvas.getHeight());
        if (true) { //TODO: only draw when necessary
            prevLevel.draw(canvas, false);
            nextLevel.draw(canvas, false);
        }
        currLevel.draw(canvas, gameState != GameState.RESPAWN);
        canvas.drawRectangle(canvas.getCamera().getX() - canvas.getWidth()/2, canvas.getCamera().getY()  - canvas.getHeight()/2, canvas.getWidth(), canvas.getHeight(), flashColor, 1, 1);
        canvas.end();
        hud.draw();

        if (debug) {
            canvas.beginDebug();
            if (levelNum > 1) prevLevel.drawDebug(canvas);
            currLevel.drawDebug(canvas);
            if (levelNum < numLevels) nextLevel.drawDebug(canvas);
            canvas.endDebug();
        }

        //box2d debug check
//        Array<Body> bodies = new Array<>();
//        world.getBodies(bodies);
//        System.out.println(bodies.size);
//        int numBodies = 0;
//        for (Level l : levels){
//            numBodies += l.objects.size();
//        }
//        System.out.println(numBodies);

    }

    /**
     * Loads in a previously stored level state. The <code>LevelState</code> that is loaded in must
     * consist of references to the same objects as the current level, i.e. the saved level cannot
     * have been reset or disposed.
     * @param state LevelState to load in
     * @param cameraMovement true if we want camera movement upon respawn, false otherwise
     */
    private void loadLevelState(Level.LevelState state, boolean cameraMovement){
        currLevel.setNumLives(state.numLives);
        for (Obstacle obs : currLevel.getObjects()){

            if (obs instanceof DeadBody){
                //need to remove and rebuild dead body array because number of dead bodies can change between saved states
                DeadBody db = (DeadBody) obs;
                currLevel.removeDeadBody(db);
            } else {

                obs.loadState(state.obstacleData.get(obs));

                if (obs instanceof Spikes){
                    ((Spikes) obs).destroyJoints(world);
                }
            }
        }

        // rebuild dead body array
        for (ObjectMap<String, Object> dbState : state.deadBodyData){
            DeadBody db = currLevel.loadDeadBodyState(dbState);
            HashMap<Spikes, Vector2> jointInfo = (HashMap<Spikes, Vector2>) dbState.get("jointInfo");
            for (Spikes s : jointInfo.keySet()){
                actionController.fixBodyToSpikes(db, s, new Vector2[]{db.getBody().getWorldPoint(jointInfo.get(s))});
            }
        }

        if (state.checkpoint != null) {
            currLevel.updateCheckpoints(state.checkpoint, false);
        } else {
            currLevel.resetCheckpoints();
        }
        if (currLevel.getCheckpoint() != null) {
            respawn(cameraMovement);
            currLevel.getCat().setActive(true);
        }
        currLevel.getCat().setPosition(currLevel.getRespawnPos());
    }
}