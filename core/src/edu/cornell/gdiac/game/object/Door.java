package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.PolygonObstacle;
import edu.cornell.gdiac.util.Direction;

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

    /**
     * Creates a new Door with specified width and height.
     * @param texture   TextureRegion for drawing.
     * @param scale     Draw scale for drawing.
     * @param width     Width of the door.
     * @param height    Height of the door.
     * @param data      JSON data for loading.
     */
    public Door(TextureRegion texture, Vector2 scale, float width, float height, JsonValue data){
        super(new float[]{0, 0, width, 0, width, height, 0, height});
        this.width = width;
        this.height = height;
        setTexture(texture);
        setDrawScale(scale);
        setBodyType(BodyDef.BodyType.StaticBody);
        setDensity(objectConstants.getFloat( "density", 0.0f ));
        setFriction(objectConstants.getFloat( "friction", 0.0f ));
        setRestitution(objectConstants.getFloat( "restitution", 0.0f ));

        angle = Direction.angleToDir(data.getInt("angle"));
        totalTicks = data.getFloat("totalTicks");
        ticks = (int) totalTicks;
        setX(data.get("pos").getFloat(0)+ objectConstants.get("offset").getFloat(0));
        setY(data.get("pos").getFloat(1)+ objectConstants.get("offset").getFloat(1));
        
        closing = 0;
        initActivations(data);
    }

    /**
     * Creates a new Door, reading width and height from the JSON data.
     * @param texture   TextureRegion for drawing.
     * @param scale     Draw scale for drawing..
     * @param data      JSON data for loading the door.
     */
    public Door(TextureRegion texture, Vector2 scale, JsonValue data){
        this(texture, scale, data.getFloat("width"), data.getFloat("height"), data);
    }

    /**
     * Update fixture and texture shape if currently closing/opening.
     * @param dt Timing values from parent loop
     */
    public void update(float dt){
        super.update(dt);
        if (closing == 1){
            //closing
            ticks++;
            if (ticks == totalTicks){
                setDimension(width , height,true);
                closing = 0;
            } else {
                switch (angle) {
                    case DOWN:
                        setY(getY() - height / totalTicks);
                        setDimension(width, getHeight() + height / totalTicks, true, width, 0);
                        break;
                    case UP:
                        setY(getY() + height / totalTicks);
                        setDimension(width, getHeight() + height / totalTicks, true, width, height);
                        break;
                    case LEFT:
                        setX(getX() - width / totalTicks);
                        setDimension(getWidth() + width / totalTicks, height, true, 0, height);
                        break;
                    case RIGHT:
                        setX(getX() + width / totalTicks);
                        setDimension(getWidth() + width / totalTicks, height, true, width, height);
                        break;
                }
            }
        } else if (closing == -1) {
            //opening
            ticks--;
            if (ticks == 0){
                setActive(false);
                closing = 0;
            } else {
                switch (angle) {
                    case DOWN:
                        setY(getY() + height / totalTicks);
                        setDimension(width, getHeight() - height / totalTicks, true, width, 0);
                        break;
                    case UP:
                        setY(getY() - height / totalTicks);
                        setDimension(width, getHeight() - height / totalTicks, true, width, height);
                        break;
                    case LEFT:
                        setX(getX() + width / totalTicks);
                        setDimension(getWidth() - width / totalTicks, height, true, 0, height);
                        break;
                    case RIGHT:
                        setX(getX() - width / totalTicks);
                        setDimension(getWidth() - width / totalTicks, height, true, width, height);
                        break;
                }
            }
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
        setActive(true);
    }

    //region ACTIVATABLE METHODS
    @Override
    public void setActivated(boolean activated){ this.activated = activated; }

    @Override
    public boolean getActivated() { return activated; }

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
}
