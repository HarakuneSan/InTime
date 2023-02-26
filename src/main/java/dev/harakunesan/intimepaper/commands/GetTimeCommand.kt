package dev.harakunesan.intimepaper.commands

import dev.harakunesan.intimepaper.InTimePlugin.Companion.plugin
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

class GetTimeCommand : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if(sender !is Player) return false
        var container = sender.persistentDataContainer
        if(args?.size!! >= 1 && sender.hasPermission("intime.command.gettime.all")) {
            val player = Bukkit.getOnlinePlayers().find { it.name == args[0] }
            if(player == null) {
                sender.sendMessage("Der Spieler ${args[0]} ist nicht online")
                return false
            }
            container = player.persistentDataContainer
        }

        val timekey = NamespacedKey(plugin, "timeleft")
        val timekeytype = PersistentDataType.INTEGER
        val timeleft = container.get(timekey, timekeytype)!!

        sender.sendMessage("Verbleibende Zeit: $timeleft Sekunden")
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): MutableList<String> {
        // Tab completion für args.size == 1
        return if (args?.size == 1 && sender.hasPermission("intime.command.gettime.all")) {
            Bukkit.getOnlinePlayers().map { player -> player.name }.toMutableList()
        } else mutableListOf()
    }
}