package pw.kaboom.commandspy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
	public void onEnable() {
		this.getCommand("commandspy").setExecutor(new CommandCommandSpy(this));
		this.getServer().getPluginManager().registerEvents(this, this);
	}

	@EventHandler
	void onCommand(PlayerCommandPreprocessEvent event) {
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
	void onSignChange(SignChangeEvent event) {
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

class CommandCommandSpy implements CommandExecutor {
	Main main;
	CommandCommandSpy(Main main) {
		this.main = main;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		final Player player = (Player) sender;

		if (main.getConfig().contains(player.getUniqueId().toString())) {
			main.getConfig().set(player.getUniqueId().toString(), null);
			main.saveConfig();
			player.sendMessage("Successfully disabled CommandSpy");
		} else {
			main.getConfig().set(player.getUniqueId().toString(), true);
			main.saveConfig();
			player.sendMessage("Successfully enabled CommandSpy");
		}
		return true;
	}
}
