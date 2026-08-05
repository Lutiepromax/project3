package com.radel.obsidiantnt.logic;

import com.radel.obsidiantnt.ObsidianTNTMod;
import com.radel.obsidiantnt.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SEntityVelocityPacket;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Uses the vanilla TNT entity type so Minecraft and OptiFine always use the
 * built-in TNT renderer. The server tracks which vanilla TNT entities are
 * super TNT and replaces their final tick with the custom explosion.
 */
@Mod.EventBusSubscriber(modid = ObsidianTNTMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PowerTNTTracker {
    private static final String POWER_TAG = ObsidianTNTMod.MOD_ID + ":power_tnt";
    private static final float STRONG_POWER = 40.0F;
    private static final double ABSORBER_DAMAGE_RADIUS = 4.0D;
    private static final double RAY_SAMPLE_STEP = 0.05D;
    private static final double KNOCKBACK_MULTIPLIER = 10.0D;

    private static final Map<UUID, TNTEntity> TRACKED = new LinkedHashMap<>();

    private PowerTNTTracker() {
    }

    public static TNTEntity spawn(World world, double x, double y, double z,
                                  @Nullable LivingEntity owner, int fuse) {
        TNTEntity tnt = new TNTEntity(world, x, y, z, owner);
        tnt.setFuse(fuse);
        tnt.getPersistentData().putBoolean(POWER_TAG, true);
        world.addFreshEntity(tnt);
        TRACKED.put(tnt.getUUID(), tnt);
        world.playSound(null, x, y, z,
                SoundEvents.TNT_PRIMED, SoundCategory.BLOCKS, 1.0F, 1.0F);
        return tnt;
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinWorldEvent event) {
        if (!event.getWorld().isClientSide && event.getEntity() instanceof TNTEntity) {
            TNTEntity tnt = (TNTEntity) event.getEntity();
            if (tnt.getPersistentData().getBoolean(POWER_TAG)) {
                TRACKED.put(tnt.getUUID(), tnt);
            }
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.world.isClientSide) {
            return;
        }

        Iterator<Map.Entry<UUID, TNTEntity>> iterator = TRACKED.entrySet().iterator();
        while (iterator.hasNext()) {
            TNTEntity tnt = iterator.next().getValue();
            if (tnt == null || !tnt.isAlive() || tnt.level != event.world) {
                iterator.remove();
                continue;
            }

            keepSolidHitbox(tnt);

            if (tnt.getFuse() <= 1) {
                explode(tnt);
                iterator.remove();
            }
        }
    }

    /** Makes the primed vanilla TNT behave like a solid 0.98 x 0.98 block. */
    private static void keepSolidHitbox(TNTEntity tnt) {
        AxisAlignedBB box = new AxisAlignedBB(
                tnt.getX() - 0.49D, tnt.getY(), tnt.getZ() - 0.49D,
                tnt.getX() + 0.49D, tnt.getY() + 0.98D, tnt.getZ() + 0.49D
        );

        List<Entity> entities = tnt.level.getEntities(tnt, box.inflate(0.08D),
                entity -> entity.isAlive() && !entity.isSpectator());

        for (Entity entity : entities) {
            double dx = entity.getX() - tnt.getX();
            double dz = entity.getZ() - tnt.getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length < 1.0E-4D) {
                dx = 1.0D;
                dz = 0.0D;
                length = 1.0D;
            }

            double push = 0.12D;
            entity.push(dx / length * push, 0.0D, dz / length * push);
            entity.hurtMarked = true;
            if (entity instanceof ServerPlayerEntity) {
                ((ServerPlayerEntity) entity).connection.send(new SEntityVelocityPacket(entity));
            }
        }
    }

    private static void explode(TNTEntity tnt) {
        double centerX = tnt.getX();
        double centerY = tnt.getY(0.0625D);
        double centerZ = tnt.getZ();

        List<BlockPos> exposedAbsorbers = findExposedAbsorbers(tnt.level, centerX, centerY, centerZ);
        Map<Entity, Vector3d> movementBefore = captureMovement(tnt, centerX, centerY, centerZ,
                STRONG_POWER * 2.0D);

        tnt.remove();
        tnt.level.explode(tnt, centerX, centerY, centerZ, STRONG_POWER, Explosion.Mode.BREAK);
        applyTenfoldKnockback(movementBefore);

        for (BlockPos pos : exposedAbsorbers) {
            damageAbsorber(tnt.level, pos);
        }
    }

    private static Map<Entity, Vector3d> captureMovement(TNTEntity source,
                                                          double x, double y, double z,
                                                          double radius) {
        AxisAlignedBB area = new AxisAlignedBB(
                x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius
        );
        List<Entity> entities = source.level.getEntities(source, area,
                entity -> entity.isAlive() && !entity.ignoreExplosion());
        Map<Entity, Vector3d> result = new IdentityHashMap<>();
        for (Entity entity : entities) {
            result.put(entity, entity.getDeltaMovement());
        }
        return result;
    }

    private static void applyTenfoldKnockback(Map<Entity, Vector3d> movementBefore) {
        for (Map.Entry<Entity, Vector3d> entry : movementBefore.entrySet()) {
            Entity entity = entry.getKey();
            if (!entity.isAlive()) {
                continue;
            }
            Vector3d before = entry.getValue();
            Vector3d after = entity.getDeltaMovement();
            Vector3d blast = after.subtract(before);
            if (blast.lengthSqr() < 1.0E-8D) {
                continue;
            }
            entity.setDeltaMovement(before.add(blast.scale(KNOCKBACK_MULTIPLIER)));
            entity.hurtMarked = true;
            if (entity instanceof ServerPlayerEntity) {
                ((ServerPlayerEntity) entity).connection.send(new SEntityVelocityPacket(entity));
            }
        }
    }

    private static List<BlockPos> findExposedAbsorbers(World world,
                                                        double centerX, double centerY, double centerZ) {
        List<BlockPos> result = new ArrayList<>();
        int radius = (int) Math.ceil(ABSORBER_DAMAGE_RADIUS);
        int baseX = (int) Math.floor(centerX);
        int baseY = (int) Math.floor(centerY);
        int baseZ = (int) Math.floor(centerZ);
        double radiusSquared = ABSORBER_DAMAGE_RADIUS * ABSORBER_DAMAGE_RADIUS;

        for (int x = baseX - radius; x <= baseX + radius; x++) {
            for (int y = baseY - radius; y <= baseY + radius; y++) {
                for (int z = baseZ - radius; z <= baseZ + radius; z++) {
                    double dx = x + 0.5D - centerX;
                    double dy = y + 0.5D - centerY;
                    double dz = z + 0.5D - centerZ;
                    if (dx * dx + dy * dy + dz * dz > radiusSquared) {
                        continue;
                    }
                    BlockPos target = new BlockPos(x, y, z);
                    if (isAbsorber(world.getBlockState(target))
                            && !hasAbsorberBeforeTarget(world, centerX, centerY, centerZ, target)) {
                        result.add(target);
                    }
                }
            }
        }
        return result;
    }

    private static boolean hasAbsorberBeforeTarget(World world,
                                                    double centerX, double centerY, double centerZ,
                                                    BlockPos target) {
        double dx = target.getX() + 0.5D - centerX;
        double dy = target.getY() + 0.5D - centerY;
        double dz = target.getZ() + 0.5D - centerZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = Math.max(1, (int) Math.ceil(distance / RAY_SAMPLE_STEP));
        BlockPos previous = null;

        for (int step = 1; step < steps; step++) {
            double progress = step / (double) steps;
            BlockPos sample = new BlockPos(
                    centerX + dx * progress,
                    centerY + dy * progress,
                    centerZ + dz * progress
            );
            if (sample.equals(target)) {
                return false;
            }
            if (sample.equals(previous)) {
                continue;
            }
            previous = sample;
            if (isAbsorber(world.getBlockState(sample))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAbsorber(BlockState state) {
        return state.is(Blocks.OBSIDIAN)
                || state.is(Blocks.CRYING_OBSIDIAN)
                || state.is(Blocks.ANCIENT_DEBRIS)
                || state.is(ModBlocks.DAMAGED_CRYING_OBSIDIAN_2.get())
                || state.is(ModBlocks.DAMAGED_CRYING_OBSIDIAN_1.get())
                || state.is(ModBlocks.DAMAGED_ANCIENT_DEBRIS_3.get())
                || state.is(ModBlocks.DAMAGED_ANCIENT_DEBRIS_2.get())
                || state.is(ModBlocks.DAMAGED_ANCIENT_DEBRIS_1.get());
    }

    private static void damageAbsorber(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.is(Blocks.OBSIDIAN)) {
            world.destroyBlock(pos, false);
            return;
        }

        if (state.is(Blocks.CRYING_OBSIDIAN)) {
            world.setBlockAndUpdate(pos, ModBlocks.DAMAGED_CRYING_OBSIDIAN_2.get().defaultBlockState());
        } else if (state.is(ModBlocks.DAMAGED_CRYING_OBSIDIAN_2.get())) {
            world.setBlockAndUpdate(pos, ModBlocks.DAMAGED_CRYING_OBSIDIAN_1.get().defaultBlockState());
        } else if (state.is(ModBlocks.DAMAGED_CRYING_OBSIDIAN_1.get())) {
            world.destroyBlock(pos, false);
        } else if (state.is(Blocks.ANCIENT_DEBRIS)) {
            world.setBlockAndUpdate(pos, ModBlocks.DAMAGED_ANCIENT_DEBRIS_3.get().defaultBlockState());
        } else if (state.is(ModBlocks.DAMAGED_ANCIENT_DEBRIS_3.get())) {
            world.setBlockAndUpdate(pos, ModBlocks.DAMAGED_ANCIENT_DEBRIS_2.get().defaultBlockState());
        } else if (state.is(ModBlocks.DAMAGED_ANCIENT_DEBRIS_2.get())) {
            world.setBlockAndUpdate(pos, ModBlocks.DAMAGED_ANCIENT_DEBRIS_1.get().defaultBlockState());
        } else if (state.is(ModBlocks.DAMAGED_ANCIENT_DEBRIS_1.get())) {
            world.destroyBlock(pos, false);
        }
    }
}
