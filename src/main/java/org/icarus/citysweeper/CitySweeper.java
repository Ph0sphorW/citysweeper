package org.icarus.citysweeper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.nio.file.Path;
import java.util.logging.Logger;

public final class CitySweeper extends JavaPlugin {

    private static final long MAINTENANCE_PERIOD_TICKS = 100L;

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private SweeperConfig config;
    private SweeperBin bin;
    private BukkitTask sweepTask;
    private Logger logger;
    private Path configFile;

    @Override
    public void onEnable() {
        this.logger = getLogger();
        this.configFile = dataFile();
        this.config = SweeperConfig.loadOrCreate(configFile, logger);
        this.bin = new SweeperBin(this, config);

        logger.info("CitySweeper - District N Sweepers");
        logger.info("Made by Ph0sphorW");

        registerCommand();
        Bukkit.getPluginManager().registerEvents(bin, this);

        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskTimer(this,
                bin::maintain,
                MAINTENANCE_PERIOD_TICKS,
                MAINTENANCE_PERIOD_TICKS);
        scheduleSweep();

        if (config.sweep().onEnable()) {
            scheduler.runTask(this, new SweepTask(this));
        }
    }

    @Override
    public void onDisable() {
        if (sweepTask != null) {
            sweepTask.cancel();
            sweepTask = null;
        }

        if (bin != null) {
            bin.clear();
        }
    }

    public void reload() {
        this.config = SweeperConfig.loadOrCreate(configFile, logger);
        bin.reconfigure(config);
        scheduleSweep();
    }

    public void sweepNow() {
        new SweepTask(this).run();
    }

    public SweeperConfig config() {
        return config;
    }

    public SweeperBin bin() {
        return bin;
    }

    public Component text(String legacy) {
        return LEGACY.deserialize(legacy);
    }

    public void send(CommandSender sender, String legacy) {
        sender.sendMessage(text(legacy));
    }

    public void broadcast(String legacy) {
        Component message = text(legacy);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
        // getServer().getConsoleSender().sendMessage(message);
    }

    private void scheduleSweep() {
        if (sweepTask != null) {
            sweepTask.cancel();
            sweepTask = null;
        }
        long interval = config.sweep().intervalTicks();
        if (interval <= 0) {
            logger.info("Disabled auto sweeping for sweep.interval-seconds is zero.");
            return;
        }
        sweepTask = Bukkit.getScheduler().runTaskTimer(this,
                this::scheduledSweepOrSkip,
                interval,
                interval);
        int types = config.sweep().typeCount();
        logger.info("For every " + (interval / 20) + " seconds, sweeps the items"
                + (types == 0 ? "." : ", and removes " + types + " type(s) of mobs."));
    }

    private void scheduledSweepOrSkip() {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }
        new SweepTask(this).run();
    }

    private Path dataFile() {
        File folder = getDataFolder();
        if (!folder.isDirectory() && !folder.mkdirs()) {
            logger.warning("Couldn't create data folder " + folder);
        }
        return folder.toPath().resolve("config.yml");
    }

    private void registerCommand() {
        PluginCommand command = getCommand("sweeper");
        if (command == null) {
            logger.warning("Couldn't find command /sweeper");
            return;
        }
        SweeperCommand executor = new SweeperCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}
