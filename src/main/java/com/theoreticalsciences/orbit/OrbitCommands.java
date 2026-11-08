package com.theoreticalsciences.orbit;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.theoreticalsciences.world.QHDimensions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * 1.3.0: {@code /theoreticalsciences station status|modules <n>|build} (operators): look at the Orbital Station, set
 * how many modules are in orbit (a pack can start a world with the station done, or hold it back), or put it together now.
 */
public final class OrbitCommands {
    public static LiteralArgumentBuilder<CommandSourceStack> station() {
        return Commands.literal("station")
                .then(Commands.literal("status").executes(ctx -> {
                    OrbitData d = OrbitData.get(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal("Orbital Station: " + d.modules + " of " + OrbitData.REQUIRED
                            + " modules in orbit; " + (d.built ? "built" : "not built yet")), false);
                    return d.modules;
                }))
                .then(Commands.literal("modules").then(Commands.argument("count", IntegerArgumentType.integer(0, 1000)).executes(ctx -> {
                    OrbitData d = OrbitData.get(ctx.getSource().getServer());
                    d.modules = IntegerArgumentType.getInteger(ctx, "count");
                    d.setDirty();
                    ctx.getSource().sendSuccess(() -> Component.literal("Orbital Station modules set to " + d.modules), true);
                    return d.modules;
                })))
                .then(Commands.literal("build").executes(ctx -> {
                    var level = ctx.getSource().getServer().getLevel(QHDimensions.ORBITAL_STATION);
                    if (level == null) return 0;
                    OrbitBuilder.ensureBuilt(level);
                    ctx.getSource().sendSuccess(() -> Component.literal("Orbital Station built"), true);
                    return 1;
                }));
    }

    private OrbitCommands() {}
}
