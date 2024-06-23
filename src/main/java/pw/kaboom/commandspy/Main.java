package pw.kaboom.commandspy;

import java.io.File;

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

        // Save the config every 30 seconds
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this.config::trySave, 600L, 600L);
    }

    @Override
    public void onDisable() {
        this.config.trySave();
    }

    private void enableCommandSpy(final Player player) {
        this.config.setCommandSpyState(player.getUniqueId(), true);
        player.sendMessage(Component.text("Successfully enabled CommandSpy"));
    }

    private void disableCommandSpy(final Player player) {
        this.config.setCommandSpyState(player.getUniqueId(), false);
        player.sendMessage(Component.text("Successfully disabled CommandSpy"));
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
        if (!(sender instanceof final Player player)) {
            sender.sendMessage(Component.text("Command has to be run by a player"));
            return true;
        }

        if (args.length >= 1 && "on".equalsIgnoreCase(args[0])) {
            enableCommandSpy(player);
            return true;
        }
        if ((args.length >= 1 && "off".equalsIgnoreCase(args[0]))
                || this.config.getCommandSpyState(player.getUniqueId())) {
            disableCommandSpy(player);
            return true;
        }
        enableCommandSpy(player);
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
}
