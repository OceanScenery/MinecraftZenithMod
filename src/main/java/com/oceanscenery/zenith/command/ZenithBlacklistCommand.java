package com.oceanscenery.zenith.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.util.AttachmentUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = TheZenithMod.MOD_ID)
public final class ZenithBlacklistCommand {
    private ZenithBlacklistCommand() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("zenithblacklist")
                .then(Commands.literal("entity")
                    .then(Commands.literal("add")
                        .then(Commands.argument("targets", StringArgumentType.greedyString())
                            .suggests(ZenithBlacklistCommand::suggestEntitySelectors)
                            .executes(context -> updateBlacklist(context, false, true, "targets"))))
                    .then(Commands.literal("remove")
                        .then(Commands.argument("targets", StringArgumentType.greedyString())
                            .suggests(ZenithBlacklistCommand::suggestEntitySelectors)
                            .executes(context -> updateBlacklist(context, false, false, "targets")))))
                .then(Commands.literal("type")
                    .then(Commands.literal("add")
                        .then(Commands.argument("target", StringArgumentType.greedyString())
                            .suggests(ZenithBlacklistCommand::suggestTypeTargets)
                            .executes(context -> updateTypeTarget(context, true))))
                    .then(Commands.literal("remove")
                        .then(Commands.argument("target", StringArgumentType.greedyString())
                            .suggests(ZenithBlacklistCommand::suggestTypeTargets)
                            .executes(context -> updateTypeTarget(context, false)))))
        );

    }

    private static CompletableFuture<Suggestions> suggestTypeTargets(
        CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder
    ) {
        if (builder.getRemaining().startsWith("@")) {
            return suggestEntitySelectors(context, builder);
        }
        return suggestEntityTypes(context, builder);
    }

    private static CompletableFuture<Suggestions> suggestEntitySelectors(
        CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder
    ) {
        StringReader reader = new StringReader(builder.getInput());
        reader.setCursor(builder.getStart());
        EntitySelectorParser parser = new EntitySelectorParser(reader, true);
        try {
            parser.parse();
        } catch (CommandSyntaxException ignored) {
        }

        return parser.fillSuggestions(builder, suggestions -> {
            SharedSuggestionProvider.suggest(
                context.getSource().getOnlinePlayerNames(),
                suggestions
            );
            SharedSuggestionProvider.suggest(
                context.getSource().getSelectedEntities(),
                suggestions
            );
        });
    }

    private static CompletableFuture<Suggestions> suggestEntityTypes(
        CommandContext<CommandSourceStack> context,
        SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggest(
            BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                .map(Identifier::toString),
            builder
        );
    }

    private static int updateTypeTarget(
        CommandContext<CommandSourceStack> context,
        boolean blacklisted) throws CommandSyntaxException {
        String input = StringArgumentType.getString(context, "target");

        if (input.indexOf(':') >= 0) {
            Identifier typeId = Identifier.tryParse(input);
            if (typeId != null && BuiltInRegistries.ENTITY_TYPE.containsKey(typeId)) {
                return updateTypeBlacklist(context, typeId, blacklisted);
            }

            context.getSource().sendFailure(
                Component.translatable(
                    "commands.the_zenith_sword.blacklist.unknown_type",
                    input
                )
            );
            return 0;
        }

        return updateBlacklist(context, true, blacklisted, "target");
    }

    private static int updateTypeBlacklist(
        CommandContext<CommandSourceStack> context,
        Identifier typeId,
        boolean blacklisted
    ) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean changed = AttachmentUtil.setTypeBlacklist(player, typeId.toString(), blacklisted);
        context.getSource().sendSuccess(
            () -> Component.translatable(
                blacklisted
                    ? changed
                        ? "commands.the_zenith_sword.blacklist.type.added"
                        : "commands.the_zenith_sword.blacklist.type.already_added"
                    : changed
                        ? "commands.the_zenith_sword.blacklist.type.removed"
                        : "commands.the_zenith_sword.blacklist.type.already_removed",
                typeId.toString()
            ),
            true
        );
        return 1;
    }

    private static int updateBlacklist(
        CommandContext<CommandSourceStack> context,
        boolean typeBlacklist,
        boolean blacklisted,
        String targetArgument
    ) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String selectorString = StringArgumentType.getString(context, targetArgument);
        StringReader reader = new StringReader(selectorString);
        EntitySelector selector = new EntitySelectorParser(reader, true).parse();

        if (reader.canRead()) {
            throw EntitySelectorParser.ERROR_INVALID_NAME_OR_UUID.createWithContext(reader);
        }

        if (selector.getMaxResults() != 1) {
            context.getSource().sendFailure(
                Component.translatable("commands.the_zenith_sword.blacklist.selector.single_only")
            );
            return 0;
        }

        var selectorSource = context.getSource()
            .withMaximumPermission(PermissionSet.ALL_PERMISSIONS);
        var targets = selector.findEntities(selectorSource);
        if (targets.size() != 1) {
            context.getSource().sendFailure(
                Component.translatable("commands.the_zenith_sword.blacklist.selector.single_only")
            );
            return 0;
        }

        Entity target = targets.getFirst();
        boolean changed = AttachmentUtil.setBlacklist(player, target, typeBlacklist, blacklisted);
        String messageKey = blacklisted
            ? changed
                ? typeBlacklist
                    ? "commands.the_zenith_sword.blacklist.type.added"
                    : "commands.the_zenith_sword.blacklist.entity.added"
                : typeBlacklist
                    ? "commands.the_zenith_sword.blacklist.type.already_added"
                    : "commands.the_zenith_sword.blacklist.entity.already_added"
            : changed
                ? typeBlacklist
                    ? "commands.the_zenith_sword.blacklist.type.removed"
                    : "commands.the_zenith_sword.blacklist.entity.removed"
                : typeBlacklist
                    ? "commands.the_zenith_sword.blacklist.type.already_removed"
                    : "commands.the_zenith_sword.blacklist.entity.already_removed";
        context.getSource().sendSuccess(
            () -> typeBlacklist
                ? Component.translatable(messageKey, EntityType.getKey(target.getType()).toString())
                : Component.translatable(messageKey, target.getDisplayName()),
            true
        );
        return 1;
    }
}
