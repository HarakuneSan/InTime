package dev.harakunesan.intimepaper.commands

import dev.harakunesan.intimepaper.InTimePlugin.Companion.plugin
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.persistence.PersistentDataType

class SetTimeCommand : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        val playername = if (args == null || args.isEmpty()) sender.name else args[0]
        val playertoset = Bukkit.getOnlinePlayers().find { it.name == playername }
        if(playertoset == null) {
            sender.sendMessage("Der spieler $playername ist nicht online")
            return false
        }

        val timetoset = if(args?.size!! >= 2) args[1] else null
        if (timetoset == null || timetoset.toIntOrNull() == null) {
            sender.sendMessage("Bitte gebe eine Zeit in Sekunden an")
            return false
        }

        val container = playertoset.persistentDataContainer
        val timekey = NamespacedKey(plugin, "timeleft")
        val timekeytype = PersistentDataType.INTEGER
        val timeleft = container.getOrDefault(timekey, timekeytype, 1*60*60*2)

        when(if(args.size >= 3) args[2] else null) {
            "add" -> {
                container.set(timekey, timekeytype, timeleft + timetoset.toInt())
                sender.sendMessage("Der Zeit des Spielers $playername wurden $timetoset Sekunden zugefügt")
            }
            "subtract" -> {
                container.set(timekey, timekeytype, timeleft - timetoset.toInt())
                sender.sendMessage("Der Zeit des Spielers $playername wurden $timetoset Sekunden abgezogen")
            }
            else -> {
                container.set(timekey, timekeytype, timetoset.toInt())
                sender.sendMessage("Die Zeit des Spielers $playername wurde auf $timetoset Sekunden gesetzt")
            }
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): MutableList<String> {
        // Tab completion für args.size == 1
        return when (args?.size) {
            1 -> {
                Bukkit.getOnlinePlayers().map { player -> player.name }.toMutableList()
            }
            3 -> mutableListOf("add", "subtract")
            else -> mutableListOf()
        }
    }
}