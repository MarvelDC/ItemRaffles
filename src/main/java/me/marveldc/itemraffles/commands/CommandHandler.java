package me.marveldc.itemraffles.commands;

import me.marveldc.itemraffles.ItemRaffles;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CommandHandler implements TabExecutor {
    private final ArrayList<SubCommand> subCommands = new ArrayList<>();

    public CommandHandler(ItemRaffles plugin) {
        this.subCommands.add(new ReloadCommand(plugin));
        this.subCommands.add(new CreateCommand(plugin));
        this.subCommands.add(new EnterCommand(plugin));
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (strings.length == 0) {
            this.getCommand("raffle").ifPresent(subCommand -> subCommand.run(commandSender, strings));
            return true;
        }

        final Optional<SubCommand> subCommand = this.getCommand(strings[0]);
        if (subCommand.isPresent()) {
            subCommand.get().run(commandSender, strings);
        } else {
            this.getCommand("raffle").ifPresent(subCmd -> subCmd.run(commandSender, strings));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (strings.length != 1) return null;

        return this.subCommands.stream().filter(subCommand ->
                subCommand.getPermission() == null || commandSender.hasPermission(subCommand.getPermission()))
                .map(SubCommand::getName).collect(Collectors.toList());
    }

    private Optional<SubCommand> getCommand(String name) {
        return this.subCommands.stream().filter(subCommand -> subCommand.getName().equalsIgnoreCase(name)).findFirst();
    }
}