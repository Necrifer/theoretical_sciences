package com.theoreticalsciences.client.codex;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.theoreticalsciences.TheoreticalSciences;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.Reader;
import java.util.*;

/**
 * Round 30: the Codex's content, read from the mod's resources (assets/theoreticalsciences/ts_codex/): index.json lists the tabs, one file per tab holds that map's nodes, and entries.json holds
 * the text of every entry (an entry can sit on several maps; its copies share the text).
 * <p>
 * Read the first time the Codex opens, and again after the resources are reloaded (F3+T, a resource pack change).
 */
public final class CodexData {
    /** One page of an entry. Only the fields its type uses are filled in. */
    public static final class Page {
        public String type = "text";          // text, spotlight, multiblock, recipe (and "auto_recipe", added by the screen)
        public String title = "";
        public String text = "";
        public List<String> items = List.of();
        public List<String> recipes = List.of();
        public String name = "";
        public List<List<String>> pattern = List.of();
        public Map<String, String> mapping = Map.of();
        /** 1.2.0: the multiblock a "multiblock" page shows, drawn from its live definition (then pattern is unused). */
        public String multiblock = "";
    }

    public static final class Entry {
        public String id, title, icon, category;
        /** The research that unlocks it (without the namespace), or null: always open. */
        public String research;
        /** A small node (one item, block, fluid, creature...) rather than a chapter. */
        public boolean node;
        public List<Page> pages = new ArrayList<>();
        /** Everything searchable, in lower case. */
        public String searchTitle = "", searchText = "";
        /** 1.3.1: an item's own page is open exactly when the entry about its concept is (null: on its own). */
        public String follows;
    }

    /** A place on a tab's map. */
    public record Node(String entry, int x, int y, List<String> parents, boolean small, String band, double[] route) {
        public Node(String entry, int x, int y, List<String> parents, boolean small) {
            this(entry, x, y, parents, small, "", null);
        }
    }

    /** Round 36: a tier band behind a map (a stage of the game), in map units. */
    public record Region(int tier, String label, double x0, double y0, double x1, double y1) {}

    public static final class Tab {
        public String id, name, description, icon;
        public List<Node> nodes = new ArrayList<>();
        public List<Region> regions = new ArrayList<>();
        /** 1.4.0: laid out as a tree (tools/relayout_codex.py): bands, chapter columns, items under their chapter. */
        public boolean tree;
        /** Grid bounds of all nodes. */
        public int minX, maxX, minY, maxY;
        /** Where each entry stands on this map (an entry is on a map at most once). */
        public final Map<String, Node> byEntry = new HashMap<>();
    }

    public static List<Tab> tabs = List.of();
    public static Map<String, Entry> entries = Map.of();
    public static Map<String, String> categories = Map.of();
    /** Lower-case title (and simple plural forms) to entry, for links in the text. */
    private static Map<String, String> byName = Map.of();
    /** Item id to the entry about it. */
    private static Map<String, String> byItem = Map.of();
    private static boolean loaded;
    public static String error;

    public static void clear() {
        loaded = false;
        CodexState.reset();
    }

    /** Loads the data if needed; false if it could not be read. */
    public static boolean ensure() {
        if (!loaded) {
            loaded = true;
            try {
                load();
                error = null;
            } catch (Exception ex) {
                TheoreticalSciences.LOGGER.error("Quantum Codex: could not read its data", ex);
                error = ex.toString();
                tabs = List.of();
                entries = Map.of();
            }
        }
        return !tabs.isEmpty();
    }

    private static JsonElement read(String path) throws Exception {
        ResourceLocation rl = TheoreticalSciences.id("ts_codex/" + path);
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(rl);
        if (res.isEmpty()) throw new java.io.FileNotFoundException(rl.toString());
        try (Reader r = res.get().openAsReader()) {
            return JsonParser.parseReader(r);
        }
    }

    /**
     * Round 35: every resource pack's copy of a Codex file, lowest priority first (the mod's own, then packs and
     * KubeJS's assets folder above it), so packs can add to the Codex instead of replacing whole files.
     */
    private static List<JsonElement> readAll(String path) {
        ResourceLocation rl = TheoreticalSciences.id("ts_codex/" + path);
        List<JsonElement> out = new ArrayList<>();
        for (Resource res : Minecraft.getInstance().getResourceManager().getResourceStack(rl)) {
            try (Reader r = res.openAsReader()) {
                out.add(JsonParser.parseReader(r));
            } catch (Exception ex) {
                TheoreticalSciences.LOGGER.warn("Quantum Codex: skipped {} from {} ({})", rl, res.sourcePackId(), ex.toString());
            }
        }
        return out;
    }

    /** entries.json from every pack: an entry id in a higher pack replaces the same id below; {"remove": true} deletes it. */
    private static JsonObject mergedEntries() throws Exception {
        List<JsonElement> all = readAll("entries.json");
        if (all.isEmpty()) return read("entries.json").getAsJsonObject();
        JsonObject out = new JsonObject();
        for (JsonElement e : all)
            if (e.isJsonObject())
                for (Map.Entry<String, JsonElement> me : e.getAsJsonObject().entrySet()) {
                    if (me.getValue().isJsonObject() && me.getValue().getAsJsonObject().has("remove")
                            && me.getValue().getAsJsonObject().get("remove").getAsBoolean()) out.remove(me.getKey());
                    else out.add(me.getKey(), me.getValue());
                }
        return out;
    }

    /** index.json from every pack: categories merged, tabs merged by id (a new id adds a tab at the end). */
    private static JsonObject mergedIndex() throws Exception {
        List<JsonElement> all = readAll("index.json");
        if (all.isEmpty()) return read("index.json").getAsJsonObject();
        JsonObject out = all.get(0).getAsJsonObject().deepCopy();
        JsonObject cats = out.has("categories") ? out.getAsJsonObject("categories") : new JsonObject();
        JsonArray tabs = out.has("tabs") ? out.getAsJsonArray("tabs") : new JsonArray();
        for (int i = 1; i < all.size(); i++) {
            if (!all.get(i).isJsonObject()) continue;
            JsonObject o = all.get(i).getAsJsonObject();
            if (o.has("home")) out.add("home", o.get("home"));
            if (o.has("categories")) for (Map.Entry<String, JsonElement> c : o.getAsJsonObject("categories").entrySet()) cats.add(c.getKey(), c.getValue());
            if (o.has("tabs"))
                for (JsonElement te : o.getAsJsonArray("tabs")) {
                    String id = str(te.getAsJsonObject(), "id");
                    boolean replaced = false;
                    for (int k = 0; k < tabs.size(); k++)
                        if (str(tabs.get(k).getAsJsonObject(), "id").equals(id)) {
                            tabs.set(k, te);
                            replaced = true;
                        }
                    if (!replaced) tabs.add(te);
                }
        }
        out.add("categories", cats);
        out.add("tabs", tabs);
        return out;
    }

    /** A tab's map from every pack: the nodes put together (a node for the same entry in a higher pack wins). */
    private static JsonObject mergedTab(String id) throws Exception {
        List<JsonElement> all = readAll(id + ".json");
        if (all.isEmpty()) return read(id + ".json").getAsJsonObject();
        Map<String, JsonElement> nodes = new LinkedHashMap<>();
        for (JsonElement f : all)
            if (f.isJsonObject() && f.getAsJsonObject().has("nodes"))
                for (JsonElement ne : f.getAsJsonObject().getAsJsonArray("nodes")) nodes.put(str(ne.getAsJsonObject(), "entry"), ne);
        JsonObject out = new JsonObject();
        JsonArray arr = new JsonArray();
        nodes.values().forEach(arr::add);
        out.add("nodes", arr);
        // 1.4.0: the bands and the layout come with the map (the highest pack that has them wins)
        for (JsonElement f : all)
            if (f.isJsonObject()) {
                if (f.getAsJsonObject().has("regions")) out.add("regions", f.getAsJsonObject().get("regions"));
                if (f.getAsJsonObject().has("layout")) out.add("layout", f.getAsJsonObject().get("layout"));
            }
        return out;
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : "";
    }

    private static List<String> strings(JsonObject o, String k) {
        List<String> out = new ArrayList<>();
        if (o.has(k) && o.get(k).isJsonArray()) for (JsonElement e : o.getAsJsonArray(k)) out.add(e.getAsString());
        return out;
    }

    private static void load() throws Exception {
        JsonObject index = mergedIndex();
        Map<String, String> cats = new HashMap<>();
        if (index.has("categories"))
            for (Map.Entry<String, JsonElement> e : index.getAsJsonObject("categories").entrySet()) cats.put(e.getKey(), e.getValue().getAsString());
        // ---- entries
        Map<String, Entry> ents = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> me : mergedEntries().entrySet()) {
            JsonObject o = me.getValue().getAsJsonObject();
            Entry e = new Entry();
            e.id = me.getKey();
            e.title = str(o, "title");
            e.icon = str(o, "icon");
            e.category = str(o, "category");
            e.research = o.has("research") ? o.get("research").getAsString() : null;
            e.node = o.has("node") && o.get("node").getAsBoolean();
            StringBuilder sb = new StringBuilder();
            if (o.has("pages"))
                for (JsonElement pe : o.getAsJsonArray("pages")) {
                    JsonObject po = pe.getAsJsonObject();
                    Page p = new Page();
                    p.type = str(po, "type");
                    p.title = str(po, "title");
                    p.text = str(po, "text");
                    p.items = strings(po, "items");
                    p.recipes = strings(po, "recipes");
                    p.name = str(po, "name");
                    p.multiblock = str(po, "multiblock");
                    if (po.has("pattern")) {
                        List<List<String>> layers = new ArrayList<>();
                        for (JsonElement le : po.getAsJsonArray("pattern")) {
                            List<String> rows = new ArrayList<>();
                            for (JsonElement re : le.getAsJsonArray()) rows.add(re.getAsString());
                            layers.add(rows);
                        }
                        p.pattern = layers;
                    }
                    if (po.has("mapping")) {
                        Map<String, String> m = new LinkedHashMap<>();
                        for (Map.Entry<String, JsonElement> x : po.getAsJsonObject("mapping").entrySet())
                            m.put(x.getKey(), x.getValue().isJsonPrimitive() ? x.getValue().getAsString() : x.getValue().toString());
                        p.mapping = m;
                    }
                    e.pages.add(p);
                    sb.append(' ').append(p.title).append(' ').append(CodexText.plain(p.text)).append(' ').append(p.name);
                }
            // 1.16.0: "In plain words" opens the entry: one plain sentence, an everyday comparison and where that comparison
            // stops being true. The picture comes before the symbols.
            if (o.has("plain") && o.get("plain").isJsonObject()) {
                JsonObject pl = o.getAsJsonObject("plain");
                String line = str(pl, "line"), like = str(pl, "like"), breaks = str(pl, "breaks");
                if (!line.isEmpty()) {
                    Page p = new Page();
                    p.type = "text";
                    p.title = net.minecraft.client.resources.language.I18n.get("gui.theoreticalsciences.codex.plain");
                    StringBuilder t = new StringBuilder(line);
                    if (!like.isEmpty()) t.append("$(br2)$(l)").append(net.minecraft.client.resources.language.I18n.get("gui.theoreticalsciences.codex.plain_like")).append("$() ").append(like);
                    if (!breaks.isEmpty()) t.append("$(br2)$(l)").append(net.minecraft.client.resources.language.I18n.get("gui.theoreticalsciences.codex.plain_breaks")).append("$() ").append(breaks);
                    p.text = t.toString();
                    e.pages.add(0, p);
                    sb.append(' ').append(CodexText.plain(p.text));
                }
            }
            e.searchTitle = e.title.toLowerCase(Locale.ROOT);
            e.searchText = sb.toString().toLowerCase(Locale.ROOT);
            ents.put(e.id, e);
        }
        // ---- tabs
        List<Tab> ts = new ArrayList<>();
        JsonArray tl = index.getAsJsonArray("tabs");
        for (JsonElement te : tl) {
            JsonObject to = te.getAsJsonObject();
            Tab t = new Tab();
            t.id = str(to, "id");
            t.name = str(to, "name");
            t.description = str(to, "description");
            t.icon = str(to, "icon");
            JsonObject file;
            try {
                file = mergedTab(t.id);
            } catch (Exception ex) {
                TheoreticalSciences.LOGGER.warn("Quantum Codex: tab {} has no map ({})", t.id, ex.toString());
                continue;
            }
            int x0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y0 = Integer.MAX_VALUE, y1 = Integer.MIN_VALUE;
            for (JsonElement ne : file.getAsJsonArray("nodes")) {
                JsonObject no = ne.getAsJsonObject();
                String id = str(no, "entry");
                if (!ents.containsKey(id) || t.byEntry.containsKey(id)) continue;
                double[] route = null;
                if (no.has("route") && no.get("route").isJsonArray() && no.getAsJsonArray("route").size() == 5) {
                    route = new double[5];
                    for (int i = 0; i < 5; i++) route[i] = no.getAsJsonArray("route").get(i).getAsDouble();
                }
                Node n = new Node(id, no.get("x").getAsInt(), no.get("y").getAsInt(), strings(no, "parents"), no.has("node") && no.get("node").getAsBoolean(),
                        str(no, "band"), route);
                t.nodes.add(n);
                t.byEntry.put(id, n);
                x0 = Math.min(x0, n.x()); x1 = Math.max(x1, n.x());
                y0 = Math.min(y0, n.y()); y1 = Math.max(y1, n.y());
            }
            if (t.nodes.isEmpty()) continue;
            t.minX = x0; t.maxX = x1; t.minY = y0; t.maxY = y1;
            t.tree = "tree".equals(str(file, "layout"));
            if (file.has("regions") && file.get("regions").isJsonArray())
                for (JsonElement re : file.getAsJsonArray("regions")) {
                    try {
                        JsonObject ro = re.getAsJsonObject();
                        t.regions.add(new Region(ro.get("tier").getAsInt(), str(ro, "label"), ro.get("x0").getAsDouble(), ro.get("y0").getAsDouble(),
                                ro.get("x1").getAsDouble(), ro.get("y1").getAsDouble()));
                    } catch (Exception ignored) {
                    }
                }
            ts.add(t);
        }
        // ---- lookups for links
        Map<String, String> names = new HashMap<>(), items = new HashMap<>();
        for (Entry e : ents.values()) {
            String k = e.searchTitle;
            if (e.node || !names.containsKey(k)) names.put(k, e.id);
            if (e.node && !e.icon.isEmpty()) items.put(e.icon, e.id);
        }
        for (Entry e : ents.values()) {
            if (e.node) continue;
            for (Page p : e.pages)
                if (!p.items.isEmpty()) items.putIfAbsent(p.items.get(0), e.id);
            items.putIfAbsent(e.icon, e.id);
        }
        // the text names things by their item names ("Structure Assembler"), which are not always an entry's title
        for (Map.Entry<String, String> it : items.entrySet()) {
            ResourceLocation rl = ResourceLocation.tryParse(it.getKey());
            if (rl == null || !net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(rl)) continue;
            String n = new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl)).getHoverName().getString().toLowerCase(Locale.ROOT);
            if (!n.isEmpty()) names.putIfAbsent(n, it.getValue());
        }
        Map<String, String> lookup = itemPages(ents, items);
        entries = ents;
        tabs = ts;
        categories = cats;
        byName = names;
        byItem = lookup;
    }

    /**
     * 1.3.1: the mod's items no longer describe themselves in tooltips, so the Codex says it: every item with something
     * to say gets it on a page of its own (its description, where it is found, what uses it, how to use it, and every
     * way to make it), linking on to the entry about its concept. An item that already has a page of its own (a small
     * node whose icon it is) gets the description as that page's first page instead. Looking an item up opens this.
     */
    private static Map<String, String> itemPages(Map<String, Entry> ents, Map<String, String> concepts) {
        Map<String, String> lookup = new HashMap<>(concepts);
        net.minecraft.world.item.Item.TooltipContext ctx = net.minecraft.client.Minecraft.getInstance().level != null
                ? net.minecraft.world.item.Item.TooltipContext.of(net.minecraft.client.Minecraft.getInstance().level)
                : net.minecraft.world.item.Item.TooltipContext.EMPTY;
        String seeAlso = net.minecraft.client.resources.language.I18n.get("gui.theoreticalsciences.codex.item_concept");
        for (net.minecraft.world.item.Item it : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            ResourceLocation rl = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(it);
            if (!TheoreticalSciences.MODID.equals(rl.getNamespace())) continue;
            List<String> paras;
            try {
                paras = itemText(it, rl, ctx);
            } catch (Throwable t) {
                continue;                                  // (an item whose text cannot be worked out here: no page)
            }
            if (paras.isEmpty()) continue;
            String id = rl.toString(), text = String.join("$(br2)", paras);
            String conceptId = concepts.get(id);
            Entry concept = conceptId == null ? null : ents.get(conceptId);
            Page p = new Page();
            p.type = "spotlight";
            p.items = List.of(id);
            if (concept != null && concept.node && id.equals(concept.icon)) {
                p.text = text;
                concept.pages.add(0, p);
                concept.searchText += " " + CodexText.plain(text).toLowerCase(Locale.ROOT);
                continue;
            }
            if (concept != null) p.text = text + "$(br2)$(l)" + seeAlso + "$() $(l:" + concept.id + ")" + concept.title + "$(/l)";
            else p.text = text;
            Entry e = new Entry();
            e.id = "item/" + rl.getPath();
            e.title = new net.minecraft.world.item.ItemStack(it).getHoverName().getString();
            e.icon = id;
            e.category = concept != null ? concept.category : "world";
            e.follows = concept != null ? concept.id : null;
            e.pages.add(p);
            e.searchTitle = e.title.toLowerCase(Locale.ROOT);
            e.searchText = CodexText.plain(text).toLowerCase(Locale.ROOT);
            ents.put(e.id, e);
            lookup.put(id, e.id);
        }
        return lookup;
    }

    /** The paragraphs an item's page says (what its tooltip used to say about the kind of item). */
    private static List<String> itemText(net.minecraft.world.item.Item it, ResourceLocation rl, net.minecraft.world.item.Item.TooltipContext ctx) {
        List<String> out = new ArrayList<>();
        String base = it.getDescriptionId();
        for (String k : new String[]{base + ".lore", base + ".shield", base + ".found"})
            if (net.minecraft.client.resources.language.I18n.exists(k)) add(out, net.minecraft.client.resources.language.I18n.get(k));
        List<net.minecraft.network.chat.Component> lines = new ArrayList<>();
        it.appendHoverText(new net.minecraft.world.item.ItemStack(it), ctx, lines, net.minecraft.world.item.TooltipFlag.NORMAL);
        StringBuilder uses = new StringBuilder();
        for (net.minecraft.network.chat.Component c : lines)
            if (com.theoreticalsciences.client.TooltipTrim.descriptive(c)) {
                String t = c.getString().trim();
                if (t.isEmpty() || out.contains(t)) continue;
                uses.append(uses.length() == 0 ? "" : "$(br)").append("$(li)").append(esc(t));
            }
        for (net.minecraft.network.chat.Component c : com.theoreticalsciences.client.ItemTips.codexFacts(it, rl))
            uses.append(uses.length() == 0 ? "" : "$(br)").append("$(li)").append(esc(c.getString().trim()));
        if (uses.length() > 0) out.add(uses.toString());
        return out;
    }

    private static void add(List<String> out, String t) {
        t = t.trim();
        if (!t.isEmpty() && !out.contains(esc(t))) out.add(esc(t));
    }

    /** Text from the language file is plain: never let it read as the Codex's formatting codes. */
    private static String esc(String t) {
        return t.replace("$(", "$ (");
    }

    /** The entry a piece of highlighted text names, or null ("Bubble Universes" finds "Bubble Universe"). */
    public static String entryNamed(String text) {
        String k = text.trim().toLowerCase(Locale.ROOT);
        if (k.isEmpty()) return null;
        String r = byName.get(k);
        if (r == null && k.endsWith("es")) r = byName.get(k.substring(0, k.length() - 2));
        if (r == null && k.endsWith("s")) r = byName.get(k.substring(0, k.length() - 1));
        if (r == null && k.startsWith("the ")) r = byName.get(k.substring(4));
        return r;
    }

    /** The entry about an item (its own node if it has one), or null. */
    public static String entryForItem(String itemId) {
        return byItem.get(itemId);
    }

    public static Tab tab(String id) {
        for (Tab t : tabs) if (t.id.equals(id)) return t;
        return tabs.isEmpty() ? null : tabs.get(0);
    }

    private CodexData() {}
}
