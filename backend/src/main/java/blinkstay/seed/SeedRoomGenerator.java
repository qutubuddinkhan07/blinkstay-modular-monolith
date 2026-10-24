package blinkstay.seed;

import blinkstay.seed.dto.SeedRoom;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class SeedRoomGenerator {

    private final SecureRandom random = new SecureRandom();

    public List<SeedRoom> generateRooms(BigDecimal basePrice) {
        List<SeedRoom> rooms = new ArrayList<>();
        int numberOfRoomTypes = random.nextInt(3) + 1;

        List<String> types = new ArrayList<>(List.of("single", "double", "suite"));

        Collections.shuffle(types, random);

        for (int i = 0; i < numberOfRoomTypes; i++) {
            SeedRoom room = new SeedRoom();
            String type = types.get(i);
            room.setType(type);
            BigDecimal adjustment;

            switch (type) {
                case "single" -> adjustment = BigDecimal.ZERO;
                case "double" -> adjustment = BigDecimal.valueOf(random.nextInt(6) * 500L);
                case "suite" -> adjustment = BigDecimal.valueOf(2500 + random.nextInt(6) * 500L);
                default -> adjustment = BigDecimal.ZERO;
            }

            room.setPriceAdjustment(adjustment);
            room.setTotalRooms(random.nextInt(20) + 5);
            rooms.add(room);
        }

        return rooms;
    }
}