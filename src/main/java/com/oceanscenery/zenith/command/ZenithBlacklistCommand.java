package com.oceanscenery.zenith.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.event.CapabilitiesHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = TheZenithMod.MOD_ID)
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
                        .then(Commands.argument("targets", EntityArgument.entity())
                            .suggests(ZenithBlacklistCommand::suggestEntitySelector)
                            .executes(context -> updateBlacklist(context, true))))
                    .then(Commands.literal("remove")
                        .then(Commands.argument("targets", EntityArgument.entity())
                            .suggests(ZenithBlacklistCommand::suggestEntitySelector)
                            .executes(context -> updateBlacklist(context, false)))))
                .then(Commands.literal("type")
                    .then(Commands.literal("add")
                        .then(Commands.argument("type",
                                ResourceArgument.resource(event.getBuildContext(), Registries.ENTITY_TYPE))
                            .executes(context -> updateTypeTarget(context, true))))
                    .then(Commands.literal("remove")
                        .then(Commands.argument("type",
                                ResourceArgument.resource(event.getBuildContext(), Registries.ENTITY_TYPE))
                            .executes(context -> updateTypeTarget(context, false))))
                    .then(Commands.literal("addfrom")
                        .then(Commands.argument("targets", EntityArgument.entity())
                            .suggests(ZenithBlacklistCommand::suggestEntitySelector)
                            .executes(ZenithBlacklistCommand::addTypeFromEntity)))
                    .then(Commands.literal("removefrom")
                        .then(Commands.argument("targets", EntityArgument.entity())
                            .suggests(ZenithBlacklistCommand::suggestEntitySelector)
                            .executes(ZenithBlacklistCommand::removeTypeFromEntity))))
        );
    }

    private static CompletableFuture<Suggestions> suggestEntitySelector(
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

        return parser.fillSuggestions(builder, suggestions ->
            SharedSuggestionProvider.suggest(
                context.getSource().getOnlinePlayerNames(),
                suggestions
            )
        );
    }

    private static CommandSourceStack selectorSource(CommandSourceStack source) {
        return source.hasPermission(2) ? source : source.withPermission(2);
    }

    private static Entity resolveSingleEntity(CommandContext<CommandSourceStack> context)
        throws CommandSyntaxException {
        EntitySelector selector = context.getArgument("targets", EntitySelector.class);
        return selector.findSingleEntity(selectorSource(context.getSource()));
    }

    private static int updateTypeTarget(
        CommandContext<CommandSourceStack> context,
        boolean blacklisted
    ) throws CommandSyntaxException {
        Holder.Reference<EntityType<?>> holder =
            ResourceArgument.getResource(context, "type", Registries.ENTITY_TYPE);

        return updateTypeBlacklist(context, holder.key().location(), blacklisted);
    }

    private static int addTypeFromEntity(
        CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        Entity target = resolveSingleEntity(context);
        return updateTypeBlacklist(context, EntityType.getKey(target.getType()), true);
    }

    private static int removeTypeFromEntity(
        CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        Entity target = resolveSingleEntity(context);
        return updateTypeBlacklist(context, EntityType.getKey(target.getType()), false);
    }

    private static int updateTypeBlacklist(
        CommandContext<CommandSourceStack> context,
        ResourceLocation typeId,
        boolean blacklisted
    ) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean changed = CapabilitiesHandler.setTypeBlacklist(player, typeId.toString(), blacklisted);
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
        boolean blacklisted
    ) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Entity target = resolveSingleEntity(context);

        boolean changed = CapabilitiesHandler.setBlacklist(player, target, false, blacklisted);
        String messageKey = blacklisted
            ? changed
            ? "commands.the_zenith_sword.blacklist.entity.added"
            : "commands.the_zenith_sword.blacklist.entity.already_added"
            : changed
            ? "commands.the_zenith_sword.blacklist.entity.removed"
            : "commands.the_zenith_sword.blacklist.entity.already_removed";
        context.getSource().sendSuccess(
            () -> Component.translatable(messageKey, target.getDisplayName()),
            true
        );
        return 1;
    }
}
