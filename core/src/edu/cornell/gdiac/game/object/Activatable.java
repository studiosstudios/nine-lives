package edu.cornell.gdiac.game.object;

import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;


/** Interface implemented by all game objects that can be activated by buttons,
 * switches etc. All objects that implement this interface should have two private
 * fields <code>activated</code> and <code>initialActivation</code>,
 * and getters and setters for them. Additionally, all objects that implement this
 * interface should call <code>initActivations()</code> in their constructor. */
public interface Activatable {

    /** called whenever the object is activated */
    void activated(World world);

    /** called whenever the object is deactivated */
    void deactivated(World world);

    //region GETTERS AND SETTERS
    void setActivated(boolean activated);

    boolean isActivated();

    boolean getInitialActivation();

    void setInitialActivation(boolean initialActivation);

    /**
     * Initializes the state of the activatable. Must be called in the constructor
     * of any class that implements this interface.
     * @param data      JSON data of the activatable
     */
    default void initActivations(JsonValue data){
        if (data == null) {
            setActivated(false);
        } else {
            setActivated(data.getBoolean("active", true));
        }
        setInitialActivation(isActivated());
    }

    default void initTiledActivations(ObjectMap<String, Object> properties){
        if (properties == null) {
            setActivated(false);
        } else {
            setActivated((boolean) properties.get("active", true));
        }
        setInitialActivation(isActivated());
    }

    /**
     * Sets the active status of the object based on the output from an activator/s.
     * @param activator  whether the corresponding activators are active
     * @param world      the Box2D world
     *
     * @return  1 if this object was just activated, -1 if this object was just deactivated, 0 otherwise
     */
    default int updateActivated(boolean activator, World world){
        boolean next = getInitialActivation() ^ activator;
        if (next && !isActivated()) {
            //state switch from inactive to active
            setActivated(true);
            activated(world);
            return 1;
        } else if (!next && isActivated()){
            //state switch from active to inactive
            setActivated(false);
            deactivated(world);
            return -1;
        }
        return 0;
    }
    float getXPos();
    float getYPos();
}
