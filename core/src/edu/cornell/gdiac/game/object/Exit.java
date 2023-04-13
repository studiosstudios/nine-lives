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

import java.util.HashMap;

/**
 * A simple BoxObstacle that represents an exit (goal or return) of a level.
 */
public class Exit extends BoxObstacle {

    /** Enum representing the two types of exits. */
    public enum ExitType {GOAL, RETURN}
    /** The exit type of this exit */
    private ExitType exitType;
    /** Constants that are shared between all instances of this class */
    private static JsonValue objectConstants;

    /**
     * Creates a new exit. Note that currently ALL exits have the same width and height, as that is read
     * from the objectConstants JSON.
     * @param scale The draw scale.
     * @param data  JSON data for loading.
     */
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

    public Exit(ObjectMap<String, Object> properties, Vector2 scale, int tileSize, int levelHeight){
        super((float) properties.get("width")/tileSize, (float) properties.get("height")/tileSize);
        setBodyType(BodyDef.BodyType.StaticBody);
        setDensity(0);
        setFriction(0);
        setRestitution(0);
        setSensor(true);
        setX((float) properties.get("x")/tileSize + getDimension().x/2);
        setY(levelHeight - (float) properties.get("y")/tileSize - getDimension().y/2);
        setDrawScale(scale);
        if (((properties.get("type", "goal")).equals("goal"))){
            exitType = ExitType.GOAL;
        } else {
            exitType = ExitType.RETURN;
        }
        System.out.println(getPosition());
        setName((String) properties.get("type"));
    }

    @Override
    public void drawDebug(GameCanvas canvas){
        float xTranslate = (canvas.getCamera().getX()-canvas.getWidth()/2)/drawScale.x;
        float yTranslate = (canvas.getCamera().getY()-canvas.getHeight()/2)/drawScale.y;
        switch (exitType){
            case GOAL:
                canvas.drawPhysics(shape, Color.GREEN,getX()-xTranslate,getY()-yTranslate,getAngle(),drawScale.x,drawScale.y);
                break;
            case RETURN:
                canvas.drawPhysics(shape,Color.BLUE,getX()-xTranslate,getY()-yTranslate,getAngle(),drawScale.x,drawScale.y);
                break;
        }
    }

    /**
     * Sets the shared constants for all instances of this class/
     * @param constants JSON storing the shared constants.
     */
    public static void setConstants(JsonValue constants){ objectConstants = constants; }

    /**
     * @return The exit type of this exit.
     */
    public ExitType exitType() {return exitType;}
}
