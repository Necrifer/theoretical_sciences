package com.theoreticalsciences.item;

import com.theoreticalsciences.machine.ControllerBlockEntity;
import com.theoreticalsciences.multiblock.MultiblockDefinition;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Structure Assembler: right-click a multiblock controller to build every missing block of its structure,
 * taking the casings from your inventory (free in creative). Sneak-right-click for a dry run: it highlights
 * what is missing and lists the materials you still need, without placing anything.
 * <p>
 * It never breaks blocks: anything in the way is only highlighted, so it cannot destroy your builds.
 * Tiered components (coils, field emitters) use the best tier in your inventory; in creative, hold the tier you
 * want in your off-hand.
 */
public class StructureAssemblerItem extends LoreItem {
    public StructureAssemblerItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer player))
            return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(ctx.getClickedPos());
        if (!(be instanceof ControllerBlockEntity controller) || controller.definition() == null) {
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.assembler_not_controller").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        MultiblockDefinition def = controller.definition();
        MultiblockDefinition.ScanResult scan = def.scan(level, controller.getBlockPos(), controller.facing(),
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("theoreticalsciences", "interface_port"));
        if (scan.complete()) {
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.assembler_complete").withStyle(ChatFormatting.AQUA), true);
            controller.reportStructure(player);
            return InteractionResult.CONSUME;
        }
        boolean dryRun = player.isShiftKeyDown();
        boolean creative = player.getAbilities().instabuild;
        // build bottom-up so nothing ends up floating mid-build
        List<BlockPos> todo = new ArrayList<>(scan.missing());
        todo.sort(Comparator.comparingInt(BlockPos::getY));
        Map<Block, Integer> lacking = new LinkedHashMap<>();
        int placed = 0, blocked = 0;
        for (BlockPos p : todo) {
            char c = scan.expected().get(p);
            BlockState current = level.getBlockState(p);
            if (c == '.') {                       // must be air, but something is there: never break it, just show it
                blocked++;
                level.sendParticles(player, com.theoreticalsciences.registry.QHParticles.FAULT.get(), true, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 1, 0, 0, 0, 0);
                continue;
            }
            List<Block> options = def.componentsFor(c, ControllerBlockEntity.portBlock());   // never the optional port
            if (options.isEmpty()) continue;
            Block want = options.get(0);
            if (options.size() > 1) {
                // tiered component (coils, emitters): creative uses the tier held in the off-hand;
                // survival uses the best tier you are carrying
                if (creative) {
                    if (player.getOffhandItem().getItem() instanceof BlockItem bi && options.contains(bi.getBlock())) want = bi.getBlock();
                } else {
                    for (int k = options.size() - 1; k >= 0; k--)
                        if (count(player.getInventory(), options.get(k)) > 0) {
                            want = options.get(k);
                            break;
                        }
                }
            }
            if (!current.canBeReplaced()) {
                blocked++;
                level.sendParticles(player, com.theoreticalsciences.registry.QHParticles.FAULT.get(), true, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 1, 0, 0, 0, 0);
                continue;
            }
            if (dryRun) {
                lacking.merge(want, 1, Integer::sum);
                level.sendParticles(player, com.theoreticalsciences.registry.QHParticles.ARC_SPARK.get(), true, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0);
                continue;
            }
            if (!player.mayBuild() || !level.mayInteract(player, p)) {
                blocked++;
                continue;
            }
            if (!creative && !takeFromInventory(player.getInventory(), want)) {
                lacking.merge(want, 1, Integer::sum);
                level.sendParticles(player, com.theoreticalsciences.registry.QHParticles.ARC_SPARK.get(), true, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0);
                continue;
            }
            level.setBlock(p, want.defaultBlockState(), 3);
            level.sendParticles(com.theoreticalsciences.registry.QHParticles.PHOTON.get(), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.25, 0.25, 0.25, 0.01);
            placed++;
        }
        if (placed > 0) {
            level.playSound(null, controller.getBlockPos(), com.theoreticalsciences.registry.QHSounds.CONFIRM.get(), SoundSource.BLOCKS, 0.8F, 1.6F);
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.assembler_placed", placed).withStyle(ChatFormatting.AQUA), false);
        }
        if (blocked > 0)
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.assembler_blocked", blocked).withStyle(ChatFormatting.RED), false);
        if (!lacking.isEmpty()) {
            player.displayClientMessage(Component.translatable(dryRun ? "message.theoreticalsciences.assembler_needs" : "message.theoreticalsciences.assembler_missing")
                    .withStyle(ChatFormatting.GOLD), false);
            for (Map.Entry<Block, Integer> e : lacking.entrySet()) {
                int have = creative ? 0 : count(player.getInventory(), e.getKey());
                player.displayClientMessage(Component.literal("  " + e.getValue() + " x ").append(e.getKey().getName())
                        .append(Component.literal(dryRun ? "  (have " + have + ")" : "")).withStyle(ChatFormatting.GRAY), false);
            }
        }
        return InteractionResult.CONSUME;
    }

    private static boolean takeFromInventory(Inventory inv, Block block) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof BlockItem bi && bi.getBlock() == block) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static int count(Inventory inv, Block block) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof BlockItem bi && bi.getBlock() == block) n += s.getCount();
        }
        return n;
    }
}
