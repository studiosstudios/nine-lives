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
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;
import edu.cornell.gdiac.game.obstacle.ComplexObstacle;

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
    /** If this flamethrower can be pushed */
    private final boolean pushable;


    /** Creates a new Flamethrower object.
     * @param flamebaseTexture   TextureRegion of flamethrower base.
     * @param flameBaseScale     Texture scale for the flamethrower base.
     * @param flameTexture       TextureRegion of flame.
     * @param flameScale         Texture scale for the flame base.
     * @param drawScale          Draw scale for drawing.
     * @param data               JSON data for loading.
     */
    public Flamethrower(TextureRegion flamebaseTexture, Vector2 flameBaseScale, TextureRegion flameTexture, Vector2 flameScale, Vector2 drawScale, JsonValue data) {
        super();
//        setName("flamethrower");

        this.flameTexture = flameTexture;

        flameBase = new BoxObstacle(flamebaseTexture.getRegionWidth()/drawScale.x*flameBaseScale.x, flamebaseTexture.getRegionHeight()/drawScale.y*flameBaseScale.y);
        flameBase.setDrawScale(drawScale);
        flameBase.setTextureScale(flameBaseScale);
        flameBase.setTexture(flamebaseTexture);
        pushable = data.getBoolean("pushable", false);
        flameBase.setFriction(objectConstants.getFloat("friction", 0));
        flameBase.setRestitution(objectConstants.getFloat("restitution", 0));
        flameBase.setDensity(objectConstants.getFloat("density", 0));
        flameBase.setMass(objectConstants.getFloat("mass", 0));
        flameBase.setName("flamethrower");
        float angle = (float) (data.getFloat("angle") * Math.PI/180);
        flameBase.setAngle(angle);
        flameBase.setX(data.get("pos").getFloat(0) + objectConstants.get("base_offset").getFloat(0));
        flameBase.setY(data.get("pos").getFloat(1) + objectConstants.get("base_offset").getFloat(1));

        flameOffset = new Vector2(objectConstants.get("flame_offset").getFloat(0)*(float)Math.cos(angle)-
                objectConstants.get("flame_offset").getFloat(1)*(float)Math.sin(angle),
                objectConstants.get("flame_offset").getFloat(1)*(float)Math.cos(angle)-
                objectConstants.get("flame_offset").getFloat(0)*(float)Math.sin(angle));
        flame = new Flame(flameTexture, drawScale, flameBase.getPosition(), flameBase.getAngle());
        flame.setTextureScale(flameScale);

        if (pushable){
            flame.setBodyType(BodyDef.BodyType.DynamicBody);
            flameBase.setBodyType(BodyDef.BodyType.DynamicBody);
        } else {
            flame.setBodyType(BodyDef.BodyType.StaticBody);
            flameBase.setBodyType(BodyDef.BodyType.StaticBody);
        }

        bodies.add(flameBase);
        bodies.add(flame);
        initActivations(data);
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
    public boolean getInitialActivation() { return initialActivation; }
    //endregion

    /**
     * Sets the shared constants for all instances of this class/
     * @param constants JSON storing the shared constants.
     */
    public static void setConstants(JsonValue constants) { objectConstants = constants; }

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
        public Flame(TextureRegion texture, Vector2 scale, Vector2 pos, float angle) {
            super(42/scale.x, 74/scale.y);
            int spriteWidth = 42;
            int spriteHeight = 74;
            spriteFrames = TextureRegion.split(texture.getTexture(), spriteWidth, spriteHeight);
            float frameDuration = 0.1f;
            animation = new Animation<>(frameDuration, spriteFrames[0]);
            animation.setPlayMode(Animation.PlayMode.LOOP);
            animationTime = 0f;
            setAngle(angle);
            setMass(0);
            setName("flame");
            setDrawScale(scale);
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
