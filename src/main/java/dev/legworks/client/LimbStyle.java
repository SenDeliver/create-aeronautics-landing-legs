package dev.legworks.client;

import dev.legworks.LegworksIds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * The drawn look of a Limb: {@code hipDrop} in blocks (Mount face to hip pivot), parts as boxes in px (1/16 block). Segment parts (upper, lower) span {@code lenFrom..lenTo} of the segment.
 * Joint parts (hip, knee, foot) use absolute y top/bottom in px from their anchor: hip = Mount face, knee = knee centre, foot = pad bottom.
 * The first foot part is the ankle; the lower segment ends at its centre.
 */
public record LimbStyle(float hipDrop, List<Part> hip, List<Part> upper, List<Part> knee, List<Part> lower, List<Part> foot) {
    public LimbStyle(float hipDrop, List<Part> upper, List<Part> knee, List<Part> lower, List<Part> foot) {
        this(hipDrop, List.of(), upper, knee, lower, foot);
    }

    public record Part(float w, float d, float xOff, float zOff, float lenFrom, float lenTo, ResourceLocation tex, boolean tileV, boolean decal) {
        public Part(float w, float d, float xOff, float lenFrom, float lenTo, ResourceLocation tex, boolean tileV) {
            this(w, d, xOff, 0, lenFrom, lenTo, tex, tileV, false);
        }
    }

    private static ResourceLocation tex(String name) {
        return LegworksIds.id("block/" + name);
    }

    private static Part seg(float w, float d, float xOff, float from, float to, String tex) {
        return new Part(w, d, xOff, from, to, tex(tex), true);
    }

    private static Part joint(float w, float h, float d, float xOff, float zOff, float yTop, String tex, boolean tileV) {
        return new Part(w, d, xOff, zOff, yTop, yTop - h, tex(tex), tileV, false);
    }

    private static Part plate(float zOff) {
        return new Part(2.5f, 1, 0, zOff, 0, 1, tex("limb_long_plate"), true, false);
    }

    private static Part cogCore(float zOff) {
        return new Part(4, 0.28f, 0, zOff, 2, -2, tex("limb_long_sleeve"), false, false);
    }

    private static Part cog(float zOff) {
        return new Part(7, 0.5f, 0, zOff, 3.5f, -3.5f, tex("limb_long_cog"), false, true);
    }

    private static Part joint(float w, float h, float d, float yTop, String tex) {
        return joint(w, h, d, 0, 0, yTop, tex, false);
    }

    public static final LimbStyle SHORT = new LimbStyle(3 / 16f,
            List.of(joint(6, 4, 6, 0, "limb_short_joint")),
            List.of(seg(3, 3, 0, 0, 1, "limb_short_rod"), seg(5, 5, 0, 0.2f, 0.75f, "limb_short_spring")),
            List.of(joint(4, 4, 4, 2, "limb_short_joint"), joint(2, 2, 7, 1, "limb_short_rod")),
            List.of(seg(2.5f, 2.5f, 0, 0, 1, "limb_short_rod")),
            List.of(joint(3, 3, 3, 5, "limb_short_joint"), joint(8, 2, 8, 0, 0, 2, "limb_short_foot", true)));

    public static final LimbStyle LONG = new LimbStyle(3 / 16f,
            List.of(joint(6, 4, 6, 0, "limb_long_joint")),
            List.of(plate(2.25f), plate(-2.25f)),
            List.of(joint(4.5f, 4.5f, 3.4f, 0, 0, 2.25f, "limb_long_sleeve", false),
                    cogCore(2.9f), cogCore(-2.9f), cog(3.05f), cog(-3.05f)),
            List.of(seg(2.8f, 2.8f, 0, 0, 0.55f, "limb_long_sleeve"), seg(1.6f, 1.6f, 0, 0, 1, "limb_long_rod")),
            List.of(joint(3, 3, 3, 5, "limb_long_joint"), joint(6, 2, 10, 0, 0, 2, "limb_long_foot", true)));

    public static final LimbStyle HEAVY = new LimbStyle(4 / 16f,
            List.of(joint(8, 5, 8, 0, "limb_heavy_joint")),
            List.of(seg(5, 5, 0, 0, 1, "limb_heavy_strut"), seg(1.5f, 6.5f, 3.25f, 0.1f, 0.85f, "limb_heavy_armor")),
            List.of(joint(7, 7, 7, 3.5f, "limb_heavy_joint"), joint(2, 8, 8, 4.5f, 0, 4, "limb_heavy_armor", false)),
            List.of(seg(4, 4, 0, 0, 1, "limb_heavy_strut")),
            List.of(joint(6, 2, 6, 5, "limb_heavy_joint"), joint(12, 3, 12, 0, 0, 3, "limb_heavy_foot", true),
                    joint(2, 1, 2, -4.5f, -4.5f, 4, "limb_heavy_joint", false), joint(2, 1, 2, 4.5f, -4.5f, 4, "limb_heavy_joint", false),
                    joint(2, 1, 2, -4.5f, 4.5f, 4, "limb_heavy_joint", false), joint(2, 1, 2, 4.5f, 4.5f, 4, "limb_heavy_joint", false)));

    private static final Map<ResourceLocation, LimbStyle> BY_ITEM = Map.of(
            LegworksIds.id(LegworksIds.SHORT_LIMB), SHORT,
            LegworksIds.id(LegworksIds.LONG_LIMB), LONG,
            LegworksIds.id(LegworksIds.HEAVY_LIMB), HEAVY);

    public static LimbStyle of(ItemStack stack) {
        return BY_ITEM.getOrDefault(BuiltInRegistries.ITEM.getKey(stack.getItem()), SHORT);
    }
}
