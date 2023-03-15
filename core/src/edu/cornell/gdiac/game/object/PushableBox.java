package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;

public class PushableBox extends BoxObstacle implements Activatable {

    protected static JsonValue objectConstants;
    private boolean activated;

    private boolean initialActivation;

    public static void setConstants(JsonValue constants) { objectConstants = constants; }

    public PushableBox(TextureRegion texture, Vector2 scale, JsonValue data){
        super(texture.getRegionWidth()/scale.x,
                texture.getRegionHeight()/scale.y);

        setBodyType(BodyDef.BodyType.DynamicBody);
        setFixedRotation(true);
        setName("box");
        setDrawScale(scale);
        setTexture(texture);;


        setFriction(data.getFloat("friction", 0));
        setDensity(data.getFloat("density", 0));
        setMass(data.getFloat("mass", 0));
        setX(data.get("pos").getFloat(0)+objectConstants.get("offset").getFloat(0));
        setY(data.get("pos").getFloat(1)+objectConstants.get("offset").getFloat(1));

        initActivations(data);
    }

    @Override
    public void activated(World world){
        setBodyType(BodyDef.BodyType.KinematicBody);
    }

    @Override
    public void deactivated(World world){
        setBodyType(BodyDef.BodyType.DynamicBody);
    }

    @Override
    public void setActivated(boolean activated) {this.activated = activated;}

    @Override
    public boolean getActivated() { return activated; }

    @Override
    public void setInitialActivation(boolean initialActivation){ this.initialActivation = initialActivation; }

    @Override
    public boolean getInitialActivation() { return initialActivation; }
}
