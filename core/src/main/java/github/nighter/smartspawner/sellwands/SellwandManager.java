package github.nighter.smartspawner.sellwands;

import github.nighter.smartspawner.SmartSpawner;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class SellwandManager {

    private ItemStack item;

    public SellwandManager(ItemStack item) {
        this.item = item;
    }

    private ItemStack setNbt(Integer uses, Float multi) {
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer container = meta.getPersistentDataContainer();

        NamespacedKey usesKey = new NamespacedKey(SmartSpawner.getInstance(), "uses");
        container.set(usesKey, PersistentDataType.INTEGER, uses);

        NamespacedKey multiKey = new NamespacedKey(SmartSpawner.getInstance(), "multi");
        container.set(multiKey, PersistentDataType.FLOAT, multi);

        NamespacedKey sellwandKey = new NamespacedKey(SmartSpawner.getInstance(), "sellwand");
        container.set(sellwandKey, PersistentDataType.BOOLEAN, true);

        item.setItemMeta(meta);
        return item;
    }

    public Integer getUses(ItemStack item) {
        if (item == null) return null;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey usesKey = new NamespacedKey(SmartSpawner.getInstance(), "uses");
        return container.get(usesKey, PersistentDataType.INTEGER);
    }

    public Float getMulti(ItemStack item) {
        if (item == null) return null;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey multiKey = new NamespacedKey(SmartSpawner.getInstance(), "multi");
        return container.get(multiKey, PersistentDataType.FLOAT);
    }

    public boolean isSellwand(ItemStack item) {
        if (item == null) return false;
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey sellwandKey = new NamespacedKey(SmartSpawner.getInstance(), "sellwand");
        Boolean value = container.get(sellwandKey, PersistentDataType.BOOLEAN);
        return value != null && value;
    }

    public Boolean sellWandUse(){
        if (item == null) return false;

        Integer uses = getUses(item);
        if (uses == null) return false;

        // Special case for infinite uses (indicated by -1)
        if (uses == -1) {
            return true;
        }

        // Regular usage for limited-use wands
        if (uses <= 0) return false;

        uses--;

        if (uses <= 0) {
            return false;
        }

        setNbt(uses, getMulti(item));

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Sellwand");
            meta.setLore(List.of("§7Right click to sell items",
                    "§7Uses: " + uses,
                    "§7Multiplier: " + getMulti(item) + "x"));
            item.setItemMeta(meta);
        }
        return true;
    }

    public ItemStack getSellwand(Integer uses, Float multi) {
        item = setNbt(uses, multi);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        meta.setDisplayName("§6Sellwand");
        if (uses == -1) {
            meta.setLore(List.of(
                    "§eRight-click to instantly sell all items in the chest!",
                    "§7Uses: §f∞",
                    "§7Current multiplier: §b" + getMulti(item) + "x",
                    "",
                    "§8A powerful tool for quick selling."
            ));
        }else {
            meta.setLore(List.of(
                    "§eRight-click to instantly sell all items in the chest!",
                    "§7Uses left: §f" + uses,
                    "§7Current multiplier: §b" + getMulti(item) + "x",
                    "",
                    "§8A powerful tool for quick selling."
            ));
        }

        item.setItemMeta(meta);

        return item;

    }
}
