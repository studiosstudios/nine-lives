package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;

public class Exit extends BoxObstacle {

    public enum ExitType {GOAL, RETURN}

    private ExitType exitType;

    private static JsonValue objectConstants;
    public Exit(Vector2 scale, JsonValue data){
        super(objectConstants.getFloat("width"), data.getFloat("height"));
        setBodyType(BodyDef.BodyType.StaticBody);
        setDensity(0);
        setFriction(0);
        setRestitution(0);
        setSensor(true);
        setY(data.get("pos").getFloat(1));
        setDrawScale(scale);
        switch (data.getString("type")){
            case "return":
                exitType = ExitType.RETURN;
                setX(data.get("pos").getFloat(0) - (1.5f * objectConstants.getFloat("width")));
                break;
            case "goal":
                exitType = ExitType.GOAL;
                setX(data.get("pos").getFloat(0) + (1.5f * objectConstants.getFloat("width")));
                break;
            default:
                throw new RuntimeException("unrecognized exit type");
        }
        setName(data.getString("type"));
    }

    @Override
    public boolean activatePhysics(World world){
        if (!super.activatePhysics(world)) {
            return false;
        }
        body.setUserData(this);
        return true;
    }

    @Override
    public void drawDebug(GameCanvas canvas){
        switch (exitType){
            case GOAL:
                canvas.drawPhysics(shape, Color.GREEN,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
                break;
            case RETURN:
                canvas.drawPhysics(shape,Color.BLUE,getX(),getY(),getAngle(),drawScale.x,drawScale.y);
                break;
        }
    }
    public static void setConstants(JsonValue constants){ objectConstants = constants; }
    public ExitType exitType() {return exitType;}
}
