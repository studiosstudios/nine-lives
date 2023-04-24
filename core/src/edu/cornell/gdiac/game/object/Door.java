package edu.cornell.gdiac.game.object;

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
import edu.cornell.gdiac.util.Direction;

import java.util.HashMap;

/**
 * An activatable that changes dimension when activated.
 */
public class Door extends BoxObstacle implements Activatable {

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

    private TextureRegion top;

    private TextureRegion bottom;

    private TextureRegion middle;

    private static float shrink;

    /**
     * Creates a new Door object.
     *
     * @param width          Width of the door
     * @param height         Height of the door
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param textureScale   Texture scale for rescaling texture
     */
    public Door(float width, float height, ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, Vector2 textureScale){
        super(width, height);
//
//        setTexture(tMap.get("door"));
        TextureRegion[][] tiles = tMap.get("door").split(tMap.get("door").getTexture(), 1024, 1024);
        top = tiles[0][1];
        middle = tiles[0][0];
        bottom = tiles[0][2];
        setDrawScale(scale);
        setTextureScale(textureScale);
        setBodyType(BodyDef.BodyType.StaticBody);
        setDensity(objectConstants.getFloat( "density", 0.0f ));
        setFriction(objectConstants.getFloat( "friction", 0.0f ));
        setRestitution(objectConstants.getFloat( "restitution", 0.0f ));

        angle = Direction.angleToDir((int) properties.get("closeAngle", 0));
        totalTicks = (int) properties.get("totalTicks", 60);
        ticks = (int) totalTicks;
        closing = 0;
        x =(float) properties.get("x") + objectConstants.get("offset").getFloat(0) + width/2f;
        y = (float) properties.get("y") + objectConstants.get("offset").getFloat(1) - height/2f;
        switch (angle) {
            case UP:
            case DOWN:
                width -= shrink;
                break;
            case RIGHT:
            case LEFT:
                height -= shrink;
                break;
        }
        this.width = width;
        this.height = height;
        setDimension(width, height);
        setX(x);
        setY(y);
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
                setY(y + height * (1-ticks / totalTicks)/2f);
                setDimension(getWidth(),  height * ticks / totalTicks);
                break;
            case UP:
                setY(y - height * (1-ticks / totalTicks)/2f);
                setDimension(getWidth(),  height * ticks / totalTicks);
                break;
            case LEFT:
                setX(x + width * (1-ticks / totalTicks)/2f);
                setDimension(width * ticks / totalTicks,  getHeight());
                break;
            case RIGHT:
                setX(x - width * (1-ticks / totalTicks)/2f);
                setDimension(width * ticks / totalTicks,  getHeight());
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
    public float getXPos() {
        return getX();
    }

    @Override
    public float getYPos() {
        return getY();
    }

    @Override
    public boolean getInitialActivation() { return initialActivation; }
    //endregion

    @Override
    public void draw(GameCanvas canvas){
        if (isActive()){
            super.draw(canvas);
        }
        float topY = y, botY = y;
        float topX = x, botX = x;
        float midX = x, midY = y;
        float rotation = 0;
        switch (angle) {
            case DOWN:
                midY += height * (1 - ticks/totalTicks);
            case UP:
                topY += height/2f - 1;
                botY -= height/2f;
                topX -= (width+shrink)/2f;
                botX -= (width+shrink)/2f;
                midX = botX;
                midY -= height/2f;
                middle.setRegionHeight((int) (height * 1024* ticks/totalTicks));
                for (float dx = 0; dx < width; dx++){
                    if (isActive()) canvas.draw(middle, Color.WHITE, 0, 0, (midX+dx)*drawScale.x, midY*drawScale.y, rotation, textureScale.x, textureScale.y);
                    canvas.draw(top, Color.WHITE, 0, 0, (topX+dx)*drawScale.x, topY*drawScale.y, rotation, textureScale.x, textureScale.y);
                    canvas.draw(bottom, Color.WHITE, 0, 0, (botX+dx)*drawScale.x, botY*drawScale.y, rotation, textureScale.x, textureScale.y);
                }
                break;
            case LEFT:
                midX += width * (1 - ticks/totalTicks);
            case RIGHT:
                rotation = (float) Math.PI/2*3;
                topX += width/2f - 1;
                botX -= width/2f;
                topY += (height+shrink)/2f;
                botY += (height+shrink)/2f;
                midX -= width/2f;
                midY = botY;
                middle.setRegionHeight((int) (width * 1024* ticks/totalTicks));
                for (float dy = 0; dy < height; dy++){
                    if (isActive()) canvas.draw(middle, Color.WHITE, 0, 0, midX*drawScale.x, (midY+dy)*drawScale.y, rotation, textureScale.x, textureScale.y);
                    canvas.draw(top, Color.WHITE, 0, 0, topX*drawScale.x, (topY+dy)*drawScale.y, rotation, textureScale.x, textureScale.y);
                    canvas.draw(bottom, Color.WHITE, 0, 0, botX*drawScale.x, (botY+dy)*drawScale.y, rotation, textureScale.x, textureScale.y);
                }
                break;
        }

    }

    /**
     * Sets the shared constants for all instances of this class/
     * @param constants JSON storing the shared constants.
     */
    public static void setConstants(JsonValue constants) {
        objectConstants = constants;
        shrink = constants.getFloat("shrink");
    }

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
