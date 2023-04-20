package edu.cornell.gdiac.game;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.audio.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.*;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.game.object.*;

import edu.cornell.gdiac.game.obstacle.*;
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
    /** Amount of time to be delayed after respawn **/
    final float RESPAWN_DELAY = 60f; //about 17ms per RESPAWN_DELAY unit (holds 1 second-0.5s on dead body, 0.5s on respawned cat)

    /**
     * PLAY: User has all controls and is in game
     * LEVEL_SWITCH: Camera transition to next level (all controls stripped from user)
     * PLAYER_PAN: Camera zooms out and player is free to pan around the level (all other gameplay controls stripped from user)
     * PAN: Camera movement not controlled by player (e.g. when activator is pressed or at beginning of level)
     * RESPAWN: Camera focuses on dead body for half of RESPAWN_DELAY and focuses on newly respawned cat for half of RESPAWN_DELAY
     */
    enum GameplayState{
        PLAY,
        LEVEL_SWITCH,
        PLAYER_PAN,
        PAN,
        RESPAWN
    }
    /** State of gameplay */
    private GameplayState gameplayState;
    /** Array storing level states of past lives. The ith element of this array is the state
     * of the level at the instant the player had died i times. */
    private LevelState[] prevLivesState = new LevelState[9];
    /** If we have respawned in preUpdate(). Needed in postUpdate() for saving level state. */
    private boolean justRespawned;
    /** The color of the flash animation after resetting/undoing */
    private Color flashColor = new Color(1, 1, 1, 0);


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
        world = new World(gravity, false);

        this.numLevels = numLevels;
        levelNum = 1;
        levels = new Level[3];
        for (int i = 0; i < 3; i++){
            levels[i] = new Level(world, scale, MAX_NUM_LIVES);
        }
        currLevelIndex = 1;

        setLevels();
        actionController = new ActionController(scale, volume);
        actionController.setLevel(levels[currLevelIndex]);
        collisionController = new CollisionController(actionController);
        collisionController.setLevel(levels[currLevelIndex]);

        gameplayState = GameplayState.PLAY;
        panTime = 0;
        respawnDelay = 0;
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
     * Steps the level
     * <br><br>
     * The previous level is set to the current level<br>
     * The current level is set to the next level<br>
     * The next level is loaded in<br>
     *
     * @param resetSpawn whether the player spawns at the respawn point of the level or at the edge
     *                    (from previous level)
     */
    public void nextLevel(boolean resetSpawn){
        levelNum++;
        prevJV = getJSON();
        setJSON(nextJV);
        setRet(false);


        currLevelIndex = (currLevelIndex + 1) % 3;
        currLevel.setComplete(false);
        setLevels();
        respawn();
        actionController.setLevel(currLevel);
        collisionController.setLevel(currLevel);

        nextLevel.dispose();
        if (levelNum < numLevels) {
            nextJV = tiledJSON(levelNum + 1);
            nextLevel.populateTiled(nextJV, currLevel.bounds.x + currLevel.bounds.width, currLevel.bounds.y, currLevel.goalY, true);
        }
    }

    /**
     * Steps the level
     * <br><br>
     * The next level is set to the current level<br>
     * The current level is set to the previous level<br>
     * The previous level is loaded in<br>
     *
     * @param resetSpawn whether the player spawns at the respawn point of the level or at the edge
     *                    (from previous level)
     */
    public void prevLevel(boolean resetSpawn){
        levelNum--;
        nextJV = getJSON();
        setJSON(prevJV);
        setRet(false);
        if (levelNum > 1) {
            prevJV = tiledJSON(levelNum - 1);
        }

        currLevelIndex = Math.floorMod(currLevelIndex - 1,  3);
        setLevels();

        actionController.setLevel(currLevel);
        collisionController.setLevel(currLevel);
        respawn();

        prevLevel.dispose();
        if (levelNum > 1) {
            prevJV = tiledJSON(levelNum - 1);
            prevLevel.populateTiled(prevJV, currLevel.bounds.x, currLevel.bounds.y, currLevel.returnY, false);
        }

    }
    public void setCurrLevel(int level) {
        if (level < numLevels) {
            setJSON(tiledJSON(level+1));
        }
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
     *
     * @param directory	Reference to global asset manager.
     */
    public void gatherAssets(AssetDirectory directory) {
        // Allocate the tiles
        // Creating the hashmaps
        textureRegionAssetMap = new HashMap<>();
        soundAssetMap = new HashMap<>();
        fontAssetMap = new HashMap<>();

        String[] names = {"cat", "sit", "deadCat", "jumpingCat", "jump_anim", "walk", "button_anim",
                "spikes", "button", "flamethrower", "flame", "laser", "checkpoint", "checkpointActive",
                "checkpoint_anim", "checkpoint_active_anim", "checkpoint_base", "checkpoint_base_active",
                "background", "flame_anim", "roboMob",
                "spirit_anim", "spirit_photon", "spirit_photon_cat", "spirit_region",
                "meow_anim", "idle_anim", "idle_anim_stand",
                "metal_tileset", "steel","burnCat"};

        for (String n : names){
            textureRegionAssetMap.put(n, new TextureRegion(directory.getEntry(n, Texture.class)));
        }

        names = new String[]{"jump", "dash", "metalLanding", "pew", "plop", "meow"};
        for (String n : names){
            soundAssetMap.put(n, directory.getEntry(n, Sound.class));
        }

        names = new String[]{"retro"};
        for (String n : names){
            fontAssetMap.put(n, directory.getEntry(n, BitmapFont.class));
        }

        constants = directory.getEntry("constants", JsonValue.class);
        this.directory = directory;

        // Giving assets to levelController
        setAssets(textureRegionAssetMap, fontAssetMap, soundAssetMap, constants);
        setJSON(tiledJSON(1));
        nextJV = tiledJSON(2);

        //Set controls
        InputController.getInstance().setControls(directory.getEntry("controls", JsonValue.class));

//		InputController.getInstance().writeTo("inputLogs/alphademo.txt");
//		InputController.getInstance().readFrom("inputLogs/alphademo.txt");
    }

    /**
     * Handles respawning the cat after their death
     * <br><br>
     * The level model died is set to false<br>
     * The level model cat is set to its respawn position
     */
    public void respawn() {
        currLevel.setDied(false);
        currLevel.getCat().setPosition(currLevel.getRespawnPos());
        currLevel.getCat().setFacingRight(true);
        currLevel.getCat().setJumpPressed(false);
        currLevel.getCat().setGrounded(true);
        currLevel.getCat().setLinearVelocity(Vector2.Zero);
        justRespawned = true;
    }

    /**
     * Resets the status of the game so that we can play again.
     *
     * Note that this method simply repopulates the existing level. Care needs to be taken to
     * properly dispose the level so that the level reset is clean.
     */
    protected void reset() {

        prevLevel.dispose();
        currLevel.dispose();
        nextLevel.dispose();
        Vector2 gravity = new Vector2( world.getGravity() );
        world.dispose();
        world = new World(gravity, false);

        justRespawned = true;
        prevLevel.setWorld(world);
        currLevel.setWorld(world);
        nextLevel.setWorld(world);
        world.setContactListener(collisionController);
        world.setContactFilter(collisionController);

        collisionController.setReturn(false);

        boolean tempRet = isRet();
        setRet(false);
        currLevel.populateTiled(levelJV);
        nextLevel.populateTiled(nextJV, currLevel.bounds.x + currLevel.bounds.width, currLevel.bounds.y, currLevel.goalY, true);
        actionController.setMobControllers(currLevel);
        prevLivesState = new LevelState[9];
        canvas.getCamera().setLevelBounds(currLevel.bounds, scale);
        canvas.getCamera().updateCamera(currLevel.getCat().getPosition().x*scale.x, currLevel.getCat().getPosition().y*scale.y, false);
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
        Camera cam = canvas.getCamera();
        if(input.didPan()){
            gameplayState = GameplayState.PLAYER_PAN;
            //move camera
            cam.updateCamera(cam.getX()+input.getCamHorizontal(),cam.getY()+ input.getCamVertical(),false);
        }
        else{
            if(gameplayState == GameplayState.PLAYER_PAN)
                gameplayState = GameplayState.PLAY;
        }
        // Toggle debug
        if (input.didDebug()) {
            debug = !debug;
//            canvas.getCamera().debugCamera(debug);
        }

        if (!currLevel.isFailure() && currLevel.getDied()) {
            respawn();
        }

        if (input.didExit()){
            pause();
            listener.exitScreen(this, EXIT_QUIT);
            return false;
        }

        if (currLevel.isFailure() || input.didReset()) {
            reset();
        } else if (currLevel.isComplete() || input.didNext()) {
            if (levelNum < numLevels) {
                pause();
                System.out.println("next: "+ currLevel);
                nextLevel(input.didNext());
                return false;
            }
        } else if (isRet() || input.didPrev()) {
            if (levelNum > 1) {
                pause();
                System.out.println("return: "+ currLevel);
                prevLevel(input.didPrev());
                return false;
            }
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
        Camera cam = canvas.getCamera();
        InputController input = InputController.getInstance();
        if (collisionController.getReturn()) {
            setRet(true);
        }
        actionController.update(dt);
        flashColor.a -= flashColor.a/10;
        for (Activator a : currLevel.getActivators()){
            if (a.isPressed() && a.getPan()){
                a.setPan(false);
                if(currLevel.getActivationRelations().containsKey(a.getID())){
                    panTarget = currLevel.getActivationRelations().get(a.getID());
                }
                gameplayState = GameplayState.PAN;
            }
        }
        if(gameplayState == GameplayState.PLAY){
            panTime = 0;
            respawnDelay = 0;
            input.setDisableAll(false);
            float x_pos = currLevel.getCat().getPosition().x*scale.x;
            float y_pos = currLevel.getCat().getPosition().y*scale.y;
            if(justRespawned) {
                gameplayState = GameplayState.RESPAWN;
            }
            //zoom normal when in play state and not panning and not switching bodies
            if(!input.holdSwitch() && !input.didPan()){
                cam.zoomOut(false);
            }
            DeadBody nextDeadBody = currLevel.getNextBody();
            if(input.holdSwitch()&&nextDeadBody != null){
                cam.setGlideMode("SWITCH_BODY");
                cam.switchBodyCam(nextDeadBody.getX()*scale.x, nextDeadBody.getY()*scale.y);
            }
            else{
                cam.setGlideMode("NORMAL");
                cam.updateCamera(x_pos, y_pos, true);
            }
        }
        else if(gameplayState == GameplayState.LEVEL_SWITCH){
            /**
             * TODO: Seamless Level Switching
             */
            gameplayState = GameplayState.PLAY;
        }
        else if(gameplayState == GameplayState.PAN){
            cam.updateCamera(panTarget.get(0).getXPos()*scale.x,panTarget.get(0).getYPos()*scale.y, true);
            if(!cam.isGliding()){
                panTime += 1;
                if(panTime == PAN_HOLD){
                    gameplayState = GameplayState.PLAY;
                }
            }
        }
        else if(gameplayState == GameplayState.PLAYER_PAN){
            cam.zoomOut(true);
        }
        else if(gameplayState == GameplayState.RESPAWN){
            justRespawned = false;
            float x_pos = currLevel.getCat().getPosition().x*scale.x;
            float y_pos = currLevel.getCat().getPosition().y*scale.y;
            input.setDisableAll(true);
            respawnDelay += 1;
            if(currLevel.getdeadBodyArray().size > 0 && respawnDelay < RESPAWN_DELAY/2){
                x_pos = currLevel.getdeadBodyArray().get(currLevel.getdeadBodyArray().size-1).getX()*scale.x;
                y_pos = currLevel.getdeadBodyArray().get(currLevel.getdeadBodyArray().size-1).getY()*scale.y;
            }
            cam.updateCamera(x_pos, y_pos, true);
            if(respawnDelay == RESPAWN_DELAY){
                respawnDelay = 0;
                input.setDisableAll(false);
                gameplayState = GameplayState.PLAY;
            }
        }
    }

    /**
     * @param gameplayState "PLAY" or "SWITCH"
     */
    public void setGameplayState(String gameplayState){
        if(gameplayState.equals("PLAY")){
            this.gameplayState = GameplayState.PLAY;
        }
        else if(gameplayState.equals("SWITCH")){
            this.gameplayState = GameplayState.LEVEL_SWITCH;
        }
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

        //Save level state if necessary. This must be done here so that we can save dead bodies after they are added
        //to the world. Ideally we could do this in respawn(), but that would involve rearranging the order of everything
        //which may be a pain. Also it does seem like saving state after stepping the world has better results - will need
        //testing.

        if (justRespawned) {
            prevLivesState[9 - currLevel.getNumLives()] = new LevelState(currLevel);
        }


        if (InputController.getInstance().didUndo()) {
            if (currLevel.getNumLives() < 9) {
                loadLevelState(prevLivesState[8 - currLevel.getNumLives()]);
                flashColor.set(1, 1, 1, 1);
            }
        }
    }

    @Override
    public void render(float delta) {
        if (preUpdate(delta)) {
            update(delta); // This is the one that must be defined.
            postUpdate(delta);
        }
        draw(delta);
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
     * Called when the Screen is resumed from a paused state.
     * <br><br>
     * This is usually when it regains focus.
     */
    public void resume() {
        // TODO Auto-generated method stub
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
        canvas.applyViewport();
        if (levelNum > 0) prevLevel.draw(canvas);
        currLevel.draw(canvas);
        //TODO: remove
        if (levelNum < numLevels) nextLevel.draw(canvas);
        canvas.drawRectangle(0, 0, currLevel.bounds.width, currLevel.bounds.height, flashColor, scale.x, scale.y);
        canvas.end();

        if (debug) {
            canvas.beginDebug();
            if (levelNum > 0) prevLevel.drawDebug(canvas);
            currLevel.drawDebug(canvas);
            if (levelNum < numLevels) nextLevel.drawDebug(canvas);
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
        currLevel.setNumLives(state.numLives);
        if (state.checkpoint != null) {
            currLevel.updateCheckpoints(state.checkpoint);
        } else {
            currLevel.resetCheckpoints();
        }
        for (Obstacle obs : currLevel.getObjects()){

            if (obs instanceof DeadBody){
                //need to remove and rebuild dead body array because number of dead bodies can change between saved states
                DeadBody db = (DeadBody) obs;
                currLevel.removeDeadBody(db);
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
            currLevel.loadDeadBodyState(dbState);
        }
    }
}