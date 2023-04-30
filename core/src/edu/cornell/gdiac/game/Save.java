package edu.cornell.gdiac.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class Save {
    private final static String prefsName = "save";

    public static boolean exists() {
        return Gdx.app.getPreferences(prefsName).getBoolean("exists", false);
    }

    public static void create() {
        Preferences prefs =  Gdx.app.getPreferences(prefsName);
        prefs.putFloat("vol", 0.5f);

        prefs.putBoolean("exists", true);
        prefs.flush();
    }

    public static float getVolume() {
        return  Gdx.app.getPreferences(prefsName).getFloat("vol");
    }

    public static void setVolume(float vol) {
        Preferences prefs = Gdx.app.getPreferences(prefsName);
        prefs.putFloat("vol", vol);
        prefs.flush();
    }
}
