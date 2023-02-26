package dev.harakunesan.intimepaper.commands

import dev.harakunesan.intimepaper.InTimePlugin
import dev.harakunesan.intimepaper.InTimePlugin.Companion.plugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

class StoreTimeCommand : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        // Check ob der Command Sender ein Spieler ist
        if (sender !is Player) return false

        // Setzen der Konstanten timekey und timekeytype
        val timekey = NamespacedKey(plugin, "timeleft")
        val timekeyforitem = NamespacedKey(plugin, "timestored")
        val timekeytype = PersistentDataType.INTEGER

        // Variablen für den Sender
        val sendercontainer = sender.persistentDataContainer
        val timeremaining = sendercontainer.getOrDefault(timekey, timekeytype, 0)
        val timetotransfer = if (args?.size!! < 1 || args[0].toIntOrNull() == null) 0 else args[0].toInt()
        val timeleftaftertransfer = timeremaining - timetotransfer

        val item = sender.inventory.itemInMainHand
        val itemmeta = item.itemMeta
        if (item.amount != 1) {
            sender.sendMessage("Bitte halte ein einzelnes Item in der Hand")
            return false
        }
        if (item.hasItemMeta()) {
            sender.sendMessage("Das ist nicht möglich mit Items, die enchantments oder Items beinhalten")
            return false
        }

        if (timeleftaftertransfer < InTimePlugin.defaultConfig.getInt("minTimeAfterTransfer")) {
            sender.sendMessage("Du musst nach dem Transfer noch mindestens 10 Minuten Zeit übrig haben")
            return false
        }

        if (timetotransfer <= 0) {
            sender.sendMessage("Du musst eine zahl in Sekunden angeben")
            return false
        }

        sender.persistentDataContainer.set(timekey, timekeytype, timeleftaftertransfer)
        itemmeta.persistentDataContainer.set(timekeyforitem, timekeytype, timetotransfer)
        itemmeta.displayName(Component.text("Zeitspeicher").color(NamedTextColor.YELLOW))
        itemmeta.lore(listOf(Component.text("$timetotransfer Sekunden").color(NamedTextColor.AQUA)))
        sender.inventory.itemInMainHand.itemMeta = itemmeta

        sender.sendMessage("Du hast $timetotransfer Sekunden auf dein Item geladen, per Rechtsklick kannst du diese wieder entladen")

        // Command erfolgreich ausgeführt
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): MutableList<String> {
        // Tab completion für args.size == 1
        return mutableListOf()
    }
}