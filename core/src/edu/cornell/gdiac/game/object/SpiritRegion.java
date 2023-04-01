package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;
import edu.cornell.gdiac.game.obstacle.PolygonObstacle;
import sun.security.provider.ConfigFile;
import java.util.concurrent.ThreadLocalRandom;

public class SpiritRegion extends BoxObstacle {

    private Color color;

    /** The frames of the spirit animation */
    private TextureRegion[][] spriteFrames;
    /** How long the flame has been animating */
    private float animationTime;
    /** Filmstrip of spirit animation */
    private Animation<TextureRegion>[] animations;
    private float[] timeOffsets;

    private final int[] listAngles = {0, 90, 180, 270};
    private int x;
    private int y;

    private int width;
    private int height;

    public SpiritRegion(TextureRegion texture, Vector2 scale, Vector2 textureScale, JsonValue data){
        super(data.getFloat("width"), data.getFloat("height"));

//        color = new Color(Color.WHITE);
        color = new Color(data.get("color").getFloat(0),
                data.get("color").getFloat(1),
                data.get("color").getFloat(2),
                data.get("color").getFloat(3));

        setDrawScale(scale);
        setTextureScale(textureScale);
        setSensor(true);
        setBodyType(BodyDef.BodyType.StaticBody);


        width = (int) getDimension().x;
        height = (int) getDimension().y;

        setX(data.get("pos").getFloat(0) - width/2);
        setY(data.get("pos").getFloat(1) - height/2);

        animations = new Animation[width * height];

        int spriteSize = 32;
        spriteFrames = TextureRegion.split(texture.getTexture(), spriteSize, spriteSize);
        timeOffsets = new float[width * height];

        System.out.println(spriteFrames.length);

        int numFrames = 34;
        float frameDuration = 0.1f;
        for (int i = 0; i < width * height; i++){
            int rand_frame = ThreadLocalRandom.current().nextInt(0, numFrames);
            animations[i] = new Animation<>(frameDuration, spriteFrames[0]);
            animations[i].setPlayMode(Animation.PlayMode.LOOP);
            timeOffsets[i] = ThreadLocalRandom.current().nextFloat(0, 60);
        }
        animationTime = 0f;
    }

    @Override
    public void draw(GameCanvas canvas){

        animationTime += Gdx.graphics.getDeltaTime();

        //draw ghosties
        for (int i = 0; i < width * height; i++) {
            animations[i].setPlayMode(Animation.PlayMode.LOOP);
            canvas.draw(animations[i].getKeyFrame(animationTime + timeOffsets[i]), color, origin.x, origin.y,
                    (getX() + i % width - width/2 - 0.5f)*drawScale.x,(getY() + i/width - height/2)*drawScale.y,getAngle(),textureScale.x,textureScale.y);
        }


    }
}
