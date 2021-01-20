package me.marveldc.itemraffles.timers;

import me.marveldc.itemraffles.Database;
import me.marveldc.itemraffles.ItemRaffles;
import me.marveldc.itemraffles.Utility;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

public class MessageTimer {
    public static void timer(ItemRaffles plugin) {
        Database database = null;

        try {
            database = new Database(plugin.getConnection());

            final ResultSet itemRaffles = database.prepare("SELECT creator, ends FROM itemRaffles WHERE claimed = 0 AND ends > ? LIMIT 1", Instant.now().toEpochMilli()).get();
            if (itemRaffles == null) {
                return;
            }

            final long difference = itemRaffles.getLong("ends") - Instant.now().toEpochMilli();
            if (difference < 0 || difference > 5 * 60 * 1000) {
                return;
            }

            if (!(difference > (5 * 60 * 1000) - 5000 && difference < 5 * 60 * 1000 ||
                    difference > (4 * 60 * 1000) - 5000 && difference < 4 * 60 * 1000 ||
                    difference > (3 * 60 * 1000) - 5000 && difference < 3 * 60 * 1000 ||
                    difference > (2 * 60 * 1000) - 5000 && difference < 2 * 60 * 1000 ||
                    difference > (60 * 1000) - 5000 && difference < 60 * 1000 ||
                    difference > (30 * 1000) - 5000 && difference < 30 * 1000 ||
                    difference > (10 * 1000) - 5000 && difference < 10 * 1000 ||
                    difference > (5 * 1000) - 5000 && difference < 5 * 1000)) {
                return;
            }

            final Player player = plugin.getServer().getPlayer(UUID.fromString(itemRaffles.getString("creator")));
            final String countMessage = String.join("\n", plugin.getConfiguration().getStringList("messages.countdown"));

            TextComponent textComponent = new TextComponent(Utility.translate(countMessage, player.getName(), Utility.getCountdown(itemRaffles.getLong("ends"))));
            textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/raffle enter " + player.getUniqueId().toString()));

//            send to proxy server with conditions, this just broadcasts this server
            plugin.getServer().spigot().broadcast(textComponent);
        } catch (SQLException error) {
            plugin.getLogger().severe("Failed in messages timer.");
            error.printStackTrace();
        } finally {
            if (database != null) database.close();
        }
    }
}
