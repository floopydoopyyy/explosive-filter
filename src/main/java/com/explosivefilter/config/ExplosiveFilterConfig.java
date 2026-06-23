package com.explosivefilter.config;

import com.explosivefilter.Explosivefilter;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class ExplosiveFilterConfig {

    private static final int CURRENT_VERSION = 2;

    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("explosivefilter.json");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public record PhraseEntry(String phrase, float power) {}

    private static final List<PhraseEntry> PHRASES = new ArrayList<>();
    private static float defaultPower = 4.0f;
    private static boolean worldDamage = true;
    private static boolean dealDamage   = true;
    private static boolean instakill    = true;
    private static boolean dropItems    = true;
    private static boolean fire         = false;
    private static boolean damageOthers = false;

    // ── Accessors ─────────────────────────────────────────────────────────────

    public static List<PhraseEntry> getPhrases() {
        return Collections.unmodifiableList(PHRASES);
    }

    public static float getDefaultPower() { return defaultPower; }
    public static boolean isWorldDamage()  { return worldDamage; }
    public static boolean isDealDamage()   { return dealDamage; }
    public static boolean isInstakill()    { return instakill; }
    public static boolean isDropItems()    { return dropItems; }
    public static boolean isFire()         { return fire; }
    public static boolean isDamageOthers() { return damageOthers; }

    // ── Setters (each persists immediately) ───────────────────────────────────

    public static void setDefaultPower(float v) { defaultPower = Math.max(0.1f, v); save(); }
    public static void setWorldDamage(boolean v)  { worldDamage  = v; save(); }
    public static void setDealDamage(boolean v)   { dealDamage   = v; save(); }
    public static void setInstakill(boolean v)    { instakill    = v; save(); }
    public static void setDropItems(boolean v)    { dropItems    = v; save(); }
    public static void setFire(boolean v)         { fire         = v; save(); }
    public static void setDamageOthers(boolean v) { damageOthers = v; save(); }

    // ── Phrase management ─────────────────────────────────────────────────────

    /** Adds or updates a phrase. Returns true if it was a new entry. */
    public static boolean addPhrase(String phrase, float power) {
        String key = phrase.strip().toLowerCase();
        if (key.isEmpty()) return false;
        for (int i = 0; i < PHRASES.size(); i++) {
            if (PHRASES.get(i).phrase().equals(key)) {
                PHRASES.set(i, new PhraseEntry(key, Math.max(0.1f, power)));
                save();
                return false;
            }
        }
        PHRASES.add(new PhraseEntry(key, Math.max(0.1f, power)));
        save();
        return true;
    }

    /** Removes a phrase. Returns true if it existed. */
    public static boolean removePhrase(String phrase) {
        String key = phrase.strip().toLowerCase();
        boolean removed = PHRASES.removeIf(e -> e.phrase().equals(key));
        if (removed) save();
        return removed;
    }

    // ── Matching ──────────────────────────────────────────────────────────────

    /**
     * Scans the phrase list for the first entry whose phrase appears in messageContent.
     * Matching is whole-word (or whole-phrase), case-insensitive, first match wins.
     */
    public static PhraseEntry findMatch(String messageContent) {
        for (PhraseEntry entry : PHRASES) {
            String phrase = entry.phrase();
            if (phrase.isEmpty()) continue;
            String lead  = isWordChar(phrase.charAt(0))                   ? "\\b" : "";
            String trail = isWordChar(phrase.charAt(phrase.length() - 1)) ? "\\b" : "";
            String regex = "(?i)" + lead + Pattern.quote(phrase) + trail;
            if (Pattern.compile(regex).matcher(messageContent).find()) {
                return entry;
            }
        }
        return null;
    }

    private static boolean isWordChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
    }

    /** Returns the explosion power for the first matching phrase, or -1 if none. */
    public static float getPowerFor(String messageContent) {
        PhraseEntry match = findMatch(messageContent);
        return match == null ? -1f : match.power();
    }

    /** Returns the matched phrase string, or "?" if none matched. */
    public static String findMatchedPhrase(String messageContent) {
        PhraseEntry match = findMatch(messageContent);
        return match == null ? "?" : match.phrase();
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    public static void load() {
        PHRASES.clear();
        File file = CONFIG_PATH.toFile();
        if (!file.exists()) {
            PHRASES.add(new PhraseEntry("creeper", 3.0f));
            PHRASES.add(new PhraseEntry("boom",    5.0f));
            PHRASES.add(new PhraseEntry("oh man",  4.0f));
            save();
            return;
        }
        try (JsonReader jsonReader = new JsonReader(new FileReader(file))) {
            jsonReader.setLenient(true);
            JsonObject root = GSON.fromJson(jsonReader, JsonObject.class);
            if (root == null) return;
            if (root.has("defaultPower")) defaultPower = root.get("defaultPower").getAsFloat();
            if (root.has("worldDamage"))  worldDamage  = root.get("worldDamage").getAsBoolean();
            if (root.has("dealDamage"))   dealDamage   = root.get("dealDamage").getAsBoolean();
            if (root.has("instakill"))    instakill    = root.get("instakill").getAsBoolean();
            if (root.has("dropItems"))     dropItems    = root.get("dropItems").getAsBoolean();
            if (root.has("fire"))          fire         = root.get("fire").getAsBoolean();
            if (root.has("damageOthers"))  damageOthers = root.get("damageOthers").getAsBoolean();

            if (root.has("phrases")) {
                for (JsonElement el : root.getAsJsonArray("phrases")) {
                    JsonObject obj = el.getAsJsonObject();
                    String phrase = obj.get("phrase").getAsString().strip().toLowerCase();
                    float  power  = obj.get("power").getAsFloat();
                    PHRASES.add(new PhraseEntry(phrase, power));
                }
            } else if (root.has("words")) {
                // Migrate from v1.0 format
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("words").entrySet()) {
                    PHRASES.add(new PhraseEntry(entry.getKey().toLowerCase(), entry.getValue().getAsFloat()));
                }
                save();
            }
        } catch (IOException | JsonParseException | IllegalStateException e) {
            Explosivefilter.LOGGER.error("[ExplosiveFilter] Failed to load config: {}", e.getMessage());
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();

        root.addProperty("_version", CURRENT_VERSION);
        root.addProperty("_info",
                "Explosive Filter config — players who say a trigger phrase get an explosion at their position. "
                + "All settings can also be changed live with /explosivefilter commands (op level 2 required).");

        // ── Global settings ────────────────────────────────────────────────
        JsonObject settingsDocs = new JsonObject();
        settingsDocs.addProperty("defaultPower",
                "Explosion power used when adding a phrase without specifying a power value. Range: 0.1 – 100.0. "
                + "Higher values produce a larger, more destructive blast.");
        settingsDocs.addProperty("worldDamage",
                "true  = explosion destroys blocks (like TNT). "
                + "false = explosion only affects entities; blocks are untouched.");
        settingsDocs.addProperty("dealDamage",
                "true  = entities within the blast radius take damage and receive knockback. "
                + "false = explosion is purely visual; no entity damage or knockback.");
        settingsDocs.addProperty("instakill",
                "true  = the speaker always dies instantly, regardless of difficulty, armour, or game mode "
                + "(uses a damage type that bypasses creative invulnerability and Peaceful scaling). "
                + "false = vanilla explosion damage applies — players may survive depending on distance and armour.");
        settingsDocs.addProperty("dropItems",
                "true  = blocks destroyed by the explosion may drop items (vanilla TNT probability — most blocks will not drop). "
                + "false = no item drops; blocks are also not broken (API limitation: drops cannot be suppressed without also preventing destruction).");
        settingsDocs.addProperty("fire",
                "true  = the explosion may ignite nearby air blocks, spreading fire. "
                + "false = no fire is spawned.");
        settingsDocs.addProperty("damageOthers",
                "true  = the explosion damages all entities in the blast radius (mobs, other players, etc.). "
                + "false = only the speaker takes damage; all other entities are unaffected by the blast.");
        root.add("_settings_help", settingsDocs);

        root.addProperty("defaultPower", defaultPower);
        root.addProperty("worldDamage",  worldDamage);
        root.addProperty("dealDamage",   dealDamage);
        root.addProperty("instakill",    instakill);
        root.addProperty("dropItems",     dropItems);
        root.addProperty("fire",          fire);
        root.addProperty("damageOthers",  damageOthers);

        // ── Phrases ────────────────────────────────────────────────────────
        root.addProperty("_phrases_help",
                "List of trigger phrases. Matching is case-insensitive and whole-word (so 'boom' won't trigger on 'kaboom'). "
                + "The first phrase in the list that matches the chat message wins. "
                + "To add an entry, copy this format into the array: { \"phrase\": \"your text here\", \"power\": 4.0 } "
                + "Multi-word phrases are supported: { \"phrase\": \"oh man\", \"power\": 5.0 } "
                + "You can also use the command: /explosivefilter add <phrase> [power]");

        JsonArray phrasesArr = new JsonArray();
        for (PhraseEntry entry : PHRASES) {
            JsonObject obj = new JsonObject();
            obj.addProperty("phrase", entry.phrase());
            obj.addProperty("power",  entry.power());
            phrasesArr.add(obj);
        }
        root.add("phrases", phrasesArr);

        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            Explosivefilter.LOGGER.error("[ExplosiveFilter] Failed to save config: {}", e.getMessage());
        }
    }
}
