package edu.cornell.gdiac.game.desktop;

import com.badlogic.gdx.graphics.glutils.HdpiMode;
import edu.cornell.gdiac.backend.GDXApp;
import edu.cornell.gdiac.backend.GDXAppSettings;
import edu.cornell.gdiac.game.NineLives;
import lwjgl3.Lwjgl3Application;
import lwjgl3.Lwjgl3ApplicationConfiguration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * The main class of the game.
 * 
 * This class sets the window size and launches the game.
 *
 * Adapted from Walker M. White's DesktopLauncher.java in Cornell CS 3152, Spring 2023.
 */
public class DesktopLauncher {
	public static boolean FULLSCREEN = false;
	private static boolean quickLaunchingFromTiled;
	private static String quickLaunchFilepath;

	/**
	 * Classic main method that all Java programmers know.
	 * 
	 * This method simply exists to start a new Lwjgl3Application.  For desktop games,
	 * LibGDX is built on top of LWJGL3 (this is not the case for Android).
	 * 
	 * @param arg Command line arguments
	 */
	public static void main (String[] arg) throws Exception {

		// Detect presence of argument, this is a filepath, used when quick launching from Tiled
		if (arg.length > 0) {
			quickLaunchingFromTiled = true;
			quickLaunchFilepath = arg[0];
		}

		GDXAppSettings config = new GDXAppSettings();
		config.title = "9 Lives";
		config.width  = 1280;
		config.height = 720;
		config.forceExit = true;
//		config.fullscreen = FULLSCREEN;
		config.getLwjgl3Configuration().setHdpiMode(HdpiMode.Pixels);
		new GDXApp(new NineLives(quickLaunchingFromTiled, quickLaunchFilepath), config);
		System.exit(0); // This works for the Packr app but there should be a more elegant way to close
	}
}
