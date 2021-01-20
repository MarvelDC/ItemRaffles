package me.marveldc.itemraffles;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.marveldc.itemraffles.commands.CommandHandler;
import me.marveldc.itemraffles.timers.ClaimTimer;
import me.marveldc.itemraffles.timers.MessageTimer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Random;

public class ItemRaffles extends JavaPlugin {
    private HikariDataSource hikariDataSource;
    private FileConfiguration configuration;

    private BukkitTask mainTimer;
    private BukkitTask messagesTimer;

    public Connection getConnection() throws SQLException {
        return this.hikariDataSource.getConnection();
    }

    public FileConfiguration getConfiguration() {
        return this.configuration;
    }

    public void setConfiguration(FileConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void onDisable() {
        super.onDisable();

        try {
            mainTimer.cancel();
            messagesTimer.cancel();

            if (this.hikariDataSource != null) {
                this.hikariDataSource.close();
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();

        this.makeConfig();

        if (!this.makeConnection()) {
            return;
        }

        this.makeTables();

        if (!this.getServer().getServerName().equals(this.getConfiguration().getString("servers.main"))) {
            return;
        }

        new CommandHandler(this);

        final Random random = new Random();
        this.mainTimer = this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> ClaimTimer.timer(this, random), 10000, 3000);
        this.messagesTimer = this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> MessageTimer.timer(this), 10000, 5000);
    }

    private void makeConfig() {
        this.saveDefaultConfig();
        this.saveConfig();
        this.configuration = this.getConfig();
    }

    private boolean makeConnection() {
        final String databaseProperties = "database.properties";

        if (!new File(this.getDataFolder(), databaseProperties).exists()) {
            this.getLogger().warning("Failed to find " + databaseProperties + ", making a new one...");

            this.saveResource(databaseProperties, true);

            this.getLogger().severe("Reload this plugin to use the new " + databaseProperties + ".");

            this.getPluginLoader().disablePlugin(this);
            return false;
        }

        try {
            HikariConfig hikariConfig = new HikariConfig(this.getDataFolder() + File.separator + databaseProperties);

            this.hikariDataSource = new HikariDataSource(hikariConfig);
        } catch (Exception error) {
            this.getLogger().severe("Failed to load " + databaseProperties + ". Most likely due to incorrect credentials.");
            error.printStackTrace();

            this.getPluginLoader().disablePlugin(this);
            return false;
        }

        return true;
    }

    private void makeTables() {
        Database database = null;

        try {
            database = new Database(this.getConnection());

            database.prepare("CREATE TABLE IF NOT EXISTS itemRaffles (" +
                    "id int(11) NOT NULL AUTO_INCREMENT," +
                    "creator varchar(36) NOT NULL," +
                    "ends bigint(20) unsigned NOT NULL," +
                    "item text NOT NULL," +
                    "claimed TINYINT(2) unsigned DEFAULT 0" +
                    "PRIMARY KEY (id)," +
                    "UNIQUE KEY id_UNIQUE (id))").run();

            database.prepare("CREATE TABLE IF NOT EXISTS itemRafflesEntries (" +
                    "id int(11) NOT NULL AUTO_INCREMENT," +
                    "itemRafflesId int(11) NOT NULL," +
                    "player varchar(36) NOT NULL," +
                    "PRIMARY KEY (id)," +
                    "UNIQUE KEY id_UNIQUE (id)," +
                    "KEY fk_itemRafflesEntries_1_idx (itemRafflesId)," +
                    "CONSTRAINT fk_itemRafflesEntries_1 FOREIGN KEY (itemRaffleId)" +
                    " REFERENCES itemRaffles (id)" +
                    " ON DELETE NO ACTION" +
                    " ON UPDATE NO ACTION)").run();
        } catch (SQLException error) {
            this.getLogger().severe("Failed to run query.");
            error.printStackTrace();
        } finally {
            if (database != null) database.close();
        }
    }
}
