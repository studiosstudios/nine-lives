package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.game.object.Particle;
import edu.cornell.gdiac.game.object.ParticlePool;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;
import edu.cornell.gdiac.game.obstacle.PolygonObstacle;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class SpiritRegion extends BoxObstacle {

    /** Color of spirit region particles */
    private Color particleColor;
    /** Color of spirit region background */
    private Color regionColor;
    /** The frames of the spirit animation */
    private TextureRegion[][] spriteFrames;
    /** How long the flame has been animating */
    private float animationTime;
    /** Filmstrip of spirit animation */
    private Animation<TextureRegion>[] animations;
    /** Texture image for the region */
    private Texture regionTexture;
    /** float offsets for randomizing animation frames */
    private float[] timeOffsets;
    /** List of angles to rotate texture */
    private final int[] listAngles = {0, 90, 180, 270};

    /** Vector2 position of bottom left corner of spirit region */
    private Vector2 pos;
    /** width of spirit region */
    private float width;
    /** height of spirit region */
    private float height;
    /** draw scale */
    private Vector2 scale;

    /** PARTICLE VARS */

    /** Respawn rate of the particles */
    public static final int PARTICLE_RESPAWN = 1;
    /** Opacity value for particles when spirit region not active on key press */
    public static final float PARTICLE_OPACITY_INACTIVE = 0.3f;
    /** Opacity value when spirit region not active on key press */
    public static final float REGION_OPACITY_INACTIVE = 0.2f;
    /** Opacity value for particles when spirit region not active on key press */
    public static final float PARTICLE_OPACITY_ACTIVE = 0.5f;
    /** Opacity value when spirit region active on key press */
    public static final float REGION_OPACITY_ACTIVE = 0.5f;
    /** Size of particles to scale */
    public static final float PARTICLE_SIZE = 6f;

    /** Collection of particle objects (MODEL) */
    private ObjectSet<Particle> particles;
    /** Texture image for the photon */
    private Texture photonTexture;
    /** Memory pool for (pre)allocation of particles (???) */
    private ParticlePool memory;
    /** Simple field to slow down the allocation of photons */
    private int cooldown = 0;

    /**
     * Creates a new SpiritRegion Model
     *
     * The Spirit Region has some color.
     *
     * @param texture The texture for the background region
     * @param photonTexture The texture for the particles
     * @param scale Drawing scale
     * @param textureScale Texture scale
     * @param data The JSON data to read from
     */
    public SpiritRegion(TextureRegion texture, TextureRegion photonTexture, Vector2 scale, Vector2 textureScale, JsonValue data){
        super(data.getFloat("width"), data.getFloat("height"));

        this.photonTexture = photonTexture.getTexture();
        this.regionTexture = texture.getTexture();
        this.scale = scale;

        particleColor = new Color(data.get("color").getFloat(0),
                data.get("color").getFloat(1),
                data.get("color").getFloat(2),
                PARTICLE_OPACITY_INACTIVE);

        regionColor = new Color(data.get("color").getFloat(0),
                data.get("color").getFloat(1),
                data.get("color").getFloat(2),
                REGION_OPACITY_INACTIVE);

        setTexture(texture);
        setDrawScale(scale);
        setTextureScale(textureScale);
        setSensor(true);
        setBodyType(BodyDef.BodyType.StaticBody);

        width = data.getFloat("width");
        height = data.getFloat("height");

        this.pos = new Vector2(data.get("pos").getFloat(0) + data.getFloat("width")/2, data.get("pos").getFloat(1) + data.getFloat("height")/2);

        setX(data.get("pos").getFloat(0) + data.getFloat("width")/2);
        setY(data.get("pos").getFloat(1) + data.getFloat("height")/2);


//        // GHOSTIES ANIMATION
//
//        animations = new Animation[width * height];
//
//        int spriteSize = 32;
//        spriteFrames = TextureRegion.split(texture.getTexture(), spriteSize, spriteSize);
//        timeOffsets = new float[width * height];
//
//        System.out.println(spriteFrames.length);
//
//        int numFrames = 34;
//        float frameDuration = 0.1f;
//        for (int i = 0; i < width * height; i++){
//            int rand_frame = ThreadLocalRandom.current().nextInt(0, numFrames);
//            animations[i] = new Animation<>(frameDuration, spriteFrames[0]);
//            animations[i].setPlayMode(Animation.PlayMode.LOOP);
//            timeOffsets[i] = ThreadLocalRandom.current().nextFloat(0, 60);
//        }
//        animationTime = 0f;


//         PHOTON PARTICLES
        particles = new ObjectSet<Particle>();
        int capacity = (int) width * (int) height*3;
        memory = new ParticlePool(capacity);

    }

    /**
     * Called when the Application is destroyed.
     *
     * This is preceded by a call to pause().
     */
    public void dispose() {
        memory.clear();
        photonTexture.dispose();
    }

    /**
     * Changes region and particle color opacity
     *
     * @param val true if spirit region is less opaque, false if more opaque
     */
    public void setSpiritRegionColorOpacity(boolean val) {
        if (val) {
            particleColor.set(particleColor.r, particleColor.g, particleColor.b,
                    PARTICLE_OPACITY_ACTIVE);
            regionColor.set(regionColor.r, regionColor.g, regionColor.b,
                    REGION_OPACITY_ACTIVE);
        } else {
            particleColor.set(particleColor.r, particleColor.g, particleColor.b,
                    PARTICLE_OPACITY_INACTIVE);
            regionColor.set(regionColor.r, regionColor.g, regionColor.b,
                    REGION_OPACITY_INACTIVE);
        }
    }

    /**
     * Updates the status of the particles.
     *
     * This method generates particles according to user input, garbage collects them, and
     * moves them.
     */
    public void update() {
        // Garbage collect the objects that go out of the region
        ObjectSet.ObjectSetIterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle item = iterator.next();
            if (item.getX() < pos.x*scale.x || item.getX() > (pos.x+width)*scale.x-5f ||
                    item.getY() < pos.y*scale.y || item.getY() > (pos.y+height)*scale.y-5f) {
                iterator.remove();
                memory.free(item);
            }
        }

        if (cooldown <= 0) {
            // Add a particle to the set
            Particle item = memory.obtain();

            // Only proceed if allocation succeeded.
            if (item != null) {
                // Initialize the object
                // Make angle mainly upwards to simulate floating up (not any direction)
                float min_angle = (float) Math.PI/3;
                float max_angle = (float) (3*Math.PI/4);
                float rand_angle = min_angle + (max_angle - min_angle) * (float) Math.random();
                // Random pos within region
                // Cluster the y pos near the bottom to give more "floating up" feeling
                Random random = new Random();

                float minValueY = pos.y;
                float maxValueY = pos.y + height;
                float meanValueY = pos.y;
                float stdDev = height/4;

                float rand_y = (float) (random.nextGaussian() * stdDev + meanValueY);
                rand_y = Math.max(minValueY, Math.min(rand_y, maxValueY));

                float rand_x = pos.x + width * (float) Math.random();

                item.setX(rand_x*scale.x);
                item.setY(rand_y*scale.y);

                item.setAngle(rand_angle);
                cooldown = PARTICLE_RESPAWN;
                // Add it to the set of objects
                particles.add(item);
            }
        }

        // Move all particles
        for(Particle item : particles) {
            item.move();
        }

        // Reset cooldown if necessary
        if (cooldown > 0) {
            cooldown--;
        }
    }

    /**
     * Draws the Spirit Region and its particles
     *
     * @param canvas Drawing context
     */
    @Override
    public void draw(GameCanvas canvas){

//        animationTime += Gdx.graphics.getDeltaTime();
//
//        //draw ghosties
//        for (int i = 0; i < width * height; i++) {
//            animations[i].setPlayMode(Animation.PlayMode.LOOP);
//            canvas.draw(animations[i].getKeyFrame(animationTime + timeOffsets[i]), color, origin.x, origin.y,
//                    (getX() + i % width - width/2 - 0.5f)*drawScale.x,(getY() + i/width - height/2)*drawScale.y,getAngle(),textureScale.x,textureScale.y);
//        }

        // Spirit Region Background
        canvas.draw(regionTexture, regionColor, (pos.x - width/2)*drawScale.x, (pos.y-height/2)*drawScale.y,
                width*drawScale.x, height*drawScale.y);

        // Draw particles
        for(Particle item : particles) {
            // Draw the object centered at x.
            // TODO: particles scaled very weirdly rn
              canvas.draw(photonTexture, particleColor, item.getX() - (width/2)*drawScale.x,
                      item.getY() - (height/2)*drawScale.y, PARTICLE_SIZE, PARTICLE_SIZE);
        }
    }
}
