package edu.cornell.gdiac.game;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;

/**
 * Different from abstract class Camera
 */
public class Camera {
    /** Camera for the underlying SpriteBatch */
    private OrthographicCamera camera;
    /** Width of visible window **/
    private float viewportWidth;
    /** Height of visible window **/
    private float viewportHeight;
    /** Bounds of current level **/
    private Rectangle levelBounds;
    /** Camera center x-coordinate **/
    private float x;
    /** Camera center y-coordinate **/
    private float y;
    /** Rate at which camera glides towards destination coordinates**/
    private final float CAMERA_GLIDE_RATE = 0.075f;
    /** Gameplay zoom **/
    private float zoom;
    /** Whether camera is moving **/
    private boolean isGliding;

    /**
     * Wrapper for the OrthographicCamera class that takes into account dynamic level metadata.
     * @param viewportWidth width of visible window
     * @param viewportHeight height of visible window
     * @param zoom zoom relative to viewport sizes
     */
    public Camera(float viewportWidth, float viewportHeight, float zoom){
        camera = new OrthographicCamera(viewportWidth, viewportHeight);
        camera.setToOrtho(false, viewportWidth, viewportHeight);
        camera.zoom = zoom;
        this.zoom = zoom;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        x = camera.position.x;
        y = camera.position.y;
        isGliding = false;
    }

    /**
     * TEMP: assuming bottom left corner of screen is pixel values 0,0
     *
     * Focus determined by caller
     * Updates camera positioning based on focus's current position (in pixels)
     * @param xPos x coordinate of focus's current location in pixels
     * @param yPos y coordinate of focus's current location in pixels
     * @param glide smoothed camera movement
     */
    public void updateCamera(float xPos, float yPos, boolean glide){
//        System.out.println(levelHeight); //levelHeight smaller for some reason?
        float width_scaled = viewportWidth*camera.zoom; //width of viewport zoomed in
        if(xPos > levelBounds.width - width_scaled + width_scaled/2){
            xPos = levelBounds.width - width_scaled + width_scaled/2;
        }
        if(xPos < width_scaled/2){
            xPos = width_scaled/2;
        }
        float height_scaled = viewportHeight*camera.zoom; //height of viewport zoomed in
        if(yPos > levelBounds.height - height_scaled + height_scaled/2){
            yPos = levelBounds.height - height_scaled + height_scaled/2;
        }
        if(yPos < height_scaled/2){
            yPos = height_scaled/2;
        }
        if(glide) {
            x += (xPos - x) * CAMERA_GLIDE_RATE;
            y += (yPos - y) * CAMERA_GLIDE_RATE;
        }
        else{
            x = xPos;
            y = yPos;
        }
        if(Math.abs((xPos - x) * CAMERA_GLIDE_RATE) < 0.1 && Math.abs((yPos - y) * CAMERA_GLIDE_RATE) < 0.1){
            isGliding = false;
            x = xPos;
            y = yPos;
        }
        else{
            isGliding = true;
        }
        camera.position.set(x, y, 0);
        camera.update();
    }

    /**
     * @return unwrapped OrthographicCamera
     */
    public OrthographicCamera getCamera(){
        return camera;
    }

    /**
     * Adjusting level size used for camera positioning calculations
     * @param bounds bounds of current level
     */
    public void setLevelBounds(Rectangle bounds){
//        System.out.println("setting level size:" + height);
        levelBounds = bounds;
    }

    /**
     * Zooms camera out to x1
     * @param z true if we want to zoom out
     */
    public void zoomOut(boolean z){
        if(z){
            camera.zoom = 1;
        }
        else{
            camera.zoom = zoom;
        }
    }

    /**
     * @return x-coordinate of camera position
     */
    public float getX(){
        return x;
    }

    /**
     * @return y-coordinate of camera position
     */
    public float getY(){
        return y;
    }

    /**
     * @return true if camera is moving
     */
    public boolean isGliding(){
        return isGliding;
    }
}
