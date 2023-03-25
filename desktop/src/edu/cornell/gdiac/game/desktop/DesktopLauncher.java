package edu.cornell.gdiac.game.desktop;

import edu.cornell.gdiac.backend.GDXApp;
import edu.cornell.gdiac.backend.GDXAppSettings;
import edu.cornell.gdiac.game.NineLives;

/**
 * The main class of the game.
 * 
 * This class sets the window size and launches the game.
 *
 * Adapted from Walker M. White's DesktopLauncher.java in Cornell CS 3152, Spring 2023.
 */
public class DesktopLauncher {
	public static boolean FULLSCREEN = false;

	/**
	 * Classic main method that all Java programmers know.
	 * 
	 * This method simply exists to start a new Lwjgl3Application.  For desktop games,
	 * LibGDX is built on top of LWJGL3 (this is not the case for Android).
	 * 
	 * @param arg Command line arguments
	 */
	public static void main (String[] arg) {
		GDXAppSettings config = new GDXAppSettings();
		config.title = "9 Lives";
		config.width  = 1024;
		config.height = 576;
		config.fullscreen = FULLSCREEN;
		new GDXApp(new NineLives(), config);
	}
}
