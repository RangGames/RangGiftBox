package wiki.creeper.creeperGiftBox.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Utility methods to safely interact with the Bukkit scheduler from async contexts.
 */
public final class SchedulerUtil {

    private SchedulerUtil() {
    }

    /**
     * Returns an {@link Executor} that always executes the task on the Bukkit primary thread.
     * If the caller is already on the primary thread the runnable executes immediately,
     * otherwise it is scheduled for the next tick. Tasks are skipped when the plugin is disabled.
     *
     * @param plugin the plugin requesting synchronous execution
     * @return executor that mirrors runTask behaviour
     */
    public static Executor syncExecutor(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return command -> {
            if (Bukkit.isPrimaryThread()) {
                command.run();
                return;
            }
            if (!plugin.isEnabled()) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, command);
        };
    }

    /**
     * Returns an {@link Executor} that delegates work to Bukkit's asynchronous scheduler,
     * avoiding direct thread creation inside the plugin.
     *
     * @param plugin the plugin requesting asynchronous execution
     * @return executor backed by Bukkit's async scheduler
     */
    public static Executor asyncExecutor(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        return command -> {
            if (!plugin.isEnabled()) {
                command.run();
                return;
            }
            Bukkit.getScheduler().runTaskAsynchronously(plugin, command);
        };
    }

    /**
     * Convenience helper to run a single task on the primary thread.
     *
     * @param plugin  the plugin requesting synchronous execution
     * @param command runnable to execute
     */
    public static void runSync(JavaPlugin plugin, Runnable command) {
        syncExecutor(plugin).execute(command);
    }
}
