package me.ggym

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import net.milkbowl.vault.permission.Permission
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID


class SetstatusCommand(private val perms: Permission, private val plugin: JavaPlugin) : CommandExecutor, TabCompleter{

    private val pendingTasks = mutableMapOf<UUID, Int>()
    private val taskStartTimes = mutableMapOf<UUID, Long>()
    val delayTicks = 20L * plugin.config.getInt("delaySeconds")

     override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        if (args.size < 1 || args.size > 3) {
            sender.sendMessage("/setstatus <PvP/Peace>")
            return true
        }

        val mode: String = args[0]

        if (mode != "pvp" && mode != "peace") {
            sender.sendMessage("/setstatus <PvP/Peace>")
            return true
        }

        // Если sender это игрок
        if (sender is Player && !sender.hasPermission("status.admin")) {
            sender.sendMessage(Component.text("edit: ${sender.hasPermission("status.edit")}", NamedTextColor.RED))
            sender.sendMessage(Component.text("conversion: ${sender.hasPermission("status.conversion")}", NamedTextColor.RED))
            sender.sendMessage(Component.text("peace: ${sender.hasPermission("status.peace")}", NamedTextColor.RED))
            sender.sendMessage(Component.text("pvp: ${sender.hasPermission("status.pvp")}", NamedTextColor.RED))
            if (sender.hasPermission("status.edit")) {

                val currentMode: String = when {
                    sender.hasPermission("status.pvp") -> "pvp"
                    sender.hasPermission("status.peace") -> "peace"
                    else -> "unset"
                }

                if (mode == currentMode) {
                    sender.sendMessage(Component.text("У вас уже такой статус!", NamedTextColor.RED))
                    return true
                }

                sender.sendMessage(Component.text("Скоро статус будет изменен! Чтобы узнать, сколько осталось ждать введите команду заново.", NamedTextColor.LIGHT_PURPLE))
                sender.playSound(sender.location, Sound.BLOCK_AMETHYST_BLOCK_STEP, 1.0f, 1.0f)

                perms.playerRemove(sender, "status.edit")
                perms.playerAdd(sender, "status.conversion")



                val taskId = plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    applyStatusUser(sender, mode)
                    pendingTasks.remove(sender.uniqueId)
                    taskStartTimes.remove(sender.uniqueId)
                }, delayTicks).taskId

                pendingTasks[sender.uniqueId] = taskId
                taskStartTimes[sender.uniqueId] = System.currentTimeMillis() + delayTicks * 50

            } else if (sender.hasPermission("status.conversion")) {
                val endTime = taskStartTimes[sender.uniqueId] ?: return true
                val remaining = endTime - System.currentTimeMillis()
                val seconds = remaining / 1000
                val minutes = remaining / 1000 / 60
                val hours = remaining / 1000 / 60 / 60
                sender.sendMessage(Component.text("Изменения вступят в силу через ${hours}:${minutes}:${seconds}", NamedTextColor.LIGHT_PURPLE))

            }
            else {
                sender.sendMessage(Component.text("У вас нет права задавать свой статус!", NamedTextColor.RED))
            }
        // Если sender это консоль или админ
        } else if (!(sender is Player) || sender.hasPermission("status.admin")) {
            val name = args[1]
            val player: Player? = plugin.server.getPlayer(name)
            if (player == null) {
                sender.sendMessage("Игрок не найден!")
                plugin.logger.info("Игрок не найден!")
                return true
            }

            if (player.hasPermission("status.conversion")) {
                val taskId = pendingTasks[player.uniqueId] ?: return true
                plugin.server.scheduler.cancelTask(taskId)
                pendingTasks.remove(player.uniqueId)
                taskStartTimes.remove(player.uniqueId)
            }
            applyStatusUser(player, mode)
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        val completions = mutableListOf<String>()

        when (args.size) {
            1 -> {
                // Автодополнение для первого аргумента (pvp/peace)
                completions.addAll(listOf("pvp", "peace"))

                // Фильтрация по введенному тексту
                return completions.filter {
                    it.startsWith(args[0].lowercase(), ignoreCase = true)
                }.sorted()
            }

            2 -> {
                // Автодополнение для второго аргумента (никнеймы)
                // Проверяем, есть ли у отправителя права администратора
                val hasAdminPermission = sender.hasPermission("status.admin") || sender is Player && sender.isOp

                if (hasAdminPermission) {
                    // Для админов показываем всех онлайн-игроков
                    Bukkit.getOnlinePlayers().forEach { player ->
                        completions.add(player.name)
                    }
                } else {
                    // Для обычных пользователей показываем только их самих
                    if (sender is Player) {
                        completions.add(sender.name)
                    }
                }

                // Фильтрация по введенному тексту
                return completions.filter {
                    it.startsWith(args[1], ignoreCase = true)
                }.sorted()
            }

            else -> return emptyList()
        }
    }

    // Задаёт статус игрока
    fun applyStatusUser(player: Player, mode: String) {

        // Убираем прошлые статусы, если такие были
        if (player.hasPermission("status.peace")) {
            perms.playerRemove(player, "status.peace")
        }
        if (player.hasPermission("status.pvp")) {
            perms.playerRemove(player, "status.pvp")
        }

        if (player.hasPermission("status.conversion")) {
            perms.playerRemove(player, "status.conversion")
            perms.playerAdd(player, "status.edit")
        }

        // Добавляем новый статус
        perms.playerAdd(player, "status.$mode")
        plugin.logger.info("Set $mode status for ${player.name}!")

        if (player.isOnline) {
        // SXF, VFX и текст
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

        // Создаём title и сообщение
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

        // Уведомляем пользователя
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