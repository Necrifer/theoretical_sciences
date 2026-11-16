package com.theoreticalsciences.client.screen;

import com.theoreticalsciences.QHConfig;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

/**
 * 1.16.0: which machines' equations this player has unlocked. A console explains its quantities in words first; the
 * equation behind each one appears once the player has watched that kind of machine finish a cycle with its screen
 * open, so nobody meets a wall of symbols before they have seen what the symbols describe.
 * <p>
 * Kept on the player's own computer (config/theoreticalsciences/seen_equations.txt), for every world: it records
 * what the player has learned, not anything about a world. Nothing here affects how a machine works.
 */
public final class EquationsSeen {
    private static Set<String> seen;

    private EquationsSeen() {}

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("theoreticalsciences").resolve("seen_equations.txt");
    }

    private static Set<String> set() {
        if (seen == null) {
            seen = new TreeSet<>();
            try {
                Path f = file();
                if (Files.exists(f))
                    for (String l : Files.readAllLines(f, StandardCharsets.UTF_8)) if (!l.isBlank()) seen.add(l.trim());
            } catch (Exception ignored) {
                // an unreadable file only means the equations are explained in words again
            }
        }
        return seen;
    }

    public static boolean shown(String machine) {
        return QHConfig.EQUATIONS_AT_ONCE.get() || set().contains(machine);
    }

    /** Records a watched cycle. True if this unlocked the machine's equations just now. */
    public static boolean mark(String machine) {
        if (!set().add(machine)) return false;
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            Files.write(f, set(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // still unlocked for this session
        }
        return !QHConfig.EQUATIONS_AT_ONCE.get();
    }
}
