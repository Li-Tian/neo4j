package neo.plugins;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.typesafe.config.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import neo.ledger.Blockchain;
import neo.log.tr.TR;
import neo.network.p2p.payloads.Transaction;
import neo.persistence.Snapshot;

public class MyPlugin extends Plugin implements IPolicyPlugin, ILogPlugin, IRpcPlugin, IPersistencePlugin {
    protected class MyLog {
        public String source = "";
        public LogLevel level = LogLevel.Info;
        public String message = "";

        public MyLog(String inputSource, LogLevel inputLevel, String inputMessage) {
            this.source = inputSource;
            this.level = inputLevel;
            this.message = inputMessage;
        }
    }
    protected boolean verifyLog (String message, LogLevel level) {
        TR.enter();
        if (level == null) {
            level = LogLevel.Info;
        }
        MyLog log2Verify = new MyLog(Plugin.class.getSimpleName() + ":" + name(), level, message);
        for (MyLog logEntry : log) {
            if (log2Verify.level == logEntry.level && log2Verify.message.equals(logEntry.message) && log2Verify.source.equals(logEntry.source)) {
                return TR.exit(true);
            }
        }
        return TR.exit(false);
    }

    protected ArrayList<MyLog> log = new ArrayList<MyLog>();
    public int maxOnImportHeight = 0;
    @Override
    public void configure() {
        TR.enter();
        File file = new File(configFile());
        if (file.exists()) {
            Config config = getConfiguration();
            maxOnImportHeight = config.getInt("MaxOnImportHeight");
        }
        TR.exit();
    }

    //IPolicyPlugin override
    @Override
    public boolean filterForMemoryPool(Transaction tx) {
        TR.enter();
        return TR.exit(true);
    }

    @Override
    public Collection<Transaction> filterForBlock(Collection<Transaction> transactions) {
        TR.enter();
        return TR.exit(transactions);
    }

    @Override
    public int maxTxPerBlock() {
        TR.enter();
        return TR.exit(10000);
    }

    @Override
    public int maxLowPriorityTxPerBlock() {
        TR.enter();
        return TR.exit(1000);
    }

    //ILogPlugin Override
    @Override
    public void log(String source, LogLevel level, String message) {
        TR.enter();
        log.add(new MyLog(source, level, message));
        TR.exit();
    }

    //IRpcPlugin Override
    @Override
    public JsonObject onProcess(HttpServletRequest req, HttpServletResponse res, String method, JsonArray _params) {
        TR.enter();
        return TR.exit(new JsonObject());
    }

    //IPersistencePlugin
    @Override
    public void onPersist(Snapshot snapshot, ArrayList<Blockchain.ApplicationExecuted> applicationExecutedList) {
        TR.enter();
        TR.exit();
    }

    @Override
    public void onCommit(Snapshot snapshot) {
        TR.enter();
        TR.exit();
    }

    @Override
    public boolean shouldThrowExceptionFromCommit(Exception ex) {
        TR.enter();
        return TR.exit(true);
    }

    @Override
    protected boolean onMessage(Object message) {
        TR.enter();
        return TR.exit(true);
    }
}