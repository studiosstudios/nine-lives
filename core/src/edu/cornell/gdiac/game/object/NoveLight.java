package edu.cornell.gdiac.game.object;

import box2dLight.ChainLight;
import box2dLight.Light;
import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;
import edu.cornell.gdiac.game.obstacle.SimpleObstacle;
import edu.cornell.gdiac.util.Direction;
import java.util.HashMap;

public class NoveLight extends BoxObstacle implements Activatable {
    /** Constants that are shared between all instances of this class*/
    protected static JsonValue objectConstants;
    private boolean activated;
    private boolean initialActivation;


    /**
     * Creates a new NoveLight object.
     *
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param textureScale   Texture scale for rescaling texture
     */
    public NoveLight(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, Vector2 textureScale){
        super(tMap.get("ceiling-light").getRegionWidth() / scale.x * textureScale.x,
                tMap.get("ceiling-light").getRegionHeight() / scale.y * textureScale.y);

        setBodyType(BodyDef.BodyType.StaticBody);
        setName("lights");
        setDrawScale(scale);
        setTexture(tMap.get("ceiling-light"));
        setTextureScale(textureScale);

        setRestitution(objectConstants.getFloat("restitution", 0));
        setFriction(objectConstants.getFloat("friction", 0));
        setDensity(objectConstants.getFloat("density", 0));
        setMass(objectConstants.getFloat("mass", 0));
        Vector2 offset = new Vector2(objectConstants.get("offset").getFloat(0), objectConstants.get("offset").getFloat(1));
        setX((float) properties.get("x") + offset.x);
        setY((float) properties.get("y") + offset.y);
        setFixedRotation(true);
        initTiledActivations(properties);
    }

    /**
     * Creates PointLight for with soft and xray true
     * @param rayHandler Ray Handler associated with the currently active box2d world
     */
    public void createLight(RayHandler rayHandler) {
        createPointLight(objectConstants.get("light"), rayHandler);
        getLight().setSoft(true);
        getLight().setXray(true);
        Vector2 offset = new Vector2(objectConstants.get("light").get("offset").getFloat(0),objectConstants.get("light").get("offset").getFloat(1));
        getLight().setPosition(getBody().getPosition().cpy().add(offset));
        System.out.println(getLight().getPosition());
    }

    @Override
    public void activated(World world) {
        getLight().setActive(true);
    }

    @Override
    public void deactivated(World world) {
        getLight().setActive(false);
    }

    @Override
    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    @Override
    public boolean isActivated() { return activated; }

    @Override
    public boolean getInitialActivation() { return initialActivation; }

    @Override
    public void setInitialActivation(boolean initialActivation){ this.initialActivation = initialActivation; }

    @Override
    public float getXPos() {
        return getX();
    }

    public float getYPos() {
        return getY();
    }

    public void draw(GameCanvas canvas) {
        super.draw(canvas);
    }


    /**
     * Sets the shared constants for all instances of this class
     * @param constants JSON storing the shared constants.
     */
    public static void setConstants(JsonValue constants) {
        objectConstants = constants;
    }
}
