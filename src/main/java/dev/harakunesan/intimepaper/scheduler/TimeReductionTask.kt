package dev.harakunesan.intimepaper.scheduler

import dev.harakunesan.intimepaper.InTimePlugin
import dev.harakunesan.intimepaper.InTimePlugin.Companion.plugin
//import net.kyori.adventure.bossbar.BossBar
import org.bukkit.boss.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.NamespacedKey
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import java.util.concurrent.TimeUnit


class TimeReductionTask : BukkitRunnable() {

    private var bossbars: Array<BossBar> = arrayOf()

    override fun run() {
        Bukkit.getOnlinePlayers().forEach { player ->
            val persistentDataContainer = player.persistentDataContainer
            val timeKey = NamespacedKey(plugin, "timeleft")
            val timekeytype = PersistentDataType.INTEGER
            var time = persistentDataContainer.getOrDefault(timeKey, timekeytype, 1 * 60 * 60 * 2)
            time--

            if(bossbars.find { bossBar -> bossBar.players.contains(player) } == null && time > 0) {
                val bossbar: BossBar = Bukkit.createBossBar("Titel der Bossbar", BarColor.BLUE, BarStyle.SOLID)

                // Wenn 1 Stunde über ist, wird die Bossbar auf Gelb gesetzt
                bossbar.setTitle(formatSeconds(time.toLong()))
                var messagetosend = Component.text("")
                if(time <= InTimePlugin.defaultConfig.getInt("warnTimes.warn", 3600)) {
                    bossbar.color = BarColor.YELLOW
                    messagetosend =  Component.text("Du hast nur noch 1 Stunde Zeit auf deinem Konto").color(NamedTextColor.YELLOW)
                }

                // Wenn 10 Minuten über sind, wird die Bossbar auf Rot gesetzt
                if(time <= InTimePlugin.defaultConfig.getInt("warnTimes.critical", 600)) {
                    bossbar.color = BarColor.RED
                }

                // Wenn einer der beiden oberen Fälle eingetreten ist, wird die Nachricht gesendet
                if(messagetosend.color() != null)
                    player.sendMessage(messagetosend)

                bossbar.addPlayer(player)
                // Bossbar zu liste der Bossbars hinzufügen
                this.bossbars += bossbar
            }

            if(time <= 0 && player.gameMode != GameMode.SPECTATOR) {
                player.gameMode = GameMode.SPECTATOR
                val bossbar = bossbars.find { bar -> bar.players.contains(player) }
                bossbar?.removePlayer(player)
                Bukkit.broadcast(Component.text("Zeit ist geld und ${player.name} ist pleite "))
                return
            }
            if (time > 0) {
                // Setze die Bossbar auf die verbleibende Zeit
                val bossbar = bossbars.find { bar -> bar.players.contains(player) }!!
                bossbar.setTitle(formatSeconds(time.toLong()))

                bossbar.color = BarColor.BLUE

                // Wenn die 1 Stundenmarke erreicht ist, wird die bossbar gelb
                if(time <= InTimePlugin.defaultConfig.getInt("warnTimes.warns", 1*60*60)) {
                    bossbar.color = BarColor.YELLOW
                }

                // Wenn die 10 Minutenmarke erreicht ist, wird es kritisch
                if(time <= InTimePlugin.defaultConfig.getInt("warnTimes.warns", 1*60*10)) {
                    bossbar.color = BarColor.RED
                }

                // Aktualisiere die Zeit im Data Container
                persistentDataContainer.set(timeKey, timekeytype, time)
            }
        }
    }

    private fun formatSeconds(seconds: Long): String {
        val day = TimeUnit.SECONDS.toDays(seconds)
        val hours = TimeUnit.SECONDS.toHours(seconds) % 24
        val minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60
        val secs = TimeUnit.SECONDS.toSeconds(seconds) % 60
        return "$day Tage, $hours Stunden, $minutes Minuten, $secs Sekunden"
    }
}