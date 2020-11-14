package raytech.warppads;

import org.bukkit.World;
import org.bukkit.util.BlockVector;
import raytech.warppads.WarpData.Warp;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Created by Maxine on 14/11/2020.
 *
 * @author Maxine
 * @since 14/11/2020
 */
public class GlobalWarps {
    public final Map<World, WarpData> warpDataMap = new HashMap<>();

    public void loadWarps(World world, WarpData warpData) {
        warpDataMap.put(world, warpData);
    }

    public void forEach(BiConsumer<World, WarpData> action) {
        warpDataMap.forEach(action);
    }

    public WarpData getWarps(World world) {
        return warpDataMap.get(world);
    }

    public WarpData getWarpsOrAdd(World world) {
        WarpData warpData = warpDataMap.get(world);
        if (warpData == null) {
            warpData = new WarpData();
            warpDataMap.put(world, warpData);
        }
        return warpData;
    }

    public void add(World world, Warp warp) {
        WarpData warpData = getWarpsOrAdd(world);
        warpData.warps.put(new BlockVector(warp.x, warp.y, warp.z), warp);
    }

    public Warp get(World world, int x, int y, int z) {
        WarpData warpData = warpDataMap.get(world);
        if (warpData == null) {
            return null;
        }

        return warpData.warps.get(new BlockVector(x, y, z));
    }

    public Warp remove(World world, int x, int y, int z) {
        WarpData warpData = warpDataMap.get(world);
        if (warpData == null) {
            return null;
        }

        return warpData.warps.remove(new BlockVector(x, y, z));
    }
}
