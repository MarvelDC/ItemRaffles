package me.marveldc.itemraffles.commands;

import me.marveldc.itemraffles.Database;
import me.marveldc.itemraffles.ItemRaffles;
import me.marveldc.itemraffles.ItemStore;
import me.marveldc.itemraffles.Utility;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

public class CreateCommand extends SubCommand {
    public CreateCommand(ItemRaffles plugin) {
        super(plugin, "create", "[duration]", "itemraffles.create");

        final long userCooldown = Utility.parseDuration(this.getPlugin().getConfiguration().getString("cooldowns.user"));
        this.setCooldown(userCooldown == -1 ? 5000 : userCooldown);
    }

    @Override
    protected void run(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("messages.no console")));
            return;
        }

        if (this.getPermission() != null && !commandSender.hasPermission(this.getPermission())) {
            commandSender.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("messages.no permission")));
            return;
        }

        long duration = -1;
        if (args.length > 0) {
            duration = Utility.parseDuration(args[0]);
            if (duration == -1) {
                commandSender.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("messages.invalid duration")));
                return;
            }

            if (!commandSender.hasPermission("itemraffles.ignorelengths")) {
                final String minimumConfig = this.getPlugin().getConfiguration().getString("lengths.min");
                final long minimum = Utility.parseDuration(minimumConfig);
                if (duration < minimum) {
                    commandSender.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("messages.length minimum"), minimumConfig));
                    return;
                }

                final String maximumConfig = this.getPlugin().getConfiguration().getString("lengths.max");
                final long maximum = Utility.parseDuration(maximumConfig);
                if (duration > maximum) {
                    commandSender.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("messages.length maximum"), maximumConfig));
                    return;
                }
            }
        }

        if (duration == -1) {
            duration = Utility.parseDuration(this.getPlugin().getConfiguration().getString("lengths.default"));
        }

        final Player player = (Player) commandSender;
        final ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null) {
            commandSender.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("messages.no hand item")));
            return;
        }

        if (this.isCooldown(player)) {
            return;
        }

        String base64 = null;
        try {
            base64 = ItemStore.itemStackToBase64(item);
        } catch (IllegalStateException error) {
            error.printStackTrace();
        }

        if (base64 == null || base64.isEmpty()) {
            commandSender.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("messages.item error")));
            return;
        }

        final boolean created = this.createRaffle(player, item, duration, base64);
        if (!created) {
            return;
        }

        commandSender.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("messages.created")));
    }

    private boolean createRaffle(Player player, ItemStack item, long duration, String base64) {
        final long now = Instant.now().toEpochMilli();
        final int simultaneousRaffles = this.getPlugin().getConfiguration().getInt("simultaneous raffles");

        Database database = null;
        try {
            database = new Database(this.getPlugin().getConnection());

            if (!player.hasPermission("itemraffles.ignorecooldowns")) {
                final long globalCooldown = Utility.parseDuration(this.getPlugin().getConfiguration().getString("cooldowns.global"));
                final ResultSet itemRafflesGlobal = database.prepare("SELECT count(*) AS size FROM itemRaffles WHERE ends > ? AND ? - ends <= ?", now, now, globalCooldown).get();
                if (itemRafflesGlobal != null && itemRafflesGlobal.getInt("size") > 0) {
                    player.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("messages.global cooldown")));
                    itemRafflesGlobal.close();
                    return false;
                }

                final long userCooldown = Utility.parseDuration(this.getPlugin().getConfiguration().getString("cooldowns.user"));
                final ResultSet itemRafflesUser = database.prepare("SELECT count(*) AS size FROM itemRaffles WHERE creator = ? AND ends > ? AND ? - ends <= ?", player.getUniqueId().toString(), now, now, userCooldown).get();
                if (itemRafflesUser != null && itemRafflesUser.getInt("size") > 0) {
                    player.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("messages.user cooldown")));
                    itemRafflesUser.close();
                    return false;
                }
            }

            final ResultSet itemRaffles = database.prepare("SELECT count(*) AS size FROM itemRaffles WHERE claimed = 0 AND ends > ?", now).get();
            if (itemRaffles != null && itemRaffles.getInt("size") >= simultaneousRaffles) {
                player.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("messages.too many raffles", itemRaffles.getString("size"))));
                itemRaffles.close();
                return false;
            }

            if (!player.getInventory().getItemInMainHand().equals(item)) {
                player.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("messages.no hand item")));
                return false;
            }

            player.getInventory().remove(item);
            return database.prepare("INSERT INTO itemRaffles (creator, ends, item) VALUES (?, ?, ?)", player.getUniqueId().toString(), now + duration, base64).run();
        } catch (SQLException error) {
            this.getPlugin().getLogger().severe("Failed to run a query.");
            error.printStackTrace();

            player.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("messages.error")));
            return false;
        } finally {
            if (database != null) database.close();
        }
    }
}
