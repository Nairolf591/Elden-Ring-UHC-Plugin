package me.uhcplugin.role;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.Location; // Import ajouté ici

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Margit implements Listener {
    private final Player player;
    private final Map<UUID, Long> cooldowns = new HashMap<>(); // Stocke les cooldowns par joueur
    private static final long COOLDOWN_TIME = 180 * 1000; // 3 minutes en millisecondes
    private static final Material HAMMER_ITEM = Material.GOLDEN_AXE; // Hache en or pour activer la compétence

    public Margit(Player player) {
        this.player = player;
        applyConstantEffects(); // Applique l'effet de force constant
        giveHammerItem(); // Donne l'item spécial après 10 secondes
    }

    // Applique l'effet de force de 10% (amplitude 1 = 20%, donc 0.5 pour 10%)
    private void applyConstantEffects() {
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, Integer.MAX_VALUE, 0, false, false));
    }

    // Donne l'item spécial après 10 secondes
    private void giveHammerItem() {
        Bukkit.getScheduler().runTaskLater(player.getServer().getPluginManager().getPlugin("UHCPlugin"), () -> {
            ItemStack hammer = new ItemStack(HAMMER_ITEM);
            ItemMeta meta = hammer.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "Marteau de Margit");
                hammer.setItemMeta(meta);
            }
            player.getInventory().addItem(hammer);
            player.sendMessage(ChatColor.GOLD + "Vous avez reçu le Marteau de Margit ! Utilisez-le pour sauter.");
        }, 200L); // 10 secondes = 200 ticks
    }

    // Gère l'activation de la compétence lorsque le joueur s'accroupit avec l'item spécial en main
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        // Vérifie si le joueur est accroupi et tient la hache en or
        if (event.isSneaking() && player.getInventory().getItemInMainHand().getType() == HAMMER_ITEM) {
            // Vérifie le cooldown
            long currentTime = System.currentTimeMillis();
            long lastUsed = cooldowns.getOrDefault(player.getUniqueId(), 0L);

            if (currentTime - lastUsed < COOLDOWN_TIME) {
                long remainingTime = (COOLDOWN_TIME - (currentTime - lastUsed)) / 1000;
                player.sendMessage(ChatColor.RED + "Vous devez attendre " + remainingTime + " secondes avant de réutiliser cette compétence.");
                return;
            }

            // Active la compétence
            performHammerJump();
            cooldowns.put(player.getUniqueId(), currentTime); // Met à jour le cooldown
        }
    }

    // Effectue le saut avec le marteau
    private void performHammerJump() {
        // Vecteur de direction pour le saut
        Vector direction = player.getLocation().getDirection().multiply(2).setY(1.5);
        player.setVelocity(direction);

        // Sons et particules pendant le saut
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);

        // Planifie l'effet de retombée après 1 seconde (20 ticks)
        Bukkit.getScheduler().runTaskLater(player.getServer().getPluginManager().getPlugin("UHCPlugin"), this::hammerLanding, 20);
    }

    // Effet de retombée du saut
    private void hammerLanding() {
        Location location = player.getLocation();

        // Sons et particules à la retombée
        player.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, location, 10, 2, 0.5, 2, 0.1);

        // Inflige des dégâts à toutes les entités proches
        for (Entity entity : player.getWorld().getNearbyEntities(location, 5, 3, 5)) {
            if (!entity.equals(player)) {
                entity.setVelocity(entity.getLocation().toVector().subtract(location.toVector()).normalize().multiply(1.5));
                if (entity instanceof Player) {
                    ((Player) entity).damage(6.0); // 3 cœurs de dégâts aux joueurs
                } else {
                    entity.setVelocity(entity.getLocation().toVector().subtract(location.toVector()).normalize().multiply(1.5));
                }
            }
        }
    }
}
