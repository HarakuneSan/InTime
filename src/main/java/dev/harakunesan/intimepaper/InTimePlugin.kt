package dev.harakunesan.intimepaper

import dev.harakunesan.intimepaper.commands.*
import dev.harakunesan.intimepaper.events.BlockBreakReward
import dev.harakunesan.intimepaper.events.ExtractTimeEvent
import dev.harakunesan.intimepaper.events.PlayerDeathPunishEvent
import dev.harakunesan.intimepaper.scheduler.TimeReductionTask
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Logger


class InTimePlugin : JavaPlugin(), Listener {

    companion object {
        lateinit var pluginLogger: Logger
        lateinit var plugin: InTimePlugin
        lateinit var defaultConfig: YamlConfiguration
    }

    override fun onEnable() {
        // lateinit einrichten
        pluginLogger = logger
        plugin = this
        defaultConfig = YamlConfiguration.loadConfiguration(getTextResource("config.yml")!!)

        // Events registrieren
        Bukkit.getPluginManager().registerEvents(this, this)
        Bukkit.getPluginManager().registerEvents(PlayerDeathPunishEvent(), this)
        Bukkit.getPluginManager().registerEvents(ExtractTimeEvent(), this)
        Bukkit.getPluginManager().registerEvents(BlockBreakReward(), this)
        Bukkit.getPluginManager().registerEvents(DuellCommand(), this)

        // Commands registrieren
        getCommand("resettime")!!.setExecutor(ResetTimeCommand())
        getCommand("gettime")!!.setExecutor(GetTimeCommand())
        getCommand("settime")!!.setExecutor(SetTimeCommand())
        getCommand("transfertime")!!.setExecutor(TransferTimeCommand())
        getCommand("storetime")!!.setExecutor(StoreTimeCommand())
        getCommand("sell")!!.setExecutor(SellCommand())
        getCommand("duell")!!.setExecutor(DuellCommand())

        // Downcounter einrichten
        val task = TimeReductionTask()
        task.runTaskTimer(plugin, 0, 20) // jede Sekunde ausführen
        saveDefaultConfig()
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        Bukkit.broadcast(Component.text("Deine Mutter"))
        event.player.sendMessage(Component.text("Willkommen ").append(event.player.displayName()).color(NamedTextColor.YELLOW))

        // Speichern von Daten für einen Spieler
        val container = event.player.persistentDataContainer
        val timekey = NamespacedKey(this, "timeleft")
        val timekeytype = PersistentDataType.INTEGER
        val time = container.get(timekey, timekeytype)
        if (time == null) {
            container.set(timekey, PersistentDataType.INTEGER, defaultConfig.getInt("defaultTimeSecs"))
        }

        event.player.sendMessage(Component.text("Willkommen zurück ${event.player.name}"))
    }
}