package raytech.warppads;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
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
public class WarpData {
    /**
     * A table mapping warp locations to every warp contained in this {@link WarpData}
     */
    public final Map<BlockVector, Warp> warps = Collections.synchronizedMap(new HashMap<>());

    /**
     * A set of players currently standing on warp pads. Updated on player move.
     */
    public final Set<Player> playersStandingOnWarps = new HashSet<>();

    public static class Warp {
        public static final Particle.DustOptions particle = new Particle.DustOptions(Color.RED, 1);
        public static final Particle.DustOptions highlightParticle = new Particle.DustOptions(Color.PURPLE, 1);
        public static final ChatColor defaultLabelColor = ChatColor.LIGHT_PURPLE;

        public final int x;
        public final int y;
        public final int z;
        public final String label;
        public Particle.DustOptions highlightColor = highlightParticle;
        public ChatColor labelColor = defaultLabelColor;

        public Warp(int x, int y, int z, String label) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.label = label;
        }

        public String save() {
            if (labelColor != defaultLabelColor) {
                return x + "," + y + "," + z + "," + label + "," + highlightColor.getColor() + "," + labelColor.getChar();
            }

            if (highlightColor != highlightParticle) {
                return x + "," + y + "," + z + "," + label + "," + highlightColor.getColor();
            }

            return x + "," + y + "," + z + "," + label;
        }

        public BlockVector getVector() {
            return new BlockVector(x, y, z);
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

                Warp warp = new Warp(Integer.parseInt(components[0]), Integer.parseInt(components[1]), Integer.parseInt(components[2]), components[3]);

                if (components.length > 5) {
                    warp.labelColor = ChatColor.getByChar(components[5]);
                }

                if (components.length > 4) {
                    warp.highlightColor = new Particle.DustOptions(Color.fromRGB(Integer.parseInt(components[4])), 1);
                }

                warpData.warps.put(warp.getVector(), warp);
            }

            return warpData;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
