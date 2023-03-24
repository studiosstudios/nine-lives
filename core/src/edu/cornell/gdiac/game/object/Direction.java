package edu.cornell.gdiac.game.object;

/** helpful enum for lasers, mirrors and doors */
public enum Direction {UP, DOWN, LEFT, RIGHT;

    public static Direction angleToDir(int angle) {
        switch (angle) {
            case 0:
                return Direction.UP;
            case 90:
                return Direction.LEFT;
            case 180:
                return Direction.DOWN;
            case 270:
                return Direction.RIGHT;
            default:
                throw new RuntimeException("undefined angle");
        }
    }
}


