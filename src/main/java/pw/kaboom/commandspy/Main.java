package pw.kaboom.commandspy;

import java.io.File;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

public final class Main extends JavaPlugin implements CommandExecutor, Listener {
    private CommandSpyState config;

    @Override
    public void onEnable() {
        this.config = new CommandSpyState(new File(this.getDataFolder(), "state.bin"));

        this.getCommand("commandspy").setExecutor(this);
        this.getServer().getPluginManager().registerEvents(this, this);

        // Save the state every 30 seconds
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this.config::trySave, 600L, 600L);
    }

    @Override
    public void onDisable() {
        this.config.trySave();
    }

    private void updateCommandSpyState(final @NotNull Player target,
                                       final @NotNull CommandSender source, final boolean state) {
        this.config.setCommandSpyState(target.getUniqueId(), state);

        final Component stateString = Component.text(state ? "enabled" : "disabled");

        target.sendMessage(Component.empty()
                .append(Component.text("Successfully "))
                .append(stateString)
                .append(Component.text(" CommandSpy.")));

        if (source != target) {
            source.sendMessage(Component.empty()
                    .append(Component.text("Successfully "))
                    .append(stateString)
                    .append(Component.text(" CommandSpy for "))
                    .append(target.name())
                    .append(Component.text("."))
            );
        }
    }

    private NamedTextColor getTextColor(final Player player) {
        if (this.config.getCommandSpyState(player.getUniqueId())) {
            return NamedTextColor.YELLOW;
        }

        return NamedTextColor.AQUA;
    }

    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd,
                             final @NotNull String label, final String[] args) {

        Player target = null;
        Boolean state = null;

        switch (args.length) {
            case 0 -> {}
            case 1, 2 -> {
                // Get the last argument as a state. Fail if we have 2 arguments.
                state = getState(args[args.length - 1]);
                if (state != null && args.length == 1) {
                    break;
                } else if (state == null && args.length == 2) {
                    return false;
                }

                // Get the first argument as a player. Fail if it can't be found.
                target = getPlayer(args[0]);
                if (target != null) {
                    break;
                }

                sender.sendMessage(Component.empty()
                        .append(Component.text("Player \""))
                        .append(Component.text(args[0]))
                        .append(Component.text("\" not found"))
                );
                return true;
            }
            default -> {
                return false;
            }
        }

        if (target == null) {
            if (!(sender instanceof final Player player)) {
                sender.sendMessage(Component.text("Command has to be run by a player"));
                return true;
            }

            target = player;
        }

        if (state == null) {
            state = !this.config.getCommandSpyState(target.getUniqueId());
        }

        this.updateCommandSpyState(target, sender, state);

        return true;
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

    private static Player getPlayer(final String arg) {
        final Player player = Bukkit.getPlayer(arg);
        if (player != null) {
            return player;
        }

        final UUID uuid;
        try {
            uuid = UUID.fromString(arg);
        } catch (final IllegalArgumentException ignored) {
            return null;
        }

        return Bukkit.getPlayer(uuid);
    }

    private static Boolean getState(final String arg) {
        switch (arg) {
            case "on", "enable" -> {
                return true;
            }
            case "off", "disable" -> {
                return false;
            }
            default -> {
                return null;
            }
        }
    }
}
