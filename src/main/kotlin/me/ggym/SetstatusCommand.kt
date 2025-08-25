package me.ggym

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import net.milkbowl.vault.permission.Permission
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin


class SetstatusCommand(private val perms: Permission, private val plugin: JavaPlugin) : CommandExecutor{
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {



        if (args.size < 1 || args.size > 3) {
            sender.sendMessage("Неверное количество аргументов!")
            return true
        }

        val mode: String = args[0]

        if (mode != "pvp" && mode != "peace") {
            sender.sendMessage("Неверные аргументы!")
            return true
        }

        // If sender is not OP or console
        if (sender is Player && sender.hasPermission("status.admin") == false) {
            if (sender.hasPermission("status.edit")) {
                applyStatusUser(sender, mode)
            } else {
                // Notify the player that permission is needed
                sender.sendMessage(
                    Component.text("У вас нет права задавать свой статус!", NamedTextColor.RED)
                )
            }
        // If sender is OP or console
        } else if (!(sender is Player) || sender.hasPermission("status.admin") == true) {
            val mode = args[0]
            val name = args[1]
            val player: Player? = plugin.server.getPlayer(name)
            if (player == null) {
                sender.sendMessage("Игрок не найден!")
                plugin.logger.info("Игрок не найден!")
                return true
            }
            applyStatusUser(player, mode)
        }
        return true
    }

    // MUST BE USED BY PLAYERS THEMSELVES
    fun applyStatusUser(player: Player, mode: String) {

        // Remove old status permissions
        if (player.hasPermission("status.peace")) {
            perms.playerRemove(player, "status.peace")
        }
        if (player.hasPermission("status.pvp")) {
            perms.playerRemove(player, "status.pvp")
        }

        // Add new status permission
        perms.playerAdd(player, "status.$mode")
        plugin.logger.info("Set $mode status for ${player.name}!")

        if (player.isOnline) {
        // SXF, VFX and text
        val statusText = when (mode) {
            "pvp" -> Component.text("PvP", NamedTextColor.RED)
            "peace" -> Component.text("Мирный", NamedTextColor.AQUA)
            else -> Component.text(mode, NamedTextColor.GRAY)
        }

        val vfx: Particle = when (mode) {
            "pvp" -> Particle.FLAME
            "peace" -> Particle.SOUL_FIRE_FLAME
            else -> Particle.PALE_OAK_LEAVES
        }

        val sfx: Sound = when (mode) {
            "pvp" -> Sound.ENTITY_WARDEN_ROAR
            "peace" -> Sound.BLOCK_BEACON_ACTIVATE
            else -> Sound.BLOCK_PALE_HANGING_MOSS_IDLE
        }

        // Title and chat message
        val title = Title.title(
            Component.text("Статус", NamedTextColor.GREEN),
            statusText, // используем тот же компонент, что и для чата
            Title.Times.times(
                java.time.Duration.ofMillis(500),
                java.time.Duration.ofSeconds(3),
                java.time.Duration.ofMillis(500)
            )
        )
        val msg = Component.text("Статус ", NamedTextColor.GREEN).append(statusText)

        // Notify the user using chat and title
        player.sendMessage(msg)
        player.showTitle(title)
        player.playSound(player.location, sfx, 1.0f, 1.0f)
        player.world.spawnParticle(
            vfx,
            player.location,
            1000,
            3.0, 3.0, 3.0
        )
        }
    }
}