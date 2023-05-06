package edu.cornell.gdiac.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.audio.AudioEngine;
import edu.cornell.gdiac.audio.AudioSource;
import edu.cornell.gdiac.audio.MusicQueue;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.game.object.DeadBody;
import edu.cornell.gdiac.util.PooledList;

import java.lang.reflect.Array;
import java.util.HashMap;

public class AudioController {

    /**
     * The hashmap for sounds
     */
    private HashMap<String, Sound> levelSoundMap;
    /**
     * The hashmap for music
     */
    private HashMap<String, AudioSource> levelMusicMap;

    /**
     * A queue to play level music
     */
    private MusicQueue levelMusic;
    private MusicQueue labMusic;
    private MusicQueue forestMusic;
    /**
     * A queue to play stage music
     */
    private MusicQueue stageMusic;
    private String currMusic = "";
    private int forestStart = 0;
    private int pos;


    /**
     * Creates a new Audio Controller
     * <p>
     * Creates the audio engine and music queues for level and stage music
     */
    public AudioController() {
        levelSoundMap = new HashMap<>();
        levelMusicMap = new HashMap<>();

        AudioEngine engine = (AudioEngine) Gdx.audio;
//        levelMusic = engine.newMusicBuffer(false, 44100);
        stageMusic = engine.newMusicBuffer(false, 44100);
        labMusic = engine.newMusicBuffer(false, 44100);
        forestMusic = engine.newMusicBuffer(false, 44100);

        levelMusic = labMusic;

        // TODO: automate this with the volume constant in internal loading json
        setVolume(0.0f);
//        levelMusic.setVolume(0.3f);
//        stageMusic.setVolume(0.3f);

        setLooping();
    }

    public void setLooping() {
        levelMusic.setLooping(true);
        stageMusic.setLooping(true);
//        levelMusic.setLoopBehavior(true);
//        stageMusic.setLoopBehavior(true);
    }

    /**
     * Sets the volume for all music and sound effects
     *
     * @param val the value to set volume to
     */
    public void setVolume(float val) {
        levelMusic.setVolume(0);
        stageMusic.setVolume(0);
        //TODO: set volume for all sound effects
//        for (HashMap.Entry<String, Sound> entry : soundAssetMap.entrySet()) {
//            Sound sound = entry.getValue();
//            sound.setVolume(val);
//        }
    }

    public String getCurrMusic() {
        return currMusic;
    }

    /**
     * Adds an AudioSource to the level music queue
     *
     * @param audio the music to add to the level music queue
     */
    public void addLevelMusic(AudioSource audio) {
        levelMusic.addSource(audio);
    }

    /**
     * Adds an AudioSource to the stage music queue
     *
     * @param audio the music to add to the stage music queue
     */
    public void addStageMusic(AudioSource audio) {
        stageMusic.addSource(audio);
    }

    /**
     * Gathers the sound effects and puts them into the sound asset map
     *
     * @param directory which holds the asset data
     * @param names     of all the sound effects to add
     */
    public void createSoundEffectMap(AssetDirectory directory, String[] names) {
        for (String n : names) {
            levelSoundMap.put(n, directory.getEntry(n, SoundEffect.class));
        }
    }

    /**
     * Gathers the level music and puts them into the music asset map
     *
     * @param directory which holds the asset data
     * @param names     of all the level music to add
     */
    public void createMusicMap(AssetDirectory directory, String[] names) {
        for (int i = 0; i < names.length; i++) {
            if (names[i].contains("forest") && forestStart == 0) {
                forestStart = i;
                forestMusic.addSource(directory.getEntry(names[i], AudioSource.class));
            } else {
                labMusic.addSource(directory.getEntry(names[i], AudioSource.class));
            }
            levelMusicMap.put(names[i], directory.getEntry(names[i], AudioSource.class));
        }
    }

    /**
     * Plays the lab music
     */
    public void playLab() {
//        for (int i = 0; i < levelMusicMap.size(); i++) {
//            levelMusic.setSource(i, levelMusicMap.get("bkg-lab"));
//        }
        levelMusic.pause();
        levelMusic = labMusic;
//        levelMusic.reset();
        currMusic = "metal";
    }

    /**
     * Plays the forest music
     */
    public void playForest() {
        levelMusic.pause();
        levelMusic = forestMusic;
//        levelMusic.jumpToSource(forestStart);
//        for (int i = 0; i < levelMusicMap.size(); i++) {
//            levelMusic.setSource(1, levelMusicMap.get("bkg-forest"));
//        }
        currMusic = "forest";
    }

    /**
     * Plays the current level music
     */
    public void playLevelMusic() {
        levelMusic.play();
    }

    /**
     * Plays a specific level music
     *
     * @param musicName the name of the music to play
     */
    public void playLevelMusic(String musicName) {
        levelMusic.setSource(1, levelMusicMap.get(musicName));
    }

    /**
     * Plays the current stage music
     */
    public void playStageMusic() {
        stageMusic.play();
        currMusic = "stage";
    }

    /**
     * Plays a specific sound effect
     *
     * @param soundName the name of the sound effect to play
     */
    public void playSoundEffect(String soundName) {
//        System.out.println("playing sound " + soundName);
        levelSoundMap.get(soundName).play();
    }

    /**
     * Pauses the current level music
     */
    public void pauseLevelMusic() {
        levelMusic.pause();
    }

    /**
     * Pauses the current stage music
     */
    public void pauseStageMusic() {
        stageMusic.pause();
    }

    /**
     * Returns the level music queue
     *
     * @return levelMusic
     */
    public MusicQueue getLevelMusic() {
        return levelMusic;
    }

    /**
     * Returns the stage music queue
     *
     * @return stageMusic
     */
    public MusicQueue getStageMusic() {
        return stageMusic;
    }

    /**
     * Resets the level music queue to the beginning of the queue
     */
    public void resetLevelMusic() {
        levelMusic.reset();
    }

    /**
     * Resets the stage music queue to the beginning of the queue
     */
    public void resetStageMusic() {
        stageMusic.reset();
    }

    /**
     * Advances the level music queue to the next AudioSource
     */
    public void nextLevelMusic() {
        levelMusic.advanceSource();
        setLooping();
    }

//    public void prevLevelMusic() {
//        levelMusic.
//    }

    /**
     * Advances the stage music queue to the next AudioSource
     */
    public void nextStageMusic() {
        stageMusic.advanceSource();
        setLooping();
    }
}