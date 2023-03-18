package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.ComplexObstacle;

public class Laser extends ComplexObstacle {
    /** The initializing data (to avoid magic numbers) */
    private final JsonValue data;

    /** Whether the laser object is on and firing a beam */
    private boolean isFiring;

    protected LaserBeam beam;
    protected TextureRegion laserTexture;

    // Dimension information
    /** The size of the entire laser */
    protected Vector2 dimension;
    /** The size of a single section of laser */
    protected Vector2 sectionsize;
    /** The length of each link */
    protected float linksize = 1.0f;
    /** The spacing between each link */
    protected float spacing = 0.0f;

    /**
     * Returns true if the laser if firing
     *
     * @return true if the laser is firing
     */
    public boolean getFiring() {
        return isFiring;
    }
    /**
     * Sets whether the laser object is firing
     *
     * @param firing whether the laser is firing
     */
    public void setFiring(boolean firing) {
        isFiring = firing;
    }

    /**
     * Creates a new LaserBase
     *
     * The size is expressed in physics units NOT pixels.  In order for
     * drawing to work properly, you MUST set the drawScale. The drawScale
     * converts the physics units to pixels.
     *
     * @param x x position of the laser base
     * @param y y position of the laser base
     */
    public Laser(JsonValue data, float x, float y, float lwidth, float lheight, String name, TextureRegion laserTexture) {
        super(x,y);
        setName(name);
        this.data = data;
        this.laserTexture = laserTexture;
        beam = new LaserBeam(data, x,y ,9f, lwidth, lheight,name + "Beam");
        beam.setTexture(laserTexture);
        bodies.add(beam);

    }

    @Override
    protected boolean createJoints(World world) {
        return true;
    }

    /**
     * Updates the object's physics state (NOT GAME LOGIC).
     *
     * We use this method to reset cooldowns.
     *
     * @param dt	Number of seconds since last animation frame
     */
    public void update(float dt) {

    }
}
