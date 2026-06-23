package com.explosivefilter.command;

import com.explosivefilter.config.ExplosiveFilterConfig;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.List;
import java.util.function.Consumer;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class FilterCommand {

    private static final int REQUIRED_PERMISSION = 2;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
            dispatcher.register(
                literal("explosivefilter")
                    .requires(src -> src.hasPermission(REQUIRED_PERMISSION))

                    // ── add <phrase> [power] ─────────────────────────────────────────
                    // greedyString captures everything after the token, including spaces.
                    // Trailing float is parsed as optional power; rest is the phrase.
                    .then(literal("add")
                        .then(argument("phrase", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String raw = StringArgumentType.getString(ctx, "phrase");
                                String[] parts = splitPhraseAndPower(raw);
                                String phrase = parts[0];
                                float  power  = parts[1] != null
                                        ? Float.parseFloat(parts[1])
                                        : ExplosiveFilterConfig.getDefaultPower();
                                boolean isNew = ExplosiveFilterConfig.addPhrase(phrase, power);
                                String key = isNew
                                        ? "commands.explosivefilter.add.success"
                                        : "commands.explosivefilter.add.updated";
                                ctx.getSource().sendSuccess(
                                        new TranslatableComponent(key, displayPhrase(phrase), power),
                                        true);
                                return 1;
                            })))

                    // ── remove <phrase> ──────────────────────────────────────────────
                    .then(literal("remove")
                        .then(argument("phrase", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String phrase = StringArgumentType.getString(ctx, "phrase").strip();
                                if (ExplosiveFilterConfig.removePhrase(phrase)) {
                                    ctx.getSource().sendSuccess(
                                            new TranslatableComponent(
                                                    "commands.explosivefilter.remove.success",
                                                    displayPhrase(phrase)),
                                            true);
                                    return 1;
                                } else {
                                    ctx.getSource().sendFailure(
                                            new TranslatableComponent(
                                                    "commands.explosivefilter.remove.fail",
                                                    displayPhrase(phrase)));
                                    return 0;
                                }
                            })))

                    // ── list ─────────────────────────────────────────────────────────
                    .then(literal("list")
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            List<ExplosiveFilterConfig.PhraseEntry> phrases =
                                    ExplosiveFilterConfig.getPhrases();
                            src.sendSuccess(
                                    new TranslatableComponent("commands.explosivefilter.list.header"),
                                    false);
                            if (phrases.isEmpty()) {
                                src.sendSuccess(
                                        new TranslatableComponent("commands.explosivefilter.list.empty"),
                                        false);
                            } else {
                                phrases.forEach(e -> src.sendSuccess(
                                        new TranslatableComponent(
                                                "commands.explosivefilter.list.entry",
                                                displayPhrase(e.phrase()), e.power()),
                                        false));
                            }
                            return phrases.size();
                        }))

                    // ── reload ───────────────────────────────────────────────────────
                    .then(literal("reload")
                        .executes(ctx -> {
                            ExplosiveFilterConfig.load();
                            int count = ExplosiveFilterConfig.getPhrases().size();
                            ctx.getSource().sendSuccess(
                                    new TranslatableComponent(
                                            "commands.explosivefilter.reload.success", count),
                                    true);
                            return count;
                        }))

                    // ── defaultpower <value> ─────────────────────────────────────────
                    .then(literal("defaultpower")
                        .then(argument("power", FloatArgumentType.floatArg(0.1f, 100f))
                            .executes(ctx -> {
                                float power = FloatArgumentType.getFloat(ctx, "power");
                                ExplosiveFilterConfig.setDefaultPower(power);
                                ctx.getSource().sendSuccess(
                                        new TranslatableComponent(
                                                "commands.explosivefilter.power.set", power),
                                        true);
                                return 1;
                            })))

                    // ── worlddamage on|off ───────────────────────────────────────────
                    .then(literal("worlddamage")
                        .then(literal("on").executes(ctx -> setBool(ctx.getSource(),
                                ExplosiveFilterConfig::setWorldDamage, true,
                                "commands.explosivefilter.worlddamage.on")))
                        .then(literal("off").executes(ctx -> setBool(ctx.getSource(),
                                ExplosiveFilterConfig::setWorldDamage, false,
                                "commands.explosivefilter.worlddamage.off"))))

                    // ── damage on|off ────────────────────────────────────────────────
                    .then(literal("damage")
                        .then(literal("on").executes(ctx -> setBool(ctx.getSource(),
                                ExplosiveFilterConfig::setDealDamage, true,
                                "commands.explosivefilter.damage.on")))
                        .then(literal("off").executes(ctx -> setBool(ctx.getSource(),
                                ExplosiveFilterConfig::setDealDamage, false,
                                "commands.explosivefilter.damage.off"))))

                    // ── instakill on|off ─────────────────────────────────────────────
                    .then(literal("instakill")
                        .then(literal("on").executes(ctx -> setBool(ctx.getSource(),
                                ExplosiveFilterConfig::setInstakill, true,
                                "commands.explosivefilter.instakill.on")))
                        .then(literal("off").executes(ctx -> setBool(ctx.getSource(),
                                ExplosiveFilterConfig::setInstakill, false,
                                "commands.explosivefilter.instakill.off"))))

                    // ── dropitems on|off ─────────────────────────────────────────────
                    .then(literal("dropitems")
                        .then(literal("on").executes(ctx -> setBool(ctx.getSource(),
                                ExplosiveFilterConfig::setDropItems, true,
                                "commands.explosivefilter.dropitems.on")))
                        .then(literal("off").executes(ctx -> setBool(ctx.getSource(),
                                ExplosiveFilterConfig::setDropItems, false,
                                "commands.explosivefilter.dropitems.off"))))

                    // ── fire on|off ──────────────────────────────────────────────────
                    .then(literal("fire")
                        .then(literal("on").executes(ctx -> setBool(ctx.getSource(),
                                ExplosiveFilterConfig::setFire, true,
                                "commands.explosivefilter.fire.on")))
                        .then(literal("off").executes(ctx -> setBool(ctx.getSource(),
                                ExplosiveFilterConfig::setFire, false,
                                "commands.explosivefilter.fire.off"))))

                    // ── damageothers on|off ──────────────────────────────────────────
                    .then(literal("damageothers")
                        .then(literal("on").executes(ctx -> setBool(ctx.getSource(),
                                ExplosiveFilterConfig::setDamageOthers, true,
                                "commands.explosivefilter.damageothers.on")))
                        .then(literal("off").executes(ctx -> setBool(ctx.getSource(),
                                ExplosiveFilterConfig::setDamageOthers, false,
                                "commands.explosivefilter.damageothers.off"))))
            ));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Wraps multi-word phrases in quotes for display; single words are shown bare. */
    private static String displayPhrase(String phrase) {
        return phrase.contains(" ") ? "\"" + phrase + "\"" : phrase;
    }

    /**
     * Parses a greedy string that may end with an optional float power value.
     *
     * "boom 5.0"              → ["boom",             "5.0"]
     * "never gonna give 7.0"  → ["never gonna give", "7.0"]
     * "creeper"               → ["creeper",           null ]
     *
     * Surrounding quotes are stripped first (Brigadier greedyString may leave them).
     */
    private static String[] splitPhraseAndPower(String raw) {
        raw = raw.strip();
        if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() > 1) {
            return new String[]{ raw.substring(1, raw.length() - 1).strip(), null };
        }
        int lastSpace = raw.lastIndexOf(' ');
        if (lastSpace >= 0) {
            String lastToken = raw.substring(lastSpace + 1);
            try {
                Float.parseFloat(lastToken);
                return new String[]{ raw.substring(0, lastSpace).strip(), lastToken };
            } catch (NumberFormatException ignored) {}
        }
        return new String[]{ raw, null };
    }

    private static int setBool(CommandSourceStack src, Consumer<Boolean> setter,
                                boolean value, String langKey) {
        setter.accept(value);
        src.sendSuccess(new TranslatableComponent(langKey), true);
        return 1;
    }
}
