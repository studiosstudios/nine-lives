package edu.cornell.gdiac.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.audio.AudioEngine;
import edu.cornell.gdiac.audio.AudioSource;
import edu.cornell.gdiac.audio.MusicQueue;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.util.PooledList;

import java.util.HashMap;

public class AudioController {

    /** The hashmap for sounds */
    private HashMap<String, Sound> soundAssetMap;
    /** The hashmap for music */
    private HashMap<String, AudioSource> musicAssetMap;
    /** A queue to play music */
    MusicQueue levelMusic;
    MusicQueue stageMusic;
    private AudioSource previousMusic;


    public AudioController() {
        soundAssetMap = new HashMap<>();
        musicAssetMap = new HashMap<>();

        AudioEngine engine = (AudioEngine) Gdx.audio;
        levelMusic = engine.newMusicBuffer( false, 44100 );
        stageMusic = engine.newMusicBuffer( false, 44100 );

        // TODO: automate this with the volume constant in internal loading json
        levelMusic.setVolume(0.3f);
        stageMusic.setVolume(0.3f);
//        music.addSource( internal.getEntry("bkg-intro", AudioSource.class) );
        levelMusic.setLooping(true);
        stageMusic.setLooping(true);
//        music.play();
    }

    public void setVolume(float val) {
        levelMusic.setVolume(val);
        stageMusic.setVolume(val);
//        for (HashMap.Entry<String, Sound> entry : soundAssetMap.entrySet()) {
//            Sound sound = entry.getValue();
//            sound.setVolume(val);
//        }
    }

    public void addMusicToLevel(AudioSource audio) {
        levelMusic.addSource(audio);
    }

    public void addMusicToStage(AudioSource audio) {
        stageMusic.addSource(audio);
    }

    public void createSoundEffectMap(AssetDirectory directory, String[] names) {
        for (String n : names){
            soundAssetMap.put(n, directory.getEntry(n, SoundEffect.class));
        }
    }

    public void createMusicMap(AssetDirectory directory, String[] names) {
        for (String n : names){
            musicAssetMap.put(n, directory.getEntry(n, AudioSource.class));
            levelMusic.addSource(directory.getEntry(n, AudioSource.class));
        }
    }

    public void playLab() {
        levelMusic.setSource(1, musicAssetMap.get("bkg-lab"));
    }

    public void playForest() {
        levelMusic.setSource(2, musicAssetMap.get("bkg-forest"));
    }

    public void playLevelMusic() {
        levelMusic.play();
    }

    public void playStageMusic() {
        stageMusic.play();
    }

    public void playLevelMusic(String musicName) {
        levelMusic.setSource( 1,musicAssetMap.get(musicName));
    }

    public void playSoundEffect(String soundName) {
//        System.out.println("playing sound " + soundName);
        soundAssetMap.get(soundName).play();
    }

    public void pauseLevelMusic() {
        levelMusic.pause();
    }

    public void pauseStageMusic() {
        stageMusic.pause();
    }

//    public void menuMusic() {
//        previousMusic  = music.getCurrent();
//        music.setSource(0, musicAssetMap.get("bkg-intro"));
//    }

    public MusicQueue getLevelMusic() {
        return levelMusic;
    }

    public MusicQueue getStageMusic() {
        return stageMusic;
    }
//
//    public void resumeMusic() {
//        if (previousMusic != null) {
//            music.setSource(1, previousMusic);
//        }
//    }

    public void nextLevelMusic() {
        levelMusic.advanceSource();
    }

    public void nextStageMusic() {
        stageMusic.advanceSource();
    }

}
