package edu.cornell.gdiac.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;

public class Save {
    private final static String prefsName = "Nine Lives";
    private final static Integer keyCount = 11;

    public static boolean exists() {
        return Gdx.app.getPreferences(prefsName).getBoolean("exists", false);
    }

    public static void create() {
        Preferences prefs =  Gdx.app.getPreferences(prefsName);
        prefs.putFloat("vol", 0.5f);
        prefs.putFloat("music", 0.5f);

        prefs.putInteger("key0", Input.Keys.UP);
        prefs.putInteger("key1", Input.Keys.DOWN);
        prefs.putInteger("key2", Input.Keys.RIGHT);
        prefs.putInteger("key3", Input.Keys.LEFT);
        prefs.putInteger("key4", Input.Keys.C);
        prefs.putInteger("key5", Input.Keys.X);
        prefs.putInteger("key6", Input.Keys.Z);
        prefs.putInteger("key7", Input.Keys.SHIFT_LEFT);
        prefs.putInteger("key8", Input.Keys.CONTROL_LEFT);
        prefs.putInteger("key9", Input.Keys.U);
        prefs.putInteger("key10", Input.Keys.TAB);

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

    public static float getMusic() {
        return  Gdx.app.getPreferences(prefsName).getFloat("music");
    }

    public static void setMusic(float vol) {
        Preferences prefs = Gdx.app.getPreferences(prefsName);
        prefs.putFloat("music", vol);
        prefs.flush();
    }
    public static int[] getControls() {
        int[] controls = new int[keyCount];
        Preferences prefs = Gdx.app.getPreferences(prefsName);
        for(int i = 0; i<keyCount; i++) {
            controls[i] = prefs.getInteger("key"+i);
        }
        return controls;
    }

    public static void setControls(int[] controls) {
        Preferences prefs = Gdx.app.getPreferences(prefsName);
        assert controls.length == keyCount;
        for(int i = 0; i< controls.length; i++) {
            prefs.putInteger("key"+i, controls[i]);
        }
        prefs.flush();
    }
}
