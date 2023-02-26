package dev.harakunesan.intimepaper.events

import dev.harakunesan.intimepaper.InTimePlugin
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.persistence.PersistentDataType

class PlayerDeathPunishEvent : Listener {
    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        val killer = event.player.killer
        val diedplayer = event.player.persistentDataContainer
        val timekey = NamespacedKey(InTimePlugin.plugin, "timeleft")
        val timekeytype = PersistentDataType.INTEGER
        val timeleft = diedplayer.getOrDefault(timekey, timekeytype, InTimePlugin.defaultConfig.getInt(""))
        when (killer is Player) {
            true -> {
                val timetosubtract = InTimePlugin.defaultConfig.getInt("deathPunish.punishOnPlayerDeath", 0)
                diedplayer.set(timekey, timekeytype, timeleft - timetosubtract)
                if (InTimePlugin.defaultConfig.getBoolean("deathPunish.transferTimeToKiller")
                ) {
                    val killercontainer = killer.persistentDataContainer
                    val killertimeleft = killercontainer.getOrDefault(
                        timekey,
                        timekeytype,
                        InTimePlugin.defaultConfig.getInt("defaultTimeSecs")
                    )
                    killercontainer.set(timekey, timekeytype, killertimeleft + timetosubtract)
                }
            }

            false -> {
                diedplayer.set(
                    timekey,
                    timekeytype,
                    timeleft - InTimePlugin.defaultConfig.getInt("deathPunish.punishOnNaturalDeath")
                )
            }
        }
    }
}