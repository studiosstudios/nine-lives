package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;
import edu.cornell.gdiac.game.obstacle.PolygonObstacle;

import java.util.HashMap;

/**
 * A Platform is a kinematic body that can move between two points.
 */
public class Platform extends PolygonObstacle implements Activatable {
    /** Current activation state */
    private boolean activated;
    /** Starting activation state */
    private boolean initialActivation;
    /** Constants that are shared between all instances of this class */
    private static JsonValue objectConstants;
    /** If this wall is climbable */
    private boolean isClimbable;
    /** The displacement of the platform when moving */
    private Vector2 disp;
    /** 1 if moving towards end point, -1 if moving towards start point, 0 if static */
    private float moving;
    /** Max speed of the platform */
    private float speed;
    /** Damping factor of the velocity update */
    private float damping;
    /** Target velocity for velocity update */
    private Vector2 targetVel = new Vector2();
    /** Target position */
    private Vector2 target;
    private Vector2 startPos;
    private TextureRegion[][] textures;
    private int tileSize;
    private Vector2 other;

    /**
     * Creates a new Door object.
     *
     * @param width          Width of the door
     * @param height         Height of the door
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param tileSize       Size in pixels of tileset tiles
     */
    public Platform(float width, float height, ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, int tileSize){
        super(new float[]{0, 0, width, 0, width, height, 0, height});
        setName("platform");
        setBodyType(BodyDef.BodyType.KinematicBody);
        setDensity(objectConstants.getFloat( "density", 0.0f ));
        setFriction(objectConstants.getFloat( "friction", 0.0f ));
        setRestitution(objectConstants.getFloat( "restitution", 0.0f ));
        setDrawScale(scale);
        setTexture(tMap.get("steel"));
        this.tileSize = tileSize;
        initTextures(tMap.get("platform"), (int) width, (int) height);
        setX((float) properties.get("x"));
        setY((float) properties.get("y") - height);
        startPos = getPosition().cpy();
        speed = (float) properties.get("speed", 5f);
        damping = (float) properties.get("damping", 0.1f);
        disp = (Vector2) properties.get("disp", Vector2.Zero);
        isClimbable = (boolean) properties.get("climbable", false);
        target = new Vector2();
        other = new Vector2();
        initTiledActivations(properties);
    }

    /**
     * Initializes the tileset TextureRegion array for this platform based on its width and height.
     *
     * @param tileset   Platform tileset (16 tiles)
     * @param width     Width in world units
     * @param height    Height in world units
     */
    private void initTextures(TextureRegion tileset, int width, int height){
        textures = new TextureRegion[width][height];
        TextureRegion[][] tiles = tileset.split(tileSize, tileSize);

        if (width == 1 && height == 1){
            textures[0][0] = tiles[0][0];
        } else if (width == 1) {
            textures[0][0] = tiles[3][0];
            for (int i = 1; i < height-1; i++) {
                textures[0][i] = tiles[2][0];
            }
            textures[0][height - 1] = tiles[1][0];
        } else if (height == 1){
            textures[0][0] = tiles[0][1];
            for (int i = 1; i < width-1; i++) {
                textures[i][0] = tiles[0][2];
            }
            textures[width - 1][0] = tiles[0][3];
        } else {

            //top row
            textures[0][height - 1] = tiles[1][1];
            for (int i = 1; i < width-1; i++) {
                textures[i][height - 1] = tiles[1][2];
            }
            textures[width - 1][height - 1] = tiles[1][3];

            //middle rows
            for (int j = 1; j < height-1; j++){
                textures[0][j] = tiles[2][1];
                for (int i = 1; i < width-1; i++) {
                    textures[i][j] = tiles[2][2];
                }
                textures[width - 1][j] = tiles[2][3];
            }

            //bottom row
            textures[0][0] = tiles[3][1];
            for (int i = 1; i < width-1; i++) {
                textures[i][0] = tiles[3][2];
            }
            textures[width - 1][0] = tiles[3][3];

        }
    }

    /**
     * Creates a new Platform object.
     *
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param tileSize       Size in pixels of tileset tiles
     */
    public Platform(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, int tileSize){
        this((float) properties.get("width"), (float) properties.get("height"), properties, tMap, scale, tileSize);
    }

    /**
     * Update velocity and target velocity if moving between points.
     * @param dt Timing values from parent loop
     */
    public void update(float dt){
        super.update(dt);
        if (moving == 0) { return; }

        //check if should start slowing down to 0
        if (target.dst(getPosition()) - estimateDist(dt) <= 0){
            targetVel.set(0, 0);
        } else {
            //in case something bad happens
            targetVel.set(disp.x * moving, disp.y * moving).nor().scl(speed);
        }

        //check if close enough to target pos
        if (getPosition().epsilonEquals(target, 0.01f)){
            moving = 0;
            setPosition(target);
            targetVel.set(0, 0);
            setVX(0);
            setVY(0);
        }

        //check if passed through target pos
        if (other.dst(getPosition()) > other.dst(target)) {
            moving = 0;
            setPosition(target);
            setVX(0);
            setVY(0);
            targetVel.set(0, 0);
        }

        //update velocity
        setVX(getVX() + (targetVel.x - getVX()) * damping);
        setVY(getVY() + (targetVel.y - getVY()) * damping);

    }

    /**
     * Estimates the distance this platform will travel if its velocity target is set to 0 at this
     * timestep, i.e. that assuming v_{t+1} = (1-damping) * v_t.
     * @param dt Time between frames
     * @return   Magnitude of distance travelled if velocity target is set to 0 vector.
     */
    private float estimateDist(float dt){ return getLinearVelocity().len()* (1-damping)/damping*dt; }

    /**
     * Creates the physics body for this object, adding them to the world. Immediately deactivates
     * self if necessary.
     * @param world Box2D world to store body
     *
     * @return      true if object allocation succeeded
     */
    public boolean activatePhysics(World world){
        if (!super.activatePhysics(world)) {
            return false;
        }
        if (!activated) {
            deactivated(world);
            setPosition(startPos.x + disp.x, startPos.y + disp.y);
        }
        moving = 0;
        return true;
    }

    public void draw(GameCanvas canvas){
        for (int dx = 0; dx < getWidth(); dx++) {
            for (int dy = 0; dy < getHeight(); dy++){
                TextureRegion platformTile = textures[dx][dy];
                canvas.draw(platformTile, Color.WHITE, 0, 0, (getX() + dx)*drawScale.x, (getY() +  dy)*drawScale.y, 0, drawScale.x/tileSize, drawScale.y/tileSize);
            }
        }
    }

    @Override
    public void activated(World world){
        moving = -1;
        targetVel.set(-disp.x, -disp.y).nor().scl(speed);
        target.set(startPos);
        other.set(disp.x + startPos.x, disp.y + startPos.y);
    }


    public void deactivated(World world){
        moving = 1;
        targetVel.set(disp.x, disp.y).nor().scl(speed);
        target.set(disp.x + startPos.x, disp.y + startPos.y);
        other.set(startPos);
    }

    //region ACTIVATABLE METHODS
    @Override
    public void setActivated(boolean activated){ this.activated = activated; }

    @Override
    public boolean isActivated() { return activated; }

    @Override
    public void setInitialActivation(boolean initialActivation){ this.initialActivation = initialActivation; }

    @Override
    public float getXPos() {
        return getX() + getWidth()/2f;
    }

    @Override
    public float getYPos() { return getY() + getHeight()/2f; }

    @Override
    public boolean getInitialActivation() { return initialActivation; }
    //endregion

    /**
     * Sets the shared constants for all instances of this class.
     * @param constants JSON storing the shared constants.
     */
    public static void setConstants(JsonValue constants) {objectConstants = constants;}

    public ObjectMap<String, Object> storeState(){
        ObjectMap<String, Object> stateMap = super.storeState();
        stateMap.put("moving", moving);
        stateMap.put("targetVel", targetVel.cpy());
        return stateMap;
    }

    public void loadState(ObjectMap<String, Object> stateMap){
        super.loadState(stateMap);
        moving = (float) stateMap.get("moving");
        targetVel.set((Vector2) stateMap.get("targetVel"));
    }

}
