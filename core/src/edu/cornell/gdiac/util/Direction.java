package edu.cornell.gdiac.util;

import com.badlogic.gdx.math.Vector2;

/** Helpful enum for lasers, mirrors, doors, or anything that has different
 * behaviour depending on its angle. */
public enum Direction {UP, DOWN, LEFT, RIGHT;

    /**
     * Converts an degree angle to a Direction enum. 0 degrees is up, and increasing
     * in angle corresponds to rotating counterclockwise.
     * @param angle  Angle in degrees, must be a multiple of 90.
     * @return       Direction enum of angle.
     */
    public static Direction angleToDir(int angle) {
        switch (angle % 360) {
            case 0:
                return Direction.UP;
            case 90:
                return Direction.LEFT;
            case 180:
                return Direction.DOWN;
            case 270:
                return Direction.RIGHT;
            default:
                throw new IllegalArgumentException("Angle cannot be cast to Direction: " + angle);
        }
    }

    /**
     * Converts a Direction enum to a degree angle.
     * @param dir Angle in Direction, where UP is 0 degrees and LEFT is 90 degrees and so forth
     * @return Corresponding Direction enum
     */
    public static float dirToDegrees(Direction dir) {
        switch (dir) {
            case UP:
                return 0;
            case LEFT:
                return 90;
            case DOWN:
                return 180;
            default:
                return 270;
        }
    }

    /**
     * Rotates a vector by a given Direction, where up is 0 degrees.
     *
     * @param vec    Vector to rotate
     * @param dir    Direction to rotate by
     */
    public static void rotateVector(Vector2 vec, Direction dir) {
        switch (dir) {
            default:
            case UP:
                break;
            case DOWN:
                vec.scl(-1);
                break;
            case LEFT:
                vec.set(-vec.y, -vec.x);
                break;
            case RIGHT:
                vec.set(vec.y, -vec.x);
                break;
        }
    }

}


