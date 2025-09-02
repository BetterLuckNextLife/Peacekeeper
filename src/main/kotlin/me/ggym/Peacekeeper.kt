package me.ggym

import net.milkbowl.vault.permission.Permission
import org.bukkit.plugin.java.JavaPlugin

class Peacekeeper : JavaPlugin() {

    lateinit var perms: Permission

    override fun onEnable() {
        saveDefaultConfig()
        logger.info("Запуск Peacekeeper...")


        val rsp = server.servicesManager.getRegistration(Permission::class.java)
        if (rsp != null) {
            perms = rsp.provider
        } else {
            logger.severe("Vault не найден!")
            server.pluginManager.disablePlugin(this)
            return
        }

        getCommand("status")?.setExecutor(StatusCommand())
        getCommand("setstatus")?.setExecutor(SetstatusCommand(perms, this))
    }

    override fun onDisable() {
        logger.info("Остановка shutdown...")
    }
}
