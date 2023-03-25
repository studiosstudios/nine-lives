package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;

/**
 * A Platform is a wall that can be moved.
 */
public class Platform extends Wall implements Activatable {
    /** Current activation state */
    private boolean activated;
    /** Starting activation state */
    private boolean initialActivation;
    /** The location of the platform when deactivated */
    private Vector2 startPoint;
    /** The location of the platform when activated */
    private Vector2 endPoint;

    /**
     * Creates a new platform object.
     * @param texture  TextureRegion for drawing.
     * @param scale    Draw scale for drawing.
     * @param data     JSON for loading.
     */
    public Platform(TextureRegion texture, Vector2 scale, JsonValue data) {
        super(texture, scale, data);
        setName("platform");
        try {
            startPoint = new Vector2(data.get("start").getFloat(0), data.get("start").getFloat(1));
        } catch (IllegalArgumentException e) {
            startPoint = new Vector2();
        }
        try {
            endPoint = new Vector2(data.get("end").getFloat( 0), data.get("end").getFloat( 1));
        } catch (IllegalArgumentException e) {
            endPoint = new Vector2();
        }
        setPosition(startPoint);

    }

    //TODO: move to endPoint
    @Override
    public void activated(World world){}

    //TODO: move to startPoint
    @Override
    public void deactivated(World world){}

    //region ACTIVATBLE METHODS
    @Override
    public void setActivated(boolean activated){ this.activated = activated; }

    @Override
    public boolean getActivated() { return activated; }

    @Override
    public void setInitialActivation(boolean initialActivation){ this.initialActivation = initialActivation; }

    @Override
    public boolean getInitialActivation() { return initialActivation; }
    //endregion

}
