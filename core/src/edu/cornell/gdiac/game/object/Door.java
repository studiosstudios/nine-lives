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
import edu.cornell.gdiac.game.obstacle.ComplexObstacle;
import edu.cornell.gdiac.util.Direction;

import java.util.HashMap;

/**
 * An activatable that changes dimension when activated.
 */
public class Door extends BoxObstacle implements Activatable {
    private Cap cap;
    private static float capSize;
    private Vector2 capClosedPos;
    private Vector2 capOpenPos;
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
    private float x;
    /** y position of the door when fully closed */
    private float y;
    private int textureSize;
    private TextureRegion top;
    private TextureRegion bottom;
    private TextureRegion middle;
    private static float shrink;

    private class Cap extends BoxObstacle {
        public Cap(float width, float height) {
            super(width, height);
        }
        @Override
        public Vector2 getLinearVelocity() { return Vector2.Zero; }
    }

    /**
     * Creates a new Door object.
     *
     * @param width          Width of the door
     * @param height         Height of the door
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param textureSize    Size of texture in pixels
     */
    public Door(float width, float height, ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, int textureSize){
        super(width, height);
        TextureRegion[][] tiles = tMap.get("door").split(tMap.get("door").getTexture(), textureSize, textureSize);
        top = tiles[0][1];
        middle = tiles[0][2];
        bottom = tiles[0][0];
        bottom.setRegion(0, textureSize/2, textureSize, textureSize/2); //remove weird line
        this.textureSize = textureSize;
        setDrawScale(scale);
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
                width -= shrink;
                cap = new Cap(width/2f, capSize);
                capClosedPos = new Vector2(x, y + height/2f - capSize/2f);
                capOpenPos = new Vector2(x, y - height/2f + capSize * 1.5f);
                cap.setPosition(capClosedPos);
                height -= capSize;
                y -= capSize/2f;
                break;
            case DOWN:
                width -= shrink;
                cap = new Cap(width/2f, capSize);
                capClosedPos = new Vector2(x, y - height/2f + capSize/2f);
                capOpenPos = new Vector2(x, y + height/2f - capSize * 1.5f);
                cap.setPosition(capClosedPos);
                height -= capSize;
                y += capSize/2f;
                break;
            case RIGHT:
                height -= shrink;
                cap = new Cap(capSize, height/2f);
                capClosedPos = new Vector2(x + width/2f - capSize/2f, y);
                capOpenPos = new Vector2(x - width/2f + capSize*1.5f, y);
                cap.setPosition(capClosedPos);
                width -= capSize;
                x -= capSize/2f;
                break;
            case LEFT:
                height -= shrink;
                cap = new Cap(capSize, height/2f);
                capClosedPos = new Vector2(x - width/2f + capSize/2f, y);
                capOpenPos = new Vector2(x + width/2f - capSize*1.5f, y);
                cap.setPosition(capClosedPos);
                width -= capSize;
                x += capSize/2f;
                break;
        }
        this.width = width;
        this.height = height;
        setX(x);
        setY(y);
        setDimension(width, height);
        setBodyType(BodyDef.BodyType.StaticBody);
        setGravityScale(0);
        setFixedRotation(true);

        cap.setBodyType(BodyDef.BodyType.DynamicBody);
        cap.setGravityScale(0);
        cap.setFixedRotation(true);
        cap.setDrawScale(scale);
        cap.setDensity(objectConstants.getFloat( "density", 0.0f ));
        cap.setFriction(objectConstants.getFloat( "friction", 0.0f ));
        cap.setRestitution(objectConstants.getFloat( "restitution", 0.0f ));

        initTiledActivations(properties);
    }

    /**
     * Creates a new Door object.
     *
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param textureSize    Size of texture in pixels
     */
    public Door(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, int textureSize){
        this((float) properties.get("width"), (float) properties.get("height"),
                properties, tMap, scale, textureSize);
    }

    /**
     * Update fixture and texture shape if currently closing/opening.
     * @param dt Timing values from parent loop
     */
    public void update(float dt) {
        super.update(dt);
        ticks += closing;

        if (closing == -1 && cap.getPosition().dst(capClosedPos) > capOpenPos.dst(capClosedPos)) {
            cap.setPosition(capOpenPos);
            cap.setActive(false);
            setActive(false);
            closing = 0;
        } else if (closing == 1 && cap.getPosition().dst(capOpenPos) > capOpenPos.dst(capClosedPos)){
            cap.setPosition(capClosedPos);
            closing = 0;
        }
        switch (angle) {
            case DOWN:
                cap.setVY(-(height - capSize) * closing / totalTicks * 60f);
                cap.setX(x);
                setY(y + (cap.getY() - (y - height/2f + capSize/2f) + capSize)/2f);
                setDimension(width,  height - (cap.getY() - (y - height/2f + capSize/2f) + capSize), false);
                break;
            case UP:
                cap.setVY((height - capSize) * closing / totalTicks * 60f);
                cap.setX(x);
                setY(y - (y + height/2f - capSize/2f - cap.getY() + capSize)/2f);
                setDimension(width,  height - (y + height/2f - capSize/2f - cap.getY() + capSize), false);
                break;
            case RIGHT:
                cap.setVX((width - capSize) * closing / totalTicks * 60f);
                cap.setY(y);
                setX(x - (x + width/2f - capSize/2f - cap.getX() + capSize)/2f);
                setDimension(width - (x + width/2f - capSize/2f - cap.getX() + capSize), height, false);
                break;
            case LEFT:
                cap.setVX(-(width - capSize) * closing / totalTicks * 60f);
                cap.setY(y);
                setX(x + (cap.getX() - (x - width/2f + capSize/2f) + capSize)/2f);
                setDimension(width - (cap.getX() - (x - width/2f + capSize/2f) + capSize), height, false);
                break;
        }
    }

    public boolean isMoving(){ return closing != 0; }


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
        if (!cap.activatePhysics(world)) {
            return false;
        }
        cap.getBody().setUserData(this);
        return true;
    }

    /**
     * Begins closing door.
     */
    @Override
    public void activated(World world){
        closing = 1;
        setActive(true);
        cap.setActive(true);
    }

    /**
     * Begins opening door.
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
        //dont worry i hate this too
        float topY, botY, topX, botX, midX, midY, rotation;
        float scale = 32f/textureSize;

        float length = capSize + (angle == Direction.UP || angle == Direction.DOWN ? getDimension().y : getDimension().x);
        float midHeight = Math.max((length - 1), 0) ;
        if (midHeight > 0){
            middle.setRegionHeight((int) (textureSize * (midHeight + 0.5f)));
            top.setRegionHeight(textureSize);
        } else {
            middle.setRegionHeight(0);
            top.setRegionHeight((int) (textureSize * length));
        }

        if (angle == Direction.UP || angle == Direction.DOWN) {
            if (angle == Direction.UP) {
                rotation = 0;
                botY = y - height/2f;
                topY = y + midHeight - height/2f;
                topX = x - (width+shrink)/2f;
            } else {
                rotation = (float) Math.PI;
                botY = y + height/2f;
                topY = y - midHeight + height/2f;
                topX = x - (width+shrink)/2f + 1;
            }
            midY = botY;
            midX = topX;
            botX = topX;
            for (float dx = 0; dx < width; dx++){
                if (isActive()) {
                    canvas.draw(top, Color.WHITE, 0, 0, (topX+dx)*drawScale.x, topY*drawScale.y, rotation, scale, scale);
                    canvas.draw(middle, Color.WHITE, 0, 0, (midX+dx)*drawScale.x, midY*drawScale.y, rotation, scale, scale);
                }
                canvas.draw(bottom, Color.WHITE, 0, 0, (botX+dx)*drawScale.x, botY*drawScale.y, rotation, scale, scale);
            }
        } else {
            if (angle == Direction.RIGHT) {
                rotation = (float) Math.PI * 3/2;
                topY = y - (height+shrink)/2f + 1;
                topX = x + midHeight - width/2f;
                midX = x - width/2f;
            } else {
                rotation = (float) Math.PI / 2;
                topY = y - (height+shrink)/2f;
                topX = x - midHeight + width/2f;
                midX = x + width/2f;
            }
            botY = topY;
            midY = topY;
            botX = midX;
            for (float dy = 0; dy < height; dy++){
                if (isActive()) {
                    canvas.draw(top, Color.WHITE, 0, 0, topX * drawScale.x, (topY + dy) * drawScale.y, rotation, scale, scale);
                    canvas.draw(middle, Color.WHITE, 0, 0, midX * drawScale.x, (midY + dy) * drawScale.y, rotation, scale, scale);
                }
                canvas.draw(bottom, Color.WHITE, 0, 0, botX*drawScale.x, (botY+dy)*drawScale.y, rotation, scale, scale);
            }
        }

    }

    @Override
    public void drawDebug(GameCanvas canvas){
        super.drawDebug(canvas);
        cap.drawDebug(canvas);
    }

    @Override
    public Vector2 getLinearVelocity(){return Vector2.Zero;}

    /**
     * Sets the shared constants for all instances of this class/
     * @param constants JSON storing the shared constants.
     */
    public static void setConstants(JsonValue constants) {
        objectConstants = constants;
        shrink = constants.getFloat("shrink");
        capSize = constants.getFloat("cap_size");
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
