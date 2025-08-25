package me.ggym

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class StatusCommand : CommandExecutor {
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        val target: Player? = if (args.isNotEmpty()) {
            Bukkit.getPlayer(args[0])
        } else {
            if (sender is Player) sender else null
        }

        if (target == null) {
            sender.sendMessage(
                Component.text("Команда применима только к игрокам!", NamedTextColor.RED)
            )
            return true
        }

        val status = when {
            target.hasPermission("status.peaceful") -> "Мирный"
            target.hasPermission("status.pvp") -> "PvP"
            else -> "не задан"
        }

        if (sender == target) {
            sender.sendMessage(
                Component.text("Твой статус: ", NamedTextColor.GREEN)
                    .append(Component.text(status, NamedTextColor.YELLOW))
            )
        } else {
            sender.sendMessage(
                Component.text("Статус ${target.name}: ", NamedTextColor.GREEN)
                    .append(Component.text(status, NamedTextColor.YELLOW))
            )
        }

        return true
    }
}
