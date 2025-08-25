package me.ggym

import org.bukkit.plugin.java.JavaPlugin

class peacekeeper : JavaPlugin() {

    override fun onEnable() {
        // Plugin startup logic
        logger.info("GGym Peacekeeper startup...");
        getCommand("status")?.setExecutor(StatusCommand())

    }


    override fun onDisable() {
        // Plugin shutdown logic
    }
}
