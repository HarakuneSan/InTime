package dev.harakunesan.intimepaper.events

import dev.harakunesan.intimepaper.InTimePlugin
import org.bukkit.NamespacedKey
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class ExtractTimeEvent : Listener {
    @EventHandler
    fun onClick(event: PlayerInteractEvent) {
        if (event.action.isLeftClick) return
        event.isCancelled = handleTime(event.player)
    }

    @EventHandler
    fun onEntityClick(event: PlayerInteractEntityEvent) {
        if (event.rightClicked is ItemFrame) return
        event.isCancelled = handleTime(event.player)
    }

    private fun handleTime(player: Player): Boolean {
        val inventory = player.inventory
        val item = if(inventory.itemInMainHand.amount == 0) inventory.itemInOffHand else inventory.itemInMainHand
        val amount = item.amount
        if(amount == 0) return false
        val timekeyforitem = NamespacedKey(InTimePlugin.plugin, "timestored")
        val timekeytype = PersistentDataType.INTEGER

        val timekeyforplayer = NamespacedKey(InTimePlugin.plugin, "timeleft")
        val timeleft = player.persistentDataContainer.getOrDefault(timekeyforplayer, timekeytype, 0)
        val seconds = item.itemMeta.persistentDataContainer.getOrDefault(timekeyforitem, timekeytype, 0) * amount
        if (seconds == 0) return false

        if(inventory.itemInMainHand.amount == 0) {
            player.inventory.setItemInOffHand(ItemStack(item.type).asQuantity(amount))
        } else {
            player.inventory.setItemInMainHand(ItemStack(item.type).asQuantity(amount))
        }
        player.persistentDataContainer.set(timekeyforplayer, timekeytype, timeleft + seconds)
        player.sendMessage("Du hast $seconds Sekunden von diesem Item erhalten")
        return true
    }
}