package com.willfp.libreforge

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.Prerequisite
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.core.integrations.IntegrationLoader
import com.willfp.libreforge.commands.CommandLibreforge
import com.willfp.libreforge.configs.ChainsYml
import com.willfp.libreforge.configs.lrcdb.CommandLrcdb
import com.willfp.libreforge.effects.Effects
import com.willfp.libreforge.effects.arguments.custom.CustomEffectArguments
import com.willfp.libreforge.integrations.aureliumskills.AureliumSkillsIntegration
import com.willfp.libreforge.integrations.citizens.CitizensIntegration
import com.willfp.libreforge.integrations.jobs.JobsIntegration
import com.willfp.libreforge.integrations.levelledmobs.LevelledMobsIntegration
import com.willfp.libreforge.integrations.mcmmo.McMMOIntegration
import com.willfp.libreforge.integrations.paper.PaperIntegration
import com.willfp.libreforge.integrations.scyther.ScytherIntegration
import com.willfp.libreforge.integrations.tmmobcoins.TMMobcoinsIntegration
import com.willfp.libreforge.integrations.vault.VaultIntegration
import com.willfp.libreforge.integrations.worldguard.WorldGuardIntegration
import com.willfp.libreforge.levels.LevelTypes
import com.willfp.libreforge.levels.placeholder.ItemDataPlaceholder
import com.willfp.libreforge.levels.placeholder.ItemLevelPlaceholder
import com.willfp.libreforge.levels.placeholder.ItemPointsPlaceholder
import com.willfp.libreforge.levels.placeholder.ItemProgressPlaceholder
import com.willfp.libreforge.levels.placeholder.ItemXPPlaceholder
import com.willfp.libreforge.levels.placeholder.ItemXPRequiredPlaceholder
import com.willfp.libreforge.placeholders.CustomPlaceholders
import com.willfp.libreforge.triggers.DispatchedTriggerFactory
import org.bukkit.Bukkit
import org.bukkit.event.Listener

internal lateinit var plugin: LibreforgeSpigotPlugin
    private set

class LibreforgeSpigotPlugin : EcoPlugin() {
    val chainsYml = ChainsYml(this)

    val dispatchedTriggerFactory = DispatchedTriggerFactory(this)

    private var hasLoaded = false

    private val configCategories = listOf(
        LevelTypes,
        CustomEffectArguments
    )

    init {
        plugin = this
    }

    override fun handleLoad() {
        for (category in configCategories) {
            category.copyConfigs(this)
            category.reload(this)
        }
    }

    override fun handleEnable() {
        if (this.configYml.getBool("show-libreforge-info")) {
            this.logger.info("")
            this.logger.info("Hey, what's this plugin doing here? I didn't install it!")
            this.logger.info("libreforge is the effects system for plugins like EcoEnchants,")
            this.logger.info("EcoJobs, EcoItems, etc. If you're looking for config options for")
            this.logger.info("things like cooldown messages, lrcdb, and stuff like that, you'll")
            this.logger.info("find it under /plugins/libreforge")
            this.logger.info("")
            this.logger.info("Don't worry about updating libreforge, it's handled automatically!")
            this.logger.info("")
        }

        if (Prerequisite.HAS_PAPER.isMet) {
            PaperIntegration.load(this)
        }

        pointsPlaceholder(this).register()
        globalPointsPlaceholder(this).register()
        ItemPointsPlaceholder(this).register()
        ItemLevelPlaceholder(this).register()
        // Register required first because it technically matches the pattern of "xp"
        ItemXPRequiredPlaceholder(this).register()
        ItemXPPlaceholder(this).register()
        ItemProgressPlaceholder(this).register()
        ItemDataPlaceholder(this).register()
    }

    override fun handleReload() {
        for (config in chainsYml.getSubsections("chains")) {
            Effects.register(
                config.getString("id"),
                Effects.compileChain(
                    config.getSubsections("effects"),
                    ViolationContext(this, "chains.yml")
                ) ?: continue
            )
        }

        for (customPlaceholder in this.configYml.getSubsections("placeholders")) {
            CustomPlaceholders.load(customPlaceholder, this)
        }

        for (category in configCategories) {
            category.reload(this)
        }

        hasLoaded = true
    }

    override fun createTasks() {
        dispatchedTriggerFactory.startTicking()

        // Poll for changes
        plugin.scheduler.runTimer(20, 20) {
            for (player in Bukkit.getOnlinePlayers()) {
                player.refreshHolders()
            }
        }
    }

    override fun loadListeners(): List<Listener> {
        return listOf(
            EffectCollisionFixer,
            ItemRefreshListener(this)
        )
    }

    override fun loadIntegrationLoaders(): List<IntegrationLoader> {
        return listOf(
            IntegrationLoader("AureliumSkills") { AureliumSkillsIntegration.load(this) },
            IntegrationLoader("Jobs") { JobsIntegration.load(this) },
            IntegrationLoader("LevelledMobs") { LevelledMobsIntegration.load(this) },
            IntegrationLoader("mcMMO") { McMMOIntegration.load(this) },
            IntegrationLoader("Citizens") { CitizensIntegration.load(this) },
            IntegrationLoader("Scyther") { ScytherIntegration.load(this) },
            IntegrationLoader("TMMobcoins") { TMMobcoinsIntegration.load(this) },
            IntegrationLoader("Vault") { VaultIntegration.load(this) },
            IntegrationLoader("WorldGuard") { WorldGuardIntegration.load(this) },
        )
    }

    override fun loadPluginCommands(): List<PluginCommand> {
        return listOf(
            CommandLrcdb(this),
            CommandLibreforge(this)
        )
    }

    override fun getMinimumEcoVersion(): String {
        return "6.63.0"
    }

    /**
     * Run a runnable when the plugin is enabled.
     */
    fun runWhenEnabled(runnable: () -> Unit) {
        if (hasLoaded) {
            runnable()
        } else {
            onReload(runnable)
        }
    }
}
