package github.nighter.smartspawner.sellwands;

import github.nighter.smartspawner.SmartSpawner;
import github.nighter.smartspawner.language.LanguageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class SellwandManager {

    private static final String KEY_USES = "uses";
    private static final String KEY_MAX_USES = "maxUses";
    private static final String KEY_MULTI = "multi";
    private static final String KEY_SELLWAND = "sellwand";
    private static final String KEY_AMOUNT_SOLD = "amountSold";
    private static final String KEY_MONEY_MADE = "moneyMade";

    private final NamespacedKey usesKey;
    private final NamespacedKey maxUsesKey;
    private final NamespacedKey multiKey;
    private final NamespacedKey sellwandKey;
    private final NamespacedKey amountSoldKey;
    private final NamespacedKey moneyMadeKey;

    private final ItemStack item;
    private final LanguageManager languageManager;
    private final MiniMessage mm;


    public SellwandManager(ItemStack item) {
        this.item = item;
        SmartSpawner spawner = SmartSpawner.getInstance();
        this.languageManager = spawner.getLanguageManager();
        this.mm = MiniMessage.miniMessage();

        // Initialize all NamespacedKey objects once
        this.usesKey = new NamespacedKey(spawner, KEY_USES);
        this.maxUsesKey = new NamespacedKey(spawner, KEY_MAX_USES);
        this.multiKey = new NamespacedKey(spawner, KEY_MULTI);
        this.sellwandKey = new NamespacedKey(spawner, KEY_SELLWAND);
        this.amountSoldKey = new NamespacedKey(spawner, KEY_AMOUNT_SOLD);
        this.moneyMadeKey = new NamespacedKey(spawner, KEY_MONEY_MADE);
    }


    private ItemStack setNbt(int uses, float multi, long amountSold, double moneyMade) {
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer container = meta.getPersistentDataContainer();

        // Set uses and maxUses
        container.set(usesKey, PersistentDataType.INTEGER, uses);
        if (!container.has(maxUsesKey, PersistentDataType.INTEGER) && uses != -1) {
            container.set(maxUsesKey, PersistentDataType.INTEGER, uses);
        }

        // Set multiplier
        container.set(multiKey, PersistentDataType.FLOAT, multi);

        // Mark as sellwand
        container.set(sellwandKey, PersistentDataType.BOOLEAN, true);

        // Update amount sold (cumulative)
        Long prevAmountSold = container.getOrDefault(amountSoldKey, PersistentDataType.LONG, 0L);
        container.set(amountSoldKey, PersistentDataType.LONG, prevAmountSold + amountSold);

        // Update money made (cumulative)
        Double prevMoneyMade = container.getOrDefault(moneyMadeKey, PersistentDataType.DOUBLE, 0.0);
        container.set(moneyMadeKey, PersistentDataType.DOUBLE, prevMoneyMade + moneyMade);

        item.setItemMeta(meta);
        return item;
    }

    public Integer getUses(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(usesKey, PersistentDataType.INTEGER);
    }

    public Float getMulti(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(multiKey, PersistentDataType.FLOAT);
    }

    public boolean isSellwand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        Boolean value = item.getItemMeta().getPersistentDataContainer()
                .get(sellwandKey, PersistentDataType.BOOLEAN);
        return Boolean.TRUE.equals(value);
    }

    public boolean sellWandUse(long amountSold, double moneyMade) {
        if (item == null) return false;

        Integer uses = getUses(item);
        if (uses == null) return false;

        Float multi = getMulti(item);
        if (multi == null) return false;

        // Handle infinite uses case
        if (uses == -1) {
            setNbt(uses, multi, amountSold, moneyMade);
            updateDisplayAndLore(uses, multi);
            return true;
        }

        // Handle limited uses case
        if (uses <= 0) return false;

        // Decrement uses and check if depleted
        uses--;
        if (uses <= 0) {
            return false;
        }

        // Update NBT data and display
        setNbt(uses, multi, amountSold, moneyMade);
        updateDisplayAndLore(uses, multi);
        return true;
    }


    private void updateDisplayAndLore(int uses, float multi) {
        getSellwand(uses, multi, getAmountSold(item), getMoneyMade(item));
    }


    private List<Component> buildSellwandLore(Integer uses, Float multi, long amountSold, double moneyMade) {
        String usesDisplay = (uses != null) ? (uses == -1 ? "∞" : String.valueOf(uses)) : "N/A";
        String multiDisplay = (multi != null) ? String.valueOf(multi) : "N/A";

        return List.of(
            mm.deserialize("<!italic><gray>"),
            mm.deserialize("<!italic><color:#FA1065>ɪɴꜰᴏ"),
            mm.deserialize("<!italic><dark_gray><bold>▍<!bold> <white>Multiplier: <color:#FA1065>" + multiDisplay + "x"),
            mm.deserialize("<!italic><dark_gray><bold>▍<!bold> <white>Uses: <color:#FA1065>" + usesDisplay),
            mm.deserialize("<!italic><gray>"),
            mm.deserialize("<!italic><color:#FA1065>ꜱᴛᴀᴛꜱ"),
            mm.deserialize("<!italic><dark_gray><bold>▍<!bold> <white>Sold Items: <color:#FA1065>" + languageManager.formatNumber(amountSold)),
            mm.deserialize("<!italic><dark_gray><bold>▍<!bold> <white>Money made: <color:#FA1065>" + languageManager.formatNumber(moneyMade) + "$"),
            mm.deserialize("<!italic><gray>"),
            mm.deserialize("<!italic><color:#FA1065><bold>><!bold> <color:#FA1065>Right Click <dark_gray>- <color:#FA1065>Sell!")
        );
    }

    public ItemStack getSellwand(Integer uses, Float multi, Long amountSold, Double moneyMade) {
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer container = meta.getPersistentDataContainer();

        // Update uses and maxUses if provided
        if (uses != null) {
            container.set(usesKey, PersistentDataType.INTEGER, uses);
            if (!container.has(maxUsesKey, PersistentDataType.INTEGER) && uses != -1) {
                container.set(maxUsesKey, PersistentDataType.INTEGER, uses);
            }
        }

        // Update multiplier if provided
        if (multi != null) {
            container.set(multiKey, PersistentDataType.FLOAT, multi);
        }

        // Ensure the sellwand flag is set
        container.set(sellwandKey, PersistentDataType.BOOLEAN, true);

        // Calculate percentage display if applicable
        String percentageStr = "";
        if (uses != null && uses > 0) {
            Integer maxUses = getMaxUses(item);
            if (maxUses != null && maxUses > 0) {
                int percent = (int) Math.round((uses * 100.0) / maxUses);
                percentageStr = "(" + percent + "%)";
            }
        }

        // Use provided values or get from item
        long displayAmountSold = (amountSold != null) ? amountSold : getAmountSold(item);
        double displayMoneyMade = (moneyMade != null) ? moneyMade : getMoneyMade(item);

        // Update display name and lore
        Component displayName = mm.deserialize("<!italic><color:#FA1065><bold>ꜱᴘᴀᴡɴᴇʀ ꜱᴇʟʟᴡᴀɴᴅ <gray><!bold>" + percentageStr);
        meta.displayName(displayName);
        meta.lore(buildSellwandLore(uses, multi, displayAmountSold, displayMoneyMade));

        item.setItemMeta(meta);
        return item;
    }

    private Integer getMaxUses(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(maxUsesKey, PersistentDataType.INTEGER);
    }

    public long getAmountSold(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0L;

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.getOrDefault(amountSoldKey, PersistentDataType.LONG, 0L);
    }

    public double getMoneyMade(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0.0;

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.getOrDefault(moneyMadeKey, PersistentDataType.DOUBLE, 0.0);
    }
}
