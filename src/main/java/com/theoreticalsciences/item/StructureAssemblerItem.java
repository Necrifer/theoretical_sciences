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
import net.minecraft.world.item.Item.TooltipContext;

/**
 * Structure Assembler: right-click a multiblock controller to build every missing block of its structure,
 * taking the casings from your inventory (free in creative). Sneak-right-click for a dry run: it highlights
 * what is missing and lists the materials you still need, without placing anything.
 * <p>
 * It never breaks blocks while building: anything in the way is only highlighted, so it cannot destroy your builds.
 * <p>
 * 1.3.6: sneak-use it in the air to switch to <b>disassembling</b>. Then using it on a controller (twice within five
 * seconds, to confirm) takes the whole structure apart - only the blocks that belong to its pattern, controller last -
 * and puts them in your inventory (what does not fit is dropped at your feet). Sneak-use on a controller in this mode
 * lists what you would get back without touching anything.
 * Tiered components (coils, field emitters) use the best tier in your inventory; in creative, hold the tier you
 * want in your off-hand. Positions that accept an Interface Port get one from your inventory first (1.3.7); the
 * plain casing goes in only when you have no more ports.
 */
public class StructureAssemblerItem extends LoreItem {
    public StructureAssemblerItem(Properties props) {
        super(props.stacksTo(1));
    }

    // ------------------------------------------------------------------ mode (build / disassemble)
    public static boolean disassembling(ItemStack stack) {
        net.minecraft.world.item.component.CustomData d = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return d != null && d.copyTag().getBoolean("Disassemble");
    }

    private static void setDisassembling(ItemStack stack, boolean on) {
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, t -> t.putBoolean("Disassemble", on));
    }

    // ------------------------------------------------------------------ 1.1.0: bound to an ME network (AE2)
    /**
     * Sneak-use the assembler on any block of an AE2 network (a cable, a drive, a terminal, an Interface Port...) to bind
     * it; do it again on the same block to unbind. While bound, building takes what your inventory lacks from that
     * network, and taking a structure apart puts the blocks back into it - from anywhere in the same dimension, as long
     * as that block's chunk is loaded and the network is powered. The network's own security and power apply.
     */
    @javax.annotation.Nullable
    static long[] binding(ItemStack stack) {
        net.minecraft.world.item.component.CustomData d = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (d == null) return null;
        var t = d.copyTag();
        return t.contains("MEPos") ? new long[]{t.getLong("MEPos")} : null;
    }

    @javax.annotation.Nullable
    static String bindingDim(ItemStack stack) {
        net.minecraft.world.item.component.CustomData d = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return d == null ? null : d.copyTag().getString("MEDim");
    }

    /** The bound network if it can be reached from where the player stands now, else null. */
    @javax.annotation.Nullable
    static Object boundGrid(ServerLevel level, ItemStack stack) {
        long[] b = binding(stack);
        if (b == null || !level.dimension().location().toString().equals(bindingDim(stack))) return null;
        return com.theoreticalsciences.compat.MEBridge.grid(level, BlockPos.of(b[0]));
    }

    private static boolean tryBind(ServerLevel level, ServerPlayer player, ItemStack stack, BlockPos pos) {
        if (!com.theoreticalsciences.compat.MEBridge.present() || com.theoreticalsciences.compat.MEBridge.grid(level, pos) == null) return false;
        long[] b = binding(stack);
        boolean same = b != null && b[0] == pos.asLong() && level.dimension().location().toString().equals(bindingDim(stack));
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, t -> {
            if (same) {
                t.remove("MEPos");
                t.remove("MEDim");
            } else {
                t.putLong("MEPos", pos.asLong());
                t.putString("MEDim", level.dimension().location().toString());
            }
        });
        player.displayClientMessage(same ? Component.translatable("message.theoreticalsciences.assembler_unbound").withStyle(ChatFormatting.GRAY)
                : Component.translatable("message.theoreticalsciences.assembler_bound", pos.toShortString()).withStyle(ChatFormatting.AQUA), true);
        level.playSound(null, pos, com.theoreticalsciences.registry.QHSounds.CONFIRM.get(), SoundSource.PLAYERS, 0.6F, same ? 0.8F : 1.8F);
        return true;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
                                                                       net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return net.minecraft.world.InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            boolean on = !disassembling(stack);
            setDisassembling(stack, on);
            player.displayClientMessage(Component.translatable(on ? "message.theoreticalsciences.assembler_mode_disassemble"
                    : "message.theoreticalsciences.assembler_mode_build").withStyle(on ? ChatFormatting.RED : ChatFormatting.AQUA), true);
            level.playSound(null, player.blockPosition(), com.theoreticalsciences.registry.QHSounds.CONFIRM.get(), SoundSource.PLAYERS, 0.6F, on ? 0.7F : 1.4F);
        }
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return disassembling(stack) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.translatable(disassembling(stack) ? "tooltip.theoreticalsciences.assembler_mode_disassemble"
                : "tooltip.theoreticalsciences.assembler_mode_build").withStyle(disassembling(stack) ? ChatFormatting.RED : ChatFormatting.AQUA));
        long[] b = binding(stack);
        if (b != null) tooltip.add(Component.translatable("tooltip.theoreticalsciences.assembler_bound", BlockPos.of(b[0]).toShortString(), bindingDim(stack))
                .withStyle(ChatFormatting.DARK_AQUA));
        super.appendHoverText(stack, ctx, tooltip, flag);
    }

    /** Pending confirmations: the controller a player last asked to take apart, and when. */
    private static final Map<UUID, long[]> CONFIRM = new HashMap<>();

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer player))
            return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(ctx.getClickedPos());
        if (player.isShiftKeyDown() && !(be instanceof ControllerBlockEntity) && tryBind(level, player, ctx.getItemInHand(), ctx.getClickedPos()))
            return InteractionResult.CONSUME;
        if (!(be instanceof ControllerBlockEntity controller) || controller.definition() == null) {
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.assembler_not_controller").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }
        Object grid = boundGrid(level, ctx.getItemInHand());
        if (binding(ctx.getItemInHand()) != null && grid == null)
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.assembler_me_offline").withStyle(ChatFormatting.GOLD), false);
        if (disassembling(ctx.getItemInHand())) return disassemble(level, player, controller, grid);
        MultiblockDefinition def = controller.definition();
        MultiblockDefinition.ScanResult scan = def.scan(level, controller.getBlockPos(), controller.facing(),
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("theoreticalsciences", "interface_port"));
        if (scan.complete()) {
            // 1.3.5: a complete variable-height tower grows by one stage with each use, up to its full height
            MultiblockDefinition next = controller.nextSize();
            if (next != null) {
                def = next;
                scan = def.scan(level, controller.getBlockPos(), controller.facing(),
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("theoreticalsciences", "interface_port"));
            }
        }
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
        int portsLeft = creative ? Integer.MAX_VALUE : count(player.getInventory(), ControllerBlockEntity.portBlock());   // (ports only from your own inventory, never the network)
        for (BlockPos p : todo) {
            char c = scan.expected().get(p);
            BlockState current = level.getBlockState(p);
            if (c == '.') {                       // must be air, but something is there: never break it, just show it
                blocked++;
                level.sendParticles(player, com.theoreticalsciences.registry.QHParticles.FAULT.get(), true, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 1, 0, 0, 0, 0);
                continue;
            }
            List<Block> options = def.componentsFor(c, ControllerBlockEntity.portBlock());   // the casing (or tiers), not the port
            if (options.isEmpty()) continue;
            Block want = options.get(0);
            // 1.3.7: where the pattern accepts an Interface Port, one from your inventory goes in first; the plain casing
            // only once the ports run out (creative: always a port)
            boolean port = def.allowsPort(c, net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(ControllerBlockEntity.portBlock())) && portsLeft > 0;
            if (port) {
                want = ControllerBlockEntity.portBlock();
                portsLeft--;
            } else if (options.size() > 1) {
                // tiered component (coils, emitters): creative uses the tier held in the off-hand;
                // survival uses the best tier you are carrying
                if (creative) {
                    if (player.getOffhandItem().getItem() instanceof BlockItem bi && options.contains(bi.getBlock())) want = bi.getBlock();
                } else {
                    for (int k = options.size() - 1; k >= 0; k--)
                        if (count(player, grid, options.get(k)) > 0) {
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
            if (!creative && !take(player, grid, want)) {
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
                int have = creative ? 0 : (int) Math.min(Integer.MAX_VALUE, count(player, grid, e.getKey()));
                player.displayClientMessage(Component.literal("  " + e.getValue() + " x ").append(e.getKey().getName())
                        .append(Component.literal(dryRun ? "  (have " + have + ")" : "")).withStyle(ChatFormatting.GRAY), false);
            }
        }
        return InteractionResult.CONSUME;
    }

    /**
     * Takes the structure apart: every block that matches its pattern (casings, ports, coils, glass...), the controller
     * last. Air cells and anything that does not belong to the pattern are left alone; so are blocks the player could not
     * break there (claims, spawn protection) and unbreakable ones. Each block comes back as its own drops, so machines
     * keep what a pickaxe would give.
     */
    private static InteractionResult disassemble(ServerLevel level, ServerPlayer player, ControllerBlockEntity controller, @javax.annotation.Nullable Object grid) {
        MultiblockDefinition cur = controller.definition();
        BlockPos cpos = controller.getBlockPos();
        // the current layout and (1.3.6) any earlier one, so a structure built to an older design can be taken apart too
        List<MultiblockDefinition> defs = new ArrayList<>();
        defs.add(cur);
        defs.addAll(cur.legacy);
        MultiblockDefinition base = com.theoreticalsciences.multiblock.Multiblocks.get(controller.type.multiblock).orElse(null);
        if (base != null && base != cur) defs.addAll(base.legacy);
        LinkedHashSet<BlockPos> found = new LinkedHashSet<>();
        for (MultiblockDefinition def : defs) {
            for (int y = def.sizeY - 1; y >= 0; y--) {             // top down, so nothing is left hanging mid-way
                List<String> rows = def.layers.get(y);
                for (int z = 0; z < rows.size(); z++) {
                    String row = rows.get(z);
                    for (int x = 0; x < row.length(); x++) {
                        char c = row.charAt(x);
                        if (c == ' ' || c == '.' || c == def.controllerChar) continue;
                        BlockPos p = def.toWorld(cpos, controller.facing(), x, y, z);
                        if (p.equals(cpos) || !level.isLoaded(p)) continue;
                        BlockState st = level.getBlockState(p);
                        if (st.isAir() || !def.matches(c, st)) continue;
                        found.add(p.immutable());
                    }
                }
            }
        }
        List<BlockPos> parts = new ArrayList<>(found);
        parts.sort((a, b) -> Integer.compare(b.getY(), a.getY()));
        parts.add(cpos);
        // the dry run: what would come back
        Map<net.minecraft.world.item.Item, Integer> back = new LinkedHashMap<>();
        for (BlockPos p : parts) {
            BlockState st = level.getBlockState(p);
            back.merge(st.getBlock().asItem(), 1, Integer::sum);
        }
        if (player.isShiftKeyDown()) {
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.assembler_disassemble_preview", parts.size())
                    .withStyle(ChatFormatting.GOLD), false);
            for (Map.Entry<net.minecraft.world.item.Item, Integer> e : back.entrySet())
                player.displayClientMessage(Component.literal("  " + e.getValue() + " x ").append(e.getKey().getDescription()).withStyle(ChatFormatting.GRAY), false);
            return InteractionResult.CONSUME;
        }
        long now = level.getGameTime();
        long[] pending = CONFIRM.get(player.getUUID());
        if (pending == null || pending[0] != cpos.asLong() || now - pending[1] > 100) {
            CONFIRM.put(player.getUUID(), new long[]{cpos.asLong(), now});
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.assembler_disassemble_confirm", parts.size())
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }
        CONFIRM.remove(player.getUUID());
        Map<net.minecraft.world.item.Item, Integer> got = new LinkedHashMap<>();
        List<ItemStack> extra = new ArrayList<>();
        int removed = 0, kept = 0;
        for (BlockPos p : parts) {
            BlockState st = level.getBlockState(p);
            if (st.isAir()) continue;
            if (st.getDestroySpeed(level, p) < 0 || !player.mayBuild() || !level.mayInteract(player, p)) {
                kept++;
                continue;
            }
            BlockEntity pbe = level.getBlockEntity(p);
            List<ItemStack> drops = Block.getDrops(st, level, p, pbe, player, ItemStack.EMPTY);
            if (drops.isEmpty()) drops = List.of(new ItemStack(st.getBlock().asItem()));
            for (ItemStack d : drops) {
                if (d.isEmpty()) continue;
                if (d.getComponentsPatch().isEmpty()) got.merge(d.getItem(), d.getCount(), Integer::sum);
                else extra.add(d.copy());               // a block that remembers something (a named or filled one): kept as it is
            }
            level.destroyBlock(p, false, player);     // a machine's own contents still spill where it stood, as when mined
            removed++;
        }
        for (Map.Entry<net.minecraft.world.item.Item, Integer> e : got.entrySet()) {
            int n = e.getValue();
            int max = new ItemStack(e.getKey()).getMaxStackSize();
            while (n > 0) {
                int k = Math.min(n, max);
                extra.add(new ItemStack(e.getKey(), k));
                n -= k;
            }
        }
        int dropped = 0;
        for (ItemStack st : extra) {
            if (grid != null) {                                    // 1.1.0: a bound network takes them first
                long in = com.theoreticalsciences.compat.MEBridge.put(grid, st, player);
                st.shrink((int) Math.min(in, st.getCount()));
                if (st.isEmpty()) continue;
            }
            if (!player.getInventory().add(st) && !st.isEmpty()) {
                player.drop(st, false);
                dropped++;
            }
        }
        level.playSound(null, cpos, com.theoreticalsciences.registry.QHSounds.VENT.get(), SoundSource.BLOCKS, 0.8F, 1.2F);
        player.displayClientMessage(Component.translatable("message.theoreticalsciences.assembler_disassembled", removed).withStyle(ChatFormatting.AQUA), false);
        if (kept > 0)
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.assembler_disassemble_kept", kept).withStyle(ChatFormatting.RED), false);
        if (dropped > 0)
            player.displayClientMessage(Component.translatable("message.theoreticalsciences.assembler_disassemble_dropped").withStyle(ChatFormatting.GOLD), false);
        return InteractionResult.CONSUME;
    }

    /** Your inventory first, then the bound network (1.1.0). */
    private static boolean take(ServerPlayer player, @javax.annotation.Nullable Object grid, Block block) {
        if (takeFromInventory(player.getInventory(), block)) return true;
        return grid != null && com.theoreticalsciences.compat.MEBridge.take(grid, block.asItem(), 1, player);
    }

    private static long count(ServerPlayer player, @javax.annotation.Nullable Object grid, Block block) {
        return count(player.getInventory(), block) + (grid == null ? 0 : com.theoreticalsciences.compat.MEBridge.count(grid, block.asItem()));
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
