package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.game.GameCanvas;

/**
 * Represents the line drawn between the player and a dead body when in spirit mode. The endpoints of a spirit line
 * are dynamically updated to move towards two target points, creating a simple animation effect.
 */
public class SpiritLine {
    /** start point of the line */
    private Vector2 start = new Vector2();
    /** end point of the line */
    private Vector2 end = new Vector2();
    /** target for the start point */
    public Vector2 startTarget = new Vector2();
    /** target for the end point */
    public Vector2 endTarget = new Vector2();
    /** outer color of the line */
    private Color outerColor = new Color();
    /** inner color of the line */
    private Color innerColor = new Color();
    /** alpha of line colors */
    private float alpha;
    /** draw scale */
    private Vector2 scale;
    /** How fast the points move toward their targets. Must be >0 and <1.
     * A <code>MOVE_SPEED</code> of 1 will instantaneously set positions to their
     * targets, and a <code>MOVE_SPEED</code> of 0 will never reach targets. */
    private static final float MOVE_SPEED = 0.2f;
    /** The alpha target value when in spirit mode */
    private static final float TARGET_ALPHA = 0.6f;
    /** How fast line fades in when exiting spirit mode */
    private static final float FADE_IN_SPEED = 0.05f;
    /** How fast line fades out when exiting spirit mode */
    private static final float FADE_OUT_SPEED = 0.2f;
    /** The thickness of the line */
    private static final float THICKNESS = 4;

    /**
     * Creates a new spirit line
     * @param ic     inner color of the line
     * @param oc     outer color of the line
     * @param scale  draw scale
     */
    public SpiritLine(Color ic, Color oc, Vector2 scale){
        innerColor.set(ic);
        outerColor.set(oc);
        this.scale = scale;
        alpha = 0;
    }

    /**
     * Updates the color, start and end position of the line.
     * @param dt           Number of seconds since last animation frame
     * @param spiritMode   true if level is in spirit mode
     */
    public void update(float dt, boolean spiritMode){
        if (spiritMode) {
            alpha += (TARGET_ALPHA - alpha) * FADE_IN_SPEED;
        } else {
            alpha -= alpha * FADE_OUT_SPEED;
        }
        start.add((startTarget.x-start.x)*MOVE_SPEED, (startTarget.y-start.y)*MOVE_SPEED);
        end.add((endTarget.x-end.x)*MOVE_SPEED, (endTarget.y-end.y)*MOVE_SPEED);
        outerColor.set(outerColor.r, outerColor.g, outerColor.b, alpha);
        innerColor.set(innerColor.r, innerColor.g, innerColor.b, alpha);
    }

    /**
     * @param start   new start position of the line
     */
    public void setStart(Vector2 start){
        this.start.set(start);
    }

    /**
     * @param end     new end position of the line
     */
    public void setEnd(Vector2 end){
        this.end.set(end);
    }

    /**
     * Draws the line to the canvas
     * @param canvas   Canvas to draw to
     */
    public void draw(GameCanvas canvas){
        canvas.drawFactoryLine(start, end, THICKNESS/4, innerColor, scale.x, scale.y);
        canvas.drawFactoryLine(start, end, THICKNESS, outerColor, scale.x, scale.y);
    }

}
