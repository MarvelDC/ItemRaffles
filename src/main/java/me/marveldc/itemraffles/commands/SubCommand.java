package me.marveldc.itemraffles.commands;

import me.marveldc.itemraffles.ItemRaffles;
import me.marveldc.itemraffles.Utility;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

public abstract class SubCommand {
    private final ItemRaffles plugin;
    private final String name;
    private final String usage;
    private final String permission;
    private final ArrayList<UUID> cooldowns = new ArrayList<>();

    private long cooldown;

    public SubCommand(ItemRaffles plugin, String name, String usage, String permission) {
        this.plugin = plugin;
        this.name = name;
        this.usage = usage;
        this.permission = permission;
    }

    public ItemRaffles getPlugin() {
        return this.plugin;
    }

    public String getName() {
        return this.name;
    }

    public String getUsage() {
        return this.usage;
    }

    public String getPermission() {
        return this.permission;
    }

    public void setCooldown(long cooldown) {
        this.cooldown = cooldown;
    }

    public boolean isCooldown(Player player) {
        if (this.cooldowns.contains(player.getUniqueId())) {
            player.sendMessage(Utility.translate(this.plugin.getConfiguration().getString("messages.command cooldown")));
            return false;
        }

        this.cooldowns.add(player.getUniqueId());
        this.plugin.getServer().getScheduler().runTaskLaterAsynchronously(this.getPlugin(), () -> this.cooldowns.remove(player.getUniqueId()), this.cooldown);

        return true;
    }

    protected abstract void run(CommandSender commandSender, String[] args);
}
