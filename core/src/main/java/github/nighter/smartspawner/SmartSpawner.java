package github.nighter.smartspawner;

import github.nighter.smartspawner.api.SmartSpawnerAPI;
import github.nighter.smartspawner.api.SmartSpawnerPlugin;
import github.nighter.smartspawner.api.SmartSpawnerAPIImpl;
import github.nighter.smartspawner.bstats.Metrics;
import github.nighter.smartspawner.commands.CommandHandler;
import github.nighter.smartspawner.commands.give.GiveCommand;
import github.nighter.smartspawner.commands.hologram.HologramCommand;
import github.nighter.smartspawner.commands.list.ListCommand;
import github.nighter.smartspawner.commands.list.SpawnerListGUI;
import github.nighter.smartspawner.commands.list.UserPreferenceCache;
import github.nighter.smartspawner.commands.reload.ReloadCommand;
import github.nighter.smartspawner.sellwands.SellwandCommand;
import github.nighter.smartspawner.spawner.natural.NaturalSpawnerListener;
import github.nighter.smartspawner.utils.TimeFormatter;
import github.nighter.smartspawner.hooks.economy.ItemPriceManager;
import github.nighter.smartspawner.hooks.economy.shops.providers.shopguiplus.SpawnerProvider;
import github.nighter.smartspawner.extras.HopperHandler;
import github.nighter.smartspawner.hooks.IntegrationManager;
import github.nighter.smartspawner.language.MessageService;
import github.nighter.smartspawner.migration.SpawnerDataMigration;
import github.nighter.smartspawner.spawner.gui.layout.GuiLayoutConfig;
import github.nighter.smartspawner.spawner.gui.main.ItemCache;
import github.nighter.smartspawner.spawner.gui.main.SpawnerMenuAction;
import github.nighter.smartspawner.spawner.gui.main.SpawnerMenuUI;
import github.nighter.smartspawner.spawner.gui.stacker.SpawnerStackerHandler;
import github.nighter.smartspawner.spawner.gui.storage.filter.FilterConfigUI;
import github.nighter.smartspawner.spawner.gui.synchronization.SpawnerGuiViewManager;
import github.nighter.smartspawner.spawner.gui.stacker.SpawnerStackerUI;
import github.nighter.smartspawner.spawner.gui.storage.SpawnerStorageUI;
import github.nighter.smartspawner.spawner.gui.storage.SpawnerStorageAction;
import github.nighter.smartspawner.spawner.interactions.click.SpawnerClickManager;
import github.nighter.smartspawner.spawner.interactions.destroy.SpawnerBreakListener;
import github.nighter.smartspawner.spawner.interactions.destroy.SpawnerExplosionListener;
import github.nighter.smartspawner.spawner.interactions.place.SpawnerPlaceListener;
import github.nighter.smartspawner.spawner.interactions.stack.SpawnerStackHandler;
import github.nighter.smartspawner.spawner.interactions.type.SpawnEggHandler;
import github.nighter.smartspawner.spawner.item.SpawnerItemFactory;
import github.nighter.smartspawner.spawner.limits.ChunkSpawnerLimiter;
import github.nighter.smartspawner.spawner.loot.EntityLootRegistry;
import github.nighter.smartspawner.spawner.lootgen.SpawnerRangeChecker;
import github.nighter.smartspawner.spawner.properties.SpawnerManager;
import github.nighter.smartspawner.spawner.sell.SpawnerSellManager;
import github.nighter.smartspawner.spawner.utils.SpawnerFileHandler;
import github.nighter.smartspawner.spawner.utils.SpawnerMobHeadTexture;
import github.nighter.smartspawner.spawner.lootgen.SpawnerLootGenerator;
import github.nighter.smartspawner.language.LanguageManager;
import github.nighter.smartspawner.updates.ConfigUpdater;
import github.nighter.smartspawner.nms.VersionInitializer;
import github.nighter.smartspawner.updates.LanguageUpdater;
import github.nighter.smartspawner.updates.UpdateChecker;
import github.nighter.smartspawner.utils.SpawnerTypeChecker;

import lombok.Getter;
import lombok.experimental.Accessors;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Level;

@Getter
@Accessors(chain = false)
public class SmartSpawner extends JavaPlugin implements SmartSpawnerPlugin {
    @Getter
    private static SmartSpawner instance;
    public final int DATA_VERSION = 3;
    private final boolean debugMode = getConfig().getBoolean("debug", false);

    // Integration Manager
    private IntegrationManager integrationManager;

    // Services
    private TimeFormatter timeFormatter;
    private ConfigUpdater configUpdater;
    private LanguageManager languageManager;
    private LanguageUpdater languageUpdater;
    private MessageService messageService;

    // Factories
    private SpawnerItemFactory spawnerItemFactory;

    // Core UI components
    private GuiLayoutConfig guiLayoutConfig;
    private final ItemCache itemCache = new ItemCache(500, 30);
    private SpawnerMenuUI spawnerMenuUI;
    private SpawnerStorageUI spawnerStorageUI;
    private FilterConfigUI filterConfigUI;
    private SpawnerStackerUI spawnerStackerUI;

    // Core handlers
    private SpawnEggHandler spawnEggHandler;
    private SpawnerClickManager spawnerClickManager;
    private SpawnerStackHandler spawnerStackHandler;

    // UI actions
    private SpawnerMenuAction spawnerMenuAction;
    private SpawnerStackerHandler spawnerStackerHandler;
    private SpawnerStorageAction spawnerStorageAction;
    private SpawnerSellManager spawnerSellManager;

    // Core managers
    private SpawnerFileHandler spawnerFileHandler;
    private SpawnerManager spawnerManager;
    private HopperHandler hopperHandler;

    // Event handlers and utilities
    private NaturalSpawnerListener naturalSpawnerListener;
    private SpawnerLootGenerator spawnerLootGenerator;
    private SpawnerListGUI spawnerListGUI;
    private SpawnerRangeChecker rangeChecker;
    private ChunkSpawnerLimiter chunkSpawnerLimiter;
    private SpawnerGuiViewManager spawnerGuiViewManager;
    private SpawnerExplosionListener spawnerExplosionListener;
    private SpawnerBreakListener spawnerBreakListener;
    private SpawnerPlaceListener spawnerPlaceListener;
    private ItemPriceManager itemPriceManager;
    private EntityLootRegistry entityLootRegistry;
    private UpdateChecker updateChecker;

    // Set up commands
    private CommandHandler commandHandler;
    private ReloadCommand reloadCommand;
    private GiveCommand giveCommand;
    private UserPreferenceCache userPreferenceCache;
    private ListCommand listCommand;
    private HologramCommand hologramCommand;

    // API implementation
    private SmartSpawnerAPIImpl apiImpl;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        instance = this;

        // Initialize version-specific components
        initializeVersionComponents();

        // Initialize plugin integrations
        this.integrationManager = new IntegrationManager(this);
        integrationManager.initializeIntegrations();

        //sellwand command reg
        getServer().getPluginCommand("smartSellwand").setExecutor(new SellwandCommand());

        // Check for data migration needs
        migrateDataIfNeeded();

        // Initialize core components
        initializeComponents();

        // Setup plugin infrastructure
        setupCommand();
        setupBtatsMetrics();
        registerListeners();

        long loadTime = System.currentTimeMillis() - startTime;
        getLogger().info("SmartSpawner has been enabled! (Loaded in " + loadTime + "ms)");
    }

    @Override
    public SmartSpawnerAPI getAPI() {
        return apiImpl;
    }

    private void initializeVersionComponents() {
        try {
            new VersionInitializer(this).initialize();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize version-specific components", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void migrateDataIfNeeded() {
        SpawnerDataMigration migration = new SpawnerDataMigration(this);
        if (migration.checkAndMigrateData()) {
            getLogger().info("Data migration completed. Loading with new format...");
        }
    }

    private void initializeComponents() {
        // Initialize services
        initializeServices();

        // Initialize factories and economy
        initializeEconomyComponents();

        // Initialize core components in order
        initializeCoreComponents();

        // Initialize handlers
        initializeHandlers();

        // Initialize UI and actions
        initializeUIAndActions();

        // Initialize hopper handler if enabled in config
        setUpHopperHandler();

        // Initialize listeners
        initializeListeners();

        // Initialize API implementation
        this.apiImpl = new SmartSpawnerAPIImpl(this);
        this.updateChecker = new UpdateChecker(this);
    }

    private void initializeServices() {
        SpawnerTypeChecker.init(this);
        this.timeFormatter = new TimeFormatter(this);
        this.configUpdater = new ConfigUpdater(this);
        configUpdater.checkAndUpdateConfig();
        this.languageManager = new LanguageManager(this);
        this.languageUpdater = new LanguageUpdater(this);
        this.messageService = new MessageService(this, languageManager);
    }

    private void initializeEconomyComponents() {
        this.itemPriceManager = new ItemPriceManager(this);
        this.itemPriceManager.init();
        this.entityLootRegistry = new EntityLootRegistry(this, itemPriceManager);
        this.spawnerItemFactory = new SpawnerItemFactory(this);
    }

    private void initializeCoreComponents() {
        this.spawnerFileHandler = new SpawnerFileHandler(this);
        this.spawnerManager = new SpawnerManager(this);
        this.spawnerManager.reloadAllHolograms();
        this.guiLayoutConfig = new GuiLayoutConfig(this);
        this.spawnerStorageUI = new SpawnerStorageUI(this);
        this.filterConfigUI = new FilterConfigUI(this);
        this.spawnerMenuUI = new SpawnerMenuUI(this);
        this.spawnerGuiViewManager = new SpawnerGuiViewManager(this);
        this.spawnerLootGenerator = new SpawnerLootGenerator(this);
        this.spawnerSellManager = new SpawnerSellManager(this);
        this.rangeChecker = new SpawnerRangeChecker(this);
    }

    private void initializeHandlers() {
        this.chunkSpawnerLimiter = new ChunkSpawnerLimiter(this);
        this.spawnerStackerUI = new SpawnerStackerUI(this);
        this.spawnEggHandler = new SpawnEggHandler(this);
        this.spawnerStackHandler = new SpawnerStackHandler(this);
        this.spawnerClickManager = new SpawnerClickManager(this);
    }

    private void initializeUIAndActions() {
        this.spawnerMenuAction = new SpawnerMenuAction(this);
        this.spawnerStackerHandler = new SpawnerStackerHandler(this);
        this.spawnerStorageAction = new SpawnerStorageAction(this);
    }

    private void initializeListeners() {
        this.naturalSpawnerListener = new NaturalSpawnerListener(this);
        this.spawnerExplosionListener = new SpawnerExplosionListener(this);
        this.spawnerBreakListener = new SpawnerBreakListener(this);
        this.spawnerPlaceListener = new SpawnerPlaceListener(this);
    }

    public void setUpHopperHandler() {
        this.hopperHandler = getConfig().getBoolean("hopper.enabled", false) ? new HopperHandler(this) : null;
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        // Register core listeners
        pm.registerEvents(naturalSpawnerListener, this);
        pm.registerEvents(spawnerListGUI, this);
        pm.registerEvents(spawnerBreakListener, this);
        pm.registerEvents(spawnerPlaceListener, this);
        pm.registerEvents(spawnerStorageAction, this);
        pm.registerEvents(spawnerExplosionListener, this);
        pm.registerEvents(spawnerGuiViewManager, this);
        pm.registerEvents(spawnerClickManager, this);
        pm.registerEvents(spawnerMenuAction, this);
        pm.registerEvents(spawnerStackerHandler, this);
    }

    private void setupCommand() {
        this.reloadCommand = new ReloadCommand(this);
        this.giveCommand = new GiveCommand(this);
        this.userPreferenceCache = new UserPreferenceCache(this);
        this.listCommand = new ListCommand(this);
        this.spawnerListGUI = new SpawnerListGUI(this);
        this.hologramCommand = new HologramCommand(this);
        this.commandHandler = new CommandHandler(this);
        Objects.requireNonNull(getCommand("smartspawner")).setExecutor(commandHandler);
        Objects.requireNonNull(getCommand("smartspawner")).setTabCompleter(commandHandler);
    }

    private void setupBtatsMetrics() {
        Metrics metrics = new Metrics(this, 24822);
        metrics.addCustomChart(new Metrics.SimplePie("holograms", () ->
                String.valueOf(getConfig().getBoolean("hologram.enabled", false)))
        );
        metrics.addCustomChart(new Metrics.SimplePie("hoppers", () ->
                String.valueOf(getConfig().getBoolean("hopper.enabled", false)))
        );
        metrics.addCustomChart(new Metrics.SimplePie("spawners", () ->
                String.valueOf(this.spawnerManager.getTotalSpawners() / 1000 * 1000))
        );
    }

    public void reload() {
        // reload gui components
        guiLayoutConfig.reloadLayouts();
        spawnerStorageAction.reload();
        spawnerStorageUI.reload();
        filterConfigUI.reload();

        // reload services
        integrationManager.reload();
        spawnerMenuAction.reload();
        timeFormatter.clearCache();
        itemCache.clear();
    }

    @Override
    public void onDisable() {
        saveAndCleanup();
        SpawnerMobHeadTexture.clearCache();
        getLogger().info("SmartSpawner has been disabled!");
    }

    private void saveAndCleanup() {
        if (spawnerManager != null) {
            try {
                if (spawnerFileHandler != null) {
                    spawnerFileHandler.shutdown();
                }

                // Clean up the spawner manager
                spawnerManager.cleanupAllSpawners();
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error saving spawner data during shutdown", e);
            }
        }

        if (itemPriceManager != null) {
            itemPriceManager.cleanup();
        }

        // Clean up resources
        cleanupResources();
    }

    private void cleanupResources() {
        if (rangeChecker != null) rangeChecker.cleanup();
        if (spawnerGuiViewManager != null) spawnerGuiViewManager.cleanup();
        if (hopperHandler != null) hopperHandler.cleanup();
        if (spawnerClickManager != null) spawnerClickManager.cleanup();
        if (spawnerStackerHandler != null) spawnerStackerHandler.cleanupAll();
        if (spawnerStorageUI != null) spawnerStorageUI.cleanup();
    }

    // Spawner Provider for ShopGUI+ integration
    public SpawnerProvider getSpawnerProvider() {
        return new SpawnerProvider(this);
    }

    public boolean hasSellIntegration() {
        if (itemPriceManager == null) {
            return false;
        }
        return itemPriceManager.hasSellIntegration();
    }

    public boolean hasShopIntegration() {
        if (itemPriceManager == null) {
            return false;
        }

        return itemPriceManager.getShopIntegrationManager() != null &&
                itemPriceManager.getShopIntegrationManager().hasActiveProvider();
    }

    public long getTimeFromConfig(String path, String defaultValue) {
        return timeFormatter.getTimeFromConfig(path, defaultValue);
    }

    public void debug(String message) {
        if (debugMode) {
            getLogger().info("[DEBUG] " + message);
        }
    }
}