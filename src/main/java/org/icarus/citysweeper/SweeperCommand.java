package org.icarus.citysweeper;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("SameReturnValue")
public final class SweeperCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("open", "sweep", "reload");

    private final CitySweeper plugin;

    public SweeperCommand(CitySweeper plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             String[] args) {
        if (args.length == 0) {
            return open(sender);
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "open" -> {
                return open(sender);
            }
            case "sweep" -> {
                return sweep(sender);
            }
            case "reload" -> {
                return reload(sender);
            }
            default -> {
                plugin.send(sender, plugin.config().messages().usage());
                return true;
            }
        }
    }

    private boolean open(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.send(sender, plugin.config().messages().playersOnly());
            return true;
        }
        if (!player.hasPermission("citysweeper.open")) {
            plugin.send(player, plugin.config().messages().noPermission());
            return true;
        }
        plugin.bin().open(player);
        plugin.send(player, plugin.config().messages().binOpened()
                .replace("%items%", Integer.toString(plugin.bin().amount())));
        return true;
    }

    private boolean sweep(CommandSender sender) {
        if (!sender.hasPermission("citysweeper.sweep")) {
            plugin.send(sender, plugin.config().messages().noPermission());
            return true;
        }
        plugin.sweepNow();
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("citysweeper.reload")) {
            plugin.send(sender, plugin.config().messages().noPermission());
            return true;
        }
        plugin.reload();
        plugin.send(sender, plugin.config().messages().reloaded());
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String sub : SUBCOMMANDS) {
            if (sub.startsWith(prefix) && sender.hasPermission("citysweeper." + sub)) {
                matches.add(sub);
            }
        }
        return matches;
    }
}
