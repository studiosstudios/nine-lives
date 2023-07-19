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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import edu.cornell.gdiac.game.GameCanvas;

/**
 * Instances are a single particle.
 *
 * A particle just has a position and an angle.  Velocity magnitude is constant,
 * so movement is always determined by the angle of movement.  The particle
 * moves out radially along that angle from the starting position.
 */
public class Particle implements Pool.Poolable {
    /** How many pixels the particle moves per animation frame */
    public static final float PARTICLE_SPEED = 0.12f;

    /** The particle position */
    private Vector2 position;

    /** The particle velocity (not directly accessible) */
    private Vector2 velocity;

    /** The particle angle of movement (according to initial position) */
    private float angle;
    /** For spirit region particles, the y value that this particle disappears at */
    private float top;
    /** For spirit region particles, the y value that this particle spawned at */
    private float bottom;
    /** Texture to draw */
    private Texture texture;
    /** Color to tint texture */
    private Color color = new Color(Color.WHITE);
    /** Draw scale for converting from game to screen coordinates */
    private Vector2 drawScale = new Vector2();
    /** Origin to center texture drawing in texture coordinates */
    private Vector2 textureOrigin = new Vector2();
    /** Width of particle in pixels */
    private float width;
    /** Height of particle in pixels */
    private float height;
    /** For body recombination particles, true if fully faded in */
    private boolean fadedIn;

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
     * Sets the angle of this particle
     *
     * When the angle is set, the particle will change its velocity
     * to (PARTICLE_SPEED,angle) in polar-coordinates.
     *
     * @param angle  the angle of this particle
     */
    public void setAngle(float angle, float speed) {
        this.angle = angle;
        velocity.set((float)(speed*Math.cos(angle)),
                (float)(speed*Math.sin(angle)));
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

    /**
     * Stores the initial y value of a spirit region particle
     *
     * @param bottom  initial y value
     */
    public void setBottom(float bottom) { this.bottom = bottom; }

    /**
     * Stores the final y value of a spirit region particle
     *
     * @param top     final y value
     */
    public void setTop(float top) { this.top = top; }

    /**
     * @return  initial y value of a spirit region particle
     */
    public float getBottom() { return bottom; }

    /**
     * @return  final y value of a spirit region particle
     */
    public float getTop() { return top; }

    /**
     * @return texture tint color for this particle when drawing
     */
    public Color getColor() { return color; }

    /**
     * Sets the texture for this particle, and updates the texture origin, width and height accordingly.
     *
     * @param texture   texture to draw
     */
    public void setTexture(Texture texture){
        this.texture = texture;
        setWidth(texture.getWidth());
        setHeight(texture.getHeight());
    }

    /**
     * @param scale  new drawscale
     */
    public void setDrawScale(Vector2 scale){ drawScale.set(scale); }

    /**
     * Sets the width of the texture to draw in pixels.
     *
     * @param width    width of texture to draw in pixels
     */
    public void setWidth(float width) {
        this.width = width;
        textureOrigin.x = width/2f;
    }

    /**
     * Sets the height of the texture to draw in pixels.
     *
     * @param height    height of texture to draw in pixels
     */
    public void setHeight(float height) {
        this.height = height;
        textureOrigin.y = height/2f;
    }

    /**
     * Sets texture tinting color
     *
     * @param color     texture tint color when drawing this particle
     */
    public void setColor(Color color) { this.color.set(color); }

    /**
     * Sets texture tinting color with RGBA values.
     */
    public void setColor(float r, float g, float b, float a) { this.color.set(r, g, b, a); }

    /**
     * Gets whether or not this particle has finished fading in. This is used for body recombination spirits.
     *
     * @return true if the particle has finished fading in.
     */
    public boolean isFadedIn() { return fadedIn; }

    /**
     * Sets whether or not this particle has finished fading in. This is used for body recombination spirits.
     *
     * @param faded  true if the particle has finished fading in.
     */
    public void setFadedIn(boolean faded) { fadedIn = faded; }

    /**
     * Draws this particle to the canvas.
     *
     * @param canvas canvas to draw to
     */
    public void draw(GameCanvas canvas) {
        canvas.draw(texture, color, textureOrigin.x, textureOrigin.y,getX()*drawScale.x, getY()*drawScale.y, width, height);
    }
}