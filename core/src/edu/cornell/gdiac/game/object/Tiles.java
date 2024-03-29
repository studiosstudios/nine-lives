package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
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
    private int fid;

    private Vector2 offset = new Vector2();

    public Tiles(JsonValue data, int tileSize, int levelWidth, int levelHeight, TextureRegion tileset, Rectangle bounds, int fid, Vector2 textureScale) {

        levelTiles = data.get("data").asIntArray();
        this.levelWidth = levelWidth;
        this.levelHeight = levelHeight;
        this.tileSize = tileSize;
        this.textureScale = textureScale;
        this.fid = fid;
        this.offset.set(bounds.x, bounds.y);

        // turn tileset into 1D texture arr for easy indexing
        // numbers in data correspond to numbers in tileset + 1

        TextureRegion[][] tiles = TextureRegion.split(tileset.getTexture(), tileSize, tileSize);

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
                try {
                    if (levelTiles[i] > 0) {
                        TextureRegion tileTexture = tileset[levelTiles[i] - fid];
                        if (tileSize == 512) {
                            canvas.draw(tileTexture, Color.WHITE, 0, 0, (x + offset.x) * 128 * textureScale.x,
                                    (y + offset.y) * 128 * textureScale.y, 0, textureScale.x, textureScale.y);
                        } else {
                            canvas.draw(tileTexture, Color.WHITE, 0, 0, (x + offset.x) * tileSize * textureScale.x,
                                    (y + offset.y) * tileSize * textureScale.y, 0, textureScale.x, textureScale.y);
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.err.println("WARNING: tile " + i + " at (" + x +", " + y + ") has invalid id " +
                            levelTiles[i] + ". (fid is " + fid + ", tileset size is " + tileset.length + ").");
                    levelTiles[i] = 0;
                }
                i++;
            }
        }
    }




}
