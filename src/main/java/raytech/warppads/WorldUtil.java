package raytech.warppads;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.function.Predicate;

/**
 * Created by Maxine on 13/11/2020.
 *
 * @author Maxine
 * @since 13/11/2020
 */
public final class WorldUtil {
    private static final Predicate<Entity> isPlayer = e -> e instanceof Player;

    public static boolean hasNearbyPlayers(World world, int x, int y, int z, int range) {
        return !world.getNearbyEntities(new BoundingBox(
                x - range, y - range, z - range, x + range, y + range, z + range
        ), isPlayer).isEmpty();
    }
}
