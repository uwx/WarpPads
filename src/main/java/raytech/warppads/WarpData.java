package raytech.warppads;

import org.apache.commons.lang.StringUtils;
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
import java.util.UUID;

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

        public final UUID authorUUID;
        public final int x;
        public final int y;
        public final int z;
        public final String label;
        public Particle.DustOptions highlightColor = highlightParticle;
        public ChatColor labelColor = defaultLabelColor;

        public Warp(UUID authorUUID, int x, int y, int z, String label) {
            this.authorUUID = authorUUID;
            this.x = x;
            this.y = y;
            this.z = z;
            this.label = label;
        }

        /**
         * Serializes a warp pod to a string.
         * @return The serialized string
         */
        public String save() {
            // Append UUID
            String str = Long.toString(authorUUID.getMostSignificantBits(), 36) + "," + Long.toString(authorUUID.getLeastSignificantBits(), 36);

            // Append position
            str += Integer.toString(x, 36) + "," + Integer.toString(y, 36) + "," + Integer.toString(z, 36);

            // Append label (sanitized)
            str += "," + StringUtils.replaceChars(label, "\n\r,", "  _");

            // Append highlight and label colors
            if (highlightColor != highlightParticle) {
                str += "," + Integer.toString(highlightColor.getColor().asRGB(), 36);

                if (labelColor != defaultLabelColor) {
                    str += "," + labelColor.getChar();
                }
            }

            return str;
        }

        /**
         * Deserializes a warp pod from a string.
         * @param str The serialized string line
         * @return The deserialized warp pod instance
         */
        public static Warp load(String str) {
            String[] components = StringUtils.split(str, ',');

            Warp warp = new Warp(
                    new UUID(Long.parseLong(components[0], 36), Long.parseLong(components[1], 36)),
                    Integer.parseInt(components[2], 36),
                    Integer.parseInt(components[3], 36),
                    Integer.parseInt(components[4], 36),
                    components[5]
            );

            if (components.length > 6) {
                warp.highlightColor = new Particle.DustOptions(Color.fromRGB(Integer.parseInt(components[6], 36)), 1);
            }

            if (components.length > 7) {
                warp.labelColor = ChatColor.getByChar(components[7]);
            }
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

                Warp warp = Warp.load(line);

                warpData.warps.put(warp.getVector(), warp);
            }

            return warpData;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

}
