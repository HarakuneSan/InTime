package dev.harakunesan.intimepaper.commands

import dev.harakunesan.intimepaper.InTimePlugin
import dev.harakunesan.intimepaper.InTimePlugin.Companion.plugin
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.persistence.PersistentDataType

class SellCommand : CommandExecutor, TabCompleter, Listener {
    private val inventories = mutableMapOf<Player, Inventory>()
    private val timemap = InTimePlugin.defaultConfig.getMapList("shop.items")
        .flatMap { it.entries }.associate { (item, seconds) -> item.toString() to seconds.toString().toInt() }
    private var reopeninventory = false


    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (sender !is Player) return false

        if (!inventories.containsKey(sender)) {
            val inventory = Bukkit.createInventory(null, 9, Component.text("Sell"))
            inventory.storageContents
            inventories[sender] = inventory
        }
        sender.openInventory(inventories[sender]!!)

        Bukkit.getPluginManager().registerEvents(this, plugin)

        return true
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (!inventories.containsKey(event.player) || event.player.openInventory.type.name != "CHEST") return
        if(reopeninventory) {
            reopeninventory = false
            return
        }
        val stack = inventories[event.player as Player]!!.contents.filterNotNull()
        var seconds = 0
        for (item in stack) {
            seconds += timemap.getValue(item.type.toString()) * item.amount
        }

        val container = event.player.persistentDataContainer
        val timekey = NamespacedKey(plugin, "timeleft")
        val timekeytype = PersistentDataType.INTEGER
        val timeleft = container.get(timekey, timekeytype)!!
        container.set(timekey, timekeytype, timeleft+seconds)
        event.player.sendMessage("Du hast $seconds Sekunden gutgeschrieben bekommen")

        inventories.remove(event.player)
        HandlerList.unregisterAll(this)
    }

    @EventHandler
    fun onClickInventory(event: InventoryClickEvent) {
        if (event.action.name == "NOTHING" || !inventories.containsKey(event.whoClicked)) return
        val invtoadd: Inventory
        val invtoremove: Inventory
        if (event.clickedInventory?.type == InventoryType.CHEST) {
            invtoadd = event.whoClicked.inventory
            invtoremove = inventories[event.whoClicked]!!
        } else {
            invtoadd = inventories[event.whoClicked]!!
            invtoremove = event.whoClicked.inventory
        }
        event.isCancelled = true
        if (!timemap.containsKey(event.currentItem!!.type.toString())) return
        invtoadd.addItem(event.currentItem!!)
        invtoremove.setItem(event.slot, null)

        val chest = inventories[event.whoClicked]!!
        val stack = chest.contents.filterNotNull()
        var seconds = 0
        for (item in stack) {
            seconds += timemap.getValue(item.type.toString()) * item.amount
            Material.SCULK_SHRIEKER
        }
        reopeninventory = true
        val contents = chest.contents
        inventories[event.whoClicked as Player] =
            Bukkit.createInventory(null, 9, Component.text("Sell - $seconds Sekunden"))
        inventories[event.whoClicked as Player]!!.contents = contents
        event.whoClicked.openInventory(inventories[event.whoClicked as Player]!!)
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): MutableList<String> {
        return mutableListOf()
    }
}