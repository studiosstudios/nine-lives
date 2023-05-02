# Tiled Jar Launcher 

## Installation
1. Extract TiledExtension.zip into the extensions directory of Tiled
	1. The extensions directory can be found in the Tiled editor or in the filesystem
	2. You are looking for a folder named extensions, in the tiled editor it will be at the bottom of the screen, and it will be a path
		- Windows/Linux : Edit > Preferenes > Plugins
			- By default on Windows this is `C:\Users\<user>\AppData\Local\Tiled\extensions`
		- Mac : Tiled > Preferences > Plugins
			- By default on Mac this is `Macintosh HD/Users/<user>/Library/Preferences/Tiled/extensions`
2. Inside your extensions folder there should now be a folder titled TiledExtension. You're done!

## Usage
1. Open a level in tiled
2. Press Ctrl+Shift+P (Cmd+Shift+P on Mac) to launch the level in your game
	- For the first usage of this extension, you will need to select the JAR of you game in the file chooser that appears. 
	- This ideally points to the `<path to project>/out/aritfacts/<jar title>/` directory so that when you rebuild the jar, it will be updated.

## Code integration
This extension works by running your JAR with an argument that is a filepath to a temp level. You can detect if your game is being run this way in DesktopLauncher. The argument that gets passed will be a filepath to the level. The temp file is a .json tiled output. Code example below:
```java
public static void main(String[] arg) {

	// Detect presence of argument, this is a filepath, used when quick launching from Tiled
	if (arg.length > 0) {
		quickLaunchingFromTiled = True;
		quickLaunchFilepath = arg;
	}
	
...
}
```

## Errors?
This extension has been extensively tested and was working on many machines when it was initially developed, however it is possible there are still bugs and quirks. If you are sure the bug is caused by the extension and not your code, if you are in 3152 please post on the Ed, otherwise send an email to eao56@cornell.edu.

## Credit
This extension was originally developed by Richard Palmer of Angry Hedgehog Studios in Spring 2022 for their game Symbiosis. ReadMe directions expanded and generalized by Elliot Overholt in Spring 2023.

