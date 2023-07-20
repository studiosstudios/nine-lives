package edu.cornell.gdiac.game;

import box2dLight.RayHandler;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.viewport.Viewport;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.game.object.*;
import edu.cornell.gdiac.game.obstacle.*;
import edu.cornell.gdiac.game.stage.CreditsStage;
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
    public static final int EXIT_QUIT = 99;
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
    /** Whether debug mode is enabled */
    private final boolean debugEnabled;
    /** Whether debug mode is active */
    private boolean debug;
    /** JSON representing the level */
    private JsonValue levelJV;
    /** Reference to the game canvas */
    protected GameCanvas canvas;
    /** The maximum number of lives in the game */
    private static final int MAX_NUM_LIVES = 9;
    /** The hashmap for texture regions */
    private HashMap<String, TextureRegion> textureRegionAssetMap;
//    /** The hashmap for sounds */
//    private HashMap<String, Sound> soundAssetMap;

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
    /** The lab background texture */
    private Texture background;
    /** Ticks since the player has undone */
    private float undoTime;
    /** The max value of undoTime such that undoing will undo to the previous checkpoint and not the current checkpoint.*/
    private static final float MAX_UNDO_TIME = 120f;
    public StageController stageController = null;
    public boolean paused = false;
    public HudStage hud;
    /** Number of frames in spirit mode. */
    private int spiritModeTicks;
    /** Number of frames required for VFX effects to reach maximum strength. */
    private final static int MAX_SPIRIT_MODE_TICKS = 30;
    /** Strength of VFX effects - always between 0 and 1. */
    private float effectSize;
    public AudioController audioController;
    /** only not null if quick launched from Tiled */
    private JsonValue quickLaunchLevel;
    private boolean LIGHTS_ACTIVE = true;
    private Color spiritModeColor = new Color(1, 1, 1, 1);
    private boolean drawAdjacentLevels;
    private boolean gameFinished;
    public CreditsStage credits;

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
        RESPAWN,
        LEVEL_SWITCH,
        COMBINING_LIVES
    }
    /** State of gameplay used for camera */
    public static GameState gameState;
    /** If we have respawned in preUpdate(). Needed in postUpdate() for saving level state. */
    private boolean justRespawned;
    /** The color of the flash animation after resetting/undoing */
    private Color flashColor = new Color(1, 1, 1, 0);

    /** RayHandler that takes care of Box2DLights. This MUST be associated with the active World at all times. */
    private RayHandler rayHandler;

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
        collisionController.setCamera(canvas.getCamera());
        actionController.setCamera(canvas.getCamera());
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
     * @param constants the JSON value for constants
     */
    public void setAssets(HashMap<String, TextureRegion> tMap, HashMap<String, BitmapFont> fMap,
                          JsonValue constants){
        //for now levelcontroller will have access to these assets, but in the future we may see that it is unnecessary
        textureRegionAssetMap = tMap;
        fontAssetMap = fMap;
        constantsJSON = constants;
        setConstants(constants);
        displayFont = fMap.get("retro");

        //send the relevant assets to classes that need them
//        actionController.setAssets(sMap);
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
        NoveLight.setConstants(constants.get("lights"));
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
     * Creates a new game world with the default values.
     * <br><br>
     * The game world is scaled so that the screen coordinates do not agree
     * with the Box2D coordinates.  The bounds are in terms of the Box2d
     * world, not the screen.
     */
    protected GameController(int numLevels, boolean debugEnabled, AudioController audioController) {
        this(new Vector2(0,DEFAULT_GRAVITY), new Vector2(DEFAULT_SCALE,DEFAULT_SCALE), numLevels, debugEnabled, audioController);
    }

    /**
     * Creates a new game world with the default values.
     * <br><br>
     * The game world is scaled so that the screen coordinates do not agree
     * with the Box2D coordinates.  The bounds are in terms of the Box2d
     * world, not the screen.
     */
    protected GameController(String filepath, boolean debugEnabled, AudioController audioController) {
        this(new Vector2(0,DEFAULT_GRAVITY), new Vector2(DEFAULT_SCALE,DEFAULT_SCALE), 1, debugEnabled, audioController);
        JsonReader json = new JsonReader();
        quickLaunchLevel = json.parse(Gdx.files.internal(filepath));
    }

    /**
     * Creates and initialize a new instance of a GameController
     * <br><br>
     * The game world is scaled so that the screen coordinates do not agree
     * with the Box2D coordinates.  The bounds are in terms of the Box2D
     * world, not the screen.
     */
    protected GameController(Vector2 gravity, Vector2 scale, int numLevels, boolean debugEnabled, AudioController audioController) {
        this.audioController = audioController;
        this.scale = scale;
        this.debugEnabled = debugEnabled;
        gameFinished = false;
        debug = false;
        world = new World(gravity, true);

        this.numLevels = numLevels;
        levelNum = 1;
        levels = new Level[3];

        SpiritLine spiritLine = new SpiritLine(Color.WHITE, Color.WHITE, scale);
        Array<DeadBody> dbArray = new Array<>();
        for (int i = 0; i < 3; i++){
            levels[i] = new Level(world, scale, MAX_NUM_LIVES, rayHandler, spiritLine, dbArray);
        }
        currLevelIndex = 1;

        setLevels();
        actionController = new ActionController(scale, audioController);
        actionController.setLevel(levels[currLevelIndex]);
        collisionController = new CollisionController(actionController);
        collisionController.setLevel(levels[currLevelIndex]);

        gameState = GameState.PLAY;
        panTime = 0;
        respawnDelay = 0;

        drawAdjacentLevels = false;

        AssetDirectory internal = new AssetDirectory("jsons/loading.json");
        internal.loadAssets();
        internal.finishLoading();

        RayHandler.useDiffuseLight(true);

        hud = new HudStage("jsons/hud-stage.json", true);
        hud.lives = currLevel.getNumLives();
    }

    /**
     * Steps the level
     * <br><br>
     * The previous level is set to the current level<br>
     * The current level is set to the next level<br>
     * The next level is loaded in<br>
     */
    private void nextLevel(){
        levelNum++;
        Save.setProgress(levelNum);
        prevJV = getJSON();
        setJSON(nextJV);

        currLevelIndex = (currLevelIndex + 1) % 3;
        currLevel.setComplete(false);
        setLevels();
//        respawn();
        currLevel.setCat(prevLevel.getCat());
        currLevel.updateCheckpoints(prevLevel.getCheckpoint(), true); //because checkpoints and exits lie on the same position
        currLevel.setNumLives(prevLevel.getNumLives());
        currLevel.syncDeadBodyObjects(-1);
        prevLevel.removeCat();

        nextLevel.dispose();
        if (levelNum < numLevels) {
            nextJV = tiledJSON(levelNum + 1);
            nextLevel.populateTiled(nextJV, currLevel.bounds.x + currLevel.bounds.width, currLevel.bounds.y, levelNum + 1, currLevel.goalY, true);
        }
        initCurrLevel(true);
        collisionController.setDidChange(true);
        drawAdjacentLevels = true;
//        collisionController.setLevel(levels[currLevelIndex]);
//        actionController.setLevel(levels[currLevelIndex]);
    }

    /**
     * Steps the level
     * <br><br>
     * The next level is set to the current level<br>
     * The current level is set to the previous level<br>
     * The previous level is loaded in<br>
     */
    private void prevLevel(){
        levelNum--;
        nextJV = getJSON();
        setJSON(prevJV);
        currLevel.setReturn(false);
        if (levelNum > 1) {
            prevJV = tiledJSON(levelNum - 1);
        }
        currLevelIndex = Math.floorMod(currLevelIndex - 1,  3);
        setLevels();

        currLevel.setCat(nextLevel.getCat());
        currLevel.updateCheckpoints(nextLevel.getCheckpoint(), true);
        currLevel.setNumLives(nextLevel.getNumLives());
        currLevel.syncDeadBodyObjects(1);
        nextLevel.removeCat();

        prevLevel.dispose();
        if (levelNum > 1) {
            prevJV = tiledJSON(levelNum - 1);
            prevLevel.populateTiled(prevJV, currLevel.bounds.x, currLevel.bounds.y, levelNum - 1, currLevel.returnY, false);
        }

        initCurrLevel(true);
        collisionController.setDidChange(true);
        drawAdjacentLevels = true;
//        collisionController.setLevel(levels[currLevelIndex]);
//        actionController.setLevel(levels[currLevelIndex]);
    }

    /**
     * Loads in the JSON of a level
     *
     * @param levelNum the number associated with the level to be loaded in
     * @return JSON of the level
     */
    private JsonValue tiledJSON(int levelNum){
        return quickLaunchLevel == null ? directory.getEntry("tiledLevel" + levelNum, JsonValue.class) : quickLaunchLevel;
    }

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
     * 4. Make sure to use the file name (with h yphens) for the assets.json key, and the texture map key as well<br>
     *
     * @param directory	Reference to global asset manager.
     */
    public void gatherAssets(AssetDirectory directory) {
        // Allocate the tiles
        // Creating the hashmaps
        textureRegionAssetMap = new HashMap<>();
//        soundAssetMap = new HashMap<>();
        fontAssetMap = new HashMap<>();

        // List of textures we extract. These should be the SAME NAME as the keys in the assets.json.
        // A couple naming conventions: use hyphens, affix animation sprites with "-anim".
        String[] names = {
                // CAT
                "cat", "walk-anim", "jump-anim", "idle-sit-anim", "idle-stand-anim", "meow-anim",
                "trans-anim","climb-anim","corpse", "corpse2", "corpse3","corpse-burnt","trans2-anim","jump-mid",
                // SPIKES
                "spikes", "forest-spikes",
                // BUTTONS & SWITCHES
                "metal-button-base", "metal-button-top", "metal-switch-top", "metal-switch-base", "forest-button-top", "forest-switch-top",
                // FLAMETHROWERS
                "flamethrower", "flame-anim", "forest-flamethrower",
                // LASERS
                "laser",
                // CHECKPOINTS
                "checkpoint-anim", "checkpoint-active-anim", "checkpoint-base", "checkpoint-base-active", "checkpoint-activation-anim",
                // GOAL
                "goal", "goal-active", "goal-bases", "goal-idle-anim", "goal-inactive", "goal-final",
                // ROBOT & MOBS
                "robot-anim", "forest-mob-anim",
                // SPIRIT BOUNDARIES
                "spirit-anim", "spirit-photon", "spirit-photon-cat", "spirit-region",
                // ACTIVATABLE LIGHTS
                "ceiling-light", "wall-light",
                // TILESETS
                "metal-tileset", "climbable-tileset", "steel", "windows-tileset", "forest-tileset",
                "forestLeaves-tileset", "forest-climbable-tileset", "lab-bkg-tileset",
                // DOORS & PLATFORMS
                "door", "platform", "forest-platform", "forest-door",
                // BOX
                "box",
                // BACKGROUNDS
                "bg-lab", "bg-forest",
                // DECOR
                "tutorial-burn", "tutorial-camera", "tutorial-checkpoint", "tutorial-dash", "tutorial-pause",
                "tutorial-side-spikes", "tutorial-spikes", "tutorial-switch", "tutorial-walk-jump",
                "tutorial-jump-dash", "tutorial-undo", "tutorial-climb", "tutorial-cancel-switch",
                "tutorial-spirit-region",
                "cabinet-left", "cabinet-mid", "cabinet-right", "goggles", "microscope",
                "cat-vinci", "cat-tank-pink", "cat-tank-green","shelf",
                "tank", "test-tubes", "coke", "broken-robot", "coming-soon", "arrow-sign",
                "tutorial-cancel-switch", "wood-arrow", "wood-sign",
                "cat-tank","cat-tank-purple","chair","dandelions","desktop","firefly","flowers",
                "mushrooms","pin-board","robo","window-robo","x-ray","shelf2",
                "bkg1", "bkg2", "bkg3", "bkg4"
                }; // Unsure if this is actually being used
        for (String n : names){
//            System.out.println(n);
//            System.out.println(directory.getEntry(n, Texture.class));
            textureRegionAssetMap.put(n, new TextureRegion(directory.getEntry(n, Texture.class)));
        }

        names = new String[]{"jump", "dash", "metal-landing", "meow-1", "meow-2", "meow-3", "death-fall",
                             "button-click", "death-spike", "death-fire", "death-laser", "death-mob"};
        audioController.createSoundEffectMap(directory, names);

        names = new String[]{"bkg-lab-1", "bkg-forest-1"};
        audioController.createMusicMap(directory, names);

        audioController.playLab();
//        audioController.playLevelMusic();

        names = new String[]{"retro"};
        for (String n : names){
            fontAssetMap.put(n, directory.getEntry(n, BitmapFont.class));
        }

        constants = directory.getEntry("constants", JsonValue.class);
        this.directory = directory;

        background = textureRegionAssetMap.get("bg-lab").getTexture();

        setAssets(textureRegionAssetMap, fontAssetMap, constants);
        setJSON(tiledJSON(1));
        nextJV = tiledJSON(2);

        //Set controls
        InputController.getInstance().setControls(directory.getEntry("controls", JsonValue.class));

        if (debugEnabled) {
//            InputController.getInstance().writeTo("debug-input/dash-death-speed-debug.txt");
//            InputController.getInstance().readFrom("debug-input/dash-death-speed-debug.txt");
        }
    }

    public void updateControls() {
        InputController.getInstance().setControls(directory.getEntry("controls", JsonValue.class));
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
        Cat cat = currLevel.getCat();
        cat.reset();
        cat.setActive(false);
        cat.setFacingRight(currLevel.getCheckpoint() != null ? currLevel.getCheckpoint().facingRight() : true);
        cat.setPosition(currLevel.getRespawnPos());
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
        this.levelNum = levelNum;

        prevLevel.dispose();
        currLevel.dispose();
        nextLevel.dispose();
        Vector2 gravity = new Vector2( world.getGravity() );
        world.dispose();
        world = new World(gravity, true);
        if (rayHandler != null) {
            rayHandler.dispose();
        }

        rayHandler = new RayHandler(world);
        rayHandler.setAmbientLight(0.5f, 0.5f, 0.5f, 1f);

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
        currLevel.getdeadBodyArray().clear();

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

        if (audioController.getCurrMusic().equals("metal") && currLevel.getBiome().equals("forest")) {
            System.out.println("switch to forest");
            audioController.playForest();
        }
        else if (audioController.getCurrMusic().equals("forest") && currLevel.getBiome().equals("metal")) {
            System.out.println("switch to lab");
            audioController.playLab();
        }
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
        canvas.getCamera().setLevelBounds(currLevel.bounds, scale, true);
        canvas.getCamera().setGameplayBounds(currLevel.bounds, scale, true);
        canvas.getCamera().updateCamera(currLevel.getCat().getPosition().x*scale.x, currLevel.getCat().getPosition().y*scale.y, cameraGlide, canvas.getCamera().getGameplayBounds());
        currLevel.unpause();
        nextLevel.pause();
        prevLevel.pause();
        undoTime = 0;

        if (audioController.getCurrMusic().equals("metal") && currLevel.getBiome().equals("forest")) {
            audioController.playForest();
        }
        else if (audioController.getCurrMusic().equals("forest") && currLevel.getBiome().equals("metal")) {
            audioController.playLab();
        }

        resume();
    }

    /**
     * Dispose of all (non-static) resources allocated to this mode.
     */
    public void dispose() {
        prevLevel.dispose();
        currLevel.dispose();
        nextLevel.dispose();
        world.dispose();
        rayHandler.dispose();
        rayHandler = null;
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
        if (debugEnabled && input.didDebug()) {
            debug = !debug;
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

        if (currLevel.canSwitch && input.holdSwitch()) {
            spiritModeTicks++;
            updateVFX(true, input.switchPressed(), dt);
        } else {
            updateVFX(false, false, dt);
            spiritModeTicks = 0;
        }

        if (currLevel.isFailure() || (input.didReset() && debugEnabled)) {
            if (currLevel.isFailure()) flashColor.set(1, 0, 0, 1);
            reset();
        } else if (currLevel.isComplete() && levelNum < numLevels) {
            pause();
            nextLevel();
            return false;
        } else if (currLevel.isReturn() && levelNum > 1) {
            pause();
            prevLevel();
            return false;
        }  else if (debugEnabled && input.didNext() && levelNum < numLevels) {
            pause();
            init(levelNum + 1);
            return false;
        }  else if (debugEnabled && input.didPrev() && levelNum > 1) {
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

        if (collisionController.isGameFinished() && !gameFinished) {
            credits = new CreditsStage("jsons/credits-stage.json", true, false);
            gameFinished = true;
        }

        actionController.update(dt);

        currLevel.getSpiritLine().setOuterColor(spiritModeColor);
        flashColor.a -= flashColor.a/10;
        updateState();

        hud.lives = currLevel.getNumLives();
        hud.updateLives();
        hud.updateMagnifying(gameState == GameState.PLAYER_PAN);
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
     * Updates parameters for the vfx depending on how long the player has been in spirit mode.
     *
     * @param increasing    true if effect strength should increase
     * @param justPressed   if the switch button was just pressed
     * @param dt            time since last frame
     */
    private void updateVFX(boolean increasing, boolean justPressed, float dt){

        if (!increasing) {
            effectSize += -effectSize/10f;
            if (effectSize - 0.05f < 0) effectSize = 0;
        } else {
            if (spiritModeTicks <= MAX_SPIRIT_MODE_TICKS) {
                effectSize = (float) Math.sin(Math.PI * (double) (spiritModeTicks / 2f / MAX_SPIRIT_MODE_TICKS));
            } else {
                effectSize = 1;
            }
        }

        Color targetColor = currLevel.getCat().getSpiritRegionColor();
        spiritModeColor.r += (targetColor.r - spiritModeColor.r) * 0.05f;
        spiritModeColor.g += (targetColor.g - spiritModeColor.g) * 0.05f;
        spiritModeColor.b += (targetColor.b - spiritModeColor.b) * 0.05f;

    }

    /**
     * Updates GameState and moves camera accordingly
     */
    public void updateState(){
        Camera cam = canvas.getCamera();
        InputController input = InputController.getInstance();

        if(drawAdjacentLevels){
            gameState = GameState.LEVEL_SWITCH;
        }
        //resetting automatically resets camera to cat
        if(justReset){
            gameState = GameState.PLAY;
        }
        if(input.didPan()){
            gameState = GameState.PLAYER_PAN;
            //move camera
            cam.updateCamera(cam.getX()+input.getCamHorizontal(),cam.getY()+ input.getCamVertical(),false, cam.getLevelBounds());
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
            } else if (actionController.isCombiningLives()) {
                gameState = GameState.COMBINING_LIVES;
            }
            else {
                currLevel.getCat().setActive(true);
                //zoom normal when in play state and not panning and not switching bodies
                if (!(currLevel.canSwitch && input.holdSwitch()) && !input.didPan()) {
                    cam.setZoom(false, -1f);
                }
                DeadBody nextDeadBody = currLevel.getNextBody();
                if (currLevel.canSwitch && input.holdSwitch() && nextDeadBody != null) {
                    cam.setGlideMode("SWITCH_BODY");
                    cam.switchBodyCam(nextDeadBody.getX() * scale.x, nextDeadBody.getY() * scale.y);
                } else {
                    if(cam.getGlideMode() == "SWITCH_BODY")
                        cam.setGlideMode("NORMAL");
                    cam.updateCamera(x_pos, y_pos, true, cam.getGameplayBounds());
                }
            }

            /** Handles cat dying in cameraRegion and respawning in non-cameraRegion **/
            if(currLevel.getCameraRegions().isEmpty() && currLevel.getCat().isActive()){
                canvas.getCamera().setDefaultZoom(Camera.CAMERA_ZOOM);
                cam.setGameplayBounds(cam.getLevelBounds(), currLevel.getScale(), false);
            }
        }
        if(gameState == GameState.PAN) {
            cam.updateCamera(panTarget.get(0).getXPos() * scale.x, panTarget.get(0).getYPos() * scale.y, true, cam.getLevelBounds());
            input.setDisableAll(true);
            if (!cam.isGliding()) {
                panTime += 1;
                if (panTime == PAN_HOLD) {
                    gameState = GameState.PLAY;
                }
            }
        }
        if(gameState == GameState.PLAYER_PAN){
            cam.setZoom(true, 1.0f);
        }
        if(gameState == GameState.RESPAWN){

            float xPos = currLevel.getRespawnPos().x*scale.x;
            float yPos = currLevel.getRespawnPos().y*scale.y;
            input.setDisableAll(true);
            respawnDelay += 1;
            if(currLevel.getdeadBodyArray().size > 0 && respawnDelay < RESPAWN_DELAY/2){
                xPos = currLevel.getdeadBodyArray().get(currLevel.getdeadBodyArray().size-1).getX()*scale.x;
                yPos = currLevel.getdeadBodyArray().get(currLevel.getdeadBodyArray().size-1).getY()*scale.y;
            }
            cam.updateCamera(xPos, yPos, true, cam.getGameplayBounds());
            if(respawnDelay == RESPAWN_DELAY){
                respawnDelay = 0;
                input.setDisableAll(false);
                gameState = GameState.PLAY;
                currLevel.getCat().setActive(true);
                currLevel.getCat().setLightActive(true);
                currLevel.getCat().setPosition(currLevel.getRespawnPos());
            }
        }
        if(gameState == GameState.LEVEL_SWITCH){
            drawAdjacentLevels = true;
            input.setDisableAll(true);
            cam.setZoom(false, -1f); //smoother level transition across different zooms
            float x_pos = currLevel.getCat().getPosition().x*scale.x;
            float y_pos = currLevel.getCat().getPosition().y*scale.y;
            cam.updateCamera(x_pos, y_pos, true, cam.getGameplayBounds());
            if(!cam.isGliding()){
                gameState = GameState.PLAY;
                drawAdjacentLevels = false;
            }
        }

        if (gameState == GameState.COMBINING_LIVES) {
            input.setDisableAll(true);
            if (!actionController.isCombiningLives()) {
                gameState = GameState.PLAY;
            }
        }
    }
    @Override
    public void render(float delta) {
        //FOR DEBUGGING
        if (debugEnabled) {
            delta = 1 / 60f;
            if (Gdx.input.isKeyPressed(Input.Keys.F)) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (!paused && preUpdate(delta)) {
            update(delta); // This is the one that must be defined.
            postUpdate(delta);
            assert currLevel.getNumLives() == 9 - currLevel.getdeadBodyArray().size;
        }
        else {
            updateState();
        }

        // Main game draw
        draw(delta);

        if (LIGHTS_ACTIVE) {
            updateAndRenderRayHandler();
        }

        if (gameFinished) {
            credits.draw();
            if (credits.finished) {
                gameFinished = false;
                credits.finished = false;
                listener.exitScreen(this, 89);
            }
        } else {
            // Menu draw
            hud.draw();
            if (paused && stageController != null) { stageController.render(delta); }
        }
    }

    /**
     * Updates the RayHandler's combined matrix to properly reflect the camera's current position
     * and viewport dimensions.
     * <br>
     * We need to do this because it is inconvenient to scale the position of box2dlights by our world
     * scale every time they move, especially when they can be attached to bodies. Handling the scaling
     * in the combined matrix takes care of the transformation for us for all lights.
     */
    private void updateAndRenderRayHandler() {
        OrthographicCamera c = canvas.getCamera().getCamera();
        rayHandler.setCombinedMatrix(
                c.combined.cpy().scl(DEFAULT_SCALE),
                c.position.x / DEFAULT_SCALE,
                c.position.y / DEFAULT_SCALE,
                c.viewportWidth * c.zoom,
                c.viewportHeight * c.zoom
        );
        Viewport vp = canvas.getViewport();
        int bufferScale = Math.round(Gdx.graphics.getBackBufferScale());
        rayHandler.useCustomViewport(
                vp.getScreenX() * bufferScale,
                vp.getScreenY() * bufferScale,
                vp.getScreenWidth() * bufferScale,
                vp.getScreenHeight() * bufferScale);
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
        audioController.pauseLevelMusic();
        paused = true;
        actionController.pause();
    }

    /**
     * Called when the Screen is resumed from a paused state.
     * <br><br>
     * This is usually when it regains focus.
     */
    public void resume() {
        audioController.playLevelMusic();
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

        if (effectSize > 0) {
            //draw spiritline into framebuffer to apply masking
            canvas.beginFrameBuffer(canvas.getSpiritLineFrameBuffer());

            Gdx.gl.glColorMask(true, true, true, true);
            canvas.setBlendState(GameCanvas.BlendState.NO_PREMULT);
            currLevel.getSpiritLine().draw(canvas);
            canvas.flush();

            Gdx.gl.glColorMask(false, false, false,                         true);
            canvas.setBlendState(GameCanvas.BlendState.MASKING);
            currLevel.getCat().drawMask(canvas);
            canvas.spriteBatch.setColor(Color.WHITE);
            if (currLevel.getNextBody() != null) currLevel.getNextBody().drawMask(canvas);

            canvas.spriteBatch.flush();
            canvas.getSpiritLineFrameBuffer().end();

            canvas.setBlendState(GameCanvas.BlendState.NO_PREMULT);
            Gdx.gl.glColorMask(true, true, true, true);
        }

        //draw everything else into main framebuffer
        canvas.beginFrameBuffer(canvas.getMainFrameBuffer());
        if (currLevel.getBiome() != null && currLevel.getBiome().equals("metal")) {
            background = textureRegionAssetMap.get("bg-lab").getTexture();
        } else {
            background = textureRegionAssetMap.get("bg-forest").getTexture();
        }

        if (effectSize > 0) { canvas.setGreyscaleShader(effectSize); }
        canvas.draw(background, Color.WHITE, canvas.getCamera().getX() - canvas.getWidth()/2f, canvas.getCamera().getY()  - canvas.getHeight()/2f, canvas.getWidth(), canvas.getHeight());

        if (drawAdjacentLevels) { //TODO: only draw when necessary
            prevLevel.draw(canvas, false, effectSize);
            nextLevel.draw(canvas, false, effectSize);
            if (effectSize > 0) { canvas.setGreyscaleShader(effectSize); }
        }
        currLevel.draw(canvas, gameState != GameState.RESPAWN, effectSize);

        if (effectSize > 0) canvas.drawFrameBuffer(canvas.getSpiritLineFrameBuffer());

        canvas.endFrameBuffer(canvas.getMainFrameBuffer());

        //draw main frame buffer to screen
        if (effectSize > 0) { //apply spirit mode shader if necessary
            canvas.setSpiritModeShader(1.8f - 0.525f * effectSize, 0.3f,
                    spiritModeColor, spiritModeColor, spiritModeTicks/60f);
        }
        canvas.applyViewport(false);
        canvas.setBlendState(GameCanvas.BlendState.ALPHA_BLEND);
        canvas.drawFrameBuffer(canvas.getMainFrameBuffer());

        if (effectSize > 0) canvas.setShader(null);
        canvas.drawRectangle(canvas.getCamera().getX() - canvas.getWidth()/2f, canvas.getCamera().getY()  - canvas.getHeight()/2f, canvas.getWidth(), canvas.getHeight(), flashColor, 1, 1);

        canvas.end();

        if (debug) {
            canvas.beginDebug();
            canvas.applyViewport(false);
            if (levelNum > 1) prevLevel.drawDebug(canvas);
            currLevel.drawDebug(canvas);
            if (levelNum < numLevels) nextLevel.drawDebug(canvas);
            canvas.endDebug();
        }

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
        }
        if (currLevel.getCheckpoint() != null) {
            respawn(cameraMovement);
            currLevel.getCat().setActive(true);
        }
        currLevel.getCat().setPosition(currLevel.getRespawnPos());
    }
}