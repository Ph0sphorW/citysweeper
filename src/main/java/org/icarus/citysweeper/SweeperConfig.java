package org.icarus.citysweeper;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurations;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@Configuration
public final class SweeperConfig {

    private static final YamlConfigurationProperties PROPERTIES = YamlConfigurationProperties.newBuilder()
            .header("后巷深宵")
            .setNameFormatter(NameFormatters.LOWER_KEBAB_CASE)
            .charset(StandardCharsets.UTF_8)
            .build();

    private Sweep sweep = new Sweep();
    private Bin bin = new Bin();
    private Messages messages = new Messages();

    public static SweeperConfig loadOrCreate(Path file, Logger logger) {
        SweeperConfig config = YamlConfigurations.update(file, SweeperConfig.class, PROPERTIES);
        config.sweep.validate(logger);
        config.bin.validate(logger);
        return config;
    }

    public Sweep sweep() {
        return sweep;
    }

    public Bin bin() {
        return bin;
    }

    public Messages messages() {
        return messages;
    }

    @Configuration
    public static final class Sweep {

        @Comment("自动回收的时间间隔（秒）。设为 0 或负数即关闭自动回收。")
        private long intervalSeconds = 300;

        @Comment("插件启用时是否立刻回收一次。")
        private boolean onEnable = false;

        @Comment("每次回收后是否向全服播报。")
        private boolean broadcast = true;

        @Comment("一并要清掉的敌对生物。留空则只清掉落物。")
        private List<String> entities = List.of("CREEPER", "SPIDER", "SKELETON", "ZOMBIE");

        private transient Set<EntityType> resolved = Set.of();

        private void validate(Logger logger) {
            if (intervalSeconds < 0) {
                intervalSeconds = 0;
            }
            Set<EntityType> types = EnumSet.noneOf(EntityType.class);
            for (String name : entities) {
                try {
                    types.add(EntityType.valueOf(name.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    logger.warning(name + " isn't a valid entity type, ignored.");
                }
            }
            resolved = types;
        }

        public long intervalTicks() {
            return intervalSeconds * 20L;
        }

        public boolean onEnable() {
            return onEnable;
        }

        public boolean broadcast() {
            return broadcast;
        }

        public int typeCount() {
            return resolved.size();
        }

        public boolean shouldSweep(Entity entity) {
            if (!resolved.contains(entity.getType())) {
                return false;
            }
            if (entity.customName() != null) {
                return false;
            }
            return !(entity instanceof LivingEntity living) || !living.isLeashed();
        }
    }

    @Configuration
    public static final class Bin {

        @Comment("回收站菜单的标题，支持 & 颜色代码。")
        private String title = "&8公共垃圾桶";

        @Comment("每页的格子数，含最后一排导航栏。必须是 9 的倍数。")
        private int size = 54;

        @Comment("单件物品在回收站里最多待多久（秒）。设为 0 表示不过期。")
        private long lifetimeSeconds = 1800;

        private void validate(Logger logger) {
            int clamped = Math.clamp(size / 9 * 9, 18, 54);
            if (clamped != size) {
                logger.warning("bin.size must be a integer between 9 and 54, and can be divided by 9.");
                size = clamped;
            }
            if (lifetimeSeconds < 0) {
                lifetimeSeconds = 0;
            }
        }

        public String title() {
            return title;
        }

        public int size() {
            return size;
        }

        public long lifetimeMillis() {
            return lifetimeSeconds * 1000L;
        }
    }

    @Configuration
    public static final class Messages {

        @Comment("所有消息的前缀，支持 & 颜色代码。")
        private String prefix = "&8[&6CitySweeper&8] &r";

        @Comment("每次回收掉落物后的全服播报。")
        private String sweptItems = "&7已回收 &f%items% &7件掉落物。";

        @Comment("每次清理敌对生物后的全服播报。")
        private String sweptMobs = "&7已清理 &f%mobs% &7个敌对生物。";

        @Comment("玩家打开回收站时的提示。")
        private String binOpened = "&7回收站里现在有 &f%items% &7件物品。";

        @Comment("背包放不下时，只取走一部分的提示。")
        private String inventoryFull = "&c你的背包塞不下了，只能取走 &f%items% &c件。";

        @Comment("玩家试图往回收站里放东西时，显示在动作栏上的提示。")
        private String binReadOnly = "&c回收站只出不进。";

        @Comment("没有权限时的提示。")
        private String noPermission = "&c你没有权限执行这个操作。";

        @Comment("重载配置成功后的提示。")
        private String reloaded = "&a配置已重新加载。";

        @Comment("控制台执行了只有玩家能用的指令时的提示。")
        private String playersOnly = "&c这条指令只能由玩家执行。";

        @Comment("指令用法提示。")
        private String usage = "&7用法：&f/sweeper [open|sweep|reload]";

        public String sweptItems() {
            return prefix + sweptItems;
        }

        public String sweptMobs() {
            return prefix + sweptMobs;
        }

        public String binOpened() {
            return prefix + binOpened;
        }

        public String inventoryFull() {
            return prefix + inventoryFull;
        }

        public String binReadOnly() {
            return binReadOnly;
        }

        public String noPermission() {
            return prefix + noPermission;
        }

        public String reloaded() {
            return prefix + reloaded;
        }

        public String playersOnly() {
            return prefix + playersOnly;
        }

        public String usage() {
            return prefix + usage;
        }
    }
}
