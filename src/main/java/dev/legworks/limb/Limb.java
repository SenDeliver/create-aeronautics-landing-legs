package dev.legworks.limb;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.legworks.registry.LegworksDataComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The legworks:limb data component. Any item carrying it is a Limb and fits any Leg Mount.
 *
 * @param reach       maximum Reach in blocks
 * @param nominalRest default rest length (Simple Leg Ride Height)
 * @param minRest     shortest rest length Leveling and Crouch may use
 * @param stiffness   base Stiffness multiplier (1 = sag of {@code restSag} at full load)
 */
public record Limb(float reach, float nominalRest, float minRest, float stiffness) {
    public static final float MAX_REACH = 16f;

    public static final Codec<Limb> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.floatRange(0.25f, MAX_REACH).fieldOf("reach").forGetter(Limb::reach),
            Codec.floatRange(0.1f, MAX_REACH).fieldOf("nominal_rest").forGetter(Limb::nominalRest),
            Codec.floatRange(0.05f, MAX_REACH).fieldOf("min_rest").forGetter(Limb::minRest),
            Codec.floatRange(0.05f, 100f).fieldOf("stiffness").forGetter(Limb::stiffness)
    ).apply(i, Limb::new));

    public static final StreamCodec<ByteBuf, Limb> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Limb::reach,
            ByteBufCodecs.FLOAT, Limb::nominalRest,
            ByteBufCodecs.FLOAT, Limb::minRest,
            ByteBufCodecs.FLOAT, Limb::stiffness,
            Limb::new);

    public static final Limb SHORT = new Limb(3.0f, 2.0f, 0.7f, 1f);
    public static final Limb LONG = new Limb(4.5f, 3.0f, 1.0f, 1f);
    public static final Limb HEAVY = new Limb(3.5f, 2.3f, 0.8f, 2f);

    public Limb {
        reach = Math.max(0.25f, Math.min(MAX_REACH, reach));
        minRest = Math.max(0.05f, Math.min(reach, minRest));
        nominalRest = Math.max(minRest, Math.min(reach, nominalRest));
        stiffness = Math.max(0.05f, stiffness);
    }

    @Nullable
    public static Limb of(ItemStack stack) {
        return stack.isEmpty() ? null : stack.get(LegworksDataComponents.LIMB.get());
    }

    public static boolean isLimb(ItemStack stack) {
        return of(stack) != null;
    }

    public double clampRest(double rest) {
        return Math.max(minRest, Math.min(reach, rest));
    }
}
