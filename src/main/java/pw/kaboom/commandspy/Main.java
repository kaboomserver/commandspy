package pw.kaboom.commandspy;

import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Main extends JavaPlugin implements CommandExecutor, Listener {
    private FileConfiguration config;

    @Override
    public void onEnable() {
        config = getConfig();
        this.getCommand("commandspy").setExecutor(this);
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    private void enableCommandSpy(final Player player) {
        config.set(player.getUniqueId().toString(), true);
        saveConfig();
        player.sendMessage(Component.text("Successfully enabled CommandSpy"));
    }

    private void disableCommandSpy(final Player player) {
        config.set(player.getUniqueId().toString(), null);
        saveConfig();
        player.sendMessage(Component.text("Successfully disabled CommandSpy"));
    }

    private NamedTextColor getTextColor(final Player player) {
        if (config.contains(player.getUniqueId().toString())) {
            return NamedTextColor.YELLOW;
        }
        return NamedTextColor.AQUA;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label,
                             final String[] args) {
        if (sender instanceof ConsoleCommandSender) {
            sender.sendMessage(Component.text("Command has to be run by a player"));
            return true;
        }

        final Player player = (Player) sender;

        if (args.length >= 1 && "on".equalsIgnoreCase(args[0])) {
            enableCommandSpy(player);
            return true;
        }
        if ((args.length >= 1 && "off".equalsIgnoreCase(args[0]))
                || config.contains(player.getUniqueId().toString())) {
            disableCommandSpy(player);
            return true;
        }
        enableCommandSpy(player);
        return true;
    }

    @EventHandler
    void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Player player = event.getPlayer();
        final NamedTextColor color = getTextColor(player);
        final Component message = Component.text(player.getName(), color)
            .append(Component.text(": "))
            .append(Component.text(event.getMessage()));

        for (String uuidString : config.getKeys(false)) {
            final UUID uuid = UUID.fromString(uuidString);
            final Player recipient = Bukkit.getPlayer(uuid);

            if (recipient == null) {
                continue;
            }
            recipient.sendMessage(message);
        }
    }

    @EventHandler
    void onSignChange(final SignChangeEvent event) {
        final Player player = event.getPlayer();
        final NamedTextColor color = getTextColor(player);
        Component message = Component.text(player.getName(), color)
            .append(Component.text(" created a sign with contents:"));

        for (Component line : event.lines()) {
            message = message
                .append(Component.text("\n "))
                .append(line);
        }

        for (String uuidString : config.getKeys(false)) {
            final UUID uuid = UUID.fromString(uuidString);
            final Player recipient = Bukkit.getPlayer(uuid);

            if (recipient == null) {
                continue;
            }
            recipient.sendMessage(message);
        }
    }
}
