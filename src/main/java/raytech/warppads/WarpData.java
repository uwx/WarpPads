package raytech.warppads;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockVector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A collection of warps, usually tied to a {@link World}.
 *
 * @author Maxine
 * @since 12/11/2020
 */
class WarpData {
    /**
     * A table mapping warp locations to every warp contained in this {@link WarpData}
     */
    public final Map<BlockVector, Warp> warps = Collections.synchronizedMap(new HashMap<>());

    /**
     * A table mapping a combined key of (x / 1024 | z / 1024 << 16) to a list of warps in the relevant range.
     * This map partitions the world in 1024x1024 chunks, which is slightly larger than the 1000x1000 maximum range for
     * tier 1 warp pads, which allows for partitioning warp lists into smaller segments, not requiring to iterate
     * through every single warp in the world.
     *
     * @see #getSectorHash1024
     */
    public final Multimap<Integer, Warp> warpsBySector1024 = ArrayListMultimap.create();
    public static int getSectorHash1024(int x, int z) {
        short xSector = (short) (x / 1024);
        short zSector = (short) (z / 1024);

        return xSector | (zSector << 16);
    }
    
    public final Set<Player> playersStandingOnWarps = new HashSet<>();

    public static class Warp {
        public int x;
        public int y;
        public int z;
        public String label;

        public Warp(int x, int y, int z, String label) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.label = label;
        }

        public String save() {
            return x + "," + y + "," + z + "," + label;
        }
    }

    public static WarpData load(File warpdataFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(warpdataFile))) {
            WarpData warpData = new WarpData();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }

                String[] components = line.split(",");

                warpData.warps.add(new WarpData.Warp(Integer.parseInt(components[0]), Integer.parseInt(components[1]), Integer.parseInt(components[2]), components[3]));
            }

            return warpData;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
