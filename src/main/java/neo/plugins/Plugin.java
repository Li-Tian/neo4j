package neo.plugins;

import java.awt.datatransfer.Clipboard;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;

import javax.security.auth.login.Configuration;

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

    private static final String pluginsPath = ".\\Plugins";
    private static WatchService configWatcher = null;

    private static int suspend = 0;

    private static NeoSystem system;

    protected void setSystem(NeoSystem inputSystem) {
        system = inputSystem;
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

    /*public String version() {
        TR.enter();
        return TR.exit("");
    }*/

    public String configFile = ".\\config.json";

    public Plugin() {
        try {
            TR.enter();
            Path toWatch = Paths.get(pluginsPath);
            if (toWatch == null) {
                TR.exit();
                throw new RuntimeException("Plugin directory not found");
            }
            configWatcher = FileSystems.getDefault().newWatchService();
            toWatch.register(configWatcher, ENTRY_CREATE, ENTRY_MODIFY);
            Thread thread = new Thread() {
                public void run() {
                    try {
                        WatchKey key = configWatcher.take();
                        while (key != null) {
                            for (WatchEvent event : key.pollEvents()) {
                                if (event.kind() == ENTRY_CREATE || event.kind() == ENTRY_MODIFY) {
                                    configWatcher_Changed();
                                }
                            }
                            key.reset();
                            key = configWatcher.take();
                        }
                    } catch (InterruptedException e) {
                        TR.error(e);
                        throw new RuntimeException(e);
                    }
                }
            };
            thread.start();
        } catch (IOException e) {
            TR.error(e);
            throw new RuntimeException(e);
        }
        TR.exit();
    }

    /*protected Plugin() {
        TR.enter();
        plugins.add(this);
        if (this instanceof ILogPlugin) loggers.add((ILogPlugin) this);
        if (this instanceof IPolicyPlugin) policies.add((IPolicyPlugin) this);
        if (this instanceof IRpcPlugin) rpcPlugins.add((IRpcPlugin) this);
        if (this instanceof IPersistencePlugin) persistencePlugins.add((IPersistencePlugin) this);
        if (this instanceof IMemoryPoolTxObserverPlugin)
            txObserverPlugins.add((IMemoryPoolTxObserverPlugin) this);

        configure();
        TR.exit();
    }*/

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

    private static void configWatcher_Changed() {
        TR.enter();
        TR.exit();
    }

    protected Configuration GetConfiguration() {
        return null;
    }

    public static void loadPlugins(NeoSystem system) {
        Plugin.system = system;
        File pluginFolder = new File(pluginsPath);
        if (!pluginFolder.exists()) return;
        for (File file : pluginFolder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                TR.enter();
                return TR.exit(pathname.getName().endsWith(".dll"));
            }
        })) {
            /*Assembly assembly = Assembly.LoadFile(filename);
            foreach(Type type in assembly.ExportedTypes)
            {
                if (!type.IsSubclassOf(typeof(Plugin))) continue;
                if (type.IsAbstract) continue;

                ConstructorInfo constructor = type.GetConstructor(Type.EmptyTypes);
                try {
                    constructor ?.Invoke(null);
                } catch (Exception ex) {
                    Log(nameof(Plugin), LogLevel.Error, $"Failed to initialize plugin: {ex.Message}");
                }
            }*/
        }
    }

    protected void log(String message, LogLevel level) {
        TR.enter();
        if (level == null) {
            level = LogLevel.Info;
        }
        log(Plugin.class.getSimpleName() + ":" + name(), level, message);
        TR.exit();
    }

    public static void log(String source, LogLevel level, String message) {
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
        /*if (Interlocked.Decrement(ref suspend) != 0) {
            return TR.exit(false);
        }*/
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

    protected static void SuspendNodeStartup() {
        //Interlocked.Increment(ref suspend);
        //system.suspendNodeStartup();
    }

    private static void CurrentDomain_AssemblyResolve() {
    }
}