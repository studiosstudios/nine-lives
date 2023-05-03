package edu.cornell.gdiac.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.audio.AudioEngine;
import edu.cornell.gdiac.audio.AudioSource;
import edu.cornell.gdiac.audio.MusicQueue;
import edu.cornell.gdiac.audio.SoundEffect;

import java.util.HashMap;

public class AudioController {

    /** The hashmap for sounds */
    private HashMap<String, Sound> soundAssetMap;
    /** The hashmap for music */
    private HashMap<String, AudioSource> musicAssetMap;
    /** A queue to play music */
    MusicQueue music;


    public AudioController() {
        soundAssetMap = new HashMap<>();
        musicAssetMap = new HashMap<>();

        AudioEngine engine = (AudioEngine) Gdx.audio;
        music = engine.newMusicBuffer( false, 44100 );
        // TODO: automate this with the volume constant in internal loading json
        music.setVolume(0.3f);
//        music.addSource( internal.getEntry("bkg-intro", AudioSource.class) );
        music.setLooping(true);
//        music.play();
    }

    public void setVolume(float val) { music.setVolume(val); }

    public void addMusic(AudioSource audio) {
        music.addSource(audio);
    }

    public void createSoundEffectMap(AssetDirectory directory, String[] names) {
        for (String n : names){
            soundAssetMap.put(n, directory.getEntry(n, SoundEffect.class));
        }
    }

    public void playMusic() {
        music.play();
    }

    public void playSoundEffect(String soundName) {
        System.out.println("playing sound " + soundName);
        soundAssetMap.get(soundName).play();
    }

    public void pauseMusic() {
        music.pause();
    }

    public MusicQueue getMusic() {
        return music;
    }

    public void nextMusic() {
        music.advanceSource();
    }

}
