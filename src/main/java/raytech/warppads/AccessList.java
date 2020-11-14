package raytech.warppads;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles player access to warp pads.
 *
 * @author Maxine
 * @since 14/11/2020
 */
public class AccessList {
    private static class TransportAccessList {
        public HashMap<String, ArrayList<String>> accessList;
    }

    private static final Yaml yaml = new Yaml();
    private final SetMultimap<UUID, UUID> accessList = HashMultimap.create();

    public void loadFromFile(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            TransportAccessList transportAccessList = yaml.loadAs(fileInputStream, TransportAccessList.class);
            transportAccessList.accessList.forEach((owner, guests) -> {
                UUID ownerUUID = UUID.fromString(owner);
                for (String guest : guests) {
                    UUID guestUUID = UUID.fromString(guest);
                    accessList.put(ownerUUID, guestUUID);
                }
            });
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public String saveToString() {
        TransportAccessList transportAccessList = new TransportAccessList();
        accessList.keySet().forEach(ownerUUID -> {
            transportAccessList.accessList.put(
                    ownerUUID.toString(),
                    accessList.get(ownerUUID)
                            .stream()
                            .map(UUID::toString)
                            .collect(Collectors.toCollection(ArrayList::new))
            );
        });

        return yaml.dump(transportAccessList);
    }

    public boolean contains(UUID owner, UUID guest) {
        return accessList.containsEntry(owner, guest);
    }

    public void add(UUID owner, UUID guest) {
        accessList.put(owner, guest);
    }

    public void remove(UUID owner, UUID guest) {
        accessList.remove(owner, guest);
    }
}
