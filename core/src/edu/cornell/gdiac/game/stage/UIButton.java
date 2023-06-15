package edu.cornell.gdiac.game.stage;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Image;

/**
 * Represents a button that changes size depending on mouse input. Requires an InputListener to be added that calls
 * <code>enter()</code>, <code>exit()</code>, <code>touchDown</code> and <code>touchUp</code>.
 */
public class UIButton extends Image {

    /** Default speed that the button changes size */
    private static final float DEFAULT_SPEED = 0.9f;
    /** Default 'bounciness' of button */
    private static final float DEFAULT_BOUNCE = 0.93f;
    /** Button scale */
    private float scale;
    /** Rate at which scale is changing */
    private float scaleVel;
    /** Target value for the button scale */
    private float scaleTarget;
    /** Button scale target when not hovered/clicked */
    private final float baseScale;
    /** Button scale target when hovered/released */
    private final float hoverScale;
    /** Button scale target when clicked */
    private final float clickedScale;
    /** Speed that button changes size */
    private final float speed;
    /** Bounciness of button size changing */
    private final float bounciness;
    /** State of button */
    private State state;

    /**
     * Represents the state of the button.
     * <br><br>
     *  - Neutral: not clicked or hovered. <br>
     *  - Hovered: mouse is currently on top of button and mouse button is up. <br>
     *  - Clicked: mouse is currently on top of button and mouse button is down. <br>
     *  - Released: mouse button is up and button was previously clicked
     */
    public enum State {
        NEUTRAL,
        HOVERED,
        CLICKED,
        RELEASED
    }

    /**
     * @return True if this button has been clicked
     */
    public boolean isClicked() { return state == State.RELEASED && Math.abs(scale - scaleTarget) < 0.02f && Math.abs(scaleVel) < 0.02f; }

    /**
     * @return State of this button
     */
    public State getState() { return state; }

    /**
     * Resets this button to its initial state
     */
    public void reset(){
        setColor(Color.WHITE);
        state = State.NEUTRAL;
        scaleTarget = baseScale;
        scale = baseScale;
        scaleVel = 0;
    }

    /**
     * Creates a new UIButton with default speed and bounciness.
     *
     * @param texture       Texture of the button
     * @param x             x coordinate (of center) of button
     * @param y             y coordinate (of center) of button
     * @param clickedScale  scale of button when clicked
     * @param baseScale     scale of button when not hovered and when clicked
     * @param hoverScale    scale of button when hovered and when released
     */
    public UIButton(Texture texture, float x, float y, float baseScale, float hoverScale, float clickedScale) {
        this(texture, x, y, baseScale, hoverScale, clickedScale, DEFAULT_SPEED, DEFAULT_BOUNCE);
    }

    /**
     * Creates a new UIButton.
     *
     * @param texture       Texture of the button
     * @param x             x coordinate (of center) of button
     * @param y             y coordinate (of center) of button
     * @param baseScale     scale of button when not hovered and when clicked
     * @param hoverScale    scale of button when hovered and when released
     * @param clickedScale  scale of button when clicked
     * @param speed         speed of scale transitions. Must be >0 and <1
     * @param bounciness    'bounciness' of scale transitions. Must be >0 and <1
     */
    public UIButton(Texture texture, float x, float y, float baseScale, float hoverScale, float clickedScale, float speed, float bounciness) {
        super(texture);
        setOrigin(texture.getWidth()/2f,texture.getHeight()/2f);
        setPosition(x, y);
        setScale(baseScale);
        this.baseScale = baseScale;
        this.hoverScale = hoverScale;
        this.clickedScale = clickedScale;
        this.speed = speed;
        this.bounciness = bounciness;
        scale = baseScale;
        scaleTarget = scale;
        scaleVel = 0;
        state = State.NEUTRAL;
    }

    /**
     * Mouse has started hovering above this button.
     */
    public void enter() {
        if (state == State.NEUTRAL) {
            setColor(Color.LIGHT_GRAY);
            state = State.HOVERED;
            scaleTarget = hoverScale;
        }
    }

    /**
     * Mouse has stopped hovering above this button.
     */
    public void exit() {
        if (state == State.HOVERED) {
            setColor(Color.WHITE);
            state = State.NEUTRAL;
            scaleTarget = baseScale;
        }
    }

    /**
     * Mouse has been clicked on this button.
     */
    public void touchDown() {
        setColor(Color.LIGHT_GRAY);
        state = State.CLICKED;
        scaleTarget = clickedScale;
    }

    /**
     * Mouse has been released after clicking this button.
     */
    public void touchUp() {
        setColor(Color.LIGHT_GRAY);
        state = State.RELEASED;
        scaleTarget = hoverScale;
    }

    /**
     * Updates scale and scale velocity according to scale target.
     *
     * @param delta Time since last frame
     */
    public void update(float delta){
        scaleVel = scaleVel * bounciness + (scaleTarget - scale) * speed;
        scale += scaleVel * delta;
        setScale(scale);
    }

}
