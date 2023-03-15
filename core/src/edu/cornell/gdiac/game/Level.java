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
    private boolean died;

    private int numLives;

    private int maxLives;

    /** hashmap to represent activator-spike relationships:
     *   keys are activator ids specified in JSON*/
    private HashMap<String, Array<Activatable>> activationRelations;

    /** object lists - in the future this will be one list maybe */
    private Array<Activator> activators;
    private Array<Activatable> activatables;
    private Array<DeadBody> deadBodyArray;
    private DeadBody newDeadBody;
    private Vector2 respawnPos;
    private float dwidth;
    private float dheight;
    private Texture background;



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

    public Array<Activator> getActivators() { return activators; }

    public HashMap<String, Array<Activatable>> getActivationRelations() { return activationRelations; }

    public Array<DeadBody> getdeadBodyArray() { return deadBodyArray; }

    public DeadBody getNewDeadBody() { return newDeadBody; }
    public void setNewDeadBody(DeadBody body) {
        newDeadBody = body;
    }

    public Vector2 getRespawnPos() { return respawnPos; }

    public float getDwidth() { return dwidth; }
    public float getDheight() { return dheight; }

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
                    activator = new Button(tMap.get("button"), scale, activatorJV);
                    break;
                case "switch":
                    activator = new Switch(tMap.get("button"), scale, activatorJV);
                    break;
                case "timed":
                    activator = new TimedButton(tMap.get("button"), scale, activatorJV);
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
            Spikes spike = new Spikes(tMap.get("spikes"), scale, spikeJV);
            loadActivatable(spike, spikeJV);
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
            Flamethrower flamethrower = new Flamethrower(tMap.get("flamethrower"), tMap.get("flame"), scale, flamethrowerJV);
            loadActivatable(flamethrower, flamethrowerJV);
        }


        // Create Laser
        JsonValue lasersJV = constants.get("laser");
        for (JsonValue laserJV : levelJV.get("lasers")) {
            float x = laserJV.get("pos").getFloat(0);
            float y = laserJV.get("pos").getFloat(1);
            LaserBeam laser = new LaserBeam(constants.get("laser"), x, y, 8, dwidth,dheight,"laserbeam");
            laser.setTexture(tMap.get("laserbeam"));
            laser.setDrawScale(scale);
            addObject(laser);
        }

        // Create cat
        dwidth  = tMap.get("cat").getRegionWidth()/scale.x;
        dheight = tMap.get("cat").getRegionHeight()/scale.y;
        cat = new Cat(levelJV.get("cat"), dwidth, dheight, ret, prevCat == null? null : prevCat.getPosition());
        cat.setDrawScale(scale);
        cat.setTexture(tMap.get("cat"));
        respawnPos = cat.getPosition();
        addObject(cat);

    }


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
     * param obj The object to add
     */
    protected void addObject(Obstacle obj) {
        assert inBounds(obj) : "Object is not in bounds";
        objects.add(obj);
        obj.activatePhysics(world);
    }

    /**
     *
     * Adds a physics object in to the insertion queue.
     *
     * Objects on the queue are added just before collision processing.  We do this to
     * control object creation.
     *
     * param obj The object to add
     */
    public void queueObject(Obstacle obj) {
        assert inBounds(obj) : "Object is not in bounds";
        addQueue.add(obj);
    }

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

    public PooledList<Obstacle> getObjects(){ return objects; }

    /**
     * Returns true if the level is completed.
     *
     * If true, the level will advance after a countdown
     *
     * @return true if the level is completed.
     */
    public boolean isComplete( ) {
        return complete;
    }

    /**
     * Returns true if the level is failed.
     *
     * If true, the level will reset after a countdown
     *
     * @return true if the level is failed.
     */
    public boolean isFailure( ) {
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

    public void addQueuedObjects() {
        while (!addQueue.isEmpty()) {
            addObject(addQueue.poll());
        }
    }

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

    public Obstacle getGoalDoor() {  return goalDoor; }

    public Obstacle getRetDoor() {  return retDoor; }

    public void setWorld(World world) { this.world = world; }
    public void setDied(boolean died){ this.died = died; }

    public boolean getDied() { return died; }

    public int getNumLives() { return numLives; }

    public void setNumLives(int val) { numLives = val; }

    public void resetLives() { numLives = maxLives; }

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


}
