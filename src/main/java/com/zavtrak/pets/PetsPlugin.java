package com.zavtrak.pets;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point for the Pets plugin.
 *
 * <p>Wires the {@link PetManager}, the {@link PetCommand} executor, and the
 * {@link PetListener} together when the plugin is enabled, and tears them
 * down cleanly when the server shuts down.</p>
 */
public final class PetsPlugin extends JavaPlugin {

    private PetManager petManager;

    @Override
    public void onEnable() {
        this.petManager = new PetManager(this);
        this.petManager.start();

        PluginCommand command = getCommand("pet");
        if (command == null) {
            getLogger().severe("/pet command is not declared in plugin.yml; disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        PetCommand executor = new PetCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        getServer().getPluginManager().registerEvents(new PetListener(this), this);

        getLogger().info("Pets plugin enabled.");
    }

    @Override
    public void onDisable() {
        if (petManager != null) {
            petManager.shutdown();
            petManager = null;
        }
        getLogger().info("Pets plugin disabled.");
    }

    public PetManager getPetManager() {
        return petManager;
    }
}
