package me.uhcplugin.roles;

import me.uhcplugin.Main;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
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

public class MalikethRole implements Listener, CommandExecutor {

    private final Main plugin;
    private final Set<UUID> hasTransformed = new HashSet<>();
    private final Set<UUID> sautCooldown = new HashSet<>();
    private final Set<UUID> benedictionCooldown = new HashSet<>();
    private final Set<UUID> ruptureCooldown = new HashSet<>();
    private final Set<UUID> eruptionCooldown = new HashSet<>();
    private final Set<UUID> assautCooldown = new HashSet<>();
    private final Set<UUID> lameCooldown = new HashSet<>();
    private final Map<UUID, Boolean> isLameActive = new HashMap<>();

    public MalikethRole(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        PluginCommand command = plugin.getCommand("maliketh_phase");
        if (command != null) {
            command.setExecutor((sender, cmd, label, args) -> {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    handlePhaseChange(player);
                }
                return true;
            });
        } else {
            plugin.getLogger().severe("La commande 'maliketh_phase' n'est pas enregistrée dans plugin.yml !");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (command.getName().equalsIgnoreCase("maliketh_phase")) {
                handlePhaseChange(player);
                return true;
            }
        }
        return false;
    }

    public void handlePhaseChange(Player player) {
        if (!"Maliketh".equalsIgnoreCase(plugin.getRoleManager().getRole(player))) {
            player.sendMessage(ChatColor.RED + "❌ Tu n'es pas Maliketh ! ");
            return;
        }

        if (hasTransformed.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "❌ Tu as déjà utilisé ta transformation !");
            return;
        }

        hasTransformed.add(player.getUniqueId());
        player.sendMessage(ChatColor.DARK_RED + "📢 \"Oh death, become my blade once more...\"");

        Location loc = player.getLocation();
        player.playSound(loc, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.5f);
        player.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.5f);

        // Particules visibles pour Bedrock (SMOKE_LARGE, SOUL, FLAME)
        player.spawnParticle(Particle.SMOKE_LARGE, loc, 200, 1, 1, 1, 0.2);
        player.spawnParticle(Particle.SOUL, loc, 300, 1, 2, 1, 0.1);
        player.spawnParticle(Particle.FLAME, loc, 150, 1, 2, 1, 0.1);

        player.setWalkSpeed(0);
        player.setInvulnerable(true); // Invincible pendant la transformation

        new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (countdown <= 0) {
                    cancel();
                    completeTransformation(player);
                } else {
                    player.sendMessage(ChatColor.DARK_RED + "⚔️ Transformation en Lame Noire... (" + countdown + "s)");
                    countdown--;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void completeTransformation(Player player) {
        player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
        player.setMaxHealth(20);

        removePhase1Items(player);
        clearPhase1Cooldowns(player);

        player.sendMessage(ChatColor.RED + "🔥 Tu es maintenant la Lame Noire !");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 1.0f);

        // Effets visuels supplémentaires lors de la transformation en Phase 2
        Location loc = player.getLocation();
        player.spawnParticle(Particle.FLAME, loc, 400, 1, 2, 1, 0.2);
        player.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 300, 1, 2, 1, 0.1);
        player.spawnParticle(Particle.SMOKE_LARGE, loc, 200, 1, 2, 1, 0.2);
        player.spawnParticle(Particle.SOUL, loc, 250, 1, 2, 1, 0.1);

        player.setWalkSpeed(0.2f);
        player.setInvulnerable(false); // Fin de l'invincibilité
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 999999, 0, false, false));

        givePhase2Items(player);
    }

    public void giveMalikethItems(Player player) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.DAMAGE_RESISTANCE,
                Integer.MAX_VALUE,
                0,
                false,
                false
        ));

        player.setMaxHealth(player.getMaxHealth() + 4);

        ItemStack ruptureBestiale = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = ruptureBestiale.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_RED + "1️⃣ Rupture Bestiale 🦴💥");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Frappe le sol et projette une onde de choc.",
                ChatColor.RED + "Dégâts : 2 cœurs",
                ChatColor.GOLD + "Repoussement : 3 blocs",
                ChatColor.AQUA + "Recharge : 3 minutes"
        ));
        ruptureBestiale.setItemMeta(meta);

        ItemStack sautBestial = new ItemStack(Material.NETHER_STAR);
        ItemMeta sautMeta = sautBestial.getItemMeta();
        sautMeta.setDisplayName(ChatColor.DARK_PURPLE + "2️⃣ Saut Bestial 🐾🔼");
        sautMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Bondit en avant sur 7 blocs.",
                ChatColor.DARK_PURPLE + "Effet : Lenteur I sur les ennemis",
                ChatColor.AQUA + "Recharge : 5 minutes"
        ));
        sautBestial.setItemMeta(sautMeta);

        ItemStack benediction = new ItemStack(Material.NETHER_STAR);
        ItemMeta beneMeta = benediction.getItemMeta();
        beneMeta.setDisplayName(ChatColor.GOLD + "3️⃣ Bénédiction de Destin 🛡️✨");
        beneMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Confère Absorption III pendant 3 minutes",
                ChatColor.AQUA + "Recharge : 1 jour"
        ));
        benediction.setItemMeta(beneMeta);

        player.getInventory().addItem(ruptureBestiale, sautBestial, benediction);
    }

    public void givePhase2Items(Player player) {
        ItemStack eruption = new ItemStack(Material.NETHER_STAR);
        ItemMeta eruptionMeta = eruption.getItemMeta();
        eruptionMeta.setDisplayName(ChatColor.DARK_RED + "1️⃣ Éruption de Mort 💀🔥");
        eruptionMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Explosion nécrotique infligeant 3 cœurs.",
                ChatColor.RED + "Effet : Wither I pendant 5 secondes",
                ChatColor.AQUA + "Recharge : 4 minutes"
        ));
        eruption.setItemMeta(eruptionMeta);

        ItemStack assaut = new ItemStack(Material.NETHER_STAR);
        ItemMeta assautMeta = assaut.getItemMeta();
        assautMeta.setDisplayName(ChatColor.DARK_PURPLE + "2️⃣ Assaut Bestial 🦇⚔️");
        assautMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Saute sur un ennemi, infligeant 3 cœurs.",
                ChatColor.DARK_PURPLE + "Effet : Faiblesse I pendant 5 secondes",
                ChatColor.AQUA + "Recharge : 6 minutes"
        ));
        assaut.setItemMeta(assautMeta);

        ItemStack lame = new ItemStack(Material.NETHER_STAR);
        ItemMeta lameMeta = lame.getItemMeta();
        lameMeta.setDisplayName(ChatColor.DARK_RED + "3️⃣ Lame de la Mort ☠️⚔️");
        lameMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Augmente les dégâts de 5% pendant 20 secondes.",
                ChatColor.RED + "Effet : Wither I à chaque attaque",
                ChatColor.AQUA + "Recharge : 1 jour"
        ));
        lame.setItemMeta(lameMeta);

        player.getInventory().addItem(eruption, assaut, lame);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!"Maliketh".equals(plugin.getRoleManager().getRole(player))) return;

        if (item.getType() == Material.NETHER_STAR && item.hasItemMeta()) {
            if (item.getItemMeta().getDisplayName().contains("Rupture Bestiale")) {
                activateRuptureBestiale(player);
            } else if (item.getItemMeta().getDisplayName().contains("Saut Bestial")) {
                activateSautBestial(player);
            } else if (item.getItemMeta().getDisplayName().contains("Bénédiction")) {
                activateBenediction(player);
            } else if (item.getItemMeta().getDisplayName().contains("Éruption de Mort")) {
                activateEruption(player);
            } else if (item.getItemMeta().getDisplayName().contains("Assaut Bestial")) {
                activateAssaut(player);
            } else if (item.getItemMeta().getDisplayName().contains("Lame de la Mort")) {
                activateLame(player);
            }
        }
    }

    public void activateRuptureBestiale(Player player) {
        if (ruptureCooldown.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "❌ Rupture Bestiale est en recharge !");
            return;
        }

        ruptureCooldown.add(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> ruptureCooldown.remove(player.getUniqueId()), 20L * 180);

        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);

        // Particules pour marquer la zone d'effet
        player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 10);
        player.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc, 100, 3, 3, 3, 0.2);

        double radius = 5.0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.equals(player) && target.getLocation().distance(loc) <= radius) {
                target.damage(4.0);
                target.setVelocity(target.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(0.5));
                target.sendMessage(ChatColor.RED + "💥 Une onde de choc t’a repoussé !");
            }
        }
    }

    private void activateSautBestial(Player player) {
        if (sautCooldown.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "❌ Saut Bestial est en recharge !");
            return;
        }

        Vector direction = player.getLocation().getDirection().normalize().multiply(7);
        Location destination = player.getLocation().add(direction);

        player.setVelocity(direction.multiply(0.5));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);

        // Particules pendant le saut
        player.spawnParticle(Particle.CLOUD, player.getLocation(), 50, 1, 1, 1, 0.1);

        player.getNearbyEntities(3, 3, 3).forEach(entity -> {
            if (entity instanceof Player target && !target.equals(player)) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 0));
            }
        });

        sautCooldown.add(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                sautCooldown.remove(player.getUniqueId()), 20L * 60 * 5);
    }

    private void activateBenediction(Player player) {
        if (benedictionCooldown.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "❌ Bénédiction de Destin est en recharge !");
            return;
        }

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.ABSORPTION,
                20 * 60 * 3,
                2
        ));

        player.spawnParticle(Particle.HEART, player.getLocation(), 30);
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);

        benedictionCooldown.add(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                benedictionCooldown.remove(player.getUniqueId()), 20L * 60 * 60 * 24);
    }

    private void removePhase1Items(Player player) {
        ItemStack[] phase1Items = {
                new ItemStack(Material.NETHER_STAR),
                new ItemStack(Material.NETHER_STAR),
                new ItemStack(Material.NETHER_STAR)
        };

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                for (ItemStack phase1Item : phase1Items) {
                    if (item.getType() == phase1Item.getType() && item.hasItemMeta()) {
                        player.getInventory().remove(item);
                    }
                }
            }
        }
    }

    private void clearPhase1Cooldowns(Player player) {
        sautCooldown.remove(player.getUniqueId());
        benedictionCooldown.remove(player.getUniqueId());
        ruptureCooldown.remove(player.getUniqueId());
    }

    private void activateEruption(Player player) {
        if (eruptionCooldown.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "❌ Éruption de Mort est en recharge !");
            return;
        }

        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.5f);

        // Particules pour marquer la zone d'effet
        player.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc, 200, 3, 3, 3, 0.2);
        player.getWorld().spawnParticle(Particle.FLAME, loc, 150, 3, 3, 3, 0.1);

        double radius = 5.0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.equals(player) && target.getLocation().distance(loc) <= radius) {
                target.damage(6.0);
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 0));
                target.sendMessage(ChatColor.RED + "💀 Une explosion nécrotique t'a frappé !");
            }
        }

        eruptionCooldown.add(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                eruptionCooldown.remove(player.getUniqueId()), 20L * 60 * 4);
    }

    private void activateAssaut(Player player) {
        if (assautCooldown.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "❌ Assaut Bestial est en recharge !");
            return;
        }

        Player target = null;
        double minDistance = Double.MAX_VALUE;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player)) {
                double distance = online.getLocation().distance(player.getLocation());
                if (distance < minDistance && distance <= 10) {
                    target = online;
                    minDistance = distance;
                }
            }
        }

        if (target == null) {
            player.sendMessage(ChatColor.RED + "❌ Aucun ennemi à proximité !");
            return;
        }

        player.teleport(target.getLocation());
        target.damage(6.0);
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
        player.spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 50, 1, 1, 1, 0.2);

        assautCooldown.add(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                assautCooldown.remove(player.getUniqueId()), 20L * 60 * 6);
    }

    private void activateLame(Player player) {
        if (lameCooldown.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "❌ Lame de la Mort est en recharge !");
            return;
        }

        player.setWalkSpeed(0);
        player.setInvulnerable(true); // Invincible pendant l'animation
        player.sendMessage(ChatColor.DARK_RED + "⚔️ Tu invoques la Lame de la Mort...");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.5f);

        // Particules pendant l'invocation
        player.spawnParticle(Particle.SOUL, player.getLocation(), 300, 1, 1, 1, 0.2);
        player.spawnParticle(Particle.FLAME, player.getLocation(), 200, 1, 1, 1, 0.1);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.setWalkSpeed(0.2f);
            player.setInvulnerable(false); // Fin de l'invincibilité
            player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 400, 0));
            player.sendMessage(ChatColor.RED + "🔥 La Lame de la Mort est active !");

            // Effets visuels lors de la recharge
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(ChatColor.GRAY + "⚔️ La Lame de la Mort s'est dissipée...");
                player.spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 100, 1, 1, 1, 0.2);
                player.spawnParticle(Particle.SOUL, player.getLocation(), 150, 1, 1, 1, 0.1);
            }, 20L * 20);
        }, 20L * 10);

        lameCooldown.add(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                lameCooldown.remove(player.getUniqueId()), 20L * 60 * 60 * 24);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (isLameActive.containsKey(player.getUniqueId())) {
                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0));
                }

                // Augmentation des dégâts de 5%
                event.setDamage(event.getDamage() * 1.05);
            }
        }
    }

    public boolean isInPhase1(UUID playerUUID) {
        return !hasTransformed.contains(playerUUID); // Si le joueur n'a pas encore transformé, il est en Phase 1
    }
}