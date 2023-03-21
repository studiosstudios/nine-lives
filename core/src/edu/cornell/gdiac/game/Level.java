/*
 * LevelMode.java
 *
 * This stores all of the information to define a level in our simple platform game.
 * We have an cat, some walls, some platforms, and an exit.  This is a refactoring
 * of WorldController in Lab 4 that separates the level data from the level control.
 *
 * Note that most of the methods are getters and setters, as is common with models.
 * The gameplay behavior is defined by GameController.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * JSON version, 3/2/2016
 */
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
 *
 * Note that the constructor does very little.  The true initialization happens
 * by reading the JSON value.  To reset a level, dispose it and reread the JSON.
 *
 * The level contains its own Box2d World, as the World settings are defined by the
 * JSON file.  However, there is absolutely no controller code in this class, as
 * the majority of the methods are getters and setters.  The getters allow the
 * GameController class to modify the level elements.
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

    /** All the objects in the world. */
    protected PooledList<Obstacle> objects  = new PooledList<Obstacle>();
    /** Queue for adding objects */
    protected PooledList<Obstacle> addQueue = new PooledList<Obstacle>();
    /** queue to add joints to the world created in beginContact() */
    protected PooledList<JointDef> jointQueue = new PooledList<JointDef>();

    /** Whether we have completed this level */
    private boolean complete;
    /** Whether we have failed at this world (and need a reset) */
    private boolean failed;
    /** Whether we have died */
    private boolean died;
    /** The number of lives remaining */
    private int numLives;
    /** The max lives allowed */
    private int maxLives;

    /** hashmap to represent activator-spike relationships:
     *   keys are activator ids specified in JSON*/
    private HashMap<String, Array<Activatable>> activationRelations;

    /** object lists - in the future this will be one list maybe */
    private Array<Activator> activators;
    private Array<Activatable> activatables;
    private Array<DeadBody> deadBodyArray;
    private Array<Mob> mobArray;
    private Checkpoint currCheckpoint;
    private Array<Laser> lasers;
    /** The respawn position of the player */
    private Vector2 respawnPos;
    /** Float value to scale width */
    private float dwidth;
    /** Float value to scale height */
    private float dheight;
    /** The background texture */
    private Texture background;
    /** JSON of the level */
    private JsonValue levelJV;

    /** texture assets */
    private HashMap<String, TextureRegion> textureRegionAssetMap;

    //region Spirit mode stuff
    /** if the player is in spirit mode */
    private boolean spiritMode;
    /** cache for efficiency */
    private Color spiritLineColor = new Color();
    /** counter of total ticks in spirit mode */
    private int spiritModeTicks;
    /** next dead body to switch into */
    private DeadBody nextDeadBody;
    private Vector2 spiritEndPos = new Vector2();
    private Vector2 spiritStartPos = new Vector2();
    private boolean bodySwitched;
    //endregion

    /**
     * Returns the bounding rectangle for the physics world
     *
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
     * Returns a reference to the exit door
     *
     * @return a reference to the exit door
     */
    public BoxObstacle getExit() {
        return goalDoor;
    }

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
     * Returns a reference to the dwidth
     *
     * @return a reference to the dwidth
     */
    public float getDwidth() { return dwidth; }

    /**
     * Returns a reference to the dheight
     *
     * @return a reference to the dheight
     */
    public float getDheight() { return dheight; }

    /**
     * Returns true if the level is completed.
     *
     * If true, the level will advance after a countdown
     *
     * @return true if the level is completed.
     */
    public boolean isComplete() {
        return complete;
    }

    /**
     * Returns true if the level is failed.
     *
     * If true, the level will reset after a countdown
     *
     * @return true if the level is failed.
     */
    public boolean isFailure() {
        return failed;
    }

    /**
     * Sets whether the level is failed.
     *
     * If true, the level will reset after a countdown
     *
     * @param value whether the level is failed.
     */
    public void setFailure(boolean value) {
        failed = value;
    }

    /**
     * Returns a reference to the goal door
     *
     * @return a reference to the goal door
     */
    public Obstacle getGoalDoor() {  return goalDoor; }

    /**
     * Returns a reference to the return door
     *
     * @return a reference to the return door
     */
    public Obstacle getRetDoor() {  return retDoor; }

    /**
     * Sets the game world
     *
     * @param world the world to set to
     */
    public void setWorld(World world) { this.world = world; }

    /**
     * Sets whether the player died in the level
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
     * Allows level to have access to textures for creating new objects
     */
    public void setAssets(HashMap<String, TextureRegion> tMap){ textureRegionAssetMap = tMap; }

    /**
     * Creates a new LevelModel
     *
     * The level is empty and there is no active physics world.  You must read
     * the JSON file to initialize the level
     */
    public Level(World world, Rectangle bounds, Vector2 scale, int numLives) {
        this.world  = world;
        this.bounds = bounds;
        this.scale = scale;
        complete = false;
        failed = false;
        died = false;
        this.numLives = numLives;
        maxLives = numLives;

        activators = new Array<>();
        activatables = new Array<>();
        deadBodyArray = new Array<>();
        lasers = new Array<>();
        mobArray = new Array<>();
        activationRelations = new HashMap<>();
    }

    /**
     * Sets whether the level is completed.
     *
     * If true, the level will advance after a countdown
     *
     * @param value whether the level is completed.
     */
    public void setComplete(boolean value) {
        complete = value;
    }

    /**
     * Updates active checkpoints and cat respawning position
     * @param c The most recent checkpoint the cat has come in contact with
     */
    public void updateCheckpoints(Checkpoint c){
        if(currCheckpoint != null){
            currCheckpoint.setActive(false);
        }
        currCheckpoint = c;
        currCheckpoint.setActive(true);
        respawnPos = currCheckpoint.getPosition();
    }

    /**
     * Lays out the game geography from the given JSON file
     *
     * @param directory 	the asset manager
     * @param levelFormat	the JSON file defining the level
     */
    /**
     * Lays out the game geography.
     */
    public void populateLevel(HashMap<String, TextureRegion> tMap, HashMap<String, BitmapFont> fMap,
                               HashMap<String, Sound> sMap, JsonValue constants, JsonValue levelJV, boolean ret, Cat prevCat) {
        this.levelJV = levelJV;
        // Add level goal
        dwidth  = tMap.get("goal").getRegionWidth()/scale.x;
        dheight = tMap.get("goal").getRegionHeight()*5/scale.y; //TEMP FIX TO CAT GETTING LOST UPON JUMPING INTO NEXT LEVEL
        System.out.println(dwidth);
        System.out.println(dheight);

        activationRelations = new HashMap<>();
        background = tMap.get("background").getTexture();

        try {
            for (JsonValue exitJV : levelJV.get("exits")){
                Exit exit = new Exit(scale, exitJV);
                addObject(exit);
            }
        } catch (NullPointerException e) {}

        JsonValue defaults = constants.get("defaults");
        // This world is heavier
        world.setGravity( new Vector2(0,defaults.getFloat("gravity",0)) );

        try {
            for (JsonValue wallJV : levelJV.get("walls")){
                Wall wall = new Wall(tMap.get("steel"), scale, wallJV);
                addObject(wall);
            }
        } catch (NullPointerException e) {}

        try {
            for (JsonValue wallJV : levelJV.get("platforms")){
                Wall wall = new Wall(tMap.get("steel"), scale, wallJV);
                addObject(wall);
            }
        } catch (NullPointerException e) {}

        try {
            for (JsonValue activatorJV : levelJV.get("activators")){
                Activator activator;
                switch (activatorJV.getString("type")){
                    case "button":
                        activator = new Button(tMap.get("button_anim"), tMap.get("button"), scale, activatorJV);
                        break;
                    case "switch":
                        activator = new Switch(tMap.get("button_anim"), tMap.get("button"),scale, activatorJV);
                        break;
                    case "timed":
                        activator = new TimedButton(tMap.get("button_anim"), tMap.get("button"),scale, activatorJV);
                        break;
                    default:
                        throw new RuntimeException("unrecognised activator type");
                }
                activators.add(activator);
                addObject(activator);
            }
        } catch (NullPointerException e) {}

        try {
            for (JsonValue spikeJV : levelJV.get("spikes")) {
                Spikes spike = new Spikes(tMap.get("spikes"), scale, spikeJV);
                loadActivatable(spike, spikeJV);
            }
        } catch (NullPointerException e) {}

        try {
            for (JsonValue checkpointJV : levelJV.get("checkpoints")){
                Checkpoint checkpoint = new Checkpoint(checkpointJV, scale, tMap.get("checkpoint"), tMap.get("checkpointActive"));
                addObject(checkpoint);
            }
        } catch (NullPointerException e) {}

        try {
            for(JsonValue boxJV : levelJV.get("boxes")){
                PushableBox box = new PushableBox(tMap.get("steel"), scale, boxJV);
                loadActivatable(box, boxJV);
            }
        } catch (NullPointerException e) {}

        try {
            for (JsonValue flamethrowerJV : levelJV.get("flamethrowers")){
                Flamethrower flamethrower = new Flamethrower(tMap.get("flamethrower"), tMap.get("flame_anim"),scale, flamethrowerJV);
                loadActivatable(flamethrower, flamethrowerJV);
            }
        } catch (NullPointerException e) {}

        try {
            for (JsonValue laserJV : levelJV.get("lasers")){
                Laser laser = new Laser(tMap.get("laser"), scale, laserJV);
                loadActivatable(laser, laserJV);
                lasers.add(laser);
            }
        } catch (NullPointerException e) {}

        try {
            for (JsonValue mirrorJV : levelJV.get("mirrors")){
                Mirror mirror = new Mirror(tMap.get("steel"), scale, mirrorJV);
                addObject(mirror);
            }
        } catch (NullPointerException e) {}

        // Create mobs
        try {
            for (JsonValue mobJV : levelJV.get("mobs")){
                Mob mob = new Mob(tMap.get("roboMob"), scale, mobJV);
                mobArray.add(mob);
                addObject(mob);
            }
        } catch (NullPointerException e) {}

        // Create cat
        dwidth  = tMap.get("cat").getRegionWidth()/scale.x;
        dheight = tMap.get("cat").getRegionHeight()/scale.y;
        Texture[] arr = new Texture[5];
        arr[0] = tMap.get("cat").getTexture();
        arr[1] = tMap.get("jumpingCat").getTexture();
        arr[2] = tMap.get("jump_anim").getTexture();
        arr[3] = tMap.get("meow_anim").getTexture();
        arr[4] = tMap.get("sit").getTexture();
        cat = new Cat(levelJV.get("cat"), dwidth, dheight, ret, prevCat == null? null : prevCat.getPosition(),arr);
        cat.setDrawScale(scale);
//        cat.setTexture(tMap.get("cat"));
        respawnPos = cat.getPosition();
        addObject(cat);

        spiritMode = false;
        bodySwitched = false;
    }

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
    }

    /**
     * Resets the status of the level.
     *
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
        numLives = maxLives;
        if (world != null) {
            world.dispose();
            world = null;
        }
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
     *
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
     *
     * Joints on the queue are added just before collision processing.  We do this to
     * control joint creation.
     *
     * @param j The jointDef to add
     */
    public void queueJoint(JointDef j){ jointQueue.add(j); }

    /**
     * Returns true if the object is in bounds.
     *
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
     *
     */
    public void addQueuedObjects() {
        while (!addQueue.isEmpty()) {
            addObject(addQueue.poll());
        }
    }

    /**
     * Adds the queued joints to the list of joints
     *
     */
    public void addQueuedJoints(){
        while (!jointQueue.isEmpty()) {
            JointDef jdef = jointQueue.poll();
            Joint joint = world.createJoint(jdef);

            //add joint to joint list of spikes
            //this is very jank and should be factored out for all gameobjects
            if (jdef.bodyA.getUserData() instanceof Spikes){
                ((Spikes) jdef.bodyA.getUserData()).addJoint(joint);
            } else if (jdef.bodyB.getUserData() instanceof Spikes) {
                ((Spikes) jdef.bodyB.getUserData()).addJoint(joint);
            }
        }
    }

    /**
     * Loads an activatable object from a JSON
     *
     * Adds the object to the list of objects
     * Adds the object to the list of activatables
     *
     * @param object the Activatable to add
     * @param objectJV the JsonValue containing the activatable data
     */
    private void loadActivatable(Activatable object, JsonValue objectJV){

        addObject((Obstacle) object);

        String activatorID = objectJV.getString("activatorID", "");
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
     * Draws the level to the given game canvas
     *
     * If debug mode is true, it will outline all physics bodies as wireframes. Otherwise
     * it will only draw the sprite representations.
     *
     * @param canvas	the drawing context
     */
    public void draw(GameCanvas canvas, boolean debug) {
        canvas.clear();

        canvas.begin();
        canvas.applyExtendViewport();
        if (background != null) {
            canvas.draw(background, 0, 0);
        }
        //draw everything except cat and dead bodies
        for(Obstacle obj : objects) {
            if (obj != cat && !(obj instanceof DeadBody)){
                obj.draw(canvas);
            }
        }
        canvas.drawFactoryLine(spiritStartPos, spiritEndPos, 2, spiritLineColor, scale.x, scale.y);
        for (DeadBody db : deadBodyArray){
            db.draw(canvas);
        }
        cat.draw(canvas);
        canvas.end();

        if (debug) {
            canvas.beginDebug();
            //draw grid
            Color lineColor = new Color(0.8f, 0.8f, 0.8f, 1);
            for (int x = 0; x < bounds.width; x++){
                Vector2 p1 = new Vector2(x, 0);
                Vector2 p2 = new Vector2(x, bounds.height);
                canvas.drawLineDebug(p1, p2, lineColor, scale.x, scale.y);
            }
            for (int y = 0; y < bounds.height; y++){
                Vector2 p1 = new Vector2(0, y);
                Vector2 p2 = new Vector2(bounds.width, y);
                canvas.drawLineDebug(p1, p2, lineColor, scale.x, scale.y);
            }
            for(Obstacle obj : objects) {
                obj.drawDebug(canvas);
            }

            canvas.endDebug();

        }
    }

    /** spawns a dead body at the location of the cat */
    public void spawnDeadBody(){
        DeadBody deadBody = new DeadBody(textureRegionAssetMap.get("deadCat"), scale, cat.getPosition());
        deadBody.setLinearVelocity(cat.getLinearVelocity());
        deadBody.setFacingRight(cat.isFacingRight());
        queueObject(deadBody);
        deadBodyArray.add(deadBody);
    }

    /** removes a DeadBody from the dead body array */
    public void removeDeadBody(DeadBody db){
        deadBodyArray.removeValue(db, true);
    }

    /**
     * Gets the next dead body to switch into. Currently selects the closest valid body.
     * @return Dead body to switch into, null if there are none.
     */
    public DeadBody getNextBody(){
        float minDist = Float.MAX_VALUE;
        DeadBody nextdb = null;
        for (DeadBody db : deadBodyArray){
            if (db.isSwitchable()){
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
     * Sets if the level is in spirit mode or not. Currently all spirit mode does is draw a line
     * from the cat to the closest valid dead body.
     * @param next new spirit mode state of level
     */
    public void setSpiritMode(boolean next) {
        if (next && !spiritMode){
            spiritEndPos.set(cat.getPosition());
        }
        spiritMode = next;
    }

    /**
     * Updates spirit mode logic
     * @param dt Number of seconds since last animation frame
     */
    public void update(float dt){
        if (bodySwitched) {
            //fade out line if body was just switched
            spiritLineColor.set(1, 1, 1, spiritLineColor.a - spiritLineColor.a / 5);
            if (spiritLineColor.a < 0.01){
                spiritLineColor.a = 0;
                bodySwitched = false;
            }
        } else {
            spiritStartPos.set(cat.getPosition());
            if (spiritMode) {
                //extend line to target
                spiritModeTicks++;
                spiritLineColor.set(1, 1, 1, spiritLineColor.a + (1 - spiritLineColor.a) / 20);
                nextDeadBody = getNextBody();
                if (nextDeadBody != null) {
                    spiritEndPos.add(nextDeadBody.getPosition().sub(spiritEndPos).scl(0.2f));
                } else {
                    spiritEndPos.set(cat.getPosition());
                }
            } else {
                //fade out line if cancelled
                spiritModeTicks = 0;
                spiritLineColor.set(1, 1, 1, spiritLineColor.a - spiritLineColor.a / 5);
            }
        }
    }

    public void setBodySwitched(boolean bs) {bodySwitched = bs;}
}
