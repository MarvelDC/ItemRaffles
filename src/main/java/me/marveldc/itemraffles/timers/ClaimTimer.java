package me.marveldc.itemraffles.timers;

import me.marveldc.itemraffles.Database;
import me.marveldc.itemraffles.ItemRaffles;
import me.marveldc.itemraffles.ItemStore;
import me.marveldc.itemraffles.Utility;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

public class ClaimTimer {
    public static void timer(ItemRaffles plugin, Random random) {
        Database database = null;

        try {
            database = new Database(plugin.getConnection());

            final ResultSet itemRaffles = database.prepare("SELECT id,item,creator,ends FROM itemRaffles WHERE claimed = 0 AND ends < ?", Instant.now().toEpochMilli()).get();
            if (itemRaffles == null) {
                return;
            }

            itemRaffles.previous();
            while (itemRaffles.next()) {
                database.prepare("UPDATE itemRaffles SET claimed = 1 WHERE id = ?", itemRaffles.getInt("id")).run();

                final Player creator = plugin.getServer().getPlayer(UUID.fromString(itemRaffles.getString("creator")));

                ItemStack item;
                try {
                    item = ItemStore.itemStackFromBase64(itemRaffles.getString("item"));
                } catch (IOException error) {
                    error.printStackTrace();
                    plugin.getLogger().warning("Failed to convert base64 item for player: " + creator.toString() + " : " + itemRaffles.getString("item"));
                    continue;
                }

                ResultSet entries = database.prepare("SELECT player FROM itemRafflesEntries WHERE itemRafflesId = ?", itemRaffles.getInt("id")).get();
                if (entries == null) {
                    if (creator == null || !creator.isOnline()) {
                        continue;
                    }

                    creator.sendMessage(Utility.translate(plugin.getConfiguration().getString("messages.refunded"), creator.getName()));

                    final int firstEmpty = creator.getInventory().firstEmpty();
                    if (firstEmpty != -1) {
                        creator.getInventory().addItem(item);
                    } else {
                        creator.getWorld().dropItem(creator.getLocation(), item);
                    }

                    return;
                }

                final ArrayList<Player> players = new ArrayList<>();
                while (entries.next()) {
                    final Player player = plugin.getServer().getPlayer(UUID.fromString(entries.getString("player")));
                    if (player == null || !player.isOnline()) {
                        continue;
                    }

                    players.add(player);
                }

                entries.close();

                final Player player = players.get(random.nextInt(players.size()));

                player.sendMessage(Utility.translate(plugin.getConfiguration().getString("messages.you won"), player.getName()));

                final int firstEmpty = player.getInventory().firstEmpty();
                if (firstEmpty != -1) {
                    player.getInventory().addItem(item);
                } else {
                    player.getWorld().dropItem(player.getLocation(), item);
                }
            }

            itemRaffles.close();
        } catch (SQLException error) {
            plugin.getLogger().severe("Failed in main timer.");
            error.printStackTrace();
        } finally {
            if (database != null) database.close();
        }
    }
}
