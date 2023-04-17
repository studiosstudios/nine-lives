package edu.cornell.gdiac.game;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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
    /** The boundary of the world */
    protected Rectangle bounds;
    /** The world scale */
    protected Vector2 scale;

    // Physics objects for the game
    /** Reference to the character cat */
    private Cat cat;
    /** Reference to the goalDoor (for collision detection) */
    private BoxObstacle goalDoor;
    /**Reference to the returnDoor (for collision detection) */
    private BoxObstacle retDoor;

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
    /** The background texture */
    private Texture background;

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
     * @param bounds World boundary
     * @param scale Drawing scale
     * @param numLives Number of lives
     */
    public Level(World world, Rectangle bounds, Vector2 scale, int numLives) {
        this.world  = world;
        this.bounds = bounds;
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
        currCheckpoint = c;
        currCheckpoint.setCurrent(true);
        respawnPos = currCheckpoint.getPosition();
    }

    public void resetCheckpoints(){
        if(currCheckpoint != null){
            currCheckpoint.setCurrent(false);
        }
        currCheckpoint = null;
        respawnPos = startRespawnPos;
    }

    /**
     * Parses Tiled file
     *
     */
    public void populateTiled(JsonValue tiledMap){


        world.setGravity( new Vector2(0,tiledMap.getFloat("gravity",-14.7f)) );
        activationRelations = new HashMap<>();
        background = textureRegionAssetMap.get("background").getTexture();

        JsonValue layers = tiledMap.get("layers");
        JsonValue tileData = layers.get(0);

        tileSize = tiledMap.getInt("tilewidth");
        int levelWidth = tiledMap.getInt("width");
        int levelHeight = tiledMap.getInt("height");

        bounds.width = levelWidth*scale.x;
        bounds.height = levelHeight*scale.y;

        Array<JsonValue> obstacleData = new Array<>();

        for (JsonValue layer : layers) {
            if (layer.getInt("id") != 1) {
                obstacleData.add(layer);
            }
        }

        populateObstacles(obstacleData, tileSize, levelHeight);
        String biome = tiledMap.get("properties").get(0).getString("value");

        TextureRegion tileset = new TextureRegion();

        int fID = 1;
        if (biome.equals("metal")) {
            tileset = textureRegionAssetMap.get("metal_tileset");
            for (JsonValue tilesetData : tiledMap.get("tilesets")){
                if (tilesetData.getString("source").equals("lab-walls.tsx")){
                    fID = tilesetData.getInt("firstgid");
                }
            }
        }
        else if (biome.equals("forest")) {
            // TODO: change this in future
            tileset = textureRegionAssetMap.get("metal_tileset");
            for (JsonValue tilesetData : tiledMap.get("tilesets")){
                if (tilesetData.getString("source").equals("lab-walls.tsx")){
                    fID = tilesetData.getInt("firstgid");
                }
            }
        }



        tiles = new Tiles(tileData, 1024, levelWidth, levelHeight, tileset, fID, new Vector2(1/32f, 1/32f));

        spiritMode = false;
        spiritLine = new SpiritLine(Color.WHITE, Color.CYAN, scale);
    }

    /**
     * Populates this level with all the obstacles defined in a Tiled JSON.
     *
     * @param data           Array of Tiled JSON layers
     * @param levelHeight    Height of the level (in grid cell units)
     * @param tileSize       Size of each tile in the Tiled JSON
     */
    public void populateObstacles(Array<JsonValue> data, int tileSize, int levelHeight) {
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
                populateCat(obstacleData, tileSize, levelHeight);
            } else if (name.equals("exits")) {
                populateExits(obstacleData, tileSize, levelHeight);
            }
        }
    }

    private void populateWalls(JsonValue data, int tileSize, int levelHeight) {

        JsonValue objects = data.get("objects");

        for (JsonValue obj : objects) {
            JsonValue points = obj.get("polygon");
            float x = obj.getFloat("x");
            float y = obj.getFloat("y");
            float[] shape = new float[points.size*2];

            int i = 0;
            for (JsonValue point : points) {

                shape[i] = (x + point.getFloat("x"))/tileSize;

                shape[i+1] = levelHeight - (y + point.getFloat("y"))/tileSize;
                i+=2;
            }

            // check climbable
            boolean isClimbable = false;

            if (obj.get("properties") != null) {
                isClimbable = obj.get("properties").get(0).getBoolean("value");
            }
            Wall wall = new Wall(textureRegionAssetMap.get("steel"), scale, shape, isClimbable);
            addObject(wall);
        }
    }


    private void populatePlatforms(JsonValue data, int tileSize, int levelHeight){
        JsonValue objects = data.get("objects");
        textureScaleCache.set(1, 1);
        for (JsonValue objJV : objects) {
            readProperties(objJV, tileSize, levelHeight);
            Platform platform = new Platform(propertiesMap, textureRegionAssetMap, scale, textureScaleCache);
            loadTiledActivatable(platform);
        }
    }


    private void populateCheckpoints(JsonValue data, int tileSize, int levelHeight) {

        JsonValue objects = data.get("objects");

        for (JsonValue obj : objects) {
            float x = obj.getFloat("x");
            float y = obj.getFloat("y");

            Vector2 pos = new Vector2(x/tileSize, levelHeight - y/tileSize);
            float angle = (float) ((360-obj.getFloat("rotation")) * Math.PI/180);

            Checkpoint checkpoint = new Checkpoint(pos, angle, scale, textureRegionAssetMap.get("checkpoint_anim"),
                    textureRegionAssetMap.get("checkpoint_active_anim"), textureRegionAssetMap.get("checkpoint_base"),
                    textureRegionAssetMap.get("checkpoint_base_active"));
            addObject(checkpoint);
        }
    }

    private void populateActivators(JsonValue data, int tileSize, int levelHeight) {
        JsonValue objects = data.get("objects");
        textureScaleCache.set(1, 1);
        for (JsonValue objJV : objects) {
            readProperties(objJV, tileSize, levelHeight);
            Activator activator;
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

    private void populateSpikes(JsonValue data, int tileSize, int levelHeight) {
        JsonValue objects = data.get("objects");
        textureScaleCache.set(1/64f, 1/64f);
        for (JsonValue objJV : objects) {
            readProperties(objJV, tileSize, levelHeight);
            Spikes spikes = new Spikes(propertiesMap, textureRegionAssetMap, scale, textureScaleCache);
            loadTiledActivatable(spikes);
        }
    }

    private void populateFlamethrowers(JsonValue data, int tileSize, int levelHeight) {
        JsonValue objects = data.get("objects");
        textureScaleCache.set(1/64f, 1/64f);
        for (JsonValue objJV : objects) {
            readProperties(objJV, tileSize, levelHeight);
            Flamethrower flamethrower = new Flamethrower(propertiesMap, textureRegionAssetMap, scale, textureScaleCache);
            loadTiledActivatable(flamethrower);
        }
    }


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

    private void populateDoors(JsonValue data, int tileSize, int levelHeight) {
        JsonValue objects = data.get("objects");
        textureScaleCache.set(1, 1);
        for (JsonValue objJV : objects) {
            readProperties(objJV, tileSize, levelHeight);
            Door door = new Door(propertiesMap, textureRegionAssetMap, scale, textureScaleCache);
            loadTiledActivatable(door);
        }
    }

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

    private void populateBoxes(JsonValue data, int tileSize, int levelHeight) {
        JsonValue objects = data.get("objects");
        textureScaleCache.set(1, 1);
        for (JsonValue objJV : objects) {
            readProperties(objJV, tileSize, levelHeight);
            PushableBox box = new PushableBox(propertiesMap, textureRegionAssetMap, scale, textureScaleCache);
            addObject(box);
        }
    }

    private void populateMirrors(JsonValue data, int tileSize, int levelHeight) {
        JsonValue objects = data.get("objects");
        textureScaleCache.set(1, 1);
        for (JsonValue objJV : objects) {
            readProperties(objJV, tileSize, levelHeight);
            Mirror mirror = new Mirror(propertiesMap, textureRegionAssetMap, scale, textureScaleCache);
            addObject(mirror);
        }
    }

    private void populateExits(JsonValue data, int tileSize, int levelHeight) {
        JsonValue objects = data.get("objects");
        for (JsonValue objJV : objects) {
            readProperties(objJV, tileSize, levelHeight);
            Exit exit = new Exit(propertiesMap, scale);
            addObject(exit);
        }
    }

    private void populateCat(JsonValue data, int tileSize, int levelHeight){
        JsonValue objects = data.get("objects");
        JsonValue catJV = objects.get(0);
        readProperties(catJV, tileSize, levelHeight);
        cat = new Cat(propertiesMap, textureRegionAssetMap, scale);
        respawnPos = cat.getPosition();
        startRespawnPos = respawnPos;
        addObject(cat);
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
        propertiesMap.put("x", x/tileSize);
        propertiesMap.put("y", levelHeight - y/tileSize);


        //object specific properties (if there are any)
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
                            Vector2 v = new Vector2(property.get("value").getFloat("x"), property.get("value").getFloat("y"));
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
     * TODO: MOVE TO LEVELCONTROLLER
     * @param constants
     */
    public static void setConstants(JsonValue constants){
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
     * Resets the status of the level.
     * <br><br>
     * This method clears objects, disposes world,
     * and sets the level to not completed and not failed.
     */
    public void dispose() {
        for(Obstacle obj : objects) {
            obj.deactivatePhysics(world);
        }
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
        if (world != null) {
            world.dispose();
            world = null;
        }
        currCheckpoint = null;
        setComplete(false);
        setFailure(false);
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
        }
    }

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
        if (background != null) {
            //scales background with level size
            float scaleX = bounds.width/background.getWidth();
            float scaleY = bounds.height/background.getHeight();
            canvas.draw(background, Color.WHITE, 0, 0, background.getWidth()*Float.max(scaleX,scaleY), background.getHeight()*Float.max(scaleX,scaleY));
//            canvas.draw(background, 0, 0);
        }

        tiles.draw(canvas);

        //draw everything except cat, dead bodies and spirit region
        for(Obstacle obj : objects) {
            if (obj != cat && !(obj instanceof DeadBody) && !(obj instanceof SpiritRegion)
                    && !(obj instanceof Wall && !(obj instanceof Platform)) ){
                obj.draw(canvas);
            }
        }

        spiritLine.draw(canvas);

        for (DeadBody db : deadBodyArray) {
            db.draw(canvas);
        }
        cat.draw(canvas);

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
    public void loadDeadBodyState(ObjectMap<String, Object> state){
        DeadBody deadBody = new DeadBody(textureRegionAssetMap.get("deadCat"), textureRegionAssetMap.get("burnCat"),scale, Vector2.Zero);
        deadBody.loadState(state);
        queueObject(deadBody);
        deadBodyArray.add(deadBody);
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
            if (sharesSpriritRegion(db.getSpiritRegions(), cat.getSpiritRegions())){
                float dist = cat.getPosition().dst(db.getPosition());
                if (dist < minDist){
                    minDist = dist;
                    nextdb = db;
                }
            }
        }
        return nextdb;
    }

    private boolean sharesSpriritRegion(ObjectSet<SpiritRegion> s1, ObjectSet<SpiritRegion> s2){
        if (s1.isEmpty() && s2.isEmpty()) return true;
        for (SpiritRegion r : s1){
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

}
