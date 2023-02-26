package dev.harakunesan.intimepaper.commands

import dev.harakunesan.intimepaper.InTimePlugin
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import dev.harakunesan.intimepaper.InTimePlugin.Companion.plugin as plugin

class ResetTimeCommand : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        val playername = if (args == null || args.isEmpty()) sender.name else args[0]
        val playertoclear = Bukkit.getOnlinePlayers().find { it.name == playername }
        if(playertoclear == null) {
            sender.sendMessage("Der spieler $playername ist nicht online")
            return false
        }

        val container = playertoclear.persistentDataContainer;
        val timekey = NamespacedKey(plugin, "timeleft")
        val timekeytype = PersistentDataType.INTEGER
        container.set(timekey, timekeytype,InTimePlugin.defaultConfig.getInt("defaultTimeSecs", 7200))

        sender.sendMessage("Der spieler $playername wurde zurückgesetzt")
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): MutableList<String> {
        if(args?.size!! > 1) return mutableListOf()
        // Tab completion für args.size == 1
        return if (args.size == 1) {
            Bukkit.getOnlinePlayers().map { player -> player.name }.toMutableList()
        } else mutableListOf()
    }
}