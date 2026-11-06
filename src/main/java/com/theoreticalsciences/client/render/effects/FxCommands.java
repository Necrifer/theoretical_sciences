package com.theoreticalsciences.client.render.effects;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.theoreticalsciences.client.render.animation.Animations;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.util.List;

/**
 * 1.2.0: {@code /tsanim}, a client command for writing animations and checking effects without building the machine:
 * <ul>
 *   <li>{@code /tsanim list}: every structure animation loaded</li>
 *   <li>{@code /tsanim preview <animation>}: shows it here, as if a controller stood at your feet facing you</li>
 *   <li>{@code /tsanim run} and {@code /tsanim stop}: set the previews running or stopping (to see the ramps)</li>
 *   <li>{@code /tsanim clear}: removes the previews</li>
 *   <li>{@code /tsanim stats}: what the animated multiblocks cost per frame</li>
 * </ul>
 */
public final class FxCommands {
    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("tsanim")
                .then(Commands.literal("list").executes(c -> {
                    List<ResourceLocation> ids = Animations.ids().stream().sorted().toList();
                    say(c, ids.size() + " structure animations: " + (ids.isEmpty() ? "none" : String.join(", ", ids.stream().map(ResourceLocation::toString).toList())));
                    return ids.size();
                }))
                .then(Commands.literal("preview").then(Commands.argument("animation", ResourceLocationArgument.id())
                        .suggests((c, b) -> SharedSuggestionProvider.suggestResource(Animations.ids(), b))
                        .executes(c -> {
                            ResourceLocation id = c.getArgument("animation", ResourceLocation.class);
                            if (Animations.get(id) == null) {
                                say(c, "No structure animation " + id + " (see /tsanim list).");
                                return 0;
                            }
                            place(FxSpec.animationOnly(id));
                            say(c, "Showing " + id + " here, running. /tsanim stop to see it slow down, /tsanim clear to remove it.");
                            return 1;
                        })))
                .then(Commands.literal("run").executes(c -> {
                    FxRenderer.PREVIEWS.forEach(p -> p.running = true);
                    say(c, "Previews running.");
                    return 1;
                }))
                .then(Commands.literal("stop").executes(c -> {
                    FxRenderer.PREVIEWS.forEach(p -> p.running = false);
                    say(c, "Previews stopping.");
                    return 1;
                }))
                .then(Commands.literal("clear").executes(c -> {
                    int n = FxRenderer.PREVIEWS.size();
                    FxRenderer.PREVIEWS.clear();
                    say(c, "Removed " + n + " previews.");
                    return n;
                }))
                .then(Commands.literal("stats").executes(c -> {
                    int[] n = FxRenderer.lastCounts();
                    say(c, String.format(java.util.Locale.ROOT, "Animated multiblocks: %d drawn last frame, %d parts, %d quads; %.3f ms a frame on average (last %.3f ms); %d animations baked.",
                            n[0], n[1], n[2], FxRenderer.averageMs(), FxRenderer.lastMs, Animations.bakedCount()));
                    return 1;
                })));
    }

    /** A preview at the player's feet, its controller facing the player (so "back" runs the way the player looks). */
    static void place(FxSpec spec) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Direction look = mc.player.getDirection();
        BlockPos at = mc.player.blockPosition();
        FxRenderer.PREVIEWS.add(new FxRenderer.Preview(mc.level, at, look.getOpposite(), spec));
    }

    static void say(CommandContext<CommandSourceStack> c, String s) {
        var mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(Component.literal(s), false);
    }

    private FxCommands() {}
}
