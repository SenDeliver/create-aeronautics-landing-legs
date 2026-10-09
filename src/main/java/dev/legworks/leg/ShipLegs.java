package dev.legworks.leg;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side index of the downward Legs on each Ship, plus the Legs with forces queued for the next flush.
 */
public final class ShipLegs {
    private static final Map<UUID, Set<Leg>> FEET = new HashMap<>();
    private static final Set<Leg> QUEUED = new LinkedHashSet<>();
    private static final Map<UUID, Long> LEVELED_AT = new HashMap<>();
    private static final Map<UUID, Long> LOST_LEG_AT = new HashMap<>();

    private ShipLegs() {
    }

    static void track(UUID ship, Leg leg) {
        FEET.computeIfAbsent(ship, u -> new HashSet<>()).add(leg);
    }

    static void untrack(UUID ship, Leg leg) {
        Set<Leg> set = FEET.get(ship);
        if (set == null) return;
        set.remove(leg);
        if (set.isEmpty()) {
            FEET.remove(ship);
            LEVELED_AT.remove(ship);
            LOST_LEG_AT.remove(ship);
        }
    }

    static List<Leg> feet(UUID ship) {
        Set<Leg> set = FEET.get(ship);
        if (set == null) return List.of();
        set.removeIf(l -> !l.isFootNow());
        List<Leg> list = new ArrayList<>(set);
        list.sort(Comparator.comparingLong(l -> l.pos().asLong()));
        return list;
    }

    static int footCount(UUID ship) {
        Set<Leg> set = FEET.get(ship);
        return set == null ? 0 : set.size();
    }

    static int groundedCount(UUID ship) {
        Set<Leg> set = FEET.get(ship);
        if (set == null) return 0;
        int n = 0;
        for (Leg l : set) if (l.ground() != null) n++;
        return n;
    }

    static boolean claimLeveling(UUID ship, long gameTime) {
        Long last = LEVELED_AT.put(ship, gameTime);
        return last == null || last != gameTime;
    }

    static void noteLegLost(UUID ship, long gameTime) {
        LOST_LEG_AT.put(ship, gameTime);
    }

    static boolean lostLegWithin(UUID ship, long gameTime, int ticks) {
        Long at = LOST_LEG_AT.get(ship);
        return at != null && gameTime - at < ticks;
    }

    static void queue(Leg leg) {
        QUEUED.add(leg);
    }

    static void dequeue(Leg leg) {
        QUEUED.remove(leg);
    }

    public static void flush() {
        for (Leg leg : QUEUED) leg.flushForces();
        QUEUED.clear();
    }

    public static void clear() {
        FEET.clear();
        QUEUED.clear();
        LEVELED_AT.clear();
        LOST_LEG_AT.clear();
    }
}
