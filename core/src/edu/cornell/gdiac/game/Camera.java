package edu.cornell.gdiac.game;

import com.badlogic.gdx.graphics.OrthographicCamera;

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
    /** Width of current level **/
    private float levelWidth;
    /** Height of current level **/
    private float levelHeight;
    /** Camera x-coordinate **/
    private float x;
    /** Camera y-coordinate **/
    private float y;
    /** Rate at which camera glides towards destination coordinates**/
    private final float CAMERA_GLIDE_RATE = 0.075f;

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
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.levelWidth = viewportWidth;
        this.levelHeight = viewportHeight;
        x = camera.position.x;
        y = camera.position.y;
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
        if(xPos > levelWidth - width_scaled + width_scaled/2){
            xPos = levelWidth - width_scaled + width_scaled/2;
        }
        if(xPos < width_scaled/2){
            xPos = width_scaled/2;
        }
        float height_scaled = viewportHeight*camera.zoom; //height of viewport zoomed in
        if(yPos > levelHeight - height_scaled + height_scaled/2){
            yPos = levelHeight - height_scaled + height_scaled/2;
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
     * @param width width of current level
     * @param height height of current level
     */
    public void setLevelSize(float width, float height){
//        System.out.println("setting level size:" + height);
        levelWidth = width;
        levelHeight = height;
    }

//
//    /**
//     * For internal uses
//     * Camera either zooms out for debugging or returns to original zoom for gameplay
//     * @param debug Whether debug mode is active
//     */
//    public void debugCamera(boolean debug){
//        if (debug)
//            camera.zoom = 1;
//        else
//            camera.zoom = zoom;
//    }

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
}
