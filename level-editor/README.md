
# A level editor for the poggiest game of all time

## Creating a level
First, when you open Tiled, make sure you are opening ```nine-lives.tiled-project```. **Before you start making a level make sure you have turned on View -> Snapping -> Snap to Grid.**

To make a level, it is easiest to copy ```level-template.tmx```. The map itself has a ```biome``` property that is parsed to specify the tileset for the walls - right now we only have the metal tileset so make sure it is set to ```metal```.

### Adding objects
To make walls, select the ```metalWalls``` layer and the ```metal-walls``` tileset. This is a tile layer, so all you can do is place (B) and erase tiles (E). Ensure that Map -> AutoMap While Drawing is turned on so that the tileset is automatically read. When you are finished with the tile layout of the walls, select the ```wallsPoly``` layer, which is layer that contains the actual Box2D shape of the walls. Each object in the ```wallsPoly``` layer must be a polygon (P), you should create as many polygons as necessary to cover all boundaries of the walls in your level.

To add the cat, checkpoints, flamethrowers, lasers, spikes, mobs and activators, select the corresponding layer and the ```objects``` tileset. Place corresponding objects into the layer by inserting tiles (T), and edit their properties if necessary. To rotate the object, change the rotation field - flipping it will not do anything. The greyed out default values of each property is the default value **parsed by us in the game**, not the default value into the JSON - if you leave the default value then there will not be a field for it in the JSON. This is not an issue **except** for activators, which must have an ```id``` otherwise they will do nothing.

Doors, platforms, spirit regions and exits are represented in their respective layer as rectangles (R), **not** polygons. To add them, select the corresponding layer, draw a rectangle, and set the class of that rectangle to corresponding class. Do not rotate any rectangles or else they will be parsed incorrectly - for doors, the ```closeAngle``` property determines the direction that the door closes in.

To add mirrors or boxes, select the layer and add any tile, since right now we do not have assets for these. For now neither of these have any custom properties so you will not need to worry about that.

Every level must contain a cat, a return exit, goal exit, and a goal. For the exits, make sure that the edge is just outside the border of the level, and that the bottom edge aligns with the edge of the walls, as shown in the template. The goal should be placed near the very end of the level, in such a way that the player cannot jump over it.

### Attaching objects to each other
All objects with an ```attachName``` field can be attached to any other object. This field refers to the default name of whatever object to be attached to - the level template has an example where the first checkpoint is attached to the platform, and the laser is attached to the box.

### Finishing the level
Currently the level template map size is **infinite**. This is because the automapping rules fail at the edges of a level and I don't want to figure out how to fix it. Whenever you are designing a level, you should keep the map size as infinite, and only resize the map once you have finished (Map -> Map properties -> uncheck infinity, Map -> Resize Map), otherwise you will have to manually place from the tileset at edges. Note that currently this means that the tileset is not used properly at the border between levels - if anyone wants to try and fix this please go ahead (I don't want to be the tiled guy), it would involve adding new rules to the automapping tileset (```./metal-wall-rules.tmx```, look [here](https://doc.mapeditor.org/en/stable/manual/automapping/) for more info).

## Playtesting the level
We now have an extension to playtest the level you are currently making directly from Tiled. To set it up:
 - Create a JAR for the game in IntelliJ as outlined [here](https://www.cs.cornell.edu/courses/cs3152/2023sp/resources/engine/#creating-a-stand-alone-jar-file)
 - In Tiled, go to Project -> Project Properties and make sure the extensions directory is ```extensions/```
 - In Tiled, press Ctrl+Shift+P (Cmd+Shift+P on Mac) -> Launch Level. This should open up a window for you to select the location of the JAR you created above.

Now, every time you want to playtest a level, pressing Ctrl+Shift+P (Cmd+Shift+P on Mac) should automatically run the JAR and load in the level you currently have open n Tiled (this must be a .json and not a .tmx). If for whatever reason the keyboard shortcut does not work, you can also go to File -> Launch Level. Note that this will only load in one level, if you want to see how your level fits into the rest of the game you will have to add it manually.


## Adding a new level to the game
This is the current workflow for actually adding the level to the game:
- Export the map as a JSON, and copy/move it into the ```assets/tiled/``` folder in the repo.
- In ```assets/assets.json```, add a new entry under the ```jsons``` field. The key must be of the form ```tiledLevel[level number]```, the value is the path to the JSON. 
- If necessary, in ```NineLives.java```, increase the total number of levels by changing the number in the method call for ```startGame``` (currently lines 118 & 120).


## Some common errors and causes
- A Box2D assertion error: you have added a rectangle/polygon with zero width/height. This one is super easy to accidentally make because Tiled automatically makes rectangles of zero width and zero height whenever you click with the rectangle tool - we should ignore these when parsing eventually. 
- Couldn't load dependencies of asset: check you have the right names in ```assets.json```
- Transitions between levels are not working: one of the two levels is missing or has misplaced a goal/return exit
- Your level is not being loaded in: check you have set the right number of levels in ```NineLives.java```
- Tiled extension is not working: check that ```extensions/TiledExtension/config.txt``` contains the right path to your JAR

## Adding a new type of object
I am unsure if we will ever need to do this, but I'll put this just in case. As of right now there are two possible types of objects we could be adding. For an object that always takes up a set amount of grid cells:
- Add the image to the ```./objects``` folder (1024x1024 is the current grid cell size).
- Open ```./objects/objects.tsx``` (this is the objects tileset), and add the image to the tileset.
- Create a class for the object by going to View -> Custom Types Editor. Deselect Property value, and use only as class of object and tile.
    - If you need properties with multiple fields (such as Vector2), or you properties with a set number of values, you can create classes/enums for them.
- Parse the object in ```level.populateObjects()```

For an object that does not always have the same shape:
- Create a class for the object as outlined above. This time there is no way as far as I know to ensure that objects within a layer all are of the same class, so we must set them manually every time we add them.
- Parse the object in ```level.populateObjects()```

