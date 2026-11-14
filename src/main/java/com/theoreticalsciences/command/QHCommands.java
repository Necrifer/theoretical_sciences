package com.theoreticalsciences.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.theoreticalsciences.research.ResearchManager;
import com.theoreticalsciences.research.ResearchSavedData;
import com.theoreticalsciences.research.ResearchService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * /theoreticalsciences research grant|revoke &lt;player&gt; &lt;id|*&gt;
 * /theoreticalsciences research list &lt;player&gt;
 * /theoreticalsciences insight add &lt;player&gt; &lt;amount&gt;
 */
public final class QHCommands {
    private static final SuggestionProvider<CommandSourceStack> RESEARCH = (ctx, b) ->
            SharedSuggestionProvider.suggestResource(ResearchManager.INSTANCE.entries().keySet(), b);

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("theoreticalsciences").requires(s -> s.hasPermission(2))
                .then(Commands.literal("research")
                        .then(Commands.literal("grant").then(Commands.argument("player", EntityArgument.players())
                                .then(Commands.literal("*").executes(ctx -> {
                                    for (ServerPlayer p : EntityArgument.getPlayers(ctx, "player"))
                                        ResearchManager.INSTANCE.entries().keySet().forEach(id -> ResearchService.grant(p, id));
                                    ctx.getSource().sendSuccess(() -> Component.literal("Granted all research"), true);
                                    return 1;
                                }))
                                .then(Commands.argument("id", ResourceLocationArgument.id()).suggests(RESEARCH).executes(ctx -> {
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
                                    for (ServerPlayer p : EntityArgument.getPlayers(ctx, "player")) ResearchService.grant(p, id);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Granted " + id), true);
                                    return 1;
                                }))))
                        .then(Commands.literal("revoke").then(Commands.argument("player", EntityArgument.players())
                                .then(Commands.literal("*").executes(ctx -> {
                                    for (ServerPlayer p : EntityArgument.getPlayers(ctx, "player"))
                                        ResearchManager.INSTANCE.entries().keySet().forEach(id -> ResearchService.revoke(p, id));
                                    ctx.getSource().sendSuccess(() -> Component.literal("Revoked all research"), true);
                                    return 1;
                                }))
                                .then(Commands.argument("id", ResourceLocationArgument.id()).suggests(RESEARCH).executes(ctx -> {
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
                                    for (ServerPlayer p : EntityArgument.getPlayers(ctx, "player")) ResearchService.revoke(p, id);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Revoked " + id), true);
                                    return 1;
                                }))))
                        .then(Commands.literal("list").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                            ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                            ResearchSavedData.Entry e = ResearchSavedData.get(p.server).of(p.getUUID());
                            ctx.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + ": insight " + e.insight
                                    + ", observations " + e.observed.size() + ", research " + e.unlocked), false);
                            return 1;
                        }))))
                .then(Commands.literal("insight").then(Commands.literal("add").then(Commands.argument("player", EntityArgument.players())
                        .then(Commands.argument("amount", IntegerArgumentType.integer()).executes(ctx -> {
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            for (ServerPlayer p : EntityArgument.getPlayers(ctx, "player")) ResearchService.addInsight(p, amount);
                            ctx.getSource().sendSuccess(() -> Component.literal("Added " + amount + " insight"), true);
                            return 1;
                        })))))
                .then(radiation())
                .then(home())                                                    // 1.8.0
                .then(testkit())                                                 // 1.10.0
                .then(windowsReach())                                            // 1.11.0
                .then(com.theoreticalsciences.orbit.OrbitCommands.station()));   // 1.3.0
    }

    /**
     * 1.11.0, for the mod's own tools: /theoreticalsciences windows reach &lt;recipe type&gt;. For every recipe of that type
     * with console windows, samples what a player can reach (every coolant, coil tiers 1 to 5, the machine's temperature
     * and state variables settled as in play) and writes the window quantities at each working point to
     * ts_window_reach/&lt;type&gt;.json in the server folder. Runs off the server thread. Operators only (level 4).
     */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> windowsReach() {
        return Commands.literal("windows").requires(s -> s.hasPermission(4)).then(Commands.literal("reach")
                .then(Commands.argument("type", com.mojang.brigadier.arguments.StringArgumentType.word()).executes(ctx -> {
                    String type = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "type");
                    var server = ctx.getSource().getServer();
                    com.theoreticalsciences.machine.MachineType mt = null;
                    for (var t : com.theoreticalsciences.machine.MachineType.values()) if (t.recipeType.equals(type)) { mt = t; break; }
                    if (mt == null) {
                        ctx.getSource().sendFailure(Component.literal("No machine makes " + type));
                        return 0;
                    }
                    final var machine = mt;
                    var recipes = new java.util.ArrayList<>(server.getRecipeManager().getAllRecipesFor(com.theoreticalsciences.registry.QHRecipes.type(type)));
                    var out = server.getServerDirectory().resolve("ts_window_reach").resolve(type + ".json");
                    java.util.concurrent.CompletableFuture.runAsync(() -> {
                        var root = new com.google.gson.JsonObject();
                        for (var h : recipes) {
                            var phys = h.value().physics;
                            if (phys.windows().isEmpty()) continue;
                            var o = new com.google.gson.JsonObject();
                            var ws = new com.google.gson.JsonArray();
                            for (var w : phys.windows()) {
                                var wo = new com.google.gson.JsonObject();
                                wo.addProperty("q", w.q());
                                wo.addProperty("min", w.min());
                                wo.addProperty("max", w.max());
                                ws.add(wo);
                            }
                            o.add("windows", ws);
                            var arr = new com.google.gson.JsonArray();
                            for (int coil = 1; coil <= 5; coil++) {
                                var pts = new com.theoreticalsciences.machine.physics.DemonSolver(machine.id(), phys, coil, Math.min(coil, 3), 0, 1, new double[0], null,
                                        h.id().hashCode() * 31L + coil).reach(400);
                                for (double[] p : pts) {
                                    var a = new com.google.gson.JsonArray();
                                    a.add(coil);
                                    for (double v : p) a.add(Double.isFinite(v) ? v : 0.0);
                                    arr.add(a);
                                }
                            }
                            o.add("samples", arr);
                            root.add(h.id().toString(), o);
                        }
                        try {
                            java.nio.file.Files.createDirectories(out.getParent());
                            java.nio.file.Files.writeString(out, root.toString());
                        } catch (java.io.IOException ex) {
                            com.theoreticalsciences.TheoreticalSciences.LOGGER.warn("windows reach: could not write {}", out, ex);
                        }
                        com.theoreticalsciences.TheoreticalSciences.LOGGER.info("windows reach: wrote {} ({} recipes)", out, root.size());
                    });
                    ctx.getSource().sendSuccess(() -> Component.literal("Sampling " + type + " off-thread; see the log"), false);
                    return 1;
                })));
    }

    /**
     * 1.10.0: /theoreticalsciences testkit &lt;players&gt; &lt;kit&gt;. Gives the items of a test kit (data/&lt;ns&gt;/ts_testkits/&lt;kit&gt;.json:
     * {"description", "items": [{"id", "count"}], "research": "*" or a list of ids}) and grants its research, for checking
     * a part of the mod without crafting up to it. Packs can add their own kits in a datapack. What does not fit in the
     * inventory waits in the player's Ender Chest rather than being thrown on the ground (no piles of items in one chunk).
     */
    private static final SuggestionProvider<CommandSourceStack> KITS = (ctx, b) ->
            SharedSuggestionProvider.suggestResource(kits(ctx.getSource().getServer()).keySet(), b);

    private static java.util.Map<ResourceLocation, com.google.gson.JsonObject> kits(net.minecraft.server.MinecraftServer server) {
        var out = new java.util.TreeMap<ResourceLocation, com.google.gson.JsonObject>();
        server.getResourceManager().listResources("ts_testkits", id -> id.getPath().endsWith(".json")).forEach((id, res) -> {
            try (var r = res.openAsReader()) {
                String path = id.getPath().substring("ts_testkits/".length(), id.getPath().length() - 5);
                out.put(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path), com.google.gson.JsonParser.parseReader(r).getAsJsonObject());
            } catch (Exception ignored) {
            }
        });
        return out;
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> testkit() {
        return Commands.literal("testkit")
                .then(Commands.literal("list").executes(ctx -> {
                    var all = kits(ctx.getSource().getServer());
                    all.forEach((id, o) -> ctx.getSource().sendSuccess(() -> Component.literal(id + ": "
                            + (o.has("description") ? o.get("description").getAsString() : "")), false));
                    return all.size();
                }))
                .then(Commands.argument("players", EntityArgument.players())
                .then(Commands.argument("kit", ResourceLocationArgument.id()).suggests(KITS).executes(ctx -> {
                    ResourceLocation kitId = ResourceLocationArgument.getId(ctx, "kit");
                    var kit = kits(ctx.getSource().getServer()).get(kitId);
                    if (kit == null) {
                        ctx.getSource().sendFailure(Component.literal("No test kit " + kitId));
                        return 0;
                    }
                    int given = 0, missing = 0;
                    for (ServerPlayer p : EntityArgument.getPlayers(ctx, "players")) {
                        for (var el : kit.getAsJsonArray("items")) {
                            var o = el.getAsJsonObject();
                            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(o.get("id").getAsString()));
                            if (item.isEmpty()) { missing++; continue; }
                            int left = o.has("count") ? o.get("count").getAsInt() : 1;
                            while (left > 0) {
                                int n = Math.min(left, item.get().getDefaultMaxStackSize());
                                var st = new net.minecraft.world.item.ItemStack(item.get(), n);
                                if (!p.getInventory().add(st) && !st.isEmpty()) {
                                    var rest = p.getEnderChestInventory().addItem(st);
                                    if (!rest.isEmpty()) p.drop(rest, false);   // only when both are full
                                }
                                left -= n;
                            }
                            given++;
                        }
                        var res = kit.get("research");
                        if (res != null && res.isJsonPrimitive() && "*".equals(res.getAsString()))
                            ResearchManager.INSTANCE.entries().keySet().forEach(id -> ResearchService.grant(p, id));
                        else if (res != null && res.isJsonArray())
                            for (var r : res.getAsJsonArray()) ResearchService.grant(p, ResourceLocation.parse(r.getAsString()));
                    }
                    final int g = given, m = missing;
                    String desc = kit.has("description") ? kit.get("description").getAsString() : "";
                    ctx.getSource().sendSuccess(() -> Component.literal("Test kit " + kitId + ": " + g + " stacks given"
                            + (m > 0 ? ", " + m + " items unknown in this game" : "") + (desc.isEmpty() ? "" : ". " + desc)), true);
                    return g;
                })));
    }

    /**
     * 1.8.0: /theoreticalsciences home unlock|complete|reset <players>. For testing Recreating Home without forging everything or
     * giving forty-two times 33,550,337 items: unlock shows the Core's tab, complete fills every line, reset starts over.
     */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> home() {
        var root = Commands.literal("home");
        for (String what : new String[]{"unlock", "complete", "reset"})
            root.then(Commands.literal(what).then(Commands.argument("players", EntityArgument.players()).executes(ctx -> {
                for (ServerPlayer p : EntityArgument.getPlayers(ctx, "players")) {
                    com.theoreticalsciences.home.HomeProgress.command(p.server, p.getUUID(), what);
                    if (what.equals("complete")) com.theoreticalsciences.home.HomePayloads.announceIfComplete(p);
                }
                ctx.getSource().sendSuccess(() -> Component.literal("Recreating Home: " + what), true);
                return 1;
            })));
        return root;
    }

    /**
     * Round 37 (1.1): /theoreticalsciences radiation reset [targets] | get <target> | set <targets> <dose>. Reset also clears the dose
     * the linked mods hold (Mekanism, GregTech), so testing starts clean everywhere.
     */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> radiation() {
        return Commands.literal("radiation")
                .then(Commands.literal("debug").executes(ctx -> {
                    // 1.2.2: what this mod reads where you stand, and what its links to other mods have done
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    var r = com.theoreticalsciences.science.RadiationService.reading(p);
                    java.util.List<String> lines = new java.util.ArrayList<>();
                    lines.add(String.format(java.util.Locale.ROOT, "Theoretical Sciences (mSv/s): carried %.4g, around %.4g (machines %.4g), background %.3g; dose %.1f mSv; machines emitting here: %d",
                            r.carried(), r.around(), r.fromMachines(), r.background(), com.theoreticalsciences.science.RadiationService.dose(p),
                            com.theoreticalsciences.science.RadiationService.emitters(p.serverLevel())));
                    lines.addAll(com.theoreticalsciences.science.RadiationCompat.debug(p));
                    for (String l : lines) ctx.getSource().sendSuccess(() -> Component.literal(l), false);
                    return 1;
                }))
                .then(Commands.literal("cleanup").executes(ctx -> cleanup(ctx.getSource(), 8))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 64)).executes(ctx -> cleanup(ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(Commands.literal("reset")
                        .executes(ctx -> resetRadiation(ctx.getSource(), java.util.List.of(ctx.getSource().getEntityOrException())))
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(ctx -> resetRadiation(ctx.getSource(), EntityArgument.getEntities(ctx, "targets")))))
                .then(Commands.literal("get").then(Commands.argument("target", EntityArgument.entity()).executes(ctx -> {
                    var e = EntityArgument.getEntity(ctx, "target");
                    double d = e.getPersistentData().getDouble("theoreticalsciences_rad_dose");
                    ctx.getSource().sendSuccess(() -> Component.translatable("commands.theoreticalsciences.radiation.get", e.getDisplayName(),
                            String.format(java.util.Locale.ROOT, "%.1f", d)), false);
                    return (int) d;
                })))
                .then(Commands.literal("set").then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.argument("dose", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(0, 100000)).executes(ctx -> {
                            double d = com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(ctx, "dose");
                            int n = 0;
                            for (var e : EntityArgument.getEntities(ctx, "targets")) {
                                if (!(e instanceof net.minecraft.world.entity.LivingEntity)) continue;
                                e.getPersistentData().putDouble("theoreticalsciences_rad_dose", d);
                                n++;
                            }
                            int count = n;
                            ctx.getSource().sendSuccess(() -> Component.translatable("commands.theoreticalsciences.radiation.set", count,
                                    String.format(java.util.Locale.ROOT, "%.1f", d)), true);
                            return n;
                        }))));
    }

    /** 1.2.3: clears Nuclear Radiation's lingering sources around the player (see RadiationCompat.cleanupLeftovers). */
    private static int cleanup(CommandSourceStack src, int radius) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        int n = com.theoreticalsciences.science.RadiationCompat.cleanupLeftovers(p.serverLevel(), p.blockPosition(), radius);
        src.sendSuccess(() -> Component.literal(n < 0 ? "Nuclear Radiation is not installed, or its sources could not be read."
                : "Removed " + n + " lingering Nuclear Radiation sources within " + radius + " blocks."), true);
        return Math.max(0, n);
    }

    private static int resetRadiation(CommandSourceStack src, java.util.Collection<? extends net.minecraft.world.entity.Entity> targets) {
        int n = 0;
        for (var e : targets) {
            if (!(e instanceof net.minecraft.world.entity.LivingEntity le)) continue;
            le.getPersistentData().remove("theoreticalsciences_rad_dose");
            le.removeEffect(com.theoreticalsciences.registry.QHEffects.RADIATION_SICKNESS);
            com.theoreticalsciences.science.RadiationCompat.reset(le);
            n++;
        }
        int count = n;
        src.sendSuccess(() -> Component.translatable("commands.theoreticalsciences.radiation.reset", count), true);
        return n;
    }

    private QHCommands() {}
}
