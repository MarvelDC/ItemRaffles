package me.marveldc.itemraffles.commands;

import me.marveldc.itemraffles.Database;
import me.marveldc.itemraffles.ItemRaffles;
import me.marveldc.itemraffles.Utility;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

public class EnterCommand extends SubCommand {
    public EnterCommand(ItemRaffles plugin) {
        super(plugin, "enter", "[raffle creator UUID]", "itemraffles.enter");
    }

    @Override
    protected void run(CommandSender commandSender, String[] args) {
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("messages.no console")));
            return;
        }

        if (args.length != 1) {
            commandSender.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("messages.bad usage"), this.getName(), this.getUsage()));
            return;
        }

        Player player = (Player) commandSender;
        if (this.isCooldown(player)) {
            return;
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(args[0]);
        } catch (IllegalArgumentException error) {
            commandSender.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("messages.bad uuid")));
            return;
        }

        if (uuid.equals(player.getUniqueId())) {
            commandSender.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("messages.no self join")));
            return;
        }

        Database database = null;
        try {
            database = new Database(this.getPlugin().getConnection());

            ResultSet itemRaffle = database.prepare("SELECT id FROM itemRaffles WHERE claimed = 0 AND player = ? AND ends > ?", uuid.toString(), Instant.now().toEpochMilli()).get();
            if (itemRaffle == null) {
                commandSender.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("messages.no raffle")));
                return;
            }

            try {
                database.prepare("INSERT INTO itemRafflesEntries (itemRafflesId, player) VALUES (?, ?)", itemRaffle.getInt("id"), uuid.toString()).run();
                commandSender.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("messages.entered")));
            } catch (SQLException ignored) {
                commandSender.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("messages.already entered")));
            } finally {
                itemRaffle.close();
            }
        } catch (SQLException error) {
            this.getPlugin().getLogger().severe("Failed to enter raffle.");
            error.printStackTrace();

            commandSender.sendMessage(Utility.translate(this.getPlugin().getConfiguration().getString("messages.error")));
        } finally {
            if (database != null) database.close();
        }
    }
}
