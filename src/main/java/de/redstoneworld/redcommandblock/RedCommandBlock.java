package de.redstoneworld.redcommandblock;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class RedCommandBlock extends JavaPlugin implements Listener {

    private Set<String> defaultWhitelist;
    private Multimap<String, String> commandWhitelist;

    @Override
    public void onEnable() {
        loadConfig();
        getCommand("redcommandblock").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
    }

    public void loadConfig() {
        saveDefaultConfig();
        reloadConfig();

        defaultWhitelist = getConfig().getStringList("command-whitelist.default").stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        commandWhitelist = MultimapBuilder.hashKeys().hashSetValues().build();
        ConfigurationSection worldsSection = getConfig().getConfigurationSection("command-whitelist.worlds");
        if (worldsSection != null) {
            for (String key : worldsSection.getKeys(false)) {
                for (String command : worldsSection.getStringList(key)) {
                    commandWhitelist.put(key.toLowerCase(), command.toLowerCase());
                }
            }
        }
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0) {
            if ("reload".equalsIgnoreCase(args[0]) && sender.hasPermission("rwm.commandblock.command.reload")) {
                loadConfig();
                sender.sendMessage(ChatColor.YELLOW + "Config reloaded!");
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onCommand(ServerCommandEvent event) {
        if (event.getSender() instanceof BlockCommandSender) {
            Block block = ((BlockCommandSender) event.getSender()).getBlock();
            handle(event, event.getCommand(), block.getType().getKey().getKey(), block.getLocation());
        } else if (event.getSender() instanceof Entity && !(event.getSender() instanceof Player)) {
            Entity entity = ((Entity) event.getSender());
            handle(event, event.getCommand(), entity.getType().getKey().getKey(), entity.getLocation());
        }
    }

    private void handle(Cancellable event, String command, String name, Location location) {
        while (command.startsWith("/")) {
            command = command.substring(1);
        }
        command = command.split(" ")[0];

        Collection<String> whitelist = commandWhitelist.containsKey(location.getWorld().getName())
                ? commandWhitelist.get(location.getWorld().getName())
                : defaultWhitelist;

        if (!whitelist.contains(command.toLowerCase())) {
            event.setCancelled(true);
            getLogger().log(Level.INFO, name + " at ["
                    + location.getWorld() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ()
                    + "] tried to execute non-whitelisted command '" + command + "'");
        }
    }
}
