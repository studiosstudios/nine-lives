package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.GameCanvas;

import java.util.Arrays;

public class Tiles {
    private TextureRegion[] tileset;
    private int[] levelTiles;
    private int levelWidth;
    private int levelHeight;
    private float tileSize;
    private Vector2 textureScale;


    public Tiles(JsonValue data, int tileSize, int levelWidth, int levelHeight, TextureRegion tileset, Vector2 textureScale) {

        levelTiles = data.get("data").asIntArray();
        this.levelWidth = levelWidth;
        this.levelHeight = levelHeight;
        this.tileSize = tileSize;
        this.textureScale = textureScale;

        // turn tileset into 1D texture arr for easy indexing
        // numbers in data correspond to numbers in tileset + 1

        TextureRegion[][] tiles = tileset.split(tileset.getTexture(), (int)(tileSize/textureScale.x), (int)(tileSize/textureScale.y));

        //flatten 2d array into 1d array
        int numTiles = 0;
        for (int i = 0; i < tiles.length; i++) {
            numTiles += tiles[i].length;
        }
        this.tileset = new TextureRegion[numTiles];
        int j = 0;
        for (int i = 0; i < tiles.length; i++) {
            System.arraycopy(tiles[i], 0, this.tileset, j, tiles[i].length);
            j += tiles[i].length;
        }

    }

    public void draw(GameCanvas canvas){
        int i = 0;
        for (float y = levelHeight - 1; y >= 0; y--){
            for (float x = 0; x < levelWidth; x++){
                if (levelTiles[i] > 0) {
                    TextureRegion tileTexture = tileset[levelTiles[i] - 1];
                    canvas.draw(tileTexture, Color.WHITE, 0, 0, x * tileSize, y * tileSize, 0, textureScale.x, textureScale.y);
                }
                i++;
            }
        }
    }




}
