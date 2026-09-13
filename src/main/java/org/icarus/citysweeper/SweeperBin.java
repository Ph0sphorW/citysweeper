package org.icarus.citysweeper;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class SweeperBin implements Listener {

    private static final int NAV_ROW = 9;

    private record Entry(ItemStack item, long storedAt) {
    }

    private final CitySweeper plugin;
    private final List<Entry> storage = new ArrayList<>();

    private String title;
    private int size;
    private long lifetimeMillis;

    public SweeperBin(CitySweeper plugin, SweeperConfig config) {
        this.plugin = plugin;
        applyConfig(config);
    }

    public void open(Player player) {
        purge();
        PageView pageView = new PageView();
        pageView.inventory = Bukkit.createInventory(pageView, size, plugin.text(title));
        render(pageView);
        player.openInventory(pageView.inventory);
    }

    public void store(Iterable<ItemStack> items) {
        long now = System.currentTimeMillis();
        for (ItemStack item : items) {
            if (isEmpty(item)) {
                continue;
            }
            ItemStack rest = item.clone();
            for (Entry entry : storage) {
                if (rest.getAmount() <= 0) {
                    break;
                }
                ItemStack stored = entry.item();
                if (!stored.isSimilar(rest)) {
                    continue;
                }
                int moved = Math.min(stored.getMaxStackSize() - stored.getAmount(), rest.getAmount());
                if (moved <= 0) {
                    continue;
                }
                stored.setAmount(stored.getAmount() + moved);
                rest.setAmount(rest.getAmount() - moved);
            }
            if (rest.getAmount() > 0) {
                storage.add(new Entry(rest, now));
            }
        }
        refreshAll();
    }

    public int purge() {
        if (lifetimeMillis <= 0) {
            return 0;
        }
        long deadline = System.currentTimeMillis() - lifetimeMillis;
        int removed = 0;
        for (int i = 0; i < storage.size(); i++) {
            if (storage.get(i).storedAt() < deadline) {
                removed += storage.remove(i--).item().getAmount();
            }
        }
        return removed;
    }

    public void maintain() {
        if (purge() > 0) {
            refreshAll();
        }
    }

    public void clear() {
        storage.clear();
        refreshAll();
    }

    public int amount() {
        int total = 0;
        for (Entry entry : storage) {
            total += entry.item().getAmount();
        }
        return total;
    }

    public void reconfigure(SweeperConfig config) {
        applyConfig(config);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof PageView) {
                player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
            }
        }
    }

    private void applyConfig(SweeperConfig config) {
        this.title = config.bin().title();
        this.size = config.bin().size();
        this.lifetimeMillis = config.bin().lifetimeMillis();
    }

    private void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof PageView pageView) {
                render(pageView);
            }
        }
    }

    private void render(PageView pageView) {
        int perPage = perPage();
        int pages = pageCount();
        pageView.page = Math.clamp(pageView.page, 0, pages - 1);

        Inventory inventory = pageView.inventory;
        inventory.clear();

        int from = pageView.page * perPage;
        for (int slot = 0; slot < perPage && from + slot < storage.size(); slot++) {
            inventory.setItem(slot, storage.get(from + slot).item().clone());
        }

        inventory.setItem(perPage, button(pageView.page > 0 ? Material.SPECTRAL_ARROW : Material.GRAY_DYE, "&e上一页"));
        inventory.setItem(perPage + 4, button(Material.BOOK,
                "&f第 &e" + (pageView.page + 1) + "&f/&e" + pages + " &f页 &8| &f共 &e" + amount() + " &f件"));
        inventory.setItem(perPage + 8, button(pageView.page < pages - 1 ? Material.ARROW : Material.GRAY_DYE, "&e下一页"));
    }

    private ItemStack button(Material material, String name) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        meta.displayName(plugin.text(name));
        button.setItemMeta(meta);
        return button;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof PageView && event.getRawSlots().stream().anyMatch(slot -> slot < top.getSize())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof PageView pageView) || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() != top) {
            if (event.isShiftClick() || event.getClick() == ClickType.DOUBLE_CLICK) {
                event.setCancelled(true);
                player.sendActionBar(plugin.text(plugin.config().messages().binReadOnly()));
            }
            return;
        }

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot >= perPage()) {
            navigate(player, pageView, slot);
        } else {
            take(player, pageView, slot, event.getClick());
        }
    }

    private void navigate(Player player, PageView pageView, int slot) {
        int target = slot == perPage() ? pageView.page - 1 : slot == perPage() + 8 ? pageView.page + 1 : pageView.page;
        if (target == pageView.page || target < 0 || target >= pageCount()) {
            return;
        }
        pageView.page = target;
        render(pageView);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
    }

    private void take(Player player, PageView pageView, int slot, ClickType click) {
        int index = pageView.page * perPage() + slot;
        if (index >= storage.size()) {
            return;
        }
        ItemStack stored = storage.get(index).item();
        ItemStack wanted = stored.clone();
        wanted.setAmount(click.isRightClick() ? 1 : stored.getAmount());

        int moved = wanted.getAmount();
        for (ItemStack leftover : player.getInventory().addItem(wanted).values()) {
            moved -= leftover.getAmount();
        }
        if (moved < wanted.getAmount()) {
            plugin.send(player, plugin.config().messages().inventoryFull()
                    .replace("%items%", Integer.toString(moved)));
        }
        if (moved <= 0) {
            return;
        }
        if (moved >= stored.getAmount()) {
            storage.remove(index);
        } else {
            stored.setAmount(stored.getAmount() - moved);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.4f, 1.4f);
        refreshAll();
    }

    private int pageCount() {
        return Math.max(1, (storage.size() + perPage() - 1) / perPage());
    }

    private int perPage() {
        return size - NAV_ROW;
    }

    private static final class PageView implements InventoryHolder {

        private int page;
        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }

    private static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getType().isAir() || stack.getAmount() <= 0;
    }
}
