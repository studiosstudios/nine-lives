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
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

import java.io.*;

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
	private static final String[] bindableControlNames = new String[] {
			"up", "down", "right", "left", "jump", "dash", "climb", "switch", "cancel", "undo", "pan"
	};
	
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

	/** Map from control names to <code>Input.Keys</code> key integer codes.*/
	private ObjectMap<String, Integer> controls = new ObjectMap<>();
	/** Map from control names to if they were pressed at previous tick*/
	private ObjectMap<String, Boolean> previousMap = new ObjectMap<>();
	/** Map from control names to if they are pressed at current tick*/
	private ObjectMap<String, Boolean> pressedMap = new ObjectMap<>();
	/** Array of control names */
	private Array<String> controlNames = new Array<>();
	/** For reading input from a text file */
	private BufferedReader readFile;
	/** For writing input to a text file */
	private BufferedWriter writeFile;
	/** Json specifying controls */
	private JsonValue controlsJSON;
	/** Disables all controls **/
	private boolean disableAll;

	/**
	 * Sets the keybindings from a JSON. The JSON must be a single object consisting only of string-string pairs, where
	 * the key is the name of the control, and the value is the string of the desired <code>Input.Keys</code> key.
	 * @param controlsJSON keybindings JSON
	 */
	public void setControls(JsonValue controlsJSON){
		this.controlsJSON = controlsJSON;
		for (JsonValue entry : controlsJSON){
			try {
				String keyName = entry.get("key").asString();
				controls.put(entry.name, Input.Keys.class.getField(keyName).getInt(Input.Keys.class.getField(keyName)));
			} catch (Exception e){
				controls.put(entry.name, Keys.UNKNOWN);
				System.err.println("Failed to bind key for " + entry.name + ":");
				e.printStackTrace();
			}
			controlNames.add(entry.name);
			previousMap.put(entry.name, false);
			pressedMap.put(entry.name, false);
		}
		int[] userControls = Save.getControls();
		for (int i = 0; i < userControls.length; i++) {
			controls.put(bindableControlNames[i], userControls[i]);
		}

	}

	public void changeControls() {

	}

	/**
	 * Sets the input controller to read input from a file instead of the keyboard.
	 * @param fileName  file to read from
	 */
	public void readFrom(String fileName){
		try {
			FileReader reader = new FileReader(fileName);
			readFile = new BufferedReader(reader);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sets the input controller to write input to a file as it reads input.
	 * @param fileName  file to write to
	 */
	public void writeTo(String fileName){
		try {
			new PrintWriter(fileName).close();
			FileWriter writer = new FileWriter(fileName);
			writeFile = new BufferedWriter(writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// Fields to manage buttons
	/** if the switch has been cancelled */
	private boolean cancelled;
	/** How much did camera move horizontally? **/
	private float camHorizontal;
	/** How much did camera move vertically? **/
	private float camVertical;
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
	 * Returns the amount of camera sideways movement.
	 *
	 * @return the amount of camera sideways movement.
	 */
	public float getCamHorizontal() {
		return camHorizontal;
	}

	/**
	 * Returns the amount of camera vertical movement.
	 *
	 * @return the amount of camera vertical movement.
	 */
	public float getCamVertical() {
		return camVertical;
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
		return pressedMap.get("jump");
	}

	/**
	 * Returns true if the key mapped to by a given control was just pressed
	 *
	 * @param control the name of the control
	 * @return true if the key mapped to by a given control was just pressed
	 */
	private boolean isClicked(String control) { return pressedMap.get(control) && !previousMap.get(control); }

	/**
	 * Returns true if the dash button was pressed.
	 *
	 * This is a one-press button. It only returns true at the moment it was
	 * pressed, and returns false at any frame afterwards.
	 *
	 * @return true if the dash button was pressed.
	 */
	public boolean didNext() { return isClicked("next");}

	/**
	 * Returns true if the dash button was pressed.
	 *
	 * This is a one-press button. It only returns true at the moment it was
	 * pressed, and returns false at any frame afterwards.
	 *
	 * @return true if the dash button was pressed.
	 */
	public boolean didPrev() { return isClicked("previous");}

	/**
	 * Returns true if the dash button was pressed.
	 *
	 * This is a one-press button. It only returns true at the moment it was
	 * pressed, and returns false at any frame afterwards.
	 *
	 * @return true if the dash button was pressed.
	 */
	public boolean didDash() { return isClicked("dash");}

	/**
	 * Returns true if the climb button was pressed.
	 *
	 * @return true if the climb button was pressed.
	 */
	public boolean didClimb() { return pressedMap.get("climb"); }

	/**
	 * Returns true if the reset button was pressed.
	 *
	 * @return true if the reset button was pressed.
	 */
	public boolean didReset() {
		return isClicked("reset");
	}
	
	/**
	 * Returns true if the player wants to go toggle the debug mode.
	 *
	 * @return true if the player wants to go toggle the debug mode.
	 */
	public boolean didDebug() {
		return isClicked("debug");
	}

	/**
	 * Returns true if player wants to pan the map
	 *
	 * @return true if the player wants to pan the map
	 */
	public boolean didPan() {
		return pressedMap.get("pan");
	}

	/**
	 * Returns true if the exit button was pressed.
	 *
	 * @return true if the exit button was pressed.
	 */
	public boolean didExit() {
		return isClicked("exit");
	}

	/**
	 * Returns true if the meow button was pressed.
	 *
	 * @return true if the meow button was pressed.
	 */
	public boolean didMeow() {
		return isClicked("meow");
	}

	/**
	 * Returns true if the undo button was pressed.
	 *
	 * @return true if the undo button was pressed.
	 */
	public boolean didUndo() { return isClicked("undo"); }

	/**
	 * Returns true if the switch button was released and not cancelled.
	 *
	 * @return true if the switch button was released and not cancelled.
	 */
	public boolean didSwitch() {
		if (!pressedMap.get("switch") && previousMap.get("switch")){
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
		cancelled = cancelled || pressedMap.get("cancel");
		return pressedMap.get("switch") & !cancelled;
	}

	/**
	 * Returns true if the switch button was just pressed.
	 *
	 * @return true if the switch button was just pressed.
	 */
	public boolean switchPressed() { return isClicked("switch"); }

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
		writeFile = null;
		readFile = null;
	}

	/**
	 * Reads the input for the player and converts the result into game logic.
	 */
	public void readInput() {
		// Copy state from last animation frame
		// Helps us ignore buttons that are held down
		for (String control : controlNames){
			previousMap.put(control, pressedMap.get(control));
		}

		if (readFile == null || !readFromFile()) {
			readKeyboard();
		}

		horizontal = 0.0f;
		vertical = 0.0f;
		camHorizontal = 0.0f;
		camVertical = 0.0f;
		if(!pressedMap.get("pan")){
			if (pressedMap.get("right")) {
				horizontal += 1.0f;
			}
			if (pressedMap.get("left")) {
				horizontal -= 1.0f;
			}
			if (pressedMap.get("up")) {
				vertical += 1.0f;
			}
			if (pressedMap.get("down")) {
				vertical -= 1.0f;
			}
		}
		else {
			if (pressedMap.get("right")) {
				camHorizontal += 8f;
			}
			if (pressedMap.get("left")) {
				camHorizontal -= 8f;
			}
			if (pressedMap.get("up")) {
				camVertical += 8f;
			}
			if (pressedMap.get("down")) {
				camVertical -= 8f;
			}
		}
		if (writeFile != null){
			try {
				String toString = pressedMap.toString();
				writeFile.write(toString.substring(1, toString.length()-1));
				writeFile.newLine();
				writeFile.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Reads keyboard input from a file by updating <code>pressedMap</code>. Each line of the file is a <code>toString()</code>
	 * representation of <code>pressedMap</code>, excluding the start and end braces.
	 *
	 * @return true if a non-null line was read
	 */
	private boolean readFromFile(){
		try {
			String line = readFile.readLine();
			if (line != null) {
				String[] args = line.split(",");
				for (String s : args) {
					String[] pair = s.split("=");
					if (pair[0].charAt(0) == ' '){
						pair[0] = pair[0].substring(1);
					}
					pressedMap.put(pair[0], pair[1].charAt(0) == 't');
				}
			} else {
				return false;
			}
		} catch (IOException e){
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Whether all user inputs should be ignored.
	 * @param b true if all inputs should be ignored
	 */
	public void setDisableAll(boolean b){
		disableAll = b;
	}

	/**
	 * Reads input from the keyboard.
	 *
	 * This controller reads from the keyboard regardless of whether or not an X-Box
	 * controller is connected.  However, if a controller is connected, this method
	 * gives priority to the X-Box controller.
	 *
	 */
	private void readKeyboard() {
		pressedMap.put("pan", Gdx.input.isKeyPressed(controls.get("pan")));
		for (String control : controlNames){
			if(control.equals("exit")){
				pressedMap.put("exit", Gdx.input.isKeyPressed(controls.get("exit")));
			}
			else if(disableAll){
				pressedMap.put(control, false);
			}
			else if(pressedMap.get("pan")) {
				pressedMap.put(control, !controlsJSON.get(control).get("disableWhenPan").asBoolean() && Gdx.input.isKeyPressed(controls.get(control)));
			}
			else {
				pressedMap.put(control, Gdx.input.isKeyPressed(controls.get(control)));
			}
		}
	}
}