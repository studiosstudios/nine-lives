package edu.cornell.gdiac.game.object;

import box2dLight.ConeLight;
import box2dLight.RayHandler;
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
import edu.cornell.gdiac.util.Direction;

import java.util.HashMap;

public class Flamethrower extends ComplexObstacle implements Activatable {
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
    public static final String flameSensorName = "flameSensor";
    private Direction dir;

    /**
     * Creates a new Flamethrower object.
     *
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param drawScale      Draw scale for drawing
     * @param textureScale   Texture scale for rescaling texture
     */
    public Flamethrower(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 drawScale, Vector2 textureScale, String biome) {
        super();


        this.flameTexture = tMap.get("flame-anim");

        flameBase = new BoxObstacle(tMap.get("flamethrower").getRegionWidth()/drawScale.x*textureScale.x,
                tMap.get("flamethrower").getRegionHeight()/drawScale.y*textureScale.y);
        setDrawScale(drawScale);
        flameBase.setDrawScale(drawScale);
        flameBase.setTextureScale(textureScale);
        flameBase.setTexture(biome.equals("metal") ? tMap.get("flamethrower") : tMap.get("forest-flamethrower"));
        flameBase.setFriction(objectConstants.getFloat("friction", 0));
        flameBase.setRestitution(objectConstants.getFloat("restitution", 0));
        flameBase.setDensity(objectConstants.getFloat("density", 0));
        flameBase.setMass(objectConstants.getFloat("mass", 0));
        flameBase.setName("flamethrower");
        float angle = (float) ((float) properties.get("rotation") * Math.PI/180);
        dir = Direction.angleToDir((int) ((float) properties.get("rotation")));
        flameBase.setAngle(angle);
        Vector2 offset = new Vector2(objectConstants.get("base_offset").getFloat(0), objectConstants.get("base_offset").getFloat(1));
        Direction.rotateVector(offset, dir);
        flameBase.setX((float) properties.get("x") + offset.x);
        flameBase.setY((float) properties.get("y") + offset.y);
        flameBase.setSensor((boolean) properties.get("baseSensor", false));



        flameOffset = new Vector2(objectConstants.get("flame_offset").getFloat(0),
                objectConstants.get("flame_offset").getFloat(1));
        Direction.rotateVector(flameOffset, dir);
        flame = new Flame(flameTexture, drawScale, flameBase.getPosition(), flameBase.getAngle(),textureScale);
        flameBase.setBodyType(properties.containsKey("attachName") ? BodyDef.BodyType.DynamicBody : BodyDef.BodyType.StaticBody);
        flame.setBodyType(flameBase.getBodyType());

        bodies.add(flameBase);
        bodies.add(flame);
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
        flameBase.setDimension(flameBase.getWidth() * objectConstants.get("solid_scale").getFloat(0),
                flameBase.getHeight() * objectConstants.get("solid_scale").getFloat(1), 0, -flameBase.getHeight()/2f, false);
        if (!activated){
            deactivated(world);
        }
        return true;
    }

    public void createLight(RayHandler rayHandler) {
        createPointLight(objectConstants.get("light"), rayHandler);
        getLight().attachToBody(flame.getBody());
        getLight().setXray(true);
        getLight().setSoft(true);
    }

    /** Turns on flames */
    @Override
    public void activated(World world){
        flame.setActive(true);
        flame.markDirty(true);
        getLight().setActive(true);
        flame.setPosition(flameBase.getX()+flameOffset.x, flameBase.getY()+flameOffset.y);
        createJoints(world);
    }

    /** Turns off flames */
    @Override
    public void deactivated(World world){
        flame.setActive(false);
        getLight().setActive(false);
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

    /**
     * Represents a flame that a flamethrower can produce.
     */
    public class Flame extends BoxObstacle {

        /** The shape of the hitbox that will kill the player */
        private PolygonShape sensorShape;
        /** Fixture of hitbox that will kill player */
        private Fixture sensorFixture;
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
            int spriteWidth = 128;
            int spriteHeight = 256;
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

            origin.set(objectConstants.get("flame_origin").getFloat(0), objectConstants.get("flame_origin").getFloat(1));

        }

        /** Creates hitbox sensor. The hitbox is smaller than the texture itself. */
        public boolean activatePhysics(World world) {
            if (!super.activatePhysics(world)) {
                return false;
            }
            return true;
        }

        @Override
        public void createFixtures(){
            super.createFixtures();
            body.getFixtureList().get(0).setUserData("");
            FixtureDef sensorDef = new FixtureDef();
            sensorDef.density = 0;
            sensorDef.isSensor = true;
            sensorShape = new PolygonShape();
            sensorShape.set(objectConstants.get("sensor_shape").asFloatArray());
            sensorDef.shape = sensorShape;

            sensorFixture = body.createFixture( sensorDef );
            sensorFixture.setUserData(flameSensorName);
        }

        @Override
        public void releaseFixtures(){
            super.releaseFixtures();
            if (sensorFixture != null) {
                body.destroyFixture(sensorFixture);
                sensorFixture = null;
            }
        }

        @Override
        public void drawDebug(GameCanvas canvas) {
            super.drawDebug(canvas);
            canvas.drawPhysics(sensorShape,Color.RED,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
        }

        @Override
        public void draw(GameCanvas canvas){
            if (isActive()){
                animationTime += Gdx.graphics.getDeltaTime();
                canvas.draw(animation.getKeyFrame(animationTime), Color.WHITE, origin.x, origin.y, getX()*drawScale.x, getY()*drawScale.y, getAngle(), textureScale.x,textureScale.y);
            }
        }

    }

}
