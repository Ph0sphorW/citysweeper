import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml
import xyz.jpenilla.resourcefactory.bukkit.Permission

plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.2.2"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.3.1"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("de.exlll:configlib-yaml:4.8.1")
}

bukkitPluginYaml {
    main = "org.icarus.citysweeper.CitySweeper"
    apiVersion = "1.21.11"

    load = BukkitPluginYaml.PluginLoadOrder.STARTUP
    authors.addAll("Ph0sphor")

    // 运行时由 Paper 从 Maven Central 拉取，不需要 shade
    libraries.addAll("de.exlll:configlib-yaml:4.8.1")

    commands {
        register("sweeper") {
            description = "打开公共回收站，或手动清理地面掉落物与敌对生物"
            aliases.addAll("cs", "recycle", "bin")
            usage = "/sweeper [open|sweep|reload]"
        }
    }

    permissions {
        register("citysweeper.admin") {
            description = "CitySweeper 的全部管理权限"
            default = Permission.Default.OP
            children("citysweeper.sweep", "citysweeper.reload")
        }
        register("citysweeper.open") {
            description = "允许打开公共回收站"
            default = Permission.Default.TRUE
        }
        register("citysweeper.sweep") {
            description = "允许手动执行一次回收"
            default = Permission.Default.OP
        }
        register("citysweeper.reload") {
            description = "允许重载 CitySweeper 配置"
            default = Permission.Default.OP
        }
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

// 源码里有中文注释和默认提示文本，必须显式按 UTF-8 编译
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21.11")
        jvmArgs("-Xms2G", "-Xmx2G", "-Dcom.mojang.eula.agree=true")
    }
}
