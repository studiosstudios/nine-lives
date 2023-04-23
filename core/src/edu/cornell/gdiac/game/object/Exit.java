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
     * Creates a new Exit.
     *
     * @param properties     String-Object map of properties for this object
     * @param scale          Draw scale for drawing
     */
    public Exit(ObjectMap<String, Object> properties, Vector2 scale){
        super((float) properties.get("width"), (float) properties.get("height"));
        setBodyType(BodyDef.BodyType.StaticBody);
        setDensity(0);
        setFriction(0);
        setRestitution(0);
        setSensor(true);
        setX((float) properties.get("x") + getDimension().x/2);
        setY((float) properties.get("y") - getDimension().y/2);
        setDrawScale(scale);
        if (((properties.get("type", "goal")).equals("goal"))){
            exitType = ExitType.GOAL;
            setX(getX() + 1);
        } else {
            exitType = ExitType.RETURN;
            setX(getX() - 1);
        }
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
