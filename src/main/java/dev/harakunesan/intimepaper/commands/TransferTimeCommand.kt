package dev.harakunesan.intimepaper.commands

import dev.harakunesan.intimepaper.InTimePlugin
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.persistence.PersistentDataType

class TransferTimeCommand : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        // Check ob der Command Sender ein Spieler ist
        if (sender !is Player) return false

        // Setzen der Konstanten timekey und timekeytype
        val timekey = NamespacedKey(InTimePlugin.plugin, "timeleft")
        val timekeytype = PersistentDataType.INTEGER

        // Check, ob der Empfängerspieler online ist und ob der Empfänger nicht der Sender ist
        val onlinePlayers = Bukkit.getOnlinePlayers()
        val playertorecieve =
            if (args?.size!!.toInt() < 1) null else onlinePlayers.find { it.name == args[0] && it.gameMode == GameMode.SURVIVAL }
        if (playertorecieve == null || playertorecieve == sender || playertorecieve.gameMode != GameMode.SURVIVAL) {
            sender.sendMessage("Gebe bitte einen Spieler an, der Online ist und nicht du bist")
            return false
        }

        // Variablen für den Sender
        val sendercontainer = sender.persistentDataContainer
        val timeremaining = sendercontainer.getOrDefault(timekey, timekeytype, 0)

        // Variablen für den Empfänger
        val recievercontainer = playertorecieve.persistentDataContainer
        val recieverseconds = recievercontainer.getOrDefault(timekey, timekeytype, 0)

        val timetotransfer = if ( args.size < 2 || args[1].toIntOrNull() == null) 0 else args[1].toInt()
        val timeleftaftertransfer = timeremaining - timetotransfer

        // Check, ob die Spieler max 4 Blöcke auseinander Stehen
        if(sender.location.distance(playertorecieve.location) > 4) {
            sender.sendMessage("Der andere Spieler muss sind in einem Radius von 4 Blöcken aufhalten")
            return false
        }

        // Check, ob die Zeit ein positiver Int ist
        if (timetotransfer <= 0) {
            sender.sendMessage("Bitte gebe eine Zeit in Sekunden an")
            return false
        }

        // Check, ob der Spieler nach dem Transfer mehr als 10 Minuten übrig hat
        if (timeleftaftertransfer <= 1 * 60 * 10) {
            sender.sendMessage("Du musst nach dem Transfer noch mehr als 10 Minuten übrig haben")
            return false
        }

        // Transferiere die Zeit vom Sender zum Empfänger
        sendercontainer.set(timekey, timekeytype, timeleftaftertransfer)
        recievercontainer.set(timekey, timekeytype, recieverseconds+timetotransfer)

        // Der Code erklärt sich selber
        sender.sendMessage("Du hast $timetotransfer Sekunden an ${playertorecieve.name} transferiert")
        playertorecieve.sendMessage("Du hast $timetotransfer Sekunden von ${sender.name} transferiert bekommen")

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
        return if (args?.size == 1) {
            Bukkit.getOnlinePlayers().filter { it.gameMode == GameMode.SURVIVAL }.map { player -> player.name }.toMutableList()
        } else mutableListOf()
    }
}