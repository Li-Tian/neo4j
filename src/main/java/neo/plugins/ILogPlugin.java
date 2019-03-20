package neo.plugins;

public interface ILogPlugin {
    void log(String source, LogLevel level, String message);
}
