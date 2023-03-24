package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;
import edu.cornell.gdiac.game.obstacle.PolygonObstacle;

public class Door extends PolygonObstacle implements Activatable {

    private boolean activated;
    private boolean initialActivation;
    private static JsonValue objectConstants;
    private float totalTicks;

    private int ticks;
    private Direction angle;
    private final float width;
    private final float height;

    /** 1 if closing, -1 if opening, 0 if static */
    private float closing;

    public Door(TextureRegion texture, Vector2 scale, float width, float height, JsonValue data){
        super(new float[]{0, 0, width, 0, width, height, 0, height});
        this.width = width;
        this.height = height;
        setTexture(texture);
        setDrawScale(scale);
        setBodyType(BodyDef.BodyType.StaticBody);
        setDensity(objectConstants.getFloat( "density", 0.0f ));
        setFriction(objectConstants.getFloat( "friction", 0.0f ));
        setRestitution(objectConstants.getFloat( "restitution", 0.0f ));

        angle = Direction.angleToDir(data.getInt("angle"));
        totalTicks = data.getFloat("totalTicks");
        ticks = (int) totalTicks;
        setX(data.get("pos").getFloat(0)+ objectConstants.get("offset").getFloat(0));
        setY(data.get("pos").getFloat(1)+ objectConstants.get("offset").getFloat(1));
        
        closing = 0;
        initActivations(data);
    }

    public Door(TextureRegion texture, Vector2 scale, JsonValue data){
        this(texture, scale, data.getFloat("width"), data.getFloat("height"), data);
    }

    public void update(float dt){
        super.update(dt);
        if (closing == 1){
            ticks++;
            if (ticks == totalTicks){
                setDimension(width , height,true);
                closing = 0;
            } else {
                switch (angle) {
                    case DOWN:
                        setY(getY() - height / totalTicks);
                    case UP:
                        setDimension(width, getHeight() + height / totalTicks, true);
                        break;
                    case LEFT:
                        setX(getX() - width / totalTicks);
                    case RIGHT:
                        setDimension(getWidth() + width / totalTicks, height, true);
                        break;
                }
            }
        } else if (closing == -1) {
            ticks--;
            if (ticks == 0){
                setActive(false);
                closing = 0;
            } else {
                switch (angle) {
                    case DOWN:
                        setY(getY() + height / totalTicks);
                    case UP:
                        setDimension(width, getHeight() - height / totalTicks, true);
                        break;
                    case LEFT:
                        setX(getX() + width / totalTicks);
                    case RIGHT:
                        setDimension(getWidth() - width / totalTicks, height, true);
                        break;
                }
            }
        }
    }
    public boolean activatePhysics(World world){
        if (!super.activatePhysics(world)) {
            return false;
        }
        if (!activated) {
            deactivated(world);
            setActive(false);
        }
        return true;
    }
    //TODO: close
    @Override
    public void activated(World world){
        closing = 1;
        setActive(true);
    }

    //TODO: open
    @Override
    public void deactivated(World world){
        closing = -1;
        setActive(true);
    }
    @Override
    public void setActivated(boolean activated){ this.activated = activated; }

    @Override
    public boolean getActivated() { return activated; }

    @Override
    public void setInitialActivation(boolean initialActivation){ this.initialActivation = initialActivation; }

    @Override
    public boolean getInitialActivation() { return initialActivation; }

    @Override
    public void draw(GameCanvas canvas){
        if (isActive()){
            super.draw(canvas);
        }
    }

    public static void setConstants(JsonValue constants) {objectConstants = constants;}
}
