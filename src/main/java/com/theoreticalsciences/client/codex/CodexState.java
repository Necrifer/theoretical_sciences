package com.theoreticalsciences.client.codex;

import com.theoreticalsciences.TheoreticalSciences;
import com.theoreticalsciences.client.ClientResearch;
import com.theoreticalsciences.research.ResearchDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Round 30: which Codex entries the player may read, and which they have read already.
 * <p>
 * An entry is locked until its research is done - the same rule as the Patchouli and Modonomicon books, which wait
 * for the advancement {@code theoreticalsciences:research/<id>} that every research grants. Here the research itself is
 * asked (the client always has the player's research, synced by the server). Entries with no research, research
 * this world does not have, creative mode, and servers that send no research at all: open.
 * <p>
 * "Read" is remembered on this computer (theoreticalsciences/codex_read.txt in the game folder), so new entries stand out.
 */
public final class CodexState {
    private static Set<String> read;
    private static boolean dirty;

    public static ResourceLocation researchId(CodexData.Entry e) {
        return e.research == null ? null : ResourceLocation.tryParse(e.research.contains(":") ? e.research : TheoreticalSciences.MODID + ":" + e.research);
    }

    // round 30b: the Codex opens up only as the player progresses, in creative too. An entry waits for its research; an
    // entry without one opens with whatever leads to it on the map (any of its parent nodes, on any tab) - only the
    // roots of each map are open from the start. Worked out once per change of the player's research.
    private static final java.util.Map<String, Boolean> MEMO = new java.util.HashMap<>();
    private static Set<ResourceLocation> memoFor;
    private static java.util.Map<String, Set<String>> parents;

    public static boolean unlocked(CodexData.Entry e) {
        if (e == null) return false;
        if (memoFor != ClientResearch.unlocked) {
            MEMO.clear();
            memoFor = ClientResearch.unlocked;
        }
        return open(e.id, new HashSet<>());
    }

    private static boolean open(String id, Set<String> visiting) {
        Boolean m = MEMO.get(id);
        if (m != null) return m;
        CodexData.Entry e = CodexData.entries.get(id);
        boolean res;
        ResourceLocation r = e == null ? null : researchId(e);
        if (e == null) res = false;
        else if (r != null && ClientResearch.defs.containsKey(r)) res = ClientResearch.unlocked.contains(r);
        else if (r != null && !ClientResearch.defs.isEmpty()) res = true;        // research this world does not have: nothing to wait for
        else {
            Set<String> ps = parents().getOrDefault(id, Set.of());
            if (ps.isEmpty()) res = true;                                        // a root of its map
            else {
                if (!visiting.add(id)) return false;                             // (a loop in the map: not through here)
                res = false;
                for (String p : ps) if (open(p, visiting)) {
                    res = true;
                    break;
                }
                visiting.remove(id);
            }
        }
        MEMO.put(id, res);
        return res;
    }

    /**
     * Round 30b: shown on the map at all? Open entries, and the locked ones right next to them (the next steps,
     * shown as "???") - everything further on stays hidden until the player gets there.
     */
    public static boolean visible(CodexData.Entry e) {
        if (e == null) return false;
        if (unlocked(e)) return true;
        for (String p : parents().getOrDefault(e.id, Set.of())) {
            CodexData.Entry pe = CodexData.entries.get(p);
            if (pe != null && unlocked(pe)) return true;
        }
        return false;
    }

    /** Every entry's parent entries, gathered over all tabs. */
    private static java.util.Map<String, Set<String>> parents() {
        if (parents == null || parents.isEmpty()) {
            java.util.Map<String, Set<String>> out = new java.util.HashMap<>();
            for (CodexData.Tab t : CodexData.tabs)
                for (CodexData.Node n : t.nodes) out.computeIfAbsent(n.entry(), k -> new HashSet<>()).addAll(n.parents());
            parents = out;
        }
        return parents;
    }

    /** The Codex data was reloaded. */
    public static void reset() {
        parents = null;
        MEMO.clear();
        memoFor = null;
    }

    /** The name of the research an entry waits for. */
    public static Component researchName(CodexData.Entry e) {
        ResourceLocation r = researchId(e);
        if (r == null) return Component.empty();
        ResearchDefinition d = ClientResearch.defs.get(r);
        return d != null ? d.titleComponent(r) : Component.literal(e.research);
    }

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("theoreticalsciences").resolve("codex_read.txt");
    }

    private static Set<String> readSet() {
        if (read == null) {
            read = new HashSet<>();
            try {
                Path f = file();
                if (Files.exists(f)) for (String l : Files.readAllLines(f, StandardCharsets.UTF_8)) if (!l.isBlank()) read.add(l.trim());
            } catch (Exception ex) {
                TheoreticalSciences.LOGGER.warn("Quantum Codex: could not read which entries were read ({})", ex.toString());
            }
        }
        return read;
    }

    /** Round 33: remembered in the read list once the first-open hint banner has been dismissed. */
    public static final String INTRO = "#intro";
    /** Round 33: the first node of the Getting Started chain on the Discovery map. */
    public static final String START = "basics/guide_00_start";

    /** A step of the Getting Started chain. */
    public static boolean isGuide(String id) {
        return id != null && id.startsWith("basics/guide_");
    }

    public static boolean isRead(String id) {
        return readSet().contains(id);
    }

    /** Unlocked but never opened. */
    public static boolean isNew(CodexData.Entry e) {
        return unlocked(e) && !isRead(e.id);
    }

    public static void markRead(String id) {
        if (readSet().add(id)) dirty = true;
    }

    /** Writes the read list (when the Codex closes). */
    public static void save() {
        if (!dirty || read == null) return;
        dirty = false;
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            Files.write(f, String.join("\n", new java.util.TreeSet<>(read)).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            TheoreticalSciences.LOGGER.warn("Quantum Codex: could not save which entries were read ({})", ex.toString());
        }
    }

    private CodexState() {}
}
