package dev.harakunesan.intimepaper.commands

import dev.harakunesan.intimepaper.InTimePlugin.Companion.plugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.persistence.PersistentDataType
import java.util.*

class DuellCommand : CommandExecutor, TabCompleter, Listener {
    companion object {
        val requests: MutableList<Map<String, Any>> = mutableListOf()
        private val playersInCooldown = mutableSetOf<Player>()
        private val playersInDuel = mutableListOf<Map<Player, Player>>()
    }


    @EventHandler
    fun onDamage(event: EntityDamageByEntityEvent) {
        val entity = event.entity
        val damager = event.damager
        if (entity is Player && playersInDuel.find { it[entity] != null } == null) if (damager is Player && playersInCooldown.contains(
                damager
            )
        ) {
            event.isCancelled = true
        }
        if (entity is Player && playersInCooldown.contains(entity)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onDeath(event: PlayerDeathEvent) {
        println("Test Death")
        val timeToTransferKey = NamespacedKey(plugin, "duelltime")
        val timeToTransferKeyType = PersistentDataType.INTEGER
        val timekey = NamespacedKey(plugin, "timeleft")
        val timekeytype = PersistentDataType.INTEGER

        val duell = playersInDuel.find { it.keys.contains(event.player) || it.values.contains(event.player) }
        if (duell != null) {
            val (player1, player2) = duell.entries.first { it.key == event.player || it.value == event.player }
            val timeToTransfer = player1.persistentDataContainer.getOrDefault(timeToTransferKey, timeToTransferKeyType, 69)
            val p1timeleft = player1.persistentDataContainer.getOrDefault(timekey, timekeytype, 69)
            val p2timeleft = player2.persistentDataContainer.getOrDefault(timekey, timekeytype, 69)
            if (event.player == player1) {
                player1.persistentDataContainer.set(timekey, timekeytype, p1timeleft - timeToTransfer)
                player2.persistentDataContainer.set(timekey, timekeytype, p2timeleft + timeToTransfer)
            } else {
                player2.persistentDataContainer.set(timekey, timekeytype, p2timeleft - timeToTransfer)
                player1.persistentDataContainer.set(timekey, timekeytype, p1timeleft + timeToTransfer)
            }
            player1.sendMessage("Das Duell wurde beendet.")
            player2.sendMessage("Das Duell wurde beendet.")
            playersInDuel.remove(duell)
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (sender !is Player) return false
        if (args?.isEmpty()!!) {
            sender.sendMessage("Nutzung: /duell (herausfordern|annehmen|ablehnen) spielername [sekunden]")
            return false
        }
        when (args[0]) {
            "annehmen" -> {
                if (args.size < 2) {
                    sender.sendMessage("Nutzung: /duell annehmen spielername")
                    return false
                }
                val anfrage = requests.find { it["receiver"] == sender.name && it["sender"] == args[1] }
                if (anfrage == null) {
                    sender.sendMessage("Diese Anfrage existiert nicht")
                    return false
                }

                val challenged = Bukkit.getOnlinePlayers().find { it.name == anfrage["receiver"] }
                val challenger = Bukkit.getOnlinePlayers().find { it.name == anfrage["sender"] }

                if (challenged == null || challenger == null) {
                    sender.sendMessage("Spieler nicht Online")
                    return false
                }
                requests.removeIf { it["receiver"] == sender.name && it["sender"] == args[1] }
                sender.sendMessage("Test")

                val countdown = 5
                challenged.walkSpeed = 0f
                challenger.walkSpeed = 0f
                playersInCooldown.addAll(setOf(challenged, challenger))

                for (i in countdown downTo 1) {
                    Bukkit.getScheduler().runTaskLater(plugin, fun() {
                        for (player in mutableListOf(challenger, challenged)) {
                            player.showTitle(
                                Title.title(
                                    Component.text("DUELL"), Component.text("Startet in $i")
                                )
                            )
                        }
                    }, (countdown - i) * 20L)
                }

                Bukkit.getScheduler().runTaskLater(plugin, fun() {
                    val duellTimeKey = NamespacedKey(plugin, "duelltime")
                    val duellTimeKeyType = PersistentDataType.INTEGER
                    challenged.persistentDataContainer.set(duellTimeKey, duellTimeKeyType, anfrage["seconds"] as Int)
                    challenger.persistentDataContainer.set(duellTimeKey, duellTimeKeyType, anfrage["seconds"] as Int)
                    playersInCooldown.removeAll(setOf(challenged, challenger))
                    playersInDuel.add(mapOf(challenged to challenger))
                    challenged.walkSpeed = 0.2f
                    challenger.walkSpeed = 0.2f
                    testDistanceLoop(challenger, challenged)
                }, countdown * 20L)
            }

            "ablehnen" -> {
                if (args.size < 2) {
                    sender.sendMessage("Nutzung: /duell ablehnen spielername")
                    return false
                }

                val anfrage = requests.find { it["sender"] == args[1] }
                if (anfrage == null) {
                    sender.sendMessage("Diese Anfrage existiert nicht")
                    return false
                }
                requests.removeIf { it["receiver"] == sender.name && it["sender"] == args[1] }
                sender.sendMessage("Die Anfrage wurde gelöscht")
            }

            "herausfordern" -> {
                if (args.size < 3) {
                    sender.sendMessage("Nutzung: /duell herausfordern spielername sekunden")
                    return false
                }
                if (getPlayer(args[1]) == null) {
                    sender.sendMessage("Bitte gebe einen Spieler an")
                    return false
                }

                val player = getPlayer(args[1])
                if (sender == player) {
                    sender.sendMessage("Gebe nicht dich selber an")
                    return false
                }
                if (sender.location.distance(player!!.location) > 4) {
                    sender.sendMessage("Der andere Spieler muss sind in einem Radius von 4 Blöcken aufhalten")
                    return false
                }

                val seconds = args[2].toIntOrNull()
                if (seconds == null) {
                    sender.sendMessage("Bitte gebe eine Zeit in Sekunden an")
                    return false
                }
                val timekey = NamespacedKey(plugin, "timeleft")
                val timekeytype = PersistentDataType.INTEGER
                val timeleft = sender.persistentDataContainer.getOrDefault(timekey, timekeytype, 0)
                if (timeleft - seconds < 600) {
                    sender.sendMessage("Du musst noch mehr als 10 Minuten übrig haben")
                    return false
                }

                if (!requests.none { it["receiver"] == player.name && it["sender"] == sender.name }) {
                    sender.sendMessage("Dieser Spieler hat bereits eine Anfrage von dir erhalten")
                    return false
                }

                player.sendMessage(
                    Component.text("${sender.name} hat dich zu einem PVP Duell um $seconds Sekunden eingeladen, möchtest du ")

                        .append(
                            Component.text("annehmen").color(NamedTextColor.YELLOW)
                                .hoverEvent(HoverEvent.showText(Component.text("Klicke zum Annehmen")))
                                .clickEvent(ClickEvent.runCommand("/duell annehmen ${sender.name}"))
                        )

                        .append(Component.text(" oder "))

                        .append(
                            Component.text("ablehnen").color(NamedTextColor.RED)
                                .hoverEvent(HoverEvent.showText(Component.text("Klicke zum Ablehnen")))
                                .clickEvent(ClickEvent.runCommand("/duell ablehnen ${sender.name}"))
                        ).append(Component.text("\nDiese Anfrage ist für 2 Minuten gültig"))
                )

                sender.sendMessage("Dem Spieler ${player.name} wurde eine PVP Duell Anfrage gesendet")

                requests.add(
                    mapOf(
                        "sender" to sender.name,
                        "receiver" to player.name,
                        "seconds" to seconds,
                    )
                )
                val timer = Timer()
                timer.schedule(object : TimerTask() {
                    override fun run() {
                        requests.remove(mapOf("sender" to sender.name, "receiver" to player.name, "seconds" to seconds))
                        timer.cancel()
                    }
                }, 120_000)
            }

            else -> {
                sender.sendMessage("Nutzung: /duell (herausfordern|annehmen|ablehnen) spielername [sekunden]")
                return false
            }
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender, command: Command, label: String, args: Array<out String>?
    ): MutableList<String> {
        if (args.isNullOrEmpty()) return mutableListOf()

        when (args.size) {
            1 -> {
                return mutableListOf("annehmen", "ablehnen", "herausfordern")
            }

            2 -> {
                println(requests.toMutableList())
                if (arrayOf(
                        "annehmen", "ablehnen"
                    ).contains(args[0])
                ) return requests.filter { it["receiver"] == sender.name }.map { it["sender"]!! as String }
                    .toMutableList()

                if (args[0] == "herausfordern") return Bukkit.getOnlinePlayers().map { player -> player.name }
                    .filter { it != sender.name }.toMutableList()
            }

            else -> return mutableListOf()
        }

        return mutableListOf()
    }

    private fun testDistanceLoop(player1: Player, player2: Player) {
        Bukkit.getScheduler().runTaskLater(plugin, fun() {
            val p2 = Bukkit.getOnlinePlayers().find { it == player1 }
            val p1 = Bukkit.getOnlinePlayers().find { it == player2 }
            if (player1.location.distance(player2.location) > 20 || p2 == null || p1 == null) {
                if (playersInDuel.find { it[player1] != null } == null) return
                player1.sendMessage("Duell abgebrochen")
                player2.sendMessage("Duell abgebrochen")
                return
            }
            testDistanceLoop(player1, player2)
        }, 20L)
    }

    private fun getPlayer(player: String): Player? {
        return Bukkit.getOnlinePlayers().find { it.name == player }
    }
}