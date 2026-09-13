package org.icarus.citysweeper;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class SweepTask implements Runnable {

    private final CitySweeper plugin;

    public SweepTask(CitySweeper plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        SweeperConfig config = plugin.config();

        List<ItemStack> collected = new ArrayList<>();
        int stacks = 0;
        int items = 0;
        int mobs = 0;

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item drop) {
                    ItemStack stack = drop.getItemStack();
                    drop.remove();
                    if (stack.getType().isAir() || stack.getAmount() <= 0) {
                        continue;
                    }
                    stacks++;
                    items += stack.getAmount();
                    collected.add(stack);
                } else if (config.sweep().shouldSweep(entity)) {
                    entity.remove();
                    mobs++;
                }
            }
        }

        if (items == 0 && mobs == 0) {
            return;
        }
        if (!collected.isEmpty()) {
            plugin.bin().store(collected);
        }
        if (!config.sweep().broadcast()) {
            return;
        }
        if (items > 0) {
            plugin.broadcast(config.messages().sweptItems()
                    .replace("%items%", Integer.toString(items))
                    .replace("%stacks%", Integer.toString(stacks)));
        }
        if (mobs > 0) {
            plugin.broadcast(config.messages().sweptMobs().replace("%mobs%", Integer.toString(mobs)));
        }
    }
}
