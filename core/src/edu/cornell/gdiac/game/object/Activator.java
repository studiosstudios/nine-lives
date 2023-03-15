package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.*;

public abstract class Activator extends PolygonObstacle {

    protected static JsonValue objectConstants;

    /** if the activator is activating objects*/
    protected boolean active;
    /** each activator has a unique string id specified in JSON*/
    protected String id;
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

    public Activator(TextureRegion texture, Vector2 scale, JsonValue data){
        super(objectConstants.get("body_shape").asFloatArray());

        setBodyType(BodyDef.BodyType.StaticBody);
        setDrawScale(scale);
        setTexture(texture);
        setFixedRotation(true);

        id = data.getString("id");
        setX(data.get("pos").getFloat(0)+objectConstants.get("offset").getFloat(0));
        setY(data.get("pos").getFloat(1)+objectConstants.get("offset").getFloat(1));
        active = false;
    }

    @Override
    public void draw(GameCanvas canvas){
        canvas.draw(texture, Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.x,getAngle(),1,1);
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
