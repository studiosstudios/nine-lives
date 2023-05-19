package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import edu.cornell.gdiac.game.GameCanvas;
import edu.cornell.gdiac.game.obstacle.BoxObstacle;

import java.util.HashMap;
import java.util.Random;

public class SpiritRegion extends BoxObstacle {

    /** Base color without greyscale */
    private final Color baseColor = new Color();
    /** Color of spirit region particles */
    private Color particleColor = new Color();
    /** Color of spirit region background */
    private Color regionColor = new Color();
    /** The frames of the spirit animation */
    private Texture regionTexture;
    /** Vector2 position of bottom left corner of spirit region */
    private Vector2 pos;
    /** width of spirit region */
    private float width;
    /** height of spirit region */
    private float height;
    private float greyScaleTarget;
    private float greyscale;

    /** PARTICLE VARS */

    /** Respawn rate of the particles */
    public static final int PARTICLE_RESPAWN = 1;
    /** Opacity value for particles when spirit region not active on key press */
    public static final float PARTICLE_OPACITY_INACTIVE = 0.5f;
    /** Opacity value when spirit region not active on key press */
    public static final float REGION_OPACITY_INACTIVE = 0.5f;
    /** Opacity value for particles when spirit region not active on key press */
    public static final float PARTICLE_OPACITY_ACTIVE = 0.75f;
    /** Opacity value when spirit region active on key press */
    public static final float REGION_OPACITY_ACTIVE = 0.75f;
    /** Size of particles to scale */
    public static final float PARTICLE_SIZE = 6f;

    private static final int MAX_PARTICLES = 100;

    /** Collection of particle objects (MODEL) */
    private ObjectSet<Particle> particles;
    /** Texture image for the photon */
    private Texture photonTexture;
    /** Memory pool for (pre)allocation of particles (???) */
    private ParticlePool memory;
    /** Simple field to slow down the allocation of photons */
    private int cooldown = 0;
    private Random random;

    public String getColorString(){ return baseColor.toString().substring(0, 6); }

    /**
     * Creates a new SpiritRegion Model.
     *
     * @param properties     String-Object map of properties for this object
     * @param tMap           Texture map for loading textures
     * @param scale          Draw scale for drawing
     * @param textureScale   Texture scale for rescaling texture
     */
    public SpiritRegion(ObjectMap<String, Object> properties, HashMap<String, TextureRegion> tMap, Vector2 scale, Vector2 textureScale){
        super((float) properties.get("width"), (float) properties.get("height"));
        this.photonTexture = tMap.get("spirit-photon").getTexture();
        this.regionTexture = tMap.get("spirit-region").getTexture();

        baseColor.set((Color) properties.get("color", Color.RED));

        particleColor.set(baseColor);
        particleColor.a = PARTICLE_OPACITY_INACTIVE;

        regionColor.set(baseColor);
        regionColor.a = REGION_OPACITY_INACTIVE;

        setTextureScale(textureScale);
        setTexture(tMap.get("spirit-region"));
        setDrawScale(scale);
        setTextureScale(textureScale);
        setSensor(true);
        setBodyType(BodyDef.BodyType.StaticBody);

        width = (float) properties.get("width");
        height = (float) properties.get("height");

        this.pos = new Vector2((float) properties.get("x") + width/2, (float) properties.get("y") - height/2);

        setPosition(pos);


//         PHOTON PARTICLES
        random = new Random();
        particles = new ObjectSet<Particle>();
        int capacity = Math.min((int) width * (int) height, MAX_PARTICLES);
        memory = new ParticlePool(capacity);
        for (int i = 0; i < capacity; i++){
            Particle item = addParticle();
            float low = item.getBottom() * drawScale.y;
            float high = item.getTop() * drawScale.y - PARTICLE_SIZE;
            item.setY(random.nextFloat()*(high-low)+low);
        }

//        Particle item = addParticle();
//        float low = item.getBottom() * drawScale.y;
//        float high = item.getTop() * drawScale.y - PARTICLE_SIZE;
//        item.setY(random.nextFloat()*(high-low)+low);



    }

    private Particle addParticle(){
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

            float minValueY = pos.y;
            float maxValueY = pos.y + height;
            float stdDev = height/5;

            float rand_bot = (float) (Math.abs(random.nextGaussian()) * stdDev + minValueY);
            rand_bot = Math.min(rand_bot, pos.y + height/2);
            item.setBottom(rand_bot);

            float rand_top = rand_bot;
            while (rand_top <= rand_bot){
                rand_top = (float) (maxValueY - Math.abs(random.nextGaussian()) * stdDev);
            }
            rand_top = Math.min(rand_top, maxValueY);
            item.setTop(rand_top);

            float rand_x = pos.x + width * (float) Math.random();

            item.setX(rand_x* drawScale.x);
            item.setY(rand_bot* drawScale.y);

            item.setAngle(rand_angle);
            cooldown = PARTICLE_RESPAWN;
            // Add it to the set of objects
            particles.add(item);
        }
        return item;
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
        if (Math.abs(greyScaleTarget - greyscale) < 0.01f) {
            greyscale = greyScaleTarget;
        } else {
            greyscale += (greyScaleTarget - greyscale) * 0.15f;
        }

        if (greyscale > 0) {
            float greyColor = greyColor(baseColor);
            particleColor.set(baseColor.r * (1-greyscale) + greyColor * greyscale,
                    baseColor.g * (1-greyscale) + greyColor * greyscale,
                    baseColor.b * (1-greyscale) + greyColor * greyscale, baseColor.a);
            regionColor.set(particleColor.r, particleColor.g, particleColor.b, regionColor.a);
        }

        if (val) {
            particleColor.a += (PARTICLE_OPACITY_ACTIVE - particleColor.a)*0.07;
            regionColor.a += (REGION_OPACITY_ACTIVE - regionColor.a)*0.07;
        } else {
            particleColor.a += (PARTICLE_OPACITY_INACTIVE - particleColor.a)*0.07;
            regionColor.a += (REGION_OPACITY_INACTIVE - regionColor.a)*0.07;
        }
    }

    public void setGreyscale(float greyscale){ greyScaleTarget = greyscale; }

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
            if (item.getX() < pos.x* drawScale.x || item.getX() > (pos.x+width)* drawScale.x-5f ||
                    item.getY() < item.getBottom()* drawScale.y || item.getY() > item.getTop()* drawScale.y - PARTICLE_SIZE) {
                iterator.remove();
                memory.free(item);
            }
        }

        if (cooldown <= 0) {
            // Add a particle to the set
            addParticle();
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
//        float bot = pos.y* drawScale.y ;
//        float top = (pos.y+height)* drawScale.y-5f;
        float left = pos.x* drawScale.x ;
        float right = (pos.x+width)* drawScale.x-5f;

        //use these two parameters to tune how quickly particles fade in and out relative to borders - higher = slower
        float xSharpness = 0.5f;
        float ySharpness = 1f;

        for(Particle item : particles) {
            // Draw the object centered at x.
            // TODO: particles scaled very weirdly rn
            float bot = item.getBottom()* drawScale.y ;
            float top = item.getTop()* drawScale.y - PARTICLE_SIZE;
            Color c = new Color(particleColor);
            float y = item.getY();
            float x = item.getX();
            c.a = c.a * (float) (Math.max(Math.pow(y-bot, ySharpness) * Math.pow(top-y, ySharpness)/Math.pow((top-bot)/2, 2*ySharpness), 0));
            c.a = c.a * (float) (Math.max(Math.pow(x-left, xSharpness) * Math.pow(right-x, xSharpness)/Math.pow((right-left)/2, 2*xSharpness), 0));
            canvas.draw(photonTexture, c, x - (width/2f)*drawScale.x,
                      y - (height/2f)*drawScale.y, PARTICLE_SIZE, PARTICLE_SIZE);
        }
    }
}
