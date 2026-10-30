package com.theoreticalsciences.compat.jade;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.machine.MachineBlock;
import com.theoreticalsciences.machine.MachineBlockEntity;
import com.theoreticalsciences.machine.MachineStatus;
import com.theoreticalsciences.machine.PortBlock;
import com.theoreticalsciences.machine.PortBlockEntity;
import com.theoreticalsciences.recipe.QHRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Jade (the HWYLA/WAILA successor) support, done the way GregTech and Modular Machinery do it: the server sends the
 * machine's working state with the block it is looking at (Jade only syncs inventories and tanks on its own), and the
 * client draws it as a status line, the recipe being made and a progress bar with the time left.
 * Looking at an Interface Port shows its controller.
 */
@WailaPlugin
public class QHJadePlugin implements IWailaPlugin {
    static final ResourceLocation UID = TheoreticalSciences.id("machine");

    @Override
    public void register(IWailaCommonRegistration reg) {
        reg.registerBlockDataProvider(Server.INSTANCE, MachineBlockEntity.class);
        reg.registerBlockDataProvider(Server.INSTANCE, PortBlockEntity.class);
        reg.registerBlockDataProvider(Server.INSTANCE, com.theoreticalsciences.extreme.CompactorBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration reg) {
        reg.registerBlockComponent(Client.INSTANCE, MachineBlock.class);
        reg.registerBlockComponent(Client.INSTANCE, PortBlock.class);
        reg.registerBlockComponent(Client.INSTANCE, com.theoreticalsciences.extreme.CompactorBlock.class);
    }

    static MachineBlockEntity machine(BlockEntity be) {
        if (be instanceof MachineBlockEntity m) return m;
        if (be instanceof PortBlockEntity p) return p.controller();
        return null;
    }

    enum Server implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor acc) {
            if (acc.getBlockEntity() instanceof com.theoreticalsciences.extreme.CompactorBlockEntity c) {
                CompoundTag t = new CompoundTag();
                t.putInt("a", c.absorbed());
                t.putInt("q", c.required());
                tag.put("qhc", t);
                return;
            }
            MachineBlockEntity m = machine(acc.getBlockEntity());
            if (m == null) return;
            CompoundTag t = new CompoundTag();
            MachineStatus st = m.getStatus();
            t.putInt("st", st.ordinal());
            t.putString("lbl", Component.Serializer.toJson(st.label(m.type), acc.getLevel().registryAccess()));
            t.putInt("p", m.getProgress());
            t.putInt("m", m.getMaxProgress());
            t.putDouble("sp", m.isActive() ? m.shownSpeed() : 0);
            t.putInt("par", m.shownParallel());
            t.putInt("oc", m.shownOverclock());
            ResourceLocation r = m.shownRecipe();
            if (r != null) t.putString("r", r.toString());
            tag.put("qh", t);
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }

    enum Client implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tip, BlockAccessor acc, IPluginConfig cfg) {
            CompoundTag all = acc.getServerData();
            if (all.contains("qhc")) {
                CompoundTag c = all.getCompound("qhc");
                int a = c.getInt("a"), q = c.getInt("q");
                IElementHelper h = IElementHelper.get();
                if (q <= 0) tip.add(Component.translatable("gui.theoreticalsciences.compactor_idle").withStyle(ChatFormatting.GRAY));
                else tip.add(h.progress(Math.min(1F, a / (float) q), Component.translatable("gui.theoreticalsciences.compactor_progress", String.format("%,d", a), String.format("%,d", q)),
                        h.progressStyle().color(0xFFB060FF, 0xFF6020C0).textColor(0xFFFFFFFF), BoxStyle.getNestedBox(), true));
                return;
            }
            if (!all.contains("qh")) return;
            CompoundTag t = all.getCompound("qh");
            MachineStatus[] values = MachineStatus.values();
            MachineStatus st = values[Math.floorMod(t.getInt("st"), values.length)];
            Component label;
            try {
                label = Component.Serializer.fromJson(t.getString("lbl"), acc.getLevel().registryAccess());
            } catch (Exception e) {
                label = null;
            }
            if (label == null) label = st.label();
            tip.add(Component.translatable("jade.theoreticalsciences.status", label.copy().withColor(st.color)));

            IElementHelper h = IElementHelper.get();
            boolean active = st == MachineStatus.RUNNING || st == MachineStatus.GENERATING;
            ItemStack out = ItemStack.EMPTY;
            Component making = null;
            if (t.contains("r")) {
                ResourceLocation id = ResourceLocation.tryParse(t.getString("r"));
                var holder = id == null ? null : acc.getLevel().getRecipeManager().byKey(id).orElse(null);
                if (holder != null && holder.value() instanceof QHRecipe q) {
                    if (!q.itemOutputs.isEmpty()) {
                        out = q.itemOutputs.get(0);
                        making = out.getHoverName();
                    } else if (!q.fluidOutputs.isEmpty()) making = q.fluidOutputs.get(0).getHoverName();
                }
            }
            if (making != null && (active || t.getInt("p") > 0)) {
                List<IElement> row = new ArrayList<>();
                if (!out.isEmpty()) row.add(h.smallItem(out));
                int par = t.getInt("par");
                Component name = par > 1 ? Component.translatable("jade.theoreticalsciences.making_par", making, par) : Component.translatable("jade.theoreticalsciences.making", making);
                row.add(h.text(name.copy().withStyle(ChatFormatting.GRAY)).translate(new Vec2(0, out.isEmpty() ? 0 : 2)));
                tip.add(row);
            }

            int max = t.getInt("m"), p = t.getInt("p");
            if (max > 0 && (active || p > 0)) {
                float ratio = Math.max(0, Math.min(1, p / (float) max));
                double sp = t.getDouble("sp");
                Component text;
                if (sp > 0) {
                    double left = (max - p) / sp / 20.0, total = max / sp / 20.0;
                    text = Component.translatable("jade.theoreticalsciences.progress_time", Math.round(ratio * 100), secs(total - left), secs(total));
                } else text = Component.translatable("jade.theoreticalsciences.progress", Math.round(ratio * 100));
                int c1 = 0xFF000000 | st.color, c2 = 0xFF000000 | darker(st.color);
                tip.add(h.progress(ratio, text, h.progressStyle().color(c1, c2).textColor(0xFFFFFFFF), BoxStyle.getNestedBox(), true));
            }
            int oc = t.getInt("oc"), par = t.getInt("par");
            if (oc > 0) tip.add(Component.translatable("jade.theoreticalsciences.overclock", oc, par).withStyle(ChatFormatting.DARK_AQUA));
        }

        private static String secs(double s) {
            if (!Double.isFinite(s) || s < 0) s = 0;
            if (s >= 3600) return String.format("%dh %02dm", (long) (s / 3600), (long) (s % 3600 / 60));
            if (s >= 60) return String.format("%dm %02ds", (long) (s / 60), (long) (s % 60));
            return String.format("%.1fs", s);
        }

        private static int darker(int c) {
            return ((c >> 16 & 0xFF) * 6 / 10) << 16 | ((c >> 8 & 0xFF) * 6 / 10) << 8 | (c & 0xFF) * 6 / 10;
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
