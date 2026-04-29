package com.zavtrak.pets;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Handles the /pet command and its subcommands.
 */
public final class PetCommand implements TabExecutor {

    private static final List<String> SUB_COMMANDS = Arrays.asList(
            "remove", "name", "list", "info");

    private final PetsPlugin plugin;

    public PetCommand(PetsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use /pet.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String head = args[0].toLowerCase(Locale.ROOT);
        switch (head) {
            case "remove" -> handleRemove(player);
            case "list" -> handleList(player);
            case "info" -> handleInfo(player);
            case "name" -> handleName(player, args);
            default -> handleSummon(player, head);
        }
        return true;
    }

    private void handleSummon(Player player, String typeName) {
        var typeOpt = PetType.match(typeName);
        if (typeOpt.isEmpty()) {
            player.sendMessage(Component.text("Unknown pet type: " + typeName, NamedTextColor.RED));
            sendUsage(player);
            return;
        }
        Pet pet = plugin.getPetManager().summon(player, typeOpt.get());
        player.sendMessage(Component.text("Summoned a ", NamedTextColor.GREEN)
                .append(Component.text(pet.getType().displayName(), NamedTextColor.AQUA))
                .append(Component.text(" companion.", NamedTextColor.GREEN)));
    }

    private void handleRemove(Player player) {
        boolean removed = plugin.getPetManager().removeFor(player);
        if (removed) {
            player.sendMessage(Component.text("Your pet has been dismissed.", NamedTextColor.YELLOW));
        } else {
            player.sendMessage(Component.text("You don't have an active pet.", NamedTextColor.RED));
        }
    }

    private void handleList(Player player) {
        String types = String.join(", ",
                Stream.of(PetType.values()).map(PetType::displayName).toList());
        player.sendMessage(Component.text("Available pets: ", NamedTextColor.GOLD)
                .append(Component.text(types, NamedTextColor.AQUA)));
    }

    private void handleInfo(Player player) {
        plugin.getPetManager().getPet(player).ifPresentOrElse(pet -> {
            var loc = pet.getEntity().getLocation();
            player.sendMessage(Component.text("Active pet: ", NamedTextColor.GOLD)
                    .append(Component.text(pet.getType().displayName(), NamedTextColor.AQUA))
                    .append(Component.text(" @ " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(),
                            NamedTextColor.GRAY)));
        }, () -> player.sendMessage(Component.text("You don't have an active pet.", NamedTextColor.RED)));
    }

    private void handleName(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /pet name <new name>", NamedTextColor.RED));
            return;
        }
        var petOpt = plugin.getPetManager().getPet(player);
        if (petOpt.isEmpty()) {
            player.sendMessage(Component.text("You don't have an active pet to rename.", NamedTextColor.RED));
            return;
        }
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (name.length() > 32) {
            player.sendMessage(Component.text("Pet names must be 32 characters or fewer.", NamedTextColor.RED));
            return;
        }
        plugin.getPetManager().rename(petOpt.get(), name);
        player.sendMessage(Component.text("Pet renamed to ", NamedTextColor.GREEN)
                .append(Component.text(name, NamedTextColor.AQUA)));
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("Pets commands:", NamedTextColor.GOLD));
        player.sendMessage(Component.text("  /pet <type> - summon a pet", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  /pet remove - dismiss your pet", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  /pet name <text> - rename your pet", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  /pet list - list pet types", NamedTextColor.GRAY));
        player.sendMessage(Component.text("  /pet info - show your active pet", NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String label,
                                      @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> options = new ArrayList<>();
            for (PetType type : PetType.values()) {
                options.add(type.displayName());
            }
            options.addAll(SUB_COMMANDS);
            List<String> matches = new ArrayList<>();
            for (String option : options) {
                if (option.startsWith(prefix)) {
                    matches.add(option);
                }
            }
            Collections.sort(matches);
            return matches;
        }
        return Collections.emptyList();
    }
}
