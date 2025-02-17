package me.uhcplugin.roles;

import me.uhcplugin.Main;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class SansEclatRole implements Listener {
    private final Main plugin;
    private final Map<UUID, Long> lameCooldown = new HashMap<>();
    private final Map<UUID, Long> criCooldown = new HashMap<>();
    private final Map<UUID, Long> frappeCooldown = new HashMap<>();
    private final Map<UUID, Long> messageCooldowns = new HashMap<>();
    private final Map<UUID, BukkitRunnable> cooldownTasks = new HashMap<>();

    public SansEclatRole(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!"Sans-éclat".equalsIgnoreCase(plugin.getRoleManager().getRole(player))) return;

        if (item.getType() == Material.NETHER_STAR && item.hasItemMeta()) {
            String displayName = item.getItemMeta().getDisplayName();

            if (displayName.contains("Lame Transcendante")) {
                handleLameTranscendante(player);
                } else if (displayName.contains("Griffe du Lion")) {
                handleCriGuerrier(player);
            } else if (displayName.contains("Frappe Rapide")) {
                handleFrappeRapide(player);
            }
        }
    }

    private void handleLameTranscendante(Player player) {
        UUID uuid = player.getUniqueId();

        if (checkCooldown(uuid, lameCooldown, 120, "Lame Transcendante")) return;
        if (!plugin.getManaManager().consumeMana(player, 30)) return;

        Location start = player.getLocation().add(0, 1.5, 0);
        Vector direction = start.getDirection().normalize();
        List<Entity> hitEntities = new ArrayList<>(); // Pour éviter les multi-hit

        new BukkitRunnable() {
            int steps = 0;
            final double STEP_SIZE = 0.5; // Précision augmentée
            final double HITBOX = 1.2; // Taille de la hitbox

            public void run() {
                if (steps++ >= 20) {
                    this.cancel();
                    return;
                }

                // Calcul position actuelle
                Vector stepVector = direction.clone().multiply(steps * STEP_SIZE);
                Location currentPoint = start.clone().add(stepVector);

                // Particules
                player.spawnParticle(Particle.SWEEP_ATTACK, currentPoint, 1);
                player.spawnParticle(Particle.CRIT_MAGIC, currentPoint, 3, 0.3, 0.3, 0.3, 0.05);

                // Détection des entités
                for (Entity entity : player.getNearbyEntities(10, 3, 10)) {
                    if (entity instanceof LivingEntity livingEntity
                            && !entity.equals(player)
                            && !hitEntities.contains(entity)
                            && entity.getLocation().distanceSquared(currentPoint) < HITBOX * HITBOX) {

                        livingEntity.damage(4.0, player); // 2 cœurs de dégâts
                        livingEntity.setVelocity(direction.multiply(0.3).setY(0.2));
                        hitEntities.add(entity);

                        // Feedback visuel
                        livingEntity.getWorld().playSound(livingEntity.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.2f, 0.8f);
                        livingEntity.getWorld().spawnParticle(
                                Particle.BLOCK_CRACK,
                                livingEntity.getLocation().add(0, 1, 0),
                                15, // Nombre de particules
                                0.5, 0.5, 0.5, // Offsets X/Y/Z
                                0.1, // Vitesse
                                Material.REDSTONE_BLOCK.createBlockData() // Données du bloc
                        );
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        startCooldown(uuid, lameCooldown, 120);
    }

    private void handleCriGuerrier(Player player) {
        UUID uuid = player.getUniqueId();

            if (checkCooldown(uuid, criCooldown, 300, "Griffe du Lion")) return;
        if (!plugin.getManaManager().consumeMana(player, 50)) return;

        // Effets de saut
        player.setVelocity(player.getLocation().getDirection().multiply(1.5).setY(0.7));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 2.0f, 0.6f);
        player.spawnParticle(Particle.CLOUD, player.getLocation(), 30, 0.5, 0, 0.5, 0.2);

        new BukkitRunnable() {
            public void run() {
                Location loc = player.getLocation();

                // Effets d'impact
                player.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
                player.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, loc, 1);
                player.getWorld().spawnParticle(Particle.CRIT_MAGIC, loc, 50, 2, 0.5, 2, 0.3);

                // Cercle de particules
                for (int i = 0; i < 36; i++) {
                    double angle = i * Math.PI / 18;
                    Location particleLoc = loc.clone().add(Math.cos(angle) * 3, 0, Math.sin(angle) * 3);
                    player.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0);
                }

                // Appliquer les effets
                for (Entity entity : player.getNearbyEntities(3, 3, 3)) {
                    if (entity instanceof LivingEntity livingEntity && !entity.equals(player)) {
                        livingEntity.damage(8.0, player);
                        livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1));
                        livingEntity.setVelocity(entity.getLocation().toVector()
                                .subtract(loc.toVector()).normalize().multiply(0.7).setY(0.3));
                    }
                }
            }
        }.runTaskLater(plugin, 10L);

        startCooldown(uuid, criCooldown, 300);
    }

    private void handleFrappeRapide(Player player) {
        UUID uuid = player.getUniqueId();

        if (checkCooldown(uuid, frappeCooldown, 240, "Frappe Rapide")) return;

        // Recherche de la cible
        LivingEntity finalTarget = null;
        double minDistance = Double.MAX_VALUE;
        for (Entity entity : player.getNearbyEntities(5, 3, 5)) {
            if (entity instanceof LivingEntity livingEntity
                    && !entity.equals(player)
                    && player.hasLineOfSight(entity)) {

                double distance = entity.getLocation().distance(player.getLocation());
                if (distance < minDistance) {
                    finalTarget = livingEntity;
                    minDistance = distance;
                }
            }
        }

        if (finalTarget != null) {
            // Copie finale pour la classe interne
            final LivingEntity target = finalTarget;

            // Téléportation précise devant la cible
            Vector direction = target.getLocation().toVector()
                    .subtract(player.getLocation().toVector()).normalize();
            Location tpLoc = target.getLocation().clone().add(direction.multiply(-1).setY(0));
            player.teleport(tpLoc);

            // Double frappe synchronisée
            new BukkitRunnable() {
                int hits = 0;

                public void run() {
                    if (hits++ >= 2) {
                        this.cancel();
                        return;
                    }

                    target.damage(1.5, player);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.2f);
                    player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation(), 3);

                    // Knockback léger
                    if (hits == 2) { // Seulement au 2ème coup
                        target.setVelocity(direction.multiply(0.3).setY(0.2));
                    }
                }
            }.runTaskTimer(plugin, 0L, 2L); // 2 ticks entre les coups
        } else {
            // Téléportation simple si pas de cible
            Location teleportLoc = player.getLocation().add(player.getLocation().getDirection().multiply(5));
            player.teleport(teleportLoc);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_WEAK, 1.0f, 1.0f);
        }

        // Effets visuels communs
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 1.8f);
        player.spawnParticle(Particle.PORTAL, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.5);

        startCooldown(uuid, frappeCooldown, 240);
    }

    private boolean checkCooldown(UUID uuid, Map<UUID, Long> cooldownMap, int seconds, String abilityName) {
        if (cooldownMap.containsKey(uuid)) {
            long remaining = (cooldownMap.get(uuid) - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                if (!messageCooldowns.containsKey(uuid) || System.currentTimeMillis() - messageCooldowns.get(uuid) > 2000) {
                    plugin.getServer().getPlayer(uuid).sendMessage(ChatColor.RED + "❌ " + abilityName + " : " + remaining + "s restantes");
                    messageCooldowns.put(uuid, System.currentTimeMillis());
                }
                return true;
            }
        }
        return false;
    }

    private void startCooldown(UUID uuid, Map<UUID, Long> cooldownMap, int seconds) {
        cooldownMap.put(uuid, System.currentTimeMillis() + (seconds * 1000L));

        // Message de recharge
        cooldownTasks.put(uuid, new BukkitRunnable() {
            public void run() {
                Player p = Bukkit.getPlayer(uuid);
                if(p != null) {
                    String abilityName = "";
                    if(cooldownMap == lameCooldown) abilityName = "Lame Transcendante";
                    else if(cooldownMap == criCooldown) abilityName = "Griffe du Lion";
                    else abilityName = "Frappe Rapide";

                    p.sendMessage(ChatColor.GREEN + "✦ " + abilityName + " est prête !");
                }
            }
        });
        cooldownTasks.get(uuid).runTaskLater(plugin, seconds * 20L);
    }

    public static ItemStack[] getSansEclatItems() {
        ItemStack lame = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = lame.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "⚔️ Lame Transcendante");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Projette une onde tranchante",
                ChatColor.RED + "Dégâts: 2 cœurs | Portée: 10 blocs",
                ChatColor.BLUE + "Mana: 30 | Recharge: 2 minutes"
        ));
        lame.setItemMeta(meta);

        ItemStack cri = new ItemStack(Material.NETHER_STAR);
        meta = cri.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "🦁 Griffe du Lion");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Bond et attaque de zone",
                ChatColor.RED + "Dégâts: 4 cœurs | Zone: 3 blocs",
                ChatColor.BLUE + "Mana: 50 | Recharge: 5 minutes"
        ));
        cri.setItemMeta(meta);

        ItemStack frappe = new ItemStack(Material.NETHER_STAR);
        meta = frappe.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "💨 Frappe Rapide");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Téléportation et attaque rapide",
                ChatColor.RED + "Dégâts: 1.5 cœurs | Téléport: 2 blocs",
                ChatColor.BLUE + "Recharge: 4 minutes"
        ));
        frappe.setItemMeta(meta);

        return new ItemStack[]{lame, cri, frappe};
    }

}