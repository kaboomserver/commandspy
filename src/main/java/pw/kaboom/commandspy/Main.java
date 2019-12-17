package pw.kaboom.commandspy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin implements CommandExecutor, Listener {
	@Override
	public void onEnable() {
		this.getCommand("commandspy").setExecutor(this);
		this.getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
		if (sender instanceof ConsoleCommandSender) {
			sender.sendMessage("Command has to be run by a player");
		} else {
			final Player player = (Player) sender;
			final JavaPlugin plugin = JavaPlugin.getPlugin(Main.class);

			if (plugin.getConfig().contains(player.getUniqueId().toString())) {
				plugin.getConfig().set(player.getUniqueId().toString(), null);
				plugin.saveConfig();
				player.sendMessage("Successfully disabled CommandSpy");
			} else {
				plugin.getConfig().set(player.getUniqueId().toString(), true);
				plugin.saveConfig();
				player.sendMessage("Successfully enabled CommandSpy");
			}
		}
		return true;
	}

	@EventHandler
	void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent event) {
		final Player commandRunner = event.getPlayer();

		for (Player messageTarget: Bukkit.getOnlinePlayers()) {
			if (getConfig().contains(messageTarget.getUniqueId().toString())) {
				final ChatColor color;

				if (getConfig().contains(commandRunner.getUniqueId().toString())) {
					color = ChatColor.GREEN;
				} else {
					color = ChatColor.RED;
				}

				messageTarget.sendMessage(color + "" + commandRunner.getName() + "" + color + ": " + event.getMessage());
			}
		}
	}

	@EventHandler
	void onSignChange(final SignChangeEvent event) {
		final Player signPlacer = event.getPlayer();

		for (Player messageTarget: Bukkit.getOnlinePlayers()) {
			if (getConfig().contains(messageTarget.getUniqueId().toString())) {
				final ChatColor color;

				if (getConfig().contains(signPlacer.getUniqueId().toString())) {
					color = ChatColor.GREEN;
				} else {
					color = ChatColor.RED;
				}

				messageTarget.sendMessage(color + "" + signPlacer.getName() + "" + color + " created a sign with contents:");
				for (String line: event.getLines()) {
					messageTarget.sendMessage(color + "  " + line);
				}
			}
		}
	}
}
