package edu.cornell.gdiac.game;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

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
    /** Bounds of current level (in pixels) **/
    private Rectangle levelBounds = new Rectangle();
    /** Camera center x-coordinate **/
    private float x;
    /** Camera center y-coordinate **/
    private float y;
    /** Rate at which camera glides towards destination coordinates**/
    private final float CAMERA_GLIDE_NORMAL = 0.075f;
    /** Rate at which camera glides towards dead body **/
    private final float CAMERA_GLIDE_SWITCH_BODY = 0.025f;
    private float cameraGlideRate;
    /** Default gameplay zoom (not necessarily CAMERA_ZOOM) **/
    private float defaultZoom;
    /** Whether camera is moving **/
    private boolean isGliding;
    /** Default camera zoom when no other specified zoom */
    public static final float CAMERA_ZOOM = 0.6f;
    /** Zoom Goal: Can only be in increments of 0.01 */
    private float zoomGoal;


    /**
     * Wrapper for the OrthographicCamera class that takes into account dynamic level metadata.
     * @param viewportWidth width of visible window
     * @param viewportHeight height of visible window
     */
    public Camera(float viewportWidth, float viewportHeight){
        camera = new OrthographicCamera(viewportWidth, viewportHeight);
        camera.setToOrtho(false, viewportWidth, viewportHeight);
        camera.zoom = CAMERA_ZOOM;
        defaultZoom = camera.zoom;
        zoomGoal = camera.zoom;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        x = camera.position.x;
        y = camera.position.y;
        isGliding = false;
        cameraGlideRate = CAMERA_GLIDE_NORMAL;
    }
    /**
     * Focus determined by caller
     * Updates camera positioning based on focus's current position (in pixels)
     * @param xPos x coordinate of focus's current location in pixels
     * @param yPos y coordinate of focus's current location in pixels
     * @param glide smoothed camera movement
     */
    public void updateCamera(float xPos, float yPos, boolean glide){
//        System.out.println("GOAL:" + zoomGoal);
//        System.out.println("cam_zoom:" + camera.zoom);
        if(Math.abs(camera.zoom - zoomGoal) > 0.01)
        {
            camera.zoom = camera.zoom < zoomGoal ? camera.zoom + 0.01f:camera.zoom - 0.01f;
        }
        else {
            camera.zoom = zoomGoal;
        }
        float width_scaled = viewportWidth*camera.zoom; //width of viewport zoomed in
        if(xPos > levelBounds.width - width_scaled/2 + levelBounds.x){
            xPos = levelBounds.width - width_scaled + width_scaled/2 + levelBounds.x;
        }
        if(xPos < width_scaled/2 + levelBounds.x){
            xPos = width_scaled/2 + levelBounds.x;
        }
        float height_scaled = viewportHeight*camera.zoom; //height of viewport zoomed in
        if(yPos > levelBounds.height - height_scaled/2 + levelBounds.y){
            yPos = levelBounds.height - height_scaled + height_scaled/2 + levelBounds.y;
        }
        if(yPos < height_scaled/2 + levelBounds.y){
            yPos = height_scaled/2 + levelBounds.y;
        }
        if(glide) {
            x += (xPos - x) * cameraGlideRate;
            y += (yPos - y) * cameraGlideRate;
        }
        else{
            x = xPos;
            y = yPos;
        }
        if(Math.abs((xPos - x) * cameraGlideRate) < 0.1 && Math.abs((yPos - y) * cameraGlideRate) < 0.1){
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
     * @param scale  draw scale
     * @param world true if bounds in world units, false if bounds in pixels
     */
    public void setLevelBounds(Rectangle bounds, Vector2 scale, boolean world){
//        System.out.println("setting level size:" + height);
        if (!world) {
            levelBounds.set(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        else {
            levelBounds.set(bounds.x * scale.x, bounds.y * scale.y, bounds.width * scale.x, bounds.height * scale.y);
        }
    }

    /**
     * Sets to default or specified zoom
     * @param z true if we want to specify zoom, false if default zoom
     * @param zoom the value to zoom the camera to
     */
    public void setZoom(boolean z, float zoom){
        if(z){
            float scaleX = levelBounds.width/viewportWidth;
            float scaleY = levelBounds.height/viewportHeight;
            zoomGoal = Float.min(scaleX,Float.min(scaleY, Float.min(1, zoom)));
            System.out.println("this ran1");
//            camera.zoom = Float.min(scaleX,Float.min(scaleY, Float.min(1, zoom)));
        }
        else{
            zoomGoal = defaultZoom;
//            camera.zoom = defaultZoom;
        }
    }

    /**
     * Set default zoom
     * @param zoom the value to zoom the camera to
     */
    public void setDefaultZoom(float zoom){
        defaultZoom = zoom;
    }
    /**
     * Camera movement when switching bodies
     * @param deadX x-coordinate of dead cat
     * @param deadY y-coordinate of dead cat
     */
    public void switchBodyCam(float deadX, float deadY){
//        float xDiff = Math.abs(deadX-catX)+50; //50 is leeway constant
//        float yDiff = Math.abs(deadY-catY)+50; //50 is leeway constant
//        float centerX = (deadX+catX)/2;
//        float centerY = (deadY+catY)/2;
        updateCamera(deadX,deadY,true);
//        updateCamera(centerX,centerY,true);
//        camera.zoom = Float.max(camera.zoom, Float.max(xDiff/viewportWidth, yDiff/viewportHeight));
    }

    /**
     * Determines at how much at an offset to draw when level bounds smaller than viewport and entire level needs to be
     * drawn at an offset
     */
    public Vector2 centerLevelTranslation(){
//        float levelHeight = levelBounds.height / camera.zoom;
//        float levelWidth = levelBounds.width / camera.zoom;
//        float xVal = 0;
//        float yVal = 0;
//        if(levelWidth < viewportWidth){
//            xVal = (viewportWidth - levelWidth)/1.4f;
//        }
//        if(levelHeight < viewportHeight){
//            yVal = (viewportHeight - levelHeight)/1.4f;
//        }
//        return new Vector2(xVal, yVal);
        return new Vector2(0,0);
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

    /**
     * Sets camera glide rate based on different game mode/functionalities
     * @param mode Either "SWITCH_BODY" or "NORMAL"
     */
    public void setGlideMode(String mode){
        if(mode.equals("SWITCH_BODY")){
            cameraGlideRate = CAMERA_GLIDE_SWITCH_BODY;
        }
        else if(mode.equals("NORMAL")){
            cameraGlideRate = CAMERA_GLIDE_NORMAL;
        }
        else{
            System.out.println("rip setGlideMode lmao");
        }
    }
}
