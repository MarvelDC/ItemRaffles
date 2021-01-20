package me.marveldc.itemraffles.commands;

import me.marveldc.itemraffles.ItemRaffles;
import me.marveldc.itemraffles.Utility;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReloadCommand extends SubCommand {
    public ReloadCommand(ItemRaffles plugin) {
        super(plugin, "reload", null, "raffle.reload");
    }

    @Override
    protected void run(CommandSender commandSender, String[] args) {
        if (commandSender instanceof Player) {
            final Player player = (Player) commandSender;

            if (!player.hasPermission(this.getPermission())) {
                commandSender.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("no permission")));
                return;
            }
        }

        this.getPlugin().reloadConfig();
        this.getPlugin().setConfiguration(this.getPlugin().getConfig());

        commandSender.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("reloaded config")));
    }
}
