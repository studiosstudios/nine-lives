/*
 * Particle.java
 *
 * Simple particle object.
 *
 * This class is a lot like the Photon objects from the game labs.  The main
 * difference is that it implements the Poolable interface.  That allows us
 * to use this object with a LibGDX Pool object for intelligent allocation.
 *
 * Author: Walker M. White
 * Version: 2/24/2015
 */
package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;

/**
 * Instances are a single particle.
 *
 * A particle just has a position and an angle.  Velocity magnitude is constant,
 * so movement is always determined by the angle of movement.  The particle
 * moves out radially along that angle from the starting position.
 */
public class Particle implements Pool.Poolable {
    /** How many pixels the particle moves per animation frame */
    public static final float PARTICLE_SPEED = 2.0f;

    /** The particle position */
    private Vector2 position;

    /** The particle velocity (not directly accessible) */
    private Vector2 velocity;

    /** The particle angle of movement (according to initial position) */
    private float angle;

    /**
     * Returns the position of this particle.
     *
     * The object returned is a reference to the position vector.  Therefore,
     * changes to this vector are reflected in the particle animation.
     *
     * @return the position of this particle
     */
    public Vector2 getPosition() {
        return position;
    }

    /**
     * Returns the x-coordinate of the particle position.
     *
     * @return the x-coordinate of the particle position
     */
    public float getX() {
        return position.x;
    }

    /**
     * Sets the x-coordinate of the particle position.
     *
     * @param x  the x-coordinate of the particle position
     */
    public void setX(float x) {
        position.x = x;
    }

    /**
     * Returns the y-coordinate of the particle position.
     *
     * @return the y-coordinate of the particle position
     */
    public float getY() {
        return position.y;
    }

    /**
     * Sets the y-coordinate of the particle position.
     *
     * @param y  the y-coordinate of the particle position
     */
    public void setY(float y) {
        position.y = y;
    }

    /**
     * Returns the angle of this particle
     *
     * The particle velocity is (PARTICLE_SPEED,angle) in polar-coordinates.
     *
     * @return the angle of this particle
     */
    public float getAngle() {
        return angle;
    }

    /**
     * Sets the angle of this particle
     *
     * When the angle is set, the particle will change its velocity
     * to (PARTICLE_SPEED,angle) in polar-coordinates.
     *
     * @param angle  the angle of this particle
     */
    public void setAngle(float angle) {
        this.angle = angle;
        velocity.set((float)(PARTICLE_SPEED*Math.cos(angle)),
                (float)(PARTICLE_SPEED*Math.sin(angle)));
    }

    /**
     * Creates a new (unitialized) Particle.
     *
     * The position and velocity are initially 0.  To initialize
     * the particle, use the appropriate setters.
     */
    public Particle() {
        position = new Vector2();
        velocity = new Vector2();
    }

    /**
     * Move the particle one frame, adding the velocity to the position.
     */
    public void move() {
        position.add(velocity);
    }

    /**
     * Resets the particle as if it were just allocated.
     *
     * This method is used by the Pool class to reuse a previously
     * allocated object.
     */
    public void reset() {
        position.set(0,0);
        velocity.set(0,0);
        angle = 0;
    }

}