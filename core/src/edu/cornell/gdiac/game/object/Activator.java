package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.graphics.g2d.Animation;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public abstract class Activator extends PolygonObstacle {

    protected static JsonValue objectConstants;
    protected Animation<TextureRegion> animation;

    /** if the activator is activating objects*/
    protected boolean active;
    /** each activator has a unique string id specified in JSON*/
    protected String id;
    protected SpriteBatch spriteBatch;
    private TextureRegion[][] spriteFrames;
    private float animationTime;
    private PolygonShape sensorShape;
    /** the number of objects pressing on this activator */
    public int numPressing;

    public boolean isActive(){ return active; }

    public boolean isPressed(){ return numPressing > 0; }

    public String getID(){ return id; }

    /** a new object is pressing the activator */
    public void addPress() { numPressing++; }

    /** an object has stopped pressing the activator */
    public void removePress() { numPressing--; }

    public abstract void updateActivated();

    public Activator(TextureRegion texture, TextureRegion texture2, Vector2 scale, JsonValue data){
        super(objectConstants.get("body_shape").asFloatArray());
        int spriteWidth = 32;
        int spriteHeight = 32;
        spriteFrames = TextureRegion.split(texture.getTexture(), spriteWidth, spriteHeight);
        float frameDuration = 0.2f;
        animation = new Animation<>(frameDuration, spriteFrames[0]);
        setBodyType(BodyDef.BodyType.StaticBody);
        animation.setPlayMode(Animation.PlayMode.REVERSED);
        spriteBatch = new SpriteBatch();
        animationTime = 0f;

        setDrawScale(scale);
        setTexture(texture2);
        setFixedRotation(true);

        id = data.getString("id");
        setX(data.get("pos").getFloat(0)+objectConstants.get("offset").getFloat(0));
        setY(data.get("pos").getFloat(1)+objectConstants.get("offset").getFloat(1));
        active = false;
    }

    @Override
    public void draw(GameCanvas canvas){
        if(isPressed()){
            animation.setPlayMode(Animation.PlayMode.REVERSED);
            animationTime += Gdx.graphics.getDeltaTime();
            TextureRegion currentFrame = animation.getKeyFrame(animationTime);
            spriteBatch.begin();
            spriteBatch.draw(currentFrame, getX()*drawScale.x,getY()*drawScale.x);
            spriteBatch.end();
        }
        else {
            animation.setPlayMode(Animation.PlayMode.NORMAL);
            animationTime += Gdx.graphics.getDeltaTime();
            TextureRegion currentFrame = animation.getKeyFrame(animationTime);
            spriteBatch.begin();
            spriteBatch.draw(currentFrame, getX()*drawScale.x,getY()*drawScale.x);
            spriteBatch.end();
        }
    }
    public boolean activatePhysics(World world){
        if (!super.activatePhysics(world)) {
            return false;
        }

        //create top sensor
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


    /**
     * Draws the outline of the physics body.
     *
     * This method can be helpful for understanding issues with collisions.
     *
     * @param canvas Drawing context
     */
    public void drawDebug(GameCanvas canvas) {
        super.drawDebug(canvas);
        canvas.drawPhysics(sensorShape,Color.RED,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
    }

    public static void setConstants(JsonValue constants) { objectConstants = constants; }
}
