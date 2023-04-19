package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;
import edu.cornell.gdiac.game.obstacle.ComplexObstacle;

import java.util.HashMap;

public class Flamethrower extends ComplexObstacle implements Activatable, Movable {
    /** Constants that are shared between all instances of this class*/
    private static JsonValue objectConstants;
    /** The flame object of this flamethrower*/
    private Flame flame;
    /** Texture of the flame */
    private TextureRegion flameTexture;
    /** The base of the flamethrower */
    private BoxObstacle flameBase;
    /** Current activation state */
    private boolean activated;
    /** Starting activation state */
    private boolean initialActivation;
    private final Vector2 flameOffset;
    /** If this flamethrower can be pushed */
    private final boolean pushable;

    private ObjectSet<Fixture> groundFixtures = new ObjectSet<>();
    private PolygonShape groundSensorShape;
    private final String groundSensorName;

    /**
     * Creates a new Flamethrower object.
     *
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param drawScale      Draw scale for drawing
     * @param textureScale   Texture scale for rescaling texture
     */
    public Flamethrower(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 drawScale, Vector2 textureScale) {
        super();


        this.flameTexture = tMap.get("flame_anim");

        flameBase = new BoxObstacle(tMap.get("flamethrower").getRegionWidth()/drawScale.x*textureScale.x, tMap.get("flamethrower").getRegionHeight()/drawScale.y*textureScale.y);
        setDrawScale(drawScale);
        flameBase.setDrawScale(drawScale);
        flameBase.setTextureScale(textureScale);
        flameBase.setTexture(tMap.get("flamethrower"));
        pushable = (boolean) properties.get("pushable", false);
        flameBase.setFriction(objectConstants.getFloat("friction", 0));
        flameBase.setRestitution(objectConstants.getFloat("restitution", 0));
        flameBase.setDensity(objectConstants.getFloat("density", 0));
        flameBase.setMass(objectConstants.getFloat("mass", 0));
        flameBase.setName("flamethrower");
        float angle = (float) ((float) properties.get("rotation") * Math.PI/180);
        flameBase.setAngle(angle);
        flameBase.setX((float) properties.get("x")+objectConstants.get("base_offset").getFloat(0));
        flameBase.setY((float) properties.get("y")+objectConstants.get("base_offset").getFloat(1));

        flameOffset = new Vector2(objectConstants.get("flame_offset").getFloat(0)*(float)Math.cos(angle)-
                objectConstants.get("flame_offset").getFloat(1)*(float)Math.sin(angle),
                objectConstants.get("flame_offset").getFloat(1)*(float)Math.cos(angle)-
                        objectConstants.get("flame_offset").getFloat(0)*(float)Math.sin(angle));
        flame = new Flame(flameTexture, drawScale, flameBase.getPosition(), flameBase.getAngle(),textureScale);

        if (pushable){
            flame.setBodyType(BodyDef.BodyType.DynamicBody);
            flameBase.setBodyType(BodyDef.BodyType.DynamicBody);
        } else {
            flame.setBodyType(BodyDef.BodyType.StaticBody);
            flameBase.setBodyType(BodyDef.BodyType.StaticBody);
        }


        bodies.add(flameBase);
        bodies.add(flame);
        groundSensorName = "flameBaseGroundSensor";
        initTiledActivations(properties);
    }

    /**
     * Welds the flame to the flamethrower base.
     *
     * @param world Box2D world to store joints
     * @return true if object allocation succeeded
     */
    @Override
    protected boolean createJoints(World world) {
        assert bodies.size > 0;

        WeldJointDef jointDef = new WeldJointDef();

        jointDef.bodyA = flameBase.getBody();
        jointDef.bodyB = flame.getBody();
        jointDef.localAnchorB.set(flame.getBody().getLocalPoint(flameBase.getPosition()));
        jointDef.collideConnected = false;
        Joint joint = world.createJoint(jointDef);
        joints.add(joint);

        return true;
    }

    /**
     * Creates the physics body for this object, adding them to the world. Immediately deactivates
     * self if necessary.
     * @param world Box2D world to store body
     *
     * @return      true if object allocation succeeded
     */
    public boolean activatePhysics(World world) {
        if (!super.activatePhysics(world)) {
            return false;
        }
        if (!activated){
            deactivated(world);
        }

        //ground sensor
        FixtureDef sensorDef = new FixtureDef();
        sensorDef.friction = 0;
        sensorDef.isSensor = true;
        Vector2 location = new Vector2(0, -flameBase.getDimension().y/2f);
        groundSensorShape = new PolygonShape();
        groundSensorShape.setAsBox(flameBase.getDimension().x/2.5f, 0.1f, location, 0.0f);
        sensorDef.shape = groundSensorShape;

        flameBase.getBody().setUserData(this);
        Fixture sensorFixture = flameBase.getBody().createFixture( sensorDef );
        sensorFixture.setUserData(groundSensorName);
        return true;
    }

    /** Turns on flames */
    @Override
    public void activated(World world){
        flame.setActive(true);
        flame.setPosition(flameBase.getX()+flameOffset.x, flameBase.getY()+flameOffset.y);
        createJoints(world);
    }

    /** Turns off flames */
    @Override
    public void deactivated(World world){
        flame.setActive(false);
        for (Joint j : joints){
            world.destroyJoint(j);
        }
        joints.clear();
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

    /**
     * Sets the shared constants for all instances of this class/
     * @param constants JSON storing the shared constants.
     */
    public static void setConstants(JsonValue constants) { objectConstants = constants; }

    public boolean isMovable() {return pushable;}

    public ObjectSet<Fixture> getGroundFixtures() { return groundFixtures; }

    public String getGroundSensorName(){ return groundSensorName; }

    public ObjectMap<String, Object> storeState(){
        ObjectMap<String, Object> stateMap = new ObjectMap<>();
        stateMap.put("basePosition", flameBase.getPosition().cpy());
        stateMap.put("flamePosition", flame.getPosition().cpy());
        stateMap.put("relativeVelocity", relativeVelocity.cpy());
        stateMap.put("baseVelocity", baseVelocity.cpy());
        stateMap.put("linearVelocity", getLinearVelocity().cpy());
        return stateMap;
    }

    public void loadState(ObjectMap<String, Object> stateMap){
        flameBase.setPosition((Vector2) stateMap.get("basePosition"));
        flame.setPosition((Vector2) stateMap.get("flamePosition"));
        setLinearVelocity((Vector2) stateMap.get("linearVelocity"));
        relativeVelocity.set((Vector2) stateMap.get("relativeVelocity"));
        baseVelocity.set((Vector2) stateMap.get("baseVelocity"));
        markDirty(true);
    }

    public void drawDebug(GameCanvas canvas) {
        super.drawDebug(canvas);
        float xTranslate = (canvas.getCamera().getX()-canvas.getWidth()/2)/drawScale.x;
        float yTranslate = (canvas.getCamera().getY()-canvas.getHeight()/2)/drawScale.y;
        canvas.drawPhysics(groundSensorShape,Color.RED,flameBase.getX()-xTranslate, flameBase.getY()-yTranslate,getAngle(),drawScale.x,drawScale.y);
    }

    /**
     * Represents a flame that a flamethrower can produce.
     */
    public class Flame extends BoxObstacle {

        /** The shape of the hitbox that will kill the player */
        private PolygonShape sensorShape;
        /** The frames of the flame animation */
        private TextureRegion[][] spriteFrames;
        /** How long the flame has been animating */
        private float animationTime;
        /** Filmstrip of flame animation */
        private Animation<TextureRegion> animation;

        /**
         * Creates a new Flame object.
         * @param texture Filmstrip texture region.
         * @param scale   Draw scale.
         * @param pos     Position of flamethrower base.
         * @param angle   Angle of flame.
         */
        public Flame(TextureRegion texture, Vector2 scale, Vector2 pos, float angle, Vector2 textureScale) {
            super(texture.getRegionWidth()/scale.x*textureScale.x/7, texture.getRegionHeight()/scale.y*textureScale.y);
            int spriteWidth = 2048;
            int spriteHeight = 4096;
            spriteFrames = TextureRegion.split(texture.getTexture(), spriteWidth, spriteHeight);
            float frameDuration = 0.1f;
            animation = new Animation<>(frameDuration, spriteFrames[0]);
            animation.setPlayMode(Animation.PlayMode.LOOP);
            animationTime = 0f;
            setAngle(angle);
            setMass(0);
            setName("flame");
            setDrawScale(scale);
            setTextureScale(textureScale);
            setSensor(true);
            setX(pos.x + flameOffset.x);
            setY(pos.y + flameOffset.y);

        }

        /** Creates hitbox sensor. The hitbox is smaller than the texture itself. */
        public boolean activatePhysics(World world) {
            if (!super.activatePhysics(world)) {
                return false;
            }
            FixtureDef sensorDef = new FixtureDef();
            sensorDef.density = 0;
            sensorDef.isSensor = true;
            sensorShape = new PolygonShape();
            sensorShape.set(objectConstants.get("sensor_shape").asFloatArray());
            sensorDef.shape = sensorShape;

            Fixture sensorFixture = body.createFixture( sensorDef );
            sensorFixture.setUserData(this);
            return true;
        }

        @Override
        public void drawDebug(GameCanvas canvas) {
            super.drawDebug(canvas);
            float xTranslate = (canvas.getCamera().getX()-canvas.getWidth()/2)/drawScale.x;
            float yTranslate = (canvas.getCamera().getY()-canvas.getHeight()/2)/drawScale.y;
            canvas.drawPhysics(sensorShape,Color.RED,getX()-xTranslate,getY()-yTranslate,getAngle(),drawScale.x,drawScale.y);
        }

        @Override
        public void draw(GameCanvas canvas){
            if (isActive()){
                animationTime += Gdx.graphics.getDeltaTime();
                setTexture(animation.getKeyFrame(animationTime));
                super.draw(canvas);
            }
        }

    }

}
