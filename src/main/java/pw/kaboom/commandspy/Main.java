package pw.kaboom.commandspy;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import pw.kaboom.commandspy.command.PlayerOrUUIDArgumentType;
import pw.kaboom.commandspy.command.StateArgumentType;

import java.io.File;
import java.util.List;

import static io.papermc.paper.command.brigadier.Commands.*;
import static pw.kaboom.commandspy.command.PlayerOrUUIDArgumentType.getPlayer;
import static pw.kaboom.commandspy.command.StateArgumentType.getState;

public final class Main extends JavaPlugin implements Listener {
    public static final SimpleCommandExceptionType ERROR_NOT_PLAYER =
        new SimpleCommandExceptionType(MessageComponentSerializer.message()
            .serialize(Component.translatable("permissions.requires.player")));

    private CommandSpyState config;

    @Override
    public void onEnable() {
        this.config = new CommandSpyState(new File(this.getDataFolder(), "state.bin"));


        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
            event -> this.registerCommands(event.registrar()));
        this.getServer().getPluginManager().registerEvents(this, this);

        // Save the state every 30 seconds
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this.config::trySave, 600L, 600L);
    }

    @Override
    public void onDisable() {
        this.config.trySave();
    }

    private void registerCommands(final Commands registrar) {
        final LiteralCommandNode<CommandSourceStack> commandSpyCommand =
            literal("commandspy")
                .requires(Commands.restricted(ctx -> ctx.getSender()
                    .hasPermission("commandspy.command")))
                .then(argument("state", new StateArgumentType())
                    .executes(ctx -> updateState(ctx.getSource().getSender(),
                        null, getState(ctx, "state"))))
                .then(argument("target", new PlayerOrUUIDArgumentType())
                    .then(argument("state", new StateArgumentType())
                        .executes(ctx -> updateState(ctx.getSource().getSender(),
                            getPlayer(ctx, "target"), getState(ctx, "state"))))
                    .executes(ctx -> updateState(ctx.getSource().getSender(),
                        getPlayer(ctx, "target"), null)))
                .executes(ctx -> updateState(ctx.getSource().getSender(),
                    null, null))
                .build();

        registrar.register(commandSpyCommand,
            "Allows you to spy on players' commands", List.of("c", "cs", "cspy"));
    }

    private int updateState(final @NotNull CommandSender source,
                            Player target, Boolean state) throws CommandSyntaxException {
        if (target == null) {
            if (!(source instanceof final Player player)) throw ERROR_NOT_PLAYER.create();
            target = player;
        }

        if (state == null) state = !this.config.getCommandSpyState(target.getUniqueId());

        this.config.setCommandSpyState(target.getUniqueId(), state);

        final Component stateString = Component.text(state ? "enabled" : "disabled");
        target.sendMessage(Component.empty()
                .append(Component.text("Successfully "))
                .append(stateString)
                .append(Component.text(" CommandSpy")));

        if (source != target) {
            source.sendMessage(Component.empty()
                    .append(Component.text("Successfully "))
                    .append(stateString)
                    .append(Component.text(" CommandSpy for "))
                    .append(target.name()));
        }

        return Command.SINGLE_SUCCESS;
    }

    private NamedTextColor getTextColor(final Player player) {
        if (this.config.getCommandSpyState(player.getUniqueId())) {
            return NamedTextColor.YELLOW;
        }

        return NamedTextColor.AQUA;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();
        final NamedTextColor color = getTextColor(player);

        final Component message = Component.text(player.getName(), color)
                .append(Component.text(": "))
                .append(Component.text(event.getMessage()));

        this.config.broadcastSpyMessage(message);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onSignChange(final SignChangeEvent event) {
        final Player player = event.getPlayer();
        final NamedTextColor color = getTextColor(player);
        Component message = Component.text(player.getName(), color)
                .append(Component.text(" created a sign with contents:"));

        for (final Component line : event.lines()) {
            message = message
                    .append(Component.text("\n "))
                    .append(line);
        }

        this.config.broadcastSpyMessage(message);
    }
}
