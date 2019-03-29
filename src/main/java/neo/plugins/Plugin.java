package neo.plugins;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import neo.NeoSystem;
import neo.log.notr.TR;
import neo.network.p2p.payloads.Transaction;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public abstract class Plugin {
    public static final ArrayList<Plugin> plugins = new ArrayList<Plugin>();
    private static final ArrayList<ILogPlugin> loggers = new ArrayList<ILogPlugin>();
    private static final ArrayList<IPolicyPlugin> policies = new ArrayList<IPolicyPlugin>();
    private static final ArrayList<IRpcPlugin> rpcPlugins = new ArrayList<IRpcPlugin>();
    private static final ArrayList<IPersistencePlugin> persistencePlugins = new ArrayList<IPersistencePlugin>();
    private static final ArrayList<IMemoryPoolTxObserverPlugin> txObserverPlugins = new ArrayList<IMemoryPoolTxObserverPlugin>();
    private static final HashMap<WatchKey, Path> keys = new HashMap<WatchKey, Path>();

    private static final String pluginsPath = System.getProperty("user.dir") + "/Plugins";
    private static WatchService configWatcher = null;
    private boolean hasBeenInitialized = false;

    private static AtomicInteger suspend = new AtomicInteger(0);

    private static NeoSystem system;

    protected void setSystem(NeoSystem inputSystem) {
        TR.enter();
        system = inputSystem;
        TR.exit();
    }

    public static ArrayList<IPolicyPlugin> getPolicies() {
        TR.enter();
        return TR.exit(policies);
    }

    public static ArrayList<IRpcPlugin> getRPCPlugins() {
        TR.enter();
        return TR.exit(rpcPlugins);
    }

    public static ArrayList<IPersistencePlugin> getPersistencePlugins() {
        TR.enter();
        return TR.exit(persistencePlugins);
    }

    public static ArrayList<IMemoryPoolTxObserverPlugin> getTXObserverPlugins() {
        TR.enter();
        return TR.exit(txObserverPlugins);
    }

    public String name() {
        TR.enter();
        return TR.exit(this.getClass().getSimpleName());
    }

    public String configFile() {
        TR.enter();
        return TR.exit(Paths.get(pluginsPath).resolve(name()).resolve("config.json").toString());
    }

    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        TR.enter();
        return TR.exit((WatchEvent<T>) event);
    }

    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) {
        try {
            TR.enter();
            WatchKey key = dir.register(configWatcher, ENTRY_CREATE, ENTRY_MODIFY);
            keys.put(key, dir);
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
        TR.exit();
    }

    /**
     * Register the given directory, and all its sub-directories, with the WatchService.
     */
    private void registerAll(final Path start) {
        // register directory and sub-directories
        try {
            TR.enter();
            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                    register(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
        TR.exit();
    }

    public Plugin() {
        TR.enter();
        if (!hasBeenInitialized) {
            try {
                Path toWatch = Paths.get(pluginsPath);
                if (toWatch == null) {
                    TR.exit();
                    throw new RuntimeException("Plugin directory not found");
                }
                configWatcher = FileSystems.getDefault().newWatchService();
                registerAll(toWatch);
                Thread thread = new Thread() {
                    public void run() {
                        try {
                            WatchKey key = configWatcher.take();
                            while (key != null) {
                                Path dir = keys.get(key);
                                for (WatchEvent event : key.pollEvents()) {
                                    WatchEvent.Kind kind = event.kind();
                                    WatchEvent<Path> ev = cast(event);
                                    Path name = ev.context();
                                    Path child = dir.resolve(name);
                                    if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                                        if (kind == ENTRY_CREATE) {
                                            registerAll(child);
                                        }
                                    } else {
                                        if (!event.context().toString().toLowerCase().endsWith(".json"))
                                            continue;
                                        if (kind == ENTRY_CREATE || kind == ENTRY_MODIFY) {
                                            configWatcher_Changed(child);
                                        }
                                    }
                                }
                                // reset key and remove from set if directory no longer accessible
                                boolean valid = key.reset();
                                if (!valid) {
                                    keys.remove(key);
                                    // all directories are inaccessible
                                    if (keys.isEmpty()) {
                                        //TBD: When no directory exists
                                    }
                                }
                                key = configWatcher.take();
                            }
                        } catch (InterruptedException e) {
                            TR.error(e);
                            throw new RuntimeException(e);
                        }
                    }
                };
                thread.start();
                hasBeenInitialized = true;
            } catch (IOException e) {
                TR.error(e);
                throw new RuntimeException(e);
            }
        }
        plugins.add(this);
        if (this instanceof ILogPlugin) loggers.add((ILogPlugin) this);
        if (this instanceof IPolicyPlugin) policies.add((IPolicyPlugin) this);
        if (this instanceof IRpcPlugin) rpcPlugins.add((IRpcPlugin) this);
        if (this instanceof IPersistencePlugin) persistencePlugins.add((IPersistencePlugin) this);
        if (this instanceof IMemoryPoolTxObserverPlugin)
            txObserverPlugins.add((IMemoryPoolTxObserverPlugin) this);
        configure();
        TR.exit();
    }

    public static boolean checkPolicy(Transaction tx) {
        TR.enter();
        for (IPolicyPlugin plugin : policies) {
            if (!plugin.filterForMemoryPool(tx)) {
                return TR.exit(false);
            }
        }
        return TR.exit(true);
    }

    public abstract void configure();

    private static void configWatcher_Changed(Path path) {
        TR.enter();
        for (Plugin plugin : plugins) {
            if (plugin.configFile().equals(path.toString())) {
                plugin.configure();
                plugin.pluginLog("Reloaded config for " + plugin.name(), null);
                break;
            }
        }
        TR.exit();
    }

    protected Config getConfiguration() {
        TR.enter();
        Config config = ConfigFactory.load("protocol.json").getConfig("PluginConfiguration");
        return TR.exit(config);
    }

    public static void loadPlugins(NeoSystem system) {
        TR.enter();
        Plugin.system = system;
        File pluginFolder = new File(pluginsPath);
        if (!pluginFolder.exists()) {
            TR.exit();
            return;
        }
        for (File file : pluginFolder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                TR.enter();
                return TR.exit(pathname.getName().endsWith(".dll"));
            }
        })) {
            System.load(file.getAbsolutePath());
            //TODO: Current code is not correct. Should find a better way to do.
            try {
                Class<?> cl = Class.forName(file.getName().split(".")[0]);
                Constructor<?> cons = cl.getConstructor(null);
            } catch (Exception e) {
                TR.error(e);
                throw new RuntimeException(e);
            }
        }
        TR.exit();
    }

    protected void pluginLog(String message, LogLevel level) {
        TR.enter();
        if (level == null) {
            level = LogLevel.Info;
        }
        pluginLog(Plugin.class.getSimpleName() + ":" + name(), level, message);
        TR.exit();
    }

    public static void pluginLog(String source, LogLevel level, String message) {
        TR.enter();
        for (ILogPlugin plugin : loggers) {
            plugin.log(source, level, message);
        }
        TR.exit();
    }

    protected boolean onMessage(Object message) {
        TR.enter();
        return TR.exit(false);
    }

    protected static boolean resumeNodeStartup() {
        TR.enter();
        if (suspend.decrementAndGet() != 0) {
            return TR.exit(false);
        }
        system.resumeNodeStartup();
        return TR.exit(true);
    }

    public static boolean sendMessage(Object message) {
        TR.enter();
        for (Plugin plugin : plugins) {
            if (plugin.onMessage(message)) {
                return TR.exit(true);
            }
        }
        return TR.exit(false);
    }

    protected static void suspendNodeStartup() {
        suspend.incrementAndGet();
        system.suspendNodeStartup();
    }
}