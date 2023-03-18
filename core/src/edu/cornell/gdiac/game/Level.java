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
import edu.cornell.gdiac.assets.AssetDirectory;
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
    private Checkpoint currCheckpoint;
    /** The new dead body to be added */
    private DeadBody newDeadBody;
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


    /** */

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
        dheight = tMap.get("goal").getRegionHeight()/scale.y;

        activationRelations = new HashMap<>();
        background = tMap.get("background").getTexture();

        JsonValue goal = levelJV.get("goal");
        JsonValue goalpos = goal.get("pos");
        goalDoor = new BoxObstacle(goalpos.getFloat(0),goalpos.getFloat(1),dwidth,dheight);
        goalDoor.setBodyType(BodyDef.BodyType.StaticBody);
        goalDoor.setDensity(goal.getFloat("density", 0));
        goalDoor.setFriction(goal.getFloat("friction", 0));
        goalDoor.setRestitution(goal.getFloat("restitution", 0));
//        goalDoor.setSensor(true);
        goalDoor.setDrawScale(scale);
        goalDoor.setTexture(tMap.get("goal"));
        goalDoor.setName("goal");
        addObject(goalDoor);

        JsonValue retgoal = levelJV.get("ret_goal");
        JsonValue retgoalpos = retgoal.get("pos");
        retDoor = new BoxObstacle(retgoalpos.getFloat(0),retgoalpos.getFloat(1),dwidth,dheight);
        retDoor.setBodyType(BodyDef.BodyType.StaticBody);
        retDoor.setDensity(retgoal.getFloat("density", 0));
        retDoor.setFriction(retgoal.getFloat("friction", 0));
        retDoor.setRestitution(retgoal.getFloat("restitution", 0));
//        goalDoor.setSensor(true);
        retDoor.setDrawScale(scale);
        retDoor.setTexture(tMap.get("goal"));
        retDoor.setName("ret_goal");
        addObject(retDoor);

        String wname = "wall";
        JsonValue walljv = levelJV.get("walls");
        JsonValue defaults = constants.get("defaults");
        for (int ii = 0; ii < walljv.size; ii++) {
            PolygonObstacle obj;
            obj = new PolygonObstacle(walljv.get(ii).asFloatArray(), 0, 0);
            obj.setBodyType(BodyDef.BodyType.StaticBody);
            obj.setDensity(defaults.getFloat( "density", 0.0f ));
            obj.setFriction(defaults.getFloat( "friction", 0.0f ));
            obj.setRestitution(defaults.getFloat( "restitution", 0.0f ));
            obj.setDrawScale(scale);
            obj.setTexture(tMap.get("steel"));
            obj.setName(wname+ii);
            addObject(obj);
        }

        String pname = "platform";
        JsonValue platjv = levelJV.get("platforms");
        for (int ii = 0; ii < platjv.size; ii++) {
            PolygonObstacle obj;
            obj = new PolygonObstacle(platjv.get(ii).asFloatArray(), 0, 0);
            obj.setBodyType(BodyDef.BodyType.StaticBody);
            obj.setDensity(defaults.getFloat( "density", 0.0f ));
            obj.setFriction(defaults.getFloat( "friction", 0.0f ));
            obj.setRestitution(defaults.getFloat( "restitution", 0.0f ));
            obj.setDrawScale(scale);
            obj.setTexture(tMap.get("steel"));
            obj.setName(pname+ii);
            addObject(obj);
        }

        // This world is heavier
        world.setGravity( new Vector2(0,defaults.getFloat("gravity",0)) );

        JsonValue activatorConstants = constants.get("activators");
        Activator.setConstants(activatorConstants);
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

        JsonValue spikesConstants = constants.get("spikes");
        Spikes.setConstants(spikesConstants);
        for (JsonValue spikeJV : levelJV.get("spikes")){
//            System.out.println(tMap.get("spikes"));
            Spikes spike = new Spikes(tMap.get("spikes"), scale, spikeJV);
            loadActivatable(spike, spikeJV);
        }

        JsonValue checkpointConstants = constants.get("checkpoint");
        Checkpoint.setConstants(checkpointConstants);
        for (JsonValue checkpointJV : levelJV.get("checkpoints")){
            Checkpoint checkpoint = new Checkpoint(checkpointJV, scale, tMap.get("checkpoint"), tMap.get("checkpointActive"));
            addObject(checkpoint);
        }

        JsonValue boxConstants = constants.get("boxes");
        PushableBox.setConstants(boxConstants);
        for(JsonValue boxJV : levelJV.get("boxes")){
            PushableBox box = new PushableBox(tMap.get("steel"), scale, boxJV);
            loadActivatable(box, boxJV);
        }

        JsonValue flamethrowerConstants = constants.get("flamethrowers");
        Flamethrower.setConstants(flamethrowerConstants);
        Flame.setConstants(flamethrowerConstants);
        for (JsonValue flamethrowerJV : levelJV.get("flamethrowers")){
            Flamethrower flamethrower = new Flamethrower(tMap.get("flamethrower"), tMap.get("flame"),scale, flamethrowerJV);
            loadActivatable(flamethrower, flamethrowerJV);
        }

        // Create Laser
        JsonValue lasersJV = constants.get("laser");
        for (JsonValue laserJV : levelJV.get("lasers")) {
            float x = laserJV.get("pos").getFloat(0);
            float y = laserJV.get("pos").getFloat(1);
            LaserBeam laser = new LaserBeam(constants.get("laser"), x, y, 8, dwidth,dheight,"laserbeam");
            laser.setTexture(tMap.get("laserBeam"));
            laser.setDrawScale(scale);
            addObject(laser);
        }

        JsonValue deadBodyConstants = constants.get("deadBody");
        DeadBody.setConstants(deadBodyConstants);

        // Create cat
        dwidth  = tMap.get("cat").getRegionWidth()/scale.x;
        dheight = tMap.get("cat").getRegionHeight()/scale.y;
        cat = new Cat(levelJV.get("cat"), dwidth, dheight, ret, prevCat == null? null : prevCat.getPosition());
        cat.setDrawScale(scale);
        cat.setTexture(tMap.get("cat"));
        respawnPos = cat.getPosition();
        addObject(cat);
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
        if (background != null) {
            canvas.draw(background, 0, 0);
        }
        for(Obstacle obj : objects) {
            obj.draw(canvas);
        }
        canvas.end();

        if (debug) {
            canvas.beginDebug();
            //draw grid
            Color lineColor = new Color(0.8f, 0.8f, 0.8f, 1);
            for (int x = 0; x < bounds.width; x++){
                Vector2 p1 = new Vector2(x, 0);
                Vector2 p2 = new Vector2(x, bounds.height);
                canvas.drawLine(p1, p2, lineColor, scale.x, scale.y);
            }
            for (int y = 0; y < bounds.height; y++){
                Vector2 p1 = new Vector2(0, y);
                Vector2 p2 = new Vector2(bounds.width, y);
                canvas.drawLine(p1, p2, lineColor, scale.x, scale.y);
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


}
