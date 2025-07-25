package github.nighter.smartspawner.sellwands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SellwandCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length == 0) {
            commandSender.sendMessage("Usage: /sellwand <uses/infinite> <multiplier>");
            return true;
        }

        if (strings.length < 2) {
            commandSender.sendMessage("You must provide both uses (or 'infinite') and a multiplier.");
            return true;
        }

        if (!(commandSender instanceof Player)){
            return false;
        }
        Player player = (Player) commandSender;

        if (!player.isOp()) {
            commandSender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        try {
            int uses;
            if (strings[0].equalsIgnoreCase("infinite")) {
                uses = -1;
            } else {
                uses = Integer.parseInt(strings[0]);
            }

            float multiplier = Float.parseFloat(strings[1]);

            SellwandManager sellwandManager = new SellwandManager(new ItemStack(Material.BLAZE_ROD));

            player.getInventory().addItem(sellwandManager.getSellwand(uses, multiplier));

            String usesText = uses == -1 ? "infinite" : String.valueOf(uses);
            commandSender.sendMessage("Sellwand updated with " + usesText + " uses and a multiplier of " + multiplier);
        } catch (NumberFormatException e) {
            commandSender.sendMessage("Invalid number format. Please provide valid integers for uses (or 'infinite') and a float for multiplier.");
        }

        return true;
    }
}
