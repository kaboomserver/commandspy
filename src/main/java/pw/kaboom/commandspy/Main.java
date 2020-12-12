package pw.kaboom.commandspy;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

public final class Main extends JavaPlugin implements CommandExecutor, Listener {
	private FileConfiguration config;

	@Override
	public void onEnable() {
		config = getConfig();
		this.getCommand("commandspy").setExecutor(this);
		this.getServer().getPluginManager().registerEvents(this, this);
	}

	private void enableCommandSpy(final Player player, final Plugin plugin) {
		plugin.getConfig().set(player.getUniqueId().toString(), true);
		plugin.saveConfig();
		player.sendMessage("Successfully enabled CommandSpy");
	}

	private void disableCommandSpy(final Player player, final Plugin plugin) {
		plugin.getConfig().set(player.getUniqueId().toString(), null);
		plugin.saveConfig();
		player.sendMessage("Successfully disabled CommandSpy");
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
		if (sender instanceof ConsoleCommandSender) {
			sender.sendMessage("Command has to be run by a player");
			return true;
		}

		final Player player = (Player) sender;
		final JavaPlugin plugin = JavaPlugin.getPlugin(Main.class);

		if (args.length == 0) {
			if (config.contains(player.getUniqueId().toString())) {
				disableCommandSpy(player, plugin);
			} else {
				enableCommandSpy(player, plugin);
			}
		} else if ("on".equalsIgnoreCase(args[0])) {
			enableCommandSpy(player, plugin);
		} else if ("off".equalsIgnoreCase(args[0])) {
			disableCommandSpy(player, plugin);
		}
		config = plugin.getConfig();
		return true;
	}

	@EventHandler
	void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event) {
		if (event.isCancelled()) {
			return;
		}

		for (String uuidString : config.getKeys(false)) {
			UUID uuid = UUID.fromString(uuidString);

			if (Bukkit.getPlayer(uuid) != null) {
				Bukkit.getPlayer(uuid).sendMessage(
						ChatColor.AQUA + ""
								+ event.getPlayer().getName() + ""
								+ ChatColor.AQUA + ": "
								+ event.getMessage()
								);
			}
		}
	}

	@EventHandler
	void onSignChange(final SignChangeEvent event) {
		for (String uuidString : config.getKeys(false)) {
			UUID uuid = UUID.fromString(uuidString);

			if (Bukkit.getPlayer(uuid) != null) {
				Bukkit.getPlayer(uuid).sendMessage(
						ChatColor.AQUA + ""
								+ event.getPlayer().getName() + ""
								+ ChatColor.AQUA
								+ " created a sign with contents:"
								);
				for (String line: event.getLines()) {
					Bukkit.getPlayer(uuid).sendMessage(ChatColor.AQUA + "  " + line);
				}
			}
		}
	}
}
