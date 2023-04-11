package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.game.CollisionController;

/**
 * Represents an obstacle that can be moved. Used for grabbing and moving platforms. All implementations of this
 * interface should create a ground sensor for adding fixtures to the ground fixture set.
 */
public interface Movable {

    /**
     * If this obstacle is moveable. This is necessary because some obstacles such as flamethrowers are not
     * always moveable.
     * @return  true if obstacle is moveable
     */
    boolean isMovable();

    /**
     * Get the fixtures that represent the ground for this Movable. This allows {@link CollisionController} to adjust
     * a Movable's velocity for the velocity of its ground (i.e. moving platforms).
     * @return  fixture set that make up the ground
     */
    ObjectSet<Fixture> getGroundFixtures();

    /**
     * Returns the name of the ground sensor
     *
     * This is used by ContactListener
     *
     * @return the name of the ground sensor
     */
    String getGroundSensorName();

}
