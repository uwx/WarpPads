package raytech.warppads;

import org.bukkit.configuration.file.FileConfiguration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by Maxine on 13/11/2020.
 *
 * @author Maxine
 * @since 13/11/2020
 */
public final class Config {
    //region Config
    /**
     * How often the decorative particles around warp pads are spawned, in server ticks.
     */
    public static long warpDecorationUpdateRate = 5L;

    /**
     * How close to a warp at least one player must be, in order for decorative particles to spawn near a warp pad, in
     * blocks.
     */
    public static int warpDecorationVisibilityDistance = 25;

    /**
     * How many particles are spawned, whenever a player is standing on a warp pad, pointing in the general direction of
     * other reachable warp pads, per warp pad.
     */
    public static int warpLineIterationCount = 5;

    /**
     * Circular radius in blocks, from one T1 warp, that other warps may be reached. Must not be greater than 46340.
     */
    public static int warpPadT1Range = 1000;
    //endregion

    public static void loadConfig(SpigotPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();

        Map<Class<?>, Function<String, Object>> getConfigFunctions = new HashMap<Class<?>, Function<String, Object>>() {{
            put(int.class, config::getInt);
            put(long.class, config::getLong);
        }};

        for (Field field : Config.class.getFields()) {
            if (!field.isAccessible() || Modifier.isFinal(field.getModifiers())) {
                continue;
            }

            if (!config.contains(field.getName())) {
                continue;
            }

            Function<String, Object> getter = getConfigFunctions.get(field.getType());
            if (getter == null) {
                throw new IllegalArgumentException("Unsupported field type: " + field.getType());
            }

            try {
                field.set(null, getter.apply(field.getName()));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
