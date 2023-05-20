package edu.cornell.gdiac.game.stage;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.game.Save;

import java.util.HashMap;

public class LevelSelectStage extends StageWrapper {
    private static final int BAR_WIDTH = 768;
    private static final int BAR_HEIGHT = STANDARD_HEIGHT - 100;
    private static final int SEGMENT_GAP = 5;
    private static final int SEGMENT_HEIGHT = 10;
    private Actor backButtonActor;
    private Actor playButtonActor;
    private Actor levelImage;
    private Actor leftArrowActor;
    private Actor rightArrowActor;
    /** State to keep track of whether the main menu button has been clicked */
    private int backButtonState;
    private int playButtonState;
    private Array<Actor> barActors;
    private HashMap<Actor, Integer> segmentIndex;
    private int selectedLevel = 0;
    private int progress;

    public boolean isBack() { return backButtonState == 2; }
    public int getBackButtonState() { return backButtonState; }
    public void setBackButtonState(int state) { backButtonState = state; }
    public boolean isPlay() { return playButtonState == 2; }
    public int getPlayButtonState() { return playButtonState; }
    public void setPlayButtonState(int state) { playButtonState = state; }
    public int getSelectedLevel() { return selectedLevel; }
//    public static void setNumLevels(int numLevels) { LevelSelectStage.numLevels = numLevels; }
    public LevelSelectStage(String internal, boolean createActors, int numLevels) {
        super(internal, createActors, false, numLevels);
//        changeLevel(Save.getProgress());
        changeLevel(numLevels);
    }

    private void createBar() {
        barActors = new Array<>(numLevels);
        segmentIndex = new HashMap<>(numLevels);
        float segmentWidth = (BAR_WIDTH / numLevels) - SEGMENT_GAP*2;
        float barStart = ((STANDARD_WIDTH - BAR_WIDTH) * 3 / 4) + SEGMENT_GAP*2;
        for (int i = 0; i < numLevels; i++) {
            Actor temp = addBarSegment(internal.getEntry("bar-segment", Texture.class), barStart+segmentWidth*i+SEGMENT_GAP*(i+1), BAR_HEIGHT, segmentWidth, SEGMENT_HEIGHT);
            if (i+1 > progress) {
                temp.setColor(Color.GRAY);
            }
            barActors.add(temp);
            segmentIndex.put(temp, i);
        }
    }

    private void createLevelImages() {

    }

    private Actor addBarSegment(Texture texture, float x, float y, float sx, float sy) {
        Actor actor = new Image(texture);
        actor.setSize(sx, sy);
        actor.setWidth(sx);
        actor.setHeight(sy);
        actor.setPosition(x, y);
        actor.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Actor a = event.getListenerActor();
                if (segmentIndex.get(a) + 1 <= progress) {
                    changeLevel(segmentIndex.get(a) + 1);
                }
            }

//            @Override
//            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
//                Actor a = event.getListenerActor();
//                a.setColor(Color.GRAY);
//            }
//
//            @Override
//            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
//                Actor a = event.getListenerActor();
//                a.setColor(Color.WHITE);
//            }
        });
        super.addActor(actor);
        return actor;
    }

    /**
     *
     */
    @Override
    public void createActors() {
//        progress = Save.getProgress();
        progress = numLevels;
        levelImage = new Actor();
        Actor background = addActor(internal.getEntry("bg-level-select", Texture.class), 0, 0);
        background.setScale(0.5f);
        backButtonActor = addActor(internal.getEntry("back", Texture.class),60,buttonY-225);
        backButtonActor.setScale(0.5f);
        playButtonActor = addActor(internal.getEntry("play", Texture.class), 60, buttonY-175);
        playButtonActor.setScale(0.5f);

        leftArrowActor = addActor(internal.getEntry("left-arrow", Texture.class), 165, 0);
        leftArrowActor.setScale(0.75f);
        leftArrowActor.setY(STANDARD_HEIGHT/2f-(leftArrowActor.getHeight()/2f));
        leftArrowActor.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (selectedLevel > 1) {
                    changeLevel(selectedLevel - 1);
                }
            }
        });

        rightArrowActor = addActor(internal.getEntry("right-arrow", Texture.class), 0, 0);
        rightArrowActor.setScale(0.75f);
        rightArrowActor.setPosition(STANDARD_WIDTH-rightArrowActor.getWidth()-150, STANDARD_HEIGHT/2f-(rightArrowActor.getHeight()/2f));
        rightArrowActor.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (selectedLevel < numLevels && selectedLevel < progress) {
                    changeLevel(selectedLevel + 1);
                }
            }
        });

        createBar();

        backButtonActor.addListener(createHoverListener(backButtonActor));
        playButtonActor.addListener(createHoverListener(playButtonActor));

        leftArrowActor.addListener(createHoverListener(leftArrowActor));
        rightArrowActor.addListener(createHoverListener(rightArrowActor));
    }

    /**
     * @param event
     * @param x
     * @param y
     * @param pointer
     * @param button
     * @return
     */
    @Override
    public boolean listenerTouchDown(InputEvent event, float x, float y, int pointer, int button) {
        Actor actor = event.getListenerActor();
        if (actor == backButtonActor) {
            backButtonState = 1;
            backButtonActor.setColor(Color.LIGHT_GRAY);
        } else if (actor == playButtonActor) {
            playButtonState = 1;
            playButtonActor.setColor(Color.LIGHT_GRAY);
        }
        return true;
    }

    /**
     * @param event
     * @param x
     * @param y
     * @param pointer
     * @param button
     */
    @Override
    public void listenerTouchUp(InputEvent event, float x, float y, int pointer, int button) {
        if (backButtonState == 1) {
            backButtonState = 2;
            backButtonActor.setColor(Color.WHITE);
        } else if (playButtonState == 1) {
            playButtonState = 2;
            playButtonActor.setColor(Color.WHITE);
        }
    }
    private void changeLevel(int level) {
        if (selectedLevel != 0) {
            barActors.get(selectedLevel-1).setColor(Color.WHITE);
        }
        selectedLevel = level;
        AssetDirectory i = new AssetDirectory("jsons/images/level" + selectedLevel + ".json");
        i.loadAssets();
        i.finishLoading();
        levelImage = createActor(i.getEntry("image", Texture.class),0,0);
        levelImage.setScale(0.35f);
        levelImage.setPosition(STANDARD_WIDTH/2f-levelImage.getWidth()*7f/40f, STANDARD_HEIGHT/2f-levelImage.getHeight()*7f/40f);
        addActor(levelImage);
        barActors.get(selectedLevel-1).setColor(Color.BROWN);
    }
}
