package com.github.shynixn.shybossbar

import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.mcCoroutineConfiguration
import com.github.shynixn.mcutils.common.ChatColor
import com.github.shynixn.mcutils.common.Version
import com.github.shynixn.mcutils.common.checkIfFoliaIsLoadable
import com.github.shynixn.mcutils.common.di.DependencyInjectionModule
import com.github.shynixn.mcutils.common.language.reloadTranslation
import com.github.shynixn.mcutils.common.placeholder.PlaceHolderService
import com.github.shynixn.mcutils.common.placeholder.PlaceHolderServiceImpl
import com.github.shynixn.mcutils.worldguard.WorldGuardService
import com.github.shynixn.mcutils.worldguard.WorldGuardServiceImpl
import com.github.shynixn.shybossbar.contract.BossBarService
import com.github.shynixn.shybossbar.entity.ShyBossBarSettings
import com.github.shynixn.shybossbar.enumeration.PlaceHolder
import com.github.shynixn.shybossbar.impl.commandexecutor.ShyBossBarCommandExecutor
import com.github.shynixn.shybossbar.impl.listener.ShyBossBarListener
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

class ShyBossBarPlugin : JavaPlugin() {
    private val prefix: String = ChatColor.BLUE.toString() + "[ShyBossBar] " + ChatColor.WHITE
    private var module: DependencyInjectionModule? = null
    private var worldGuardService: WorldGuardService? = null

    companion object {
        private val areLegacyVersionsIncluded: Boolean by lazy {
            try {
                Class.forName("com.github.shynixn.shybossbar.lib.com.github.shynixn.mcutils.packet.nms.v1_8_R3.PacketSendServiceImpl")
                true
            } catch (e: ClassNotFoundException) {
                false
            }
        }
    }

    override fun onEnable() {
        Bukkit.getServer().consoleSender.sendMessage(prefix + ChatColor.GREEN + "Loading ShyBossBar ...")
        this.saveDefaultConfig()
        this.reloadConfig()
        
        // Log server version information
        logger.log(Level.INFO, "Server is running version ${Version.serverVersion}")

        if (mcCoroutineConfiguration.isFoliaLoaded && !checkIfFoliaIsLoadable()) {
            logger.log(Level.SEVERE, "================================================")
            logger.log(Level.SEVERE, "ShyBossBar for Folia requires ShyBossBar-Premium-Folia.jar")
            logger.log(Level.SEVERE, "Go to https://www.patreon.com/Shynixn to download it.")
            logger.log(Level.SEVERE, "Plugin gets now disabled!")
            logger.log(Level.SEVERE, "================================================")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }

        // Register Language
        val language = ShyBossBarLanguageImpl()
        reloadTranslation(language)
        logger.log(Level.INFO, "Loaded language file.")

        // Module
        val plugin = this
        val settings = ShyBossBarSettings { settings ->
            settings.joinDelaySeconds = plugin.config.getInt("global.joinDelaySeconds")
            settings.checkForChangeChangeSeconds = plugin.config.getInt("global.checkForPermissionChangeSeconds")
        }
        settings.reload()
        val placeHolderService = PlaceHolderServiceImpl(this)
        this.module = ShyBossBarDependencyInjectionModule(this, settings, language, worldGuardService!!, placeHolderService).build()

        // Register PlaceHolders
        PlaceHolder.registerAll(
            this,
            this.module!!.getService<PlaceHolderService>(),
        )

        // Register Listeners
        Bukkit.getPluginManager().registerEvents(module!!.getService<ShyBossBarListener>(), this)

        // Register CommandExecutor
        module!!.getService<ShyBossBarCommandExecutor>()
        val bossBarService = module!!.getService<BossBarService>()
        plugin.launch {
            bossBarService.reload()
            Bukkit.getServer().consoleSender.sendMessage(prefix + ChatColor.GREEN + "Enabled ShyBossBar " + plugin.description.version + " by Shynixn")
        }
    }

    override fun onLoad() {
        // Register Flags
        worldGuardService = WorldGuardServiceImpl(this)
        worldGuardService!!.registerFlag("shybossbar", String::class.java)
    }

    override fun onDisable() {
        if (module == null) {
            return
        }

        module!!.close()
        module = null
    }
}
