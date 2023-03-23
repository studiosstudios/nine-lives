/*
 * InputController.java
 *
 * This class buffers in input from the devices and converts it into its
 * semantic meaning. If your game had an option that allows the player to
 * remap the control keys, you would store this information in this class.
 * That way, the main GameEngine does not have to keep track of the current
 * key mapping.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * LibGDX version, 2/6/2015
 */
package edu.cornell.gdiac.game;

import com.badlogic.gdx.*;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.*;

import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.util.*;

/**
 * Class for reading player input. 
 *
 * This supports both a keyboard and X-Box controller. In previous solutions, we only 
 * detected the X-Box controller on start-up.  This class allows us to hot-swap in
 * a controller via the new XBox360Controller class.
 */
public class InputController {
	/** The singleton instance of the input controller */
	private static InputController theController = null;
	
	/** 
	 * Return the singleton instance of the input controller
	 *
	 * @return the singleton instance of the input controller
	 */
	public static InputController getInstance() {
		if (theController == null) {
			theController = new InputController();
		}
		return theController;
	}
	
	// Fields to manage buttons
	/** Whether the reset button was pressed. */
	private boolean resetPressed;
	private boolean resetPrevious;
	/** Whether the jump action button was pressed. */
	private boolean jumpPressed;
	private boolean jumpPrevious;
	/** Whether the dash button was pressed. */
	private boolean dashPressed;
	/** Whether the meow button was pressed. */
	private boolean meowPressed;
	private boolean meowPrevious;
	private boolean dashPrevious;

	/** Whether the climb button was pressed. */
	private boolean climbPressed;

	/** Whether the secondary action button was pressed. */
	private boolean secondPressed;
	private boolean secondPrevious;
	/** Whether the teritiary action button was pressed. */
	private boolean tertiaryPressed;
	/** Whether the debug toggle was pressed. */
	private boolean debugPressed;
	private boolean debugPrevious;
	/** Whether the exit button was pressed. */
	private boolean exitPressed;
	private boolean exitPrevious;
	/** Whether the next button was pressed. */
	private boolean nextPressed;
	private boolean nextPrevious;
	/** Whether the previous button was pressed. */
	private boolean prevPressed;
	private boolean prevPrevious;

	/** Whether the body switch button was pressed. */
	private boolean switchPressed;
	private boolean switchPrevious;

	/** Whether the cancel button was pressed. */
	private boolean cancelPressed;
	private boolean cancelPrevious;

	/** if the cancel button was pressed */
	private boolean didCancel;
	/** if the switch has been cancelled */
	private boolean cancelled;
	
	/** How much did we move horizontally? */
	private float horizontal;
	/** How much did we move vertically? */
	private float vertical;
	/** The crosshair position (for raddoll) */
	private Vector2 crosshair;
	/** The crosshair cache (for using as a return value) */
	private Vector2 crosscache;
	
	/**
	 * Returns the amount of sideways movement. 
	 *
	 * -1 = left, 1 = right, 0 = still
	 *
	 * @return the amount of sideways movement. 
	 */
	public float getHorizontal() {
		return horizontal;
	}
	
	/**
	 * Returns the amount of vertical movement. 
	 *
	 * -1 = down, 1 = up, 0 = still
	 *
	 * @return the amount of vertical movement. 
	 */
	public float getVertical() {
		return vertical;
	}
	
	/**
	 * Returns the current position of the crosshairs on the screen.
	 *
	 * This value does not return the actual reference to the crosshairs position.
	 * That way this method can be called multiple times without any fair that 
	 * the position has been corrupted.  However, it does return the same object
	 * each time.  So if you modify the object, the object will be reset in a
	 * subsequent call to this getter.
	 *
	 * @return the current position of the crosshairs on the screen.
	 */
	public Vector2 getCrossHair() {
		return crosscache.set(crosshair);
	}

	/**
	 * Returns true if the primary action button was pressed.
	 *
	 * This is a one-press button. It only returns true at the moment it was
	 * pressed, and returns false at any frame afterwards.
	 *
	 * @return true if the primary action button was pressed.
	 */
	public boolean didJump() {
//		return jumpPressed && !jumpPrevious;
		return jumpPressed;
	}
	/**
	 * Returns true if the dash button was pressed.
	 *
	 * This is a one-press button. It only returns true at the moment it was
	 * pressed, and returns false at any frame afterwards.
	 *
	 * @return true if the dash button was pressed.
	 */
	public boolean didNext() { return nextPressed && !nextPrevious;}

	/**
	 * Returns true if the dash button was pressed.
	 *
	 * This is a one-press button. It only returns true at the moment it was
	 * pressed, and returns false at any frame afterwards.
	 *
	 * @return true if the dash button was pressed.
	 */
	public boolean didPrev() { return prevPressed && !prevPrevious;}

	/**
	 * Returns true if the dash button was pressed.
	 *
	 * This is a one-press button. It only returns true at the moment it was
	 * pressed, and returns false at any frame afterwards.
	 *
	 * @return true if the dash button was pressed.
	 */
	public boolean didDash() { return dashPressed && !dashPrevious;}

	/**
	 * Returns true if the climb button was pressed.
	 *
	 * @return true if the climb button was pressed.
	 */
	public boolean didClimb() { return climbPressed; }

	/**
	 * Returns true if the secondary action button was pressed.
	 *
	 * This is a one-press button. It only returns true at the moment it was
	 * pressed, and returns false at any frame afterwards.
	 *
	 * @return true if the secondary action button was pressed.
	 */
	public boolean didSecondary() {
		return secondPressed && !secondPrevious;
	}
	/**
	 * Returns true if the tertiary action button was pressed.
	 *
	 * This is a sustained button. It will returns true as long as the player
	 * holds it down.
	 *
	 * @return true if the secondary action button was pressed.
	 */
	public boolean didTertiary() {
		return tertiaryPressed;
	}

	/**
	 * Returns true if the reset button was pressed.
	 *
	 * @return true if the reset button was pressed.
	 */
	public boolean didReset() {
		return resetPressed && !resetPrevious;
	}
	
	/**
	 * Returns true if the player wants to go toggle the debug mode.
	 *
	 * @return true if the player wants to go toggle the debug mode.
	 */
	public boolean didDebug() {
		return debugPressed && !debugPrevious;
	}

	/**
	 * Returns true if the exit button was pressed.
	 *
	 * @return true if the exit button was pressed.
	 */
	public boolean didExit() {
		return exitPressed && !exitPrevious;
	}

	/**
	 * Returns true if the meow button was pressed.
	 *
	 * @return true if the meow button was pressed.
	 */
	public boolean didMeow() {
		return meowPressed && !meowPrevious;
	}

	/**
	 * Returns true if the switch button was released.
	 *
	 * @return true if the switch button was released.
	 */
	public boolean didSwitch() {
		if (!switchPressed && switchPrevious){
			if (cancelled){
				cancelled = false;
				return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * Returns true if the switch button is being held.
	 *
	 * @return true if the switch button is being held.
	 */
	public boolean holdSwitch() {
		didCancel = cancelPressed && !cancelPrevious;
		cancelled = cancelled || didCancel;
		return switchPressed & !cancelled;
	}

	/**
	 * Creates a new input controller
	 * 
	 * The input controller attempts to connect to the X-Box controller at device 0,
	 * if it exists.  Otherwise, it falls back to the keyboard control.
	 */
	public InputController() {
		// If we have a game-pad for id, then use it.
		crosshair = new Vector2();
		crosscache = new Vector2();
	}

	/**
	 * Reads the input for the player and converts the result into game logic.
	 *
	 * The method provides both the input bounds and the drawing scale.  It needs
	 * the drawing scale to convert screen coordinates to world coordinates.  The
	 * bounds are for the crosshair.  They cannot go outside of this zone.
	 *
	 * @param bounds The input bound/s for the crosshair.
	 * @param scale  The drawing scale
	 */
	public void readInput(Rectangle bounds, Vector2 scale) {
		// Copy state from last animation frame
		// Helps us ignore buttons that are held down
		jumpPrevious  = jumpPressed;
		dashPrevious = dashPressed;
		secondPrevious = secondPressed;
		resetPrevious  = resetPressed;
		debugPrevious  = debugPressed;
		exitPrevious = exitPressed;
		meowPrevious = meowPressed;
		nextPrevious  = nextPressed;
		prevPrevious = prevPressed;
		switchPrevious = switchPressed;
		cancelPressed = cancelPrevious;
		
		readKeyboard(bounds, scale);
	}

	/**
	 * Reads input from the keyboard.
	 *
	 * This controller reads from the keyboard regardless of whether or not an X-Box
	 * controller is connected.  However, if a controller is connected, this method
	 * gives priority to the X-Box controller.
	 *
	 */
	private void readKeyboard(Rectangle bounds, Vector2 scale) {
		resetPressed = (Gdx.input.isKeyPressed(Input.Keys.R));
		debugPressed = (Gdx.input.isKeyPressed(Input.Keys.B));
		jumpPressed = (Gdx.input.isKeyPressed(Keys.C));
		secondPressed = (Gdx.input.isKeyPressed(Input.Keys.SPACE));
		dashPressed = (Gdx.input.isKeyPressed(Keys.X));
		climbPressed = (Gdx.input.isKeyPressed(Keys.Z));
		exitPressed  = (Gdx.input.isKeyPressed(Input.Keys.ESCAPE));
		meowPressed = (Gdx.input.isKeyPressed(Input.Keys.M));
		switchPressed = (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT));
		cancelPressed = (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT));

		//useful keys for testing/debugging
		nextPressed = (Gdx.input.isKeyPressed(Input.Keys.N));
		prevPressed  = (Gdx.input.isKeyPressed(Input.Keys.P));



		// Directional controls
		horizontal = 0.0f;
		if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
			horizontal += 1.0f;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
			horizontal -= 1.0f;
		}

		vertical = 0.0f;
		if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
			vertical += 1.0f;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
			vertical -= 1.0f;
		}
		
		// Mouse results
        	tertiaryPressed = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
		crosshair.set(Gdx.input.getX(), Gdx.input.getY());
		crosshair.scl(1/scale.x,-1/scale.y);
		crosshair.y += bounds.height;
		clampPosition(bounds);
	}
	
	/**
	 * Clamp the cursor position so that it does not go outside the window
	 *
	 * While this is not usually a problem with mouse control, this is critical 
	 * for the gamepad controls.
	 */
	private void clampPosition(Rectangle bounds) {
		crosshair.x = Math.max(bounds.x, Math.min(bounds.x+bounds.width, crosshair.x));
		crosshair.y = Math.max(bounds.y, Math.min(bounds.y+bounds.height, crosshair.y));
	}
}