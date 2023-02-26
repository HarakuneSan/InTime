package dev.harakunesan.intimepaper.events

import dev.harakunesan.intimepaper.InTimePlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.persistence.PersistentDataType

class BlockBreakReward : Listener {
    private val timerewards = InTimePlugin.defaultConfig.getMapList("rewards.blocks")
        .flatMap { it.entries }.associate { (item, seconds) -> item.toString() to seconds.toString().toInt() }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if(!timerewards.containsKey(event.block.type.name)) return

        val blockname = event.block.type.name
        val player = event.player
        val timekey = NamespacedKey(InTimePlugin.plugin, "timeleft")

        val timekeytype = PersistentDataType.INTEGER
        val timeleft = player.persistentDataContainer.getOrDefault(timekey, timekeytype, InTimePlugin.defaultConfig.getInt("defaultTimeSecs"))
        val timetoadd = timerewards[blockname]!!
        player.persistentDataContainer.set(timekey, timekeytype, timeleft + timetoadd)
        player.sendActionBar(Component.text("+$timetoadd Sek").color(NamedTextColor.YELLOW))
        player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1f)
    }
}