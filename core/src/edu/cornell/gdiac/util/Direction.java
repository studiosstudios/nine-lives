package edu.cornell.gdiac.util;

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
}


