package me.uhcplugin;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

public class CountdownTimer {
    private final Main plugin;
    private final int totalSeconds;
    private int secondsLeft;
    private final Consumer<Integer> onTick;

    public CountdownTimer(Main plugin, int totalSeconds, Consumer<Integer> onTick) {
        this.plugin = plugin;
        this.totalSeconds = totalSeconds;
        this.secondsLeft = totalSeconds;
        this.onTick = onTick;
    }

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    cancel();
                    return;
                }

                onTick.accept(secondsLeft);
                secondsLeft--;
            }
        }.runTaskTimer(plugin, 0, 20); // Exécute chaque seconde (20 ticks)
    }
}
