package me.ggym

import org.bukkit.plugin.java.JavaPlugin
import net.milkbowl.vault.permission.Permission

class peacekeeper : JavaPlugin() {

    lateinit var perms: Permission

    override fun onEnable() {


        val rsp = server.servicesManager.getRegistration(Permission::class.java)
        if (rsp != null) {
            perms = rsp.provider
        } else {
            logger.severe("Vault или Permission плагин не найден!")
            server.pluginManager.disablePlugin(this)
        }

        logger.info("GGym Peacekeeper startup...")
        getCommand("status")?.setExecutor(StatusCommand())
        getCommand("setstatus")?.setExecutor(SetstatusCommand(perms, this))
    }


    override fun onDisable() {
        // Plugin shutdown logic
    }
}
