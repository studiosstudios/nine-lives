package edu.cornell.gdiac.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.physics.box2d.*;
import edu.cornell.gdiac.game.object.*;

import edu.cornell.gdiac.game.obstacle.*;
import edu.cornell.gdiac.util.PooledList;

import java.util.HashMap;
/**
 * Represents a single level in our game
 * <br><br>
 * Note that the constructor does very little.  The true initialization happens
 * by reading the JSON value.  To reset a level, dispose it and reread the JSON.
 * <br><br>
 * Adapted from Walker M. White's LevelMode.java in Cornell CS 3152, Spring 2023.
 */
public class Level {

    /** The Box2D world */
    protected World world;
    /** The boundary of the level */
    protected Rectangle bounds;
    /** The world scale */
    protected Vector2 scale;
    // Physics objects for the game
    /** Reference to the character cat */
    private Cat cat;
    /** Reference to the goal exit */
    private Exit goalExit;
    protected float goalY;
    /**Reference to the return exit */
    private Exit returnExit;
    protected float returnY;

    /** Tiles of level */
    protected Tiles tiles;
    /** All the objects in the world. */
    protected PooledList<Obstacle> objects  = new PooledList<>();
    /** Queue for adding objects */
    protected PooledList<Obstacle> addQueue = new PooledList<>();
    /** Queue to add joints to the world */
    protected PooledList<JointDef> jointQueue = new PooledList<>();
    /** Whether we have completed this level */
    private boolean complete;
    /** Whether we have failed at this world (and need a reset) */
    private boolean failed;
    /** Whether we have died */
    private boolean died;
    /** The number of lives remaining */
    private int numLives;
    /** The max lives allowed */
    private final int maxLives;
    private int tileSize;
    /** cache for setting texture scales */
    private Vector2 textureScaleCache = new Vector2();

    /** hashmap to represent activator-activatable relationships:
     *   keys are activator ids specified in JSON*/
    private HashMap<String, Array<Activatable>> activationRelations;

    /** object arrays */
    private final Array<Activator> activators;
    private final Array<Activatable> activatables;
    private final Array<DeadBody> deadBodyArray;
    private final Array<Mob> mobArray;
    private Checkpoint currCheckpoint;
    private final Array<Laser> lasers;
    private final Array<SpiritRegion> spiritRegionArray;
    /** The respawn position of the player */
    private Vector2 respawnPos;

    /** texture assets */
    private HashMap<String, TextureRegion> textureRegionAssetMap;

    //region Spirit mode stuff
    private boolean spiritMode;
    /** The spirit line */
    private SpiritLine spiritLine;
    //endregion
    /** the initial respawn position for this level */
    private Vector2 startRespawnPos;
    /** properties map cache */
    private ObjectMap<String, Object> propertiesMap = new ObjectMap<>();

    private Array<LevelState> levelStates;

    /**
     * Returns the bounding rectangle for the physics world
     * <br><br>
     * The size of the rectangle is in physics, coordinates, not screen coordinates
     *
     * @return the bounding rectangle for the physics world
     */
    public Rectangle getBounds() {
        return bounds;
    }

    /**
     * Returns the scaling factor to convert physics coordinates to screen coordinates
     *
     * @return the scaling factor to convert physics coordinates to screen coordinates
     */
    public Vector2 getScale() {
        return scale;
    }

    /**
     * Returns a reference to the Box2D World
     *
     * @return a reference to the Box2D World
     */
    public World getWorld() {
        return world;
    }


    /**
     * Returns a reference to the player cat
     *
     * @return a reference to the player cat
     */
    public Cat getCat() {
        return cat;
    }

    /**
     * Sets the cat for this level. This is used for level switching.
     *
     * @param cat New cat
     */
    public void setCat(Cat cat) {
        objects.remove(this.cat);
        objects.add(cat);
        this.cat = cat;
    }

    /**
     * Removes the cat from this level. This is used for level switching.
     */
    public void removeCat() { objects.remove(cat); cat = null; }

    public Checkpoint getCheckpoint() {return currCheckpoint;}

    /**
     * Returns a reference to the array of activators
     *
     * @return a reference to the activators
     */
    public Array<Activator> getActivators() { return activators; }

    public Array<Laser> getLasers() { return lasers; }

    /**
     * Returns a reference to the hashmap of activation relations
     *
     * @return a reference to the activation relations
     */
    public HashMap<String, Array<Activatable>> getActivationRelations() { return activationRelations; }

    /**
     * Returns a reference to the list of objects
     *
     * @return a reference to all objects
     */
    public PooledList<Obstacle> getObjects(){ return objects; }

    /**
     * Returns a reference to the array of dead bodies
     *
     * @return a reference to the dead body array
     */
    public Array<DeadBody> getdeadBodyArray() { return deadBodyArray; }

    /**
     * Returns a reference to the array of mobs
     *
     * @return a reference to the mob array
     */
    public Array<Mob> getMobArray() { return mobArray; }

    /**
     * Returns a reference to the array of spirit regions
     *
     * @return a reference to the spirit region array
     */
    public Array<SpiritRegion> getSpiritRegionArray() { return spiritRegionArray; }

    /**
     * Returns a reference to the respawn position
     *
     * @return a reference to the respawn position
     */
    public Vector2 getRespawnPos() { return respawnPos; }

    /**
     * Sets the respawn position
     *
     * @param pos the Vector2 value to set respawn position to
     */
    public void setRespawnPos(Vector2 pos) { respawnPos = pos; }

    /**
     * Returns true if the level is completed.
     * <br><br>
     * If true, the level will advance after a countdown
     *
     * @return true if the level is completed.
     */
    public boolean isComplete() {
        return complete;
    }

    /**
     * Returns true if the level is failed.
     * <br><br>
     * If true, the level will reset after a countdown
     *
     * @return true if the level is failed.
     */
    public boolean isFailure() {
        return failed;
    }

    /**
     * Sets whether the level is failed.
     * <br><br>
     * If true, the level will reset after a countdown
     *
     * @param value whether the level is failed.
     */
    public void setFailure(boolean value) {
        failed = value;
    }

    /**
     * Sets the game world
     *
     * @param world the world to set to
     */
    public void setWorld(World world) { this.world = world; }

    /**
     * Sets whether the player died in the level
     *
     * @param died the value to set died to
     */
    public void setDied(boolean died){ this.died = died; }

    /**
     * Returns a reference to whether the player has died
     *
     * @return a reference to died
     */
    public boolean getDied() { return died; }

    /**
     * Returns a reference to the number of lives the player has remaining in this level
     *
     * @return a reference to number of lives
     */
    public int getNumLives() { return numLives; }

    public Exit getGoalExit() { return goalExit; }

    public Exit getReturnExit() { return returnExit; }

    /**
     * Sets the number of lives in this level
     *
     * @param val the value to set numLives to
     */
    public void setNumLives(int val) { numLives = val; }

    /**
     * Resets the number of lives to the max number of lives
     */
    public void resetLives() { numLives = maxLives; }


    /**
     * Stores textures for creating new objects in the level
     *
     * @param tMap Texture map for objects
     */
    public void setAssets(HashMap<String, TextureRegion> tMap){ textureRegionAssetMap = tMap; }

    /**
     * Creates a new LevelModel
     * <br><br>
     * The level is empty and there is no active physics world.  You must read
     * the JSON file to initialize the level
     *
     * @param world Box2D world containing all game simulations
     * @param scale Drawing scale
     * @param numLives Number of lives
     */
    public Level(World world, Vector2 scale, int numLives) {
        this.world  = world;
        this.bounds = new Rectangle();
        this.scale = scale;
        this.numLives = numLives;
        maxLives = numLives;
        complete = false;
        failed = false;
        died = false;

        activators = new Array<>();
        activatables = new Array<>();
        deadBodyArray = new Array<>();
        lasers = new Array<>();
        mobArray = new Array<>();
        spiritRegionArray = new Array<>();
        activationRelations = new HashMap<>();
        spiritMode = false;
        spiritLine = new SpiritLine(Color.WHITE, Color.CYAN, scale);
    }

    /**
     * Sets whether the level is completed.
     * <br><br>
     * If true, the level will advance after a countdown
     *
     * @param value whether the level is completed.
     */
    public void setComplete(boolean value) {
        complete = value;
    }

    /**
     * Updates active checkpoints and cat respawning position
     *
     * @param c The most recent checkpoint the cat has come in contact with
     */
    public void updateCheckpoints(Checkpoint c){
        if(currCheckpoint != null){
            currCheckpoint.setCurrent(false);
        }
        boolean shouldSave = c != currCheckpoint;
        currCheckpoint = c;
        currCheckpoint.setCurrent(true);
        respawnPos = currCheckpoint.getPosition();
        if (shouldSave) saveState();
    }

    public void resetCheckpoints(){
        if(currCheckpoint != null){
            currCheckpoint.setCurrent(false);
        }
        currCheckpoint = null;
        respawnPos = startRespawnPos;
    }

    private void loadExitPositions(JsonValue data, int tileSize, int levelHeight){
        goalY = 0;
        returnY = 0;
        JsonValue objects = data.get("objects");
        for (JsonValue objJV : objects) {
            readProperties(objJV, tileSize, levelHeight);
            if (propertiesMap.get("type", "goal").equals("goal")){
                goalY = (float)propertiesMap.get("y") - (float)propertiesMap.get("height") - bounds.y;
            } else {
                returnY = (float)propertiesMap.get("y") - (float)propertiesMap.get("height") - bounds.y;
            }
        }
    }

    /**
     * Populates this level from data from a Tiled file.
     *
     * @param tiledMap Tiled Json
     */
     public void populateTiled(JsonValue tiledMap){
        populateTiled(tiledMap, 0, 0, returnY, null);
    }

    /**
     * Populates this level from data from a Tiled file. Places the level into the world such that the exits between this
     * level and an adjacent level are aligned.
     *
     * @param tiledMap    Tiled JSON
     * @param xOffset     The x offset in Box2D coordinates to place this world at
     * @param yOffset     The y offset in Box2D coordinates to place this world at
     * @param prevExitY   The y position of the bottom left edge of the adjacent level's exit
     * @param next        True if we are progressing from the previous level (i.e to the right),
     *                    null if we should ignore offsets
     */
    public void populateTiled(JsonValue tiledMap, float xOffset, float yOffset, float prevExitY, Boolean next){
        world.setGravity( new Vector2(0,tiledMap.getFloat("gravity",-14.7f)) );
        activationRelations = new HashMap<>();
        levelStates = new Array<>();

        JsonValue layers = tiledMap.get("layers");
        JsonValue tileData = layers.get(0);

        tileSize = tiledMap.getInt("tilewidth");
        int levelWidth = tiledMap.getInt("width");
        int levelHeight = tiledMap.getInt("height");
        bounds.set(xOffset, yOffset, levelWidth, levelHeight);

        Array<JsonValue> obstacleData = new Array<>();
        for (JsonValue layer : layers) {
            if (!layer.getString("type").equals("tilelayer")) {
                obstacleData.add(layer);
                if (layer.getString("name").equals("exits")){
                    loadExitPositions(layer, tileSize, levelHeight);
                }
            }
        }
        if (next != null) {
            if (next) {
                bounds.y += prevExitY - returnY;
            } else {
                bounds.y += prevExitY - goalY;
                bounds.x -= bounds.width;
            }
        }

        populateObstacles(obstacleData, tileSize, levelHeight, next == null);
        String biome = tiledMap.get("properties").get(0).getString("value");

        TextureRegion tileset = new TextureRegion();

        int fID = 1;
        if (biome.equals("metal")) {
            tileset = textureRegionAssetMap.get("metal_tileset");
            for (JsonValue tilesetData : tiledMap.get("tilesets")){
                if (tilesetData.getString("source").equals("metal-walls.tsx")){
                    fID = tilesetData.getInt("firstgid");
                }
            }
        }
        else if (biome.equals("forest")) {
            // TODO: change this in future
            tileset = textureRegionAssetMap.get("metal_tileset");
            for (JsonValue tilesetData : tiledMap.get("tilesets")){
                if (tilesetData.getString("source").equals("metal-walls.tsx")){
                    fID = tilesetData.getInt("firstgid");
                }
            }
        }
        tiles = new Tiles(tileData, 1024, levelWidth, levelHeight, tileset, bounds, fID, new Vector2(1/32f, 1/32f));
        saveState();
    }

    /**
     * Populates this level with all the obstacles defined in a Tiled JSON.
     *
     * @param data           Array of Tiled JSON layers
     * @param levelHeight    Height of the level (in grid cell units)
     * @param tileSize       Size of each tile in the Tiled JSON
     * @param populateCat    True if we want to populate the cat
     */
    public void populateObstacles(Array<JsonValue> data, int tileSize, int levelHeight, boolean populateCat) {
        for (JsonValue obstacleData : data) {
            String name = obstacleData.getString("name");
            if (name.equals("wallsPoly")) {
                populateWalls(obstacleData, tileSize, levelHeight);
            } else if (name.equals("platforms")) {
                populatePlatforms(obstacleData, tileSize, levelHeight);
            } else if (name.equals("checkpoints")) {
                populateCheckpoints(obstacleData, tileSize, levelHeight);
            } else if (name.equals("activators")) {
                populateActivators(obstacleData, tileSize, levelHeight);
            } else if (name.equals("lasers")) {
                populateLasers(obstacleData, tileSize, levelHeight);
            } else if (name.equals("spikes")) {
                populateSpikes(obstacleData, tileSize, levelHeight);
            } else if (name.equals("flamethrowers")){
                populateFlamethrowers(obstacleData, tileSize, levelHeight);
            }  else if (name.equals("doors")) {
                populateDoors(obstacleData, tileSize, levelHeight);
            }  else if (name.equals("spiritRegions")) {
                populateSpiritRegions(obstacleData, tileSize, levelHeight);
            } else if (name.equals("mobs")) {
                populateMobs(obstacleData, tileSize, levelHeight);
            } else if (name.equals("boxes")) {
                populateBoxes(obstacleData, tileSize, levelHeight);
            } else if (name.equals("mirrors")) {
                populateMirrors(obstacleData, tileSize, levelHeight);
            } else if (name.equals("cat")) {
                populateCat(obstacleData, tileSize, levelHeight, populateCat);
            } else if (name.equals("exits")) {
                populateExits(obstacleData, tileSize, levelHeight);
            }
        }
    }

    /**
     * Populates the walls for this level.
     *
     * @param data          Tiled JSON data for all walls
     * @param tileSize      Tile size in the Tiled JSON
     * @param levelHeight   Level height in Box2D units
     */
    private void populateWalls(JsonValue data, int tileSize, int levelHeight) {
        JsonValue objects = data.get("objects");
        for (JsonValue objJV : objects) {
            readProperties(objJV, tileSize, levelHeight);
            Wall wall = new Wall(propertiesMap, scale);
            addObject(wall);
        }
    }

    /**
     * Populates the platforms for this level.
     *
     * @param data          Tiled JSON data for all platforms
     * @param tileSize      Tile size in the Tiled JSON
     * @param levelHeight   Level height in Box2D units
     */
    private void populatePlatforms(JsonValue data, int tileSize, int levelHeight){
        JsonValue objects = data.get("objects");
        textureScaleCache.set(1, 1);
        for (JsonValue objJV : objects) {
            readProperties(objJV, tileSize, levelHeight);
            Platform platform = new Platform(propertiesMap, textureRegionAssetMap, scale, textureScaleCache);
            loadTiledActivatable(platform);
        }
    }

    /**
     * Populates the checkpoints for this level.
     *
     * @param data          Tiled JSON data for all checkpoints
     * @param tileSize      Tile size in the Tiled JSON
     * @param levelHeight   Level height in Box2D units
     */
    private void populateCheckpoints(JsonValue data, int tileSize, int levelHeight) {
        JsonValue objects = data.get("objects");
        textureScaleCache.set(1/32f, 1/32f);
        for (JsonValue objJV : objects) {
            readProperties(objJV, tileSize, levelHeight);
            Checkpoint checkpoint = new Checkpoint(propertiesMap, textureRegionAssetMap, scale, textureScaleCache);
            addObject(checkpoint);
        }
    }

    /**
     * Populates the activators for this level.
     *
     * @param data          Tiled JSON data for all activators
     * @param tileSize      Tile size in the Tiled JSON
     * @param levelHeight   Level height in Box2D units
     */
    private void populateActivators(JsonValue data, int tileSize, int levelHeight) {
        JsonValue objects = data.get("objects");
        textureScaleCache.set(1, 1);
        for (JsonValue objJV : objects) {
            readProperties(objJV, tileSize, levelHeight);
            Activator activator;
            //TODO: developers should be able to specify in json if they want first pan or not
            switch ((String) propertiesMap.get("type", "button")){
                case "button":
                    activator = new Button(propertiesMap, textureRegionAssetMap, scale, textureScaleCache);
                    break;
                case "switch":
                    activator = new Switch(propertiesMap, textureRegionAssetMap, scale, textureScaleCache);
                    break;
                case "timed":
                    activator = new TimedButton(propertiesMap, textureRegionAssetMap, scale, textureScaleCache);
                    break;
                default:
                    throw new RuntimeException("unrecognised activator type");
            }
            activators.add(activator);
            addObject(activator);
        }
    }

    /**
     * Populates the spikes for this level.
     *
     * @param data          Tiled JSON data for all spikes
     * @param tileSize      Tile size in the Tiled JSON
     * @param levelHeight   Level height in Box2D units
     */
    private void populateSpikes(JsonValue data, int tileSize, int levelHeight) {
        JsonValue objects = data.get("objects");
        textureScaleCache.set(1/64f, 1/64f);
        for (JsonValue objJV : objects) {
            readProperties(objJV, tileSize, levelHeight);
            Spikes spikes = new Spikes(propertiesMap, textureRegionAssetMap, scale, textureScaleCache);
            loadTiledActivatable(spikes);
        }
    }

    /**
     * Populates the flamethrowers for this level.
     *
     * @param data          Tiled JSON data for all flamethrowers
     * @param tileSize      Tile size in the Tiled JSON
     * @param levelHeight   Level height in Box2D units
     */
    private void populateFlamethrowers(JsonValue data, int tileSize, int levelHeight) {
        JsonValue objects = data.get("objects");
        textureScaleCache.set(1/64f, 1/64f);
        for (JsonValue objJV : objects) {
            readProperties(objJV, tileSize, levelHeight);
            Flamethrower flamethrower = new Flamethrower(propertiesMap, textureRegionAssetMap, scale, textureScaleCache);
            loadTiledActivatable(flamethrower);
        }
    }

    /**
     * Populates the lasers for this level.
     *
     * @param data          Tiled JSON data for all lasers
     * @param tileSize      Tile size in the Tiled JSON
     * @param levelHeight   Level height in Box2D units
     */
    private void populateLasers(JsonValue data, int tileSize, int levelHeight) {
        JsonValue objects = data.get("objects");
        textureScaleCache.set(1, 1);
        for (JsonValue objJV : objects) {
            readProperties(objJV, tileSize, levelHeight);
            Laser laser = new Laser(propertiesMap, textureRegionAssetMap, scale, textureScaleCache);
            loadTiledActivatable(laser);
            lasers.add(laser);
        }
    }

    /**
     * Populates the doors for this level.
     *
     * @param data          Tiled JSON data for all doors
     * @param tileSize      Tile size in the Tiled JSON
     * @param levelHeight   Level height in Box2D units
     */
    private void populateDoors(JsonValue data, int tileSize, int levelHeight) {
        JsonValue objects = data.get("objects");
        textureScaleCache.set(1, 1);
        for (JsonValue objJV : objects) {
            readProperties(objJV, tileSize, levelHeight);
            Door door = new Door(propertiesMap, textureRegionAssetMap, scale, textureScaleCache);
            loadTiledActivatable(door);
        }
    }

    /**
     * Populates the spirit regions for this level.
     *
     * @param data          Tiled JSON data for all spirit regions
     * @param tileSize      Tile size in the Tiled JSON
     * @param levelHeight   Level height in Box2D units
     */
    private void populateSpiritRegions(JsonValue data, int tileSize, int levelHeight) {
        JsonValue objects = data.get("objects");
        textureScaleCache.set(1, 1);
        for (JsonValue objJV : objects) {
            readProperties(objJV, tileSize, levelHeight);
            SpiritRegion spiritRegion = new SpiritRegion(propertiesMap, textureRegionAssetMap, scale, textureScaleCache);
            spiritRegionArray.add(spiritRegion);
            addObject(spiritRegion);
        }
    }

    /**
     * Populates the mobs for this level.
     *
     * @param data          Tiled JSON data for all mobs
     * @param tileSize      Tile size in the Tiled JSON
     * @param levelHeight   Level height in Box2D units
     */
    private void populateMobs(JsonValue data, int tileSize, int levelHeight) {
        JsonValue objects = data.get("objects");
        textureScaleCache.set(1/32f, 1/32f);
        for (JsonValue objJV : objects) {
            readProperties(objJV, tileSize, levelHeight);
            Mob mob = new Mob(propertiesMap, textureRegionAssetMap, scale, textureScaleCache);
            mobArray.add(mob);
            addObject(mob);
        }
    }

    /**
     * Populates the boxes for this level.
     *
     * @param data          Tiled JSON data for all boxes
     * @param tileSize      Tile size in the Tiled JSON
     * @param levelHeight   Level height in Box2D units
     */
    private void populateBoxes(JsonValue data, int tileSize, int levelHeight) {
        JsonValue objects = data.get("objects");
        textureScaleCache.set(1, 1);
        for (JsonValue objJV : objects) {
            readProperties(objJV, tileSize, levelHeight);
            PushableBox box = new PushableBox(propertiesMap, textureRegionAssetMap, scale, textureScaleCache);
            addObject(box);
        }
    }

    /**
     * Populates the mirrors for this level.
     *
     * @param data          Tiled JSON data for all mirrors
     * @param tileSize      Tile size in the Tiled JSON
     * @param levelHeight   Level height in Box2D units
     */
    private void populateMirrors(JsonValue data, int tileSize, int levelHeight) {
        JsonValue objects = data.get("objects");
        textureScaleCache.set(1, 1);
        for (JsonValue objJV : objects) {
            readProperties(objJV, tileSize, levelHeight);
            Mirror mirror = new Mirror(propertiesMap, textureRegionAssetMap, scale, textureScaleCache);
            addObject(mirror);
        }
    }

    /**
     * Populates the exits for this level.
     *
     * @param data          Tiled JSON data for all exits
     * @param tileSize      Tile size in the Tiled JSON
     * @param levelHeight   Level height in Box2D units
     */
    private void populateExits(JsonValue data, int tileSize, int levelHeight) {
        JsonValue objects = data.get("objects");
        for (JsonValue objJV : objects) {
            readProperties(objJV, tileSize, levelHeight);
            Exit exit = new Exit(propertiesMap, scale);
            addObject(exit);
            if (exit.exitType() == Exit.ExitType.GOAL) goalExit = exit;
            if (exit.exitType() == Exit.ExitType.RETURN) returnExit = exit;
        }
    }

    /**
     * Populates the cat for this level.
     *
     * @param data          Tiled JSON data for the cat
     * @param tileSize      Tile size in the Tiled JSON
     * @param levelHeight   Level height in Box2D units
     */
        private void populateCat(JsonValue data, int tileSize, int levelHeight, boolean shouldPopulate){
        JsonValue objects = data.get("objects");
        JsonValue catJV = objects.get(0);
        readProperties(catJV, tileSize, levelHeight);
        cat = new Cat(propertiesMap, textureRegionAssetMap, scale);
        respawnPos = cat.getPosition();
        startRespawnPos = respawnPos;
        if (shouldPopulate) {
            addObject(cat);
        } else {
            cat = null;
        }
    }

    /**
     * Reads the properties array of an object in a Tiled JSON, and puts it into <code>propertiesMap</code>.
     *
     * @param objectJV   JSON of the object
     * @param tileSize   Size of the tiles in the JSON
     */
    private void readProperties(JsonValue objectJV, int tileSize, int levelHeight){
        propertiesMap.clear();

        propertiesMap.put("width", objectJV.getFloat("width")/tileSize);
        propertiesMap.put("height", objectJV.getFloat("height")/tileSize);
        float angle = (360 - objectJV.getFloat("rotation")) % 360;
        propertiesMap.put("rotation", angle);

        //this is because tiled rotates about the top left corner
        float x, y;
        switch ((int) angle) {
            default:
            case 0:
                x = objectJV.getFloat("x");
                y = objectJV.getFloat("y");
                break;
            case 90:
                x = objectJV.getFloat("x");
                y = objectJV.getFloat("y") - tileSize;
                break;
            case 180:
                x = objectJV.getFloat("x") - tileSize;
                y = objectJV.getFloat("y") - tileSize;
                break;
            case 270:
                x = objectJV.getFloat("x") - tileSize;
                y = objectJV.getFloat("y");
                break;
        }
        x = x/tileSize + bounds.x;
        y = levelHeight - y/tileSize + bounds.y;
        propertiesMap.put("x", x);
        propertiesMap.put("y", y);

        //read polygon if there is one
        JsonValue poly = objectJV.get("polygon");
        if (poly != null) {
            float[] shape = new float[poly.size * 2];
            int i = 0;
            for (JsonValue point : poly) {
                shape[i] = x + point.getFloat("x") / tileSize;
                shape[i + 1] = y - (point.getFloat("y")) / tileSize;
                i += 2;
            }
            propertiesMap.put("polygon", shape);
        }

        //object specific properties if there are any
        JsonValue properties = objectJV.get("properties");
        if (properties == null) { return; }
        for (JsonValue property : properties){
            String name = property.getString("name");
            switch (property.getString("type")){
                case "string":
                    propertiesMap.put(name, property.getString("value"));
                    break;
                case "int":
                    propertiesMap.put(name, property.getInt("value"));
                    break;
                case "bool":
                    propertiesMap.put(name, property.getBoolean("value"));
                    break;
                case "float":
                    propertiesMap.put(name, property.getFloat("value"));
                    break;
                case "color":
                    propertiesMap.put(name, Color.valueOf(property.getString("value")));
                    break;
                case "class":
                    switch (property.getString("propertytype")){
                        //currently only one class defined in our level editor, but this allows us to be flexible to add more
                        case "Vector2":
                            Vector2 v = new Vector2(property.get("value").getFloat("x", 0), property.get("value").getFloat("y", 0));
                            propertiesMap.put(name, v);
                            break;
                        default:
                            throw new IllegalArgumentException("unexpected class: " + property.getString("type"));
                    }
                    break;
                default:
                    throw new IllegalArgumentException("unexpected property type: " + property.getString("type"));
            }
        }
    }

    /**
     * Resets the status of the level.
     * <br><br>
     * This method clears objects, disposes world,
     * and sets the level to not completed and not failed.
     */
    public void dispose() {
        for(Obstacle obj : objects) {
            obj.deactivatePhysics(world);
        }
        cat = null;
        addQueue.clear();
        objects.clear();
        activators.clear();
        lasers.clear();
        deadBodyArray.clear();
        activatables.clear();
        mobArray.clear();
        spiritRegionArray.clear();
        numLives = maxLives;
        currCheckpoint = null;
        setComplete(false);
        setFailure(false);
    }

    /**
     * Pauses all objects in the level: stores state locally and sets velocity to 0.
     */
    public void pause(){
        for (Obstacle obj : objects) {
            obj.pause();
        }
    }

    /**
     * Unpauses objects in the level: loads locally stored state if there is one.
     */
    public void unpause(){
        for (Obstacle obj : objects) {
            obj.unpause();
        }
    }

    /**
     * Immediately adds the object to the physics world
     *
     * @param obj The object to add
     */
    protected void addObject(Obstacle obj) {
        assert inBounds(obj) : "Object is not in bounds";
        objects.add(obj);
        obj.activatePhysics(world);
    }

    /**
     * Adds a physics object in to the insertion queue.
     * <br><br>
     * Objects on the queue are added just before collision processing.  We do this to
     * control object creation.
     *
     * @param obj The object to add
     */
    public void queueObject(Obstacle obj) {
        assert inBounds(obj) : "Object is not in bounds";
        addQueue.add(obj);
    }

    /**
     * Adds a jointDef to the joint queue.
     * <br><br>
     * Joints on the queue are added just before collision processing.  We do this to
     * control joint creation.
     *
     * @param j The jointDef to add
     */
    public void queueJoint(JointDef j) { jointQueue.add(j); }

    /**
     * Returns true if the object is in bounds.
     * <br><br>
     * This assertion is useful for debugging the physics.
     *
     * @param obj The object to check.
     *
     * @return true if the object is in bounds.
     */
    private boolean inBounds(Obstacle obj) {
        boolean horiz = (bounds.x <= obj.getX() && obj.getX() <= bounds.x+bounds.width);
        boolean vert  = (bounds.y <= obj.getY() && obj.getY() <= bounds.y+bounds.height);
        return horiz && vert;
    }

    /**
     * Adds the queued objects to the list of objects
     */
    public void addQueuedObjects() {
        while (!addQueue.isEmpty()) {
            addObject(addQueue.poll());
        }
    }

    /**
     * Adds the queued joints to the list of joints
     */
    public void addQueuedJoints(){
        while (!jointQueue.isEmpty()) {
            JointDef jdef = jointQueue.poll();
            Joint joint = world.createJoint(jdef);

            if (jdef.bodyA.getUserData() instanceof Spikes){
                ((Spikes) jdef.bodyA.getUserData()).addJoint(joint);
            } else if (jdef.bodyB.getUserData() instanceof Spikes) {
                ((Spikes) jdef.bodyB.getUserData()).addJoint(joint);
            }

            if (jdef.bodyA.getUserData() instanceof DeadBody){
                ((DeadBody) jdef.bodyA.getUserData()).addJoint(joint);
            } else if (jdef.bodyB.getUserData() instanceof DeadBody) {
                ((DeadBody) jdef.bodyB.getUserData()).addJoint(joint);
            }
        }
    }

    /**
     * Loads an activatable into this world. Assumes that the properties for the object is currently stored in
     * <code>propertiesMap</code> (see {@link Level#readProperties(JsonValue, int, int)}).
     *
     * @param object  Activatable to load.
     */
    private void loadTiledActivatable(Activatable object){

        addObject((Obstacle) object);

        String activatorID = (String) propertiesMap.get("activatorID", "");
        if (!activatorID.equals("")) {
            if (activationRelations.containsKey(activatorID)) {
                activationRelations.get(activatorID).add(object);
            } else {
                activationRelations.put(activatorID, new Array<>(new Activatable[]{object}));
            }
        }

        activatables.add(object);
    }

    /**
     * Draws the level to the given game canvas. Assumes <code>canvas.begin()</code> has already been called.
     *
     * @param canvas	the drawing context
     */
    public void draw(GameCanvas canvas) {
//        if (background != null) {
//            //scales background with level size
//            float scaleX = bounds.width/background.getWidth() * scale.x;
//            float scaleY = bounds.height/background.getHeight() * scale.y;
//            canvas.draw(background, Color.WHITE, bounds.x * scale.x, bounds.y * scale.y, background.getWidth()*Float.max(scaleX,scaleY), background.getHeight()*Float.max(scaleX,scaleY));
////            canvas.draw(background, 0, 0);
//        }

        for (Laser l : lasers){
            l.drawLaser(canvas);
        }

        //draw everything except cat, dead bodies and spirit region
        for(Obstacle obj : objects) {
            if (obj != cat && !(obj instanceof DeadBody) && !(obj instanceof SpiritRegion)
                    && !(obj instanceof Wall && !(obj instanceof Platform)) && !(obj instanceof Activator) ){
                obj.draw(canvas);
            }
        }

        if (tiles != null) tiles.draw(canvas);

        for (Activator a : activators) {
            a.draw(canvas);
        }

        spiritLine.draw(canvas);

        for (DeadBody db : deadBodyArray) {
            db.draw(canvas);
        }
        if (cat != null) cat.draw(canvas);

        for (SpiritRegion s : spiritRegionArray) {
            s.draw(canvas);
        }

        if (currCheckpoint != null) {
            currCheckpoint.drawBase(canvas);
        }
    }

    /**
     * Draws the wireframe debug of the level to the given game canvas. Assumes <code>canvas.beginDebug()</code> has already been called.
     *
     * @param canvas	the drawing context
     */
    public void drawDebug(GameCanvas canvas){
        //draw grid
        Color lineColor = new Color(0.8f, 0.8f, 0.8f, 1);
        float xTranslate = (canvas.getCamera().getX()-canvas.getWidth()/2)/scale.x;
        float yTranslate = (canvas.getCamera().getY()-canvas.getHeight()/2)/scale.y;
        for (int x = 0; x < bounds.width; x++) {
            Vector2 p1 = new Vector2(x-xTranslate, 0-yTranslate);
            Vector2 p2 = new Vector2(x-xTranslate, bounds.height-yTranslate);
            canvas.drawLineDebug(p1, p2, lineColor, scale.x, scale.y);
        }
        for (int y = 0; y < bounds.height; y++) {
            Vector2 p1 = new Vector2(0-xTranslate, y-yTranslate);
            Vector2 p2 = new Vector2(bounds.width-xTranslate, y-yTranslate);
            canvas.drawLineDebug(p1, p2, lineColor, scale.x, scale.y);
        }
        for (Obstacle obj : objects) {
            obj.drawDebug(canvas);
        }
    }



    /**
     * Spawns a dead body at the location of the cat
     * */
    public void spawnDeadBody(){
        DeadBody deadBody = new DeadBody(textureRegionAssetMap.get("deadCat"),textureRegionAssetMap.get("burnCat"), scale, cat.getPosition());
        deadBody.setLinearVelocity(cat.getLinearVelocity());
        deadBody.setFacingRight(cat.isFacingRight());
        queueObject(deadBody);
        deadBodyArray.add(deadBody);
    }

    /**
     * Loads a dead body into this level from a saved state.
     * @param state Map of arguments for the dead body, called from storeState() in {@link DeadBody}.
     */
    public DeadBody loadDeadBodyState(ObjectMap<String, Object> state){
        DeadBody deadBody = new DeadBody(textureRegionAssetMap.get("deadCat"), textureRegionAssetMap.get("burnCat"),scale, Vector2.Zero);
        deadBody.loadState(state);
        addObject(deadBody);
        deadBodyArray.add(deadBody);
        return deadBody;
    }

    /**
     * Removes a DeadBody from the dead body array
     * */
    public void removeDeadBody(DeadBody db){
        db.markRemoved(true);
        deadBodyArray.removeValue(db, true);
    }

    /**
     * Gets the next dead body to switch into. Currently selects the closest valid body.
     *
     * @return Dead body to switch into, null if there are none.
     */
    public DeadBody getNextBody(){
        float minDist = Float.MAX_VALUE;
        DeadBody nextdb = null;
        for (DeadBody db : deadBodyArray){
            if (sharesElement(db.getSpiritRegions(), cat.getSpiritRegions())){
                float dist = cat.getPosition().dst(db.getPosition());
                if (dist < minDist){
                    minDist = dist;
                    nextdb = db;
                }
            }
        }
        return nextdb;
    }

    /**
     * Checks if two sets share an element.
     *
     * @param s1  Set 1
     * @param s2  Set 2
     * @return    True if set 1 and set 2 share any element, or if both are empty.
     * @param <T> The type of elements in s1 and s2
     */
    private <T> boolean sharesElement(ObjectSet<T> s1, ObjectSet<T> s2){
        if (s1.isEmpty() && s2.isEmpty()) return true;
        for (T r : s1){
            if (s2.contains(r)) return true;
        }
        return false;
    }

    /**
     * @return The spirit line instance of this level.
     */
    public SpiritLine getSpiritLine(){ return spiritLine; }

    /**
     * @return If the level is in spirit mode.
     */
    public boolean isSpiritMode(){ return spiritMode; }

    /**
     * Sets the spirit mode of this level.
     * @param spiritMode   Next spirit mode state
     */
    public void setSpiritMode(boolean spiritMode){ this.spiritMode = spiritMode; }

    /**
     * Stores a snapshot of the current level state into the level states array.
     */
    public void saveState() { levelStates.add(new LevelState(this)); }

    /**
     * @return  <code>LevelStates</code> array
     */
    public Array<LevelState> levelStates() { return levelStates; }

    /**
     * Stores a snapshot of the state of a level. A new <code>LevelState</code> instance is created
     * at the beginning of the level, and everytime the player changes their checkpoint.
     */
    protected static class LevelState {
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

}
