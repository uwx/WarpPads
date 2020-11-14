package raytech.warppads;

import org.bukkit.util.NumberConversions;

/**
 * Helpful class for handling vectors as primitives
 *
 * @author Maxine
 * @since 13/11/2020
 */
public final class VectorUtil {
    /**
     * Performs the operation:
     * <pre>
     * new Vector(x1, y1, z1)
     *   .distanceSquared(new Vector(x2, y2, z2));
     * </pre>
     *
     * @param x1 The x component of the first vector
     * @param y1 The y component of the first vector
     * @param z1 The z component of the first vector
     * @param x2 The x component of the second vector
     * @param y2 The y component of the second vector
     * @param z2 The z component of the second vector
     * @return The squared distance between both vectors
     */
    public static double distanceSquared(double x1, double y1, double z1, double x2, double y2, double z2) {
        return NumberConversions.square(x1 - x2) + NumberConversions.square(y1 - y2) + NumberConversions.square(z1 - z2);
    }

    /**
     * Performs the combo operation:
     * <pre>
     * (float) new Vector(x1, y1, z1)
     *   .subtract(new Vector(x2, y2, z2))
     *   .normalize()
     *   .distanceSquared(new Vector(dx, dy, dz));
     * </pre>
     *
     * @param x1 The x component of the input (destination) vector
     * @param y1 The y component of the input (destination) vector
     * @param z1 The z component of the input (destination) vector
     * @param x2 The x component of the intermediate (origin) vector
     * @param y2 The y component of the intermediate (origin) vector
     * @param z2 The z component of the intermediate (origin) vector
     * @param dx The x component of the vector to compare the distance against
     * @param dy The y component of the vector to compare the distance against
     * @param dz The z component of the vector to compare the distance against
     * @return The input vector, subtracted with the intermediate vector, normalized, and squared distance computed with
     *         the comparison vector.
     */
    public static float subtractNormalizeDistanceSquared(double x1, double y1, double z1, double x2, double y2, double z2, double dx, double dy, double dz) {
        // Subtract
        x1 -= x2;
        y1 -= y2;
        z1 -= z2;

        // Normalize
        double length = Math.sqrt((x1 * x1) + (y1 * y1) + (z1 * z1));

        x1 /= length;
        y1 /= length;
        z1 /= length;

        // Square of distance
        x1 -= dx;
        y1 -= dy;
        z1 -= dz;

        return (float) ((x1 * x1) + (y1 * y1) + (z1 * z1));
    }
}
