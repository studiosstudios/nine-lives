package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.PolygonObstacle;
import edu.cornell.gdiac.util.Direction;

import java.util.HashMap;

/**
 * An activatable that changes dimension when activated.
 */
public class Door extends PolygonObstacle implements Activatable {

    /** Current activation state */
    private boolean activated;
    /** Starting activation state */
    private boolean initialActivation;
    /** Constants that are shared between all instances of this class*/
    private static JsonValue objectConstants;
    /** The total number of ticks for the door to open/close */
    private float totalTicks;
    /** ticks/totalTicks represents the fraction of the door that is closed */
    private int ticks;
    /** the angle that the door opens/closes towards */
    private Direction angle;
    /** width of the door when fully closed */
    private final float width;
    /** height of the door when fully closed */
    private final float height;
    /** 1 if closing, -1 if opening, 0 if static */
    private float closing;
    /** x position of the door when fully closed */
    private final float x;
    /** y position of the door when fully closed */
    private final float y;

    /**
     * Creates a new Door object.
     *
     * @param width          Width of the door
     * @param height         Height of the door
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param tileSize       Tile size of the Tiled map for loading positions
     * @param levelHeight    Height of level (in grid cell units) for loading y position
     * @param textureScale   Texture scale for rescaling texture
     */
    public Door(float width, float height, ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, Vector2 textureScale){
        super(new float[]{0, 0, width, 0, width, height, 0, height});
        this.width = width;
        this.height = height;
        setTexture(tMap.get("steel"));
        setTextureScale(textureScale);
        setDrawScale(scale);
        setBodyType(BodyDef.BodyType.StaticBody);
        setDensity(objectConstants.getFloat( "density", 0.0f ));
        setFriction(objectConstants.getFloat( "friction", 0.0f ));
        setRestitution(objectConstants.getFloat( "restitution", 0.0f ));

        angle = Direction.angleToDir((int) properties.get("closeAngle", 0));
        totalTicks = (int) properties.get("totalTicks", 60);
        ticks = (int) totalTicks;
        x =(float) properties.get("x")+ objectConstants.get("offset").getFloat(0);
        y = (float) properties.get("y") + objectConstants.get("offset").getFloat(1) - height;
        setX(x);
        setY(y);
        closing = 0;
        initTiledActivations(properties);
    }

    /**
     * Creates a new Door object.
     *
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale      Draw scale for drawing
     * @param textureScale   Texture scale for rescaling texture
     */
    public Door(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, Vector2 textureScale){
        this((float) properties.get("width"), (float) properties.get("height"),
                properties, tMap, scale, textureScale);
    }

    /**
     * Update fixture and texture shape if currently closing/opening.
     * @param dt Timing values from parent loop
     */
    public void update(float dt) {
        super.update(dt);
        ticks += closing;
        if (ticks <= 0){
            setActive(false);
            closing = 0;
            ticks = 0;
            return;
        }
        if (ticks >= totalTicks){
            ticks = (int) totalTicks;
            closing = 0;
        }
        switch (angle) {
            case DOWN:
                setY(y + height * (1-ticks / totalTicks));
                setDimension(width,  height * ticks / totalTicks, true, width, 0);
                break;
            case UP:
                setY(y - height * (1-ticks / totalTicks));
                setDimension(width,  height * ticks / totalTicks, true, width, height);
                break;
            case LEFT:
                setX(x + width * (1-ticks / totalTicks));
                setDimension(width * ticks / totalTicks,  height, true, 0, height);
                break;
            case RIGHT:
                setX(x - width * (1-ticks / totalTicks));
                setDimension(width * ticks / totalTicks,  height, true, width, height);
                break;
        }
    }


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
            setActive(false);
        }
        return true;
    }

    /**
     * Begins closing door.
     */
    @Override
    public void activated(World world){
        closing = 1;
        setActive(true);
    }

    /**
     * Begins opens door.
     */
    @Override
    public void deactivated(World world){
        closing = -1;
    }

    //region ACTIVATABLE METHODS
    @Override
    public void setActivated(boolean activated){ this.activated = activated; }

    @Override
    public boolean isActivated() { return activated; }

    @Override
    public void setInitialActivation(boolean initialActivation){ this.initialActivation = initialActivation; }

    @Override
    public boolean getInitialActivation() { return initialActivation; }
    //endregion

    @Override
    public void draw(GameCanvas canvas){
        if (isActive()){
            super.draw(canvas);
        }
    }

    /**
     * Sets the shared constants for all instances of this class/
     * @param constants JSON storing the shared constants.
     */
    public static void setConstants(JsonValue constants) {objectConstants = constants;}

    public ObjectMap<String, Object> storeState(){
        ObjectMap<String, Object> stateMap = super.storeState();
        stateMap.put("ticks", ticks);
        stateMap.put("closing", closing);
        stateMap.put("activated", activated);
        return stateMap;
    }

    public void loadState(ObjectMap<String, Object> stateMap){
        super.loadState(stateMap);
        ticks = (int) stateMap.get("ticks");
        closing = (float) stateMap.get("closing");
    }
}
