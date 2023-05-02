"use strict";
/// <reference types="@mapeditor/tiled-api" />
const action = tiled.registerAction("LaunchSymbiosis", function (action) {
    if (tiled.activeAsset.isTileMap) {
        const parentDirectory = tiled.projectFilePath.slice(0, tiled.projectFilePath.lastIndexOf('/')) + "/extensions/TiledExtension/";
        try {
            const configFile = new TextFile(parentDirectory + "config.txt");
            const symbiosisLocation = configFile.readLine();
            let symbiosisDirectory;
            if (tiled.platform == "windows") {
                symbiosisDirectory = symbiosisLocation.slice(0, symbiosisLocation.lastIndexOf('\\') + 1);
            } else {
                symbiosisDirectory = symbiosisLocation.slice(0, symbiosisLocation.lastIndexOf('/') + 1);
            }
            const tempFilePath = symbiosisDirectory + "temp.json";
            const writeResult = tiled.mapFormat("json").write(tiled.activeAsset, tempFilePath);
            if (writeResult != undefined) {
                tiled.error("An error occured while writing the temp file. Please send Ricky a screenshot of the error message.", () => {
                    tiled.alert(writeResult, "Write Error");
                });
            }
            else {
                tiled.log("Playing " + tiled.activeAsset.fileName);
                startLauncher(parentDirectory, tempFilePath);
            }
        }
        catch (e) {
            //This generally means no config file was found
            tiled.log("Setting up launcher config");
            startConfig(parentDirectory);
        }
    }
});
action.text = "Launch Level";
action.enabled = tiled.activeAsset != null && tiled.activeAsset.isTileMap;
action.shortcut = "Ctrl+Shift+P"; //Ctrl + P is used by another command by default
tiled.extendMenu("File", [
    { action: "LaunchSymbiosis", before: "Reload" },
    { separator: true }
]);
tiled.activeAssetChanged.connect(asset => {
    action.enabled = asset != null && asset.isTileMap; //User should only be able to launch levels
});
function startLauncher(parentDirectory, tempFilePath) {
    const os = tiled.platform;
    const process = new Process();
    process.workingDirectory = parentDirectory;
    if (!process.start("java", ["GameLauncher", tempFilePath, os])) {
        tiled.error("An error occured while launching Symbiosis. Please tell Ricky.", () => { });
    }
    process.waitForFinished();
    tiled.log("Finished Playing");
    const error = process.readStdErr();
    const output = process.readStdOut();
    if (error.length > 0) {
        tiled.alert(error);
    }
    tiled.log(output);
}
function startConfig(parentDirectory) {
    const process = new Process();
    process.workingDirectory = parentDirectory;
    process.exec("java", ["GameConfig"]);
    const error = process.readStdErr();
    const output = process.readStdOut();
    if (error.length > 0) {
        tiled.error("An error occured while creating the launch configuration. Please send Ricky a screenshot of the error message.", () => {
            tiled.alert(error, "Launch Error");
        });
    }
    tiled.log(output);
}
