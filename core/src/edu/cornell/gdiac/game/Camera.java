package edu.cornell.gdiac.game;

import com.badlogic.gdx.graphics.OrthographicCamera;

/**
 * Different from abstract class Camera
 */
public class Camera {
    /** Camera for the underlying SpriteBatch */
    private OrthographicCamera camera;
    private float viewportWidth;
    private float viewportHeight;
    private float zoom;
    private float x;
    private float y;
    private final float CAMERA_GLIDE_RATE = 0.075f;

    public Camera(float viewportWidth, float viewportHeight, float zoom){
        camera = new OrthographicCamera(viewportWidth, viewportHeight);
        camera.setToOrtho(false, viewportWidth, viewportHeight);
        this.zoom = zoom;
        camera.zoom = zoom;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        x = camera.position.x;
        y = camera.position.y;
    }
    /**
     * Focus determined by caller
     * Updates camera positioning based on focus's current position (in pixels)
     * @param xPos x coordinate of focus's current location in pixels
     * @param yPos y coordinate of focus's current location in pixels
     * @param glide smoothed camera movement
     */
    public void updateCamera(float xPos, float yPos, boolean glide){
        float width_scaled = viewportWidth*camera.zoom;
        if(xPos > viewportWidth - width_scaled + width_scaled/2){
            xPos = viewportWidth - width_scaled + width_scaled/2;
        }
        if(xPos < width_scaled/2){
            xPos = width_scaled/2;
        }
        float height_scaled = viewportHeight*camera.zoom;
        if(yPos > viewportHeight - height_scaled + height_scaled/2){
            yPos = viewportHeight - height_scaled + height_scaled/2;
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
    public OrthographicCamera getCamera(){
        return camera;
    }
    /**
     * For internal uses
     * Camera either zooms out for debugging or returns to original zoom for gameplay
     * @param debug Whether debug mode is active
     */
    public void debugCamera(boolean debug){
        if (debug)
            camera.zoom = 1;
        else
            camera.zoom = zoom;
    }
}
