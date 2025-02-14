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
    private final Map<UUID, Long> sautCooldown = new HashMap<>();
    private final Map<UUID, Long> benedictionCooldown = new HashMap<>();
    private final Map<UUID, Long> ruptureCooldown = new HashMap<>();
    private final Map<UUID, Long> eruptionCooldown = new HashMap<>();
    private final Map<UUID, Long> assautCooldown = new HashMap<>();
    private final Map<UUID, Long> lameCooldown = new HashMap<>();
    private final Map<UUID, Boolean> isLameActive = new HashMap<>();
    private final Map<UUID, Long> messageCooldowns = new HashMap<>(); // Nouvelle map pour gérer les messages

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

        player.spawnParticle(Particle.SMOKE_LARGE, loc, 200, 1, 1, 1, 0.2);
        player.spawnParticle(Particle.SOUL, loc, 300, 1, 2, 1, 0.1);
        player.spawnParticle(Particle.FLAME, loc, 150, 1, 2, 1, 0.1);

        player.setWalkSpeed(0);
        player.setInvulnerable(true);

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
        // Phase 2 : 8 cœurs max
        player.setMaxHealth(16); // 8 cœurs (1 cœur = 2 points de vie)
        player.setHealth(16); // Rétablit la vie à 8 cœurs

        removePhase1Items(player);
        clearPhase1Cooldowns(player);

        player.sendMessage(ChatColor.RED + "🔥 Tu es maintenant la Lame Noire !");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 1.0f);

        Location loc = player.getLocation();
        player.spawnParticle(Particle.FLAME, loc, 400, 1, 2, 1, 0.2);
        player.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 300, 1, 2, 1, 0.1);
        player.spawnParticle(Particle.SMOKE_LARGE, loc, 200, 1, 2, 1, 0.2);
        player.spawnParticle(Particle.SOUL, loc, 250, 1, 2, 1, 0.1);

        player.setWalkSpeed(0.2f);
        player.setInvulnerable(false);
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

        player.setMaxHealth(24); // Phase 1 : 12 cœurs

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
        if (ruptureCooldown.containsKey(player.getUniqueId())) {
            long cooldownEnd = ruptureCooldown.get(player.getUniqueId());
            long secondsLeft = (cooldownEnd - System.currentTimeMillis()) / 1000;

            // 🔄 Vérifie si le message a déjà été envoyé il y a moins de 2 secondes
            if (!messageCooldowns.containsKey(player.getUniqueId()) ||
                    System.currentTimeMillis() - messageCooldowns.get(player.getUniqueId()) > 2000) {

                player.sendMessage(ChatColor.RED + "❌ Rupture Bestiale : " + secondsLeft + "s restantes");
                messageCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            }
            return;
        }

        ruptureCooldown.put(player.getUniqueId(), System.currentTimeMillis() + (3 * 60 * 1000)); // 3 minutes

        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
        player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, loc, 10);
        player.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc, 100, 3, 3, 3, 0.2);

        double radius = 5.0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.equals(player) && target.getLocation().distance(loc) <= radius) {
                // Applique les dégâts
                target.damage(4.0, player); // 2 cœurs de dégâts

                // Applique le repoussement
                Vector direction = target.getLocation().toVector().subtract(loc.toVector()).normalize();
                direction.setY(0.5); // Ajoute un peu de hauteur pour le repoussement
                target.setVelocity(direction.multiply(1.5)); // Force du repoussement

                target.sendMessage(ChatColor.RED + "💥 Une onde de choc t’a repoussé !");
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ruptureCooldown.remove(player.getUniqueId());
            if (player.isOnline()) {
                player.sendMessage(ChatColor.GREEN + "✅ Rupture Bestiale est prête !");
            }
        }, 3 * 60 * 20L); // 3 minutes
    }

    public void activateSautBestial(Player player) {
        if (sautCooldown.containsKey(player.getUniqueId())) {
            long cooldownEnd = sautCooldown.get(player.getUniqueId());
            long secondsLeft = (cooldownEnd - System.currentTimeMillis()) / 1000;

            if (!messageCooldowns.containsKey(player.getUniqueId()) ||
                    System.currentTimeMillis() - messageCooldowns.get(player.getUniqueId()) > 2000) {

                player.sendMessage(ChatColor.RED + "❌ Saut Bestial : " + secondsLeft + "s restantes");
                messageCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            }
            return;
        }


        sautCooldown.put(player.getUniqueId(), System.currentTimeMillis() + (5 * 60 * 1000)); // 5 minutes

        Vector direction = player.getLocation().getDirection().normalize().multiply(7);
        Location destination = player.getLocation().add(direction);

        player.setVelocity(direction.multiply(0.5));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
        player.spawnParticle(Particle.CLOUD, player.getLocation(), 50, 1, 1, 1, 0.1);

        player.getNearbyEntities(3, 3, 3).forEach(entity -> {
            if (entity instanceof Player target && !target.equals(player)) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 0));
            }
        });

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            sautCooldown.remove(player.getUniqueId());
            if (player.isOnline()) {
                player.sendMessage(ChatColor.GREEN + "✅ Saut Bestial est prêt !");
            }
        }, 5 * 60 * 20L); // 5 minutes
    }

    public void activateBenediction(Player player) {
        if (benedictionCooldown.containsKey(player.getUniqueId())) {
            long cooldownEnd = benedictionCooldown.get(player.getUniqueId());
            long secondsLeft = (cooldownEnd - System.currentTimeMillis()) / 1000;

            if (!messageCooldowns.containsKey(player.getUniqueId()) ||
                    System.currentTimeMillis() - messageCooldowns.get(player.getUniqueId()) > 2000) {

                player.sendMessage(ChatColor.RED + "❌ Bénédiction de Destin : " + secondsLeft + "s restantes");
                messageCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            }
            return;
        }

        // Applique le cooldown (1 jour)
        benedictionCooldown.put(player.getUniqueId(), System.currentTimeMillis() + (24 * 60 * 60 * 1000));

        // Vérifie si le joueur a déjà l'effet Absorption
        if (!player.hasPotionEffect(PotionEffectType.ABSORPTION)) {
            // Applique l'effet Absorption III pendant 3 minutes
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.ABSORPTION,
                    20 * 60 * 3, // 3 minutes (en ticks)
                    2 // Niveau III
            ));

            // Effets visuels et sonores
            player.spawnParticle(Particle.HEART, player.getLocation(), 30);
            player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);

            player.sendMessage(ChatColor.GOLD + "✨ Bénédiction de Destin active ! Tu as reçu Absorption III pendant 3 minutes.");
        } else {
            player.sendMessage(ChatColor.RED + "❌ Tu as déjà l'effet Absorption !");
            return; // On arrête ici si le joueur a déjà l'effet
        }

        // Message de recharge après 1 jour
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            benedictionCooldown.remove(player.getUniqueId());
            if (player.isOnline()) {
                player.sendMessage(ChatColor.GREEN + "✅ Bénédiction de Destin est prête !");
            }
        }, 24 * 60 * 60 * 20L); // 1 jour en ticks
    }

    private void removePhase1Items(Player player) {
        player.getInventory().remove(Material.NETHER_STAR);
    }

    private void clearPhase1Cooldowns(Player player) {
        sautCooldown.remove(player.getUniqueId());
        benedictionCooldown.remove(player.getUniqueId());
        ruptureCooldown.remove(player.getUniqueId());
    }

    public void activateEruption(Player player) {
        if (eruptionCooldown.containsKey(player.getUniqueId())) {
            long cooldownEnd = eruptionCooldown.get(player.getUniqueId());
            long secondsLeft = (cooldownEnd - System.currentTimeMillis()) / 1000;

            // 🔄 Vérifie si le message a déjà été envoyé il y a moins de 2 secondes
            if (!messageCooldowns.containsKey(player.getUniqueId()) ||
                    System.currentTimeMillis() - messageCooldowns.get(player.getUniqueId()) > 2000) {

                player.sendMessage(ChatColor.RED + "❌ Éruption de Mort : " + secondsLeft + "s restantes");
                messageCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            }
            return;
        }

        eruptionCooldown.put(player.getUniqueId(), System.currentTimeMillis() + (4 * 60 * 1000)); // 4 minutes

        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.ENTITY_WITHER_SHOOT, 1.5f, 0.5f);
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

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            eruptionCooldown.remove(player.getUniqueId());
            if (player.isOnline()) {
                player.sendMessage(ChatColor.GREEN + "✅ Éruption de Mort est prête !");
            }
        }, 4 * 60 * 20L); // 4 minutes
    }

    public void activateAssaut(Player player) {
        if (assautCooldown.containsKey(player.getUniqueId())) {
            long cooldownEnd = assautCooldown.get(player.getUniqueId());
            long secondsLeft = (cooldownEnd - System.currentTimeMillis()) / 1000;

            // 🔄 Vérifie si le message a déjà été envoyé il y a moins de 2 secondes
            if (!messageCooldowns.containsKey(player.getUniqueId()) ||
                    System.currentTimeMillis() - messageCooldowns.get(player.getUniqueId()) > 2000) {

                player.sendMessage(ChatColor.RED + "❌ Assaut Bestial : " + secondsLeft + "s restantes");
                messageCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            }
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
            return; // On arrête ici si aucun joueur n'est trouvé
        }

        // Applique le cooldown uniquement si la compétence est utilisée
        assautCooldown.put(player.getUniqueId(), System.currentTimeMillis() + (6 * 60 * 1000)); // 6 minutes

        // Téléporte Maliketh sur le joueur cible
        player.teleport(target.getLocation());
        target.damage(6.0, player); // 3 cœurs de dégâts
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0)); // Faiblesse I pendant 5 secondes

        // Effets visuels et sonores
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.5f);
        player.spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 50, 1, 1, 1, 0.2);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            assautCooldown.remove(player.getUniqueId());
            if (player.isOnline()) {
                player.sendMessage(ChatColor.GREEN + "✅ Assaut Bestial est prêt !");
            }
        }, 6 * 60 * 20L); // 6 minutes
    }

    public void activateLame(Player player) {
        if (lameCooldown.containsKey(player.getUniqueId())) {
            long cooldownEnd = lameCooldown.get(player.getUniqueId());
            long secondsLeft = (cooldownEnd - System.currentTimeMillis()) / 1000;

            // 🔄 Vérifie si le message a déjà été envoyé il y a moins de 2 secondes
            if (!messageCooldowns.containsKey(player.getUniqueId()) ||
                    System.currentTimeMillis() - messageCooldowns.get(player.getUniqueId()) > 2000) {

                player.sendMessage(ChatColor.RED + "❌ Lame de la Mort : " + secondsLeft + "s restantes");
                messageCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            }
            return;
        }


        lameCooldown.put(player.getUniqueId(), System.currentTimeMillis() + (24 * 60 * 60 * 1000)); // 1 jour

        player.setWalkSpeed(0);
        player.setInvulnerable(true);
        player.sendMessage(ChatColor.DARK_RED + "⚔️ Tu invoques la Lame de la Mort...");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.5f);

        player.spawnParticle(Particle.SOUL, player.getLocation(), 300, 1, 1, 1, 0.2);
        player.spawnParticle(Particle.FLAME, player.getLocation(), 200, 1, 1, 1, 0.1);

        // Active l'effet Wither sur les attaques
        isLameActive.put(player.getUniqueId(), true);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.setWalkSpeed(0.2f);
            player.setInvulnerable(false);
            player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 400, 0)); // +5% de dégâts
            player.sendMessage(ChatColor.RED + "🔥 La Lame de la Mort est active !");

            // Désactive l'effet Wither après 20 secondes
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                isLameActive.remove(player.getUniqueId());
                player.sendMessage(ChatColor.GRAY + "⚔️ La Lame de la Mort s'est dissipée...");
                player.spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 100, 1, 1, 1, 0.2);
                player.spawnParticle(Particle.SOUL, player.getLocation(), 150, 1, 1, 1, 0.1);
            }, 20L * 20); // 20 secondes
        }, 20L * 10); // 10 secondes d'invocation

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            lameCooldown.remove(player.getUniqueId());
            if (player.isOnline()) {
                player.sendMessage(ChatColor.GREEN + "✅ Lame de la Mort est prête !");
            }
        }, 24 * 60 * 60 * 20L); // 1 jour

        if (isLameActive.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "❌ La Lame de la Mort est déjà active !");
            return;
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();

            // Vérifie si la Lame de la Mort est active
            if (isLameActive.containsKey(player.getUniqueId())) {
                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();

                    // Applique Wither I pendant 2 secondes (40 ticks)
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 0));

                    // Augmente les dégâts de 5%
                    event.setDamage(event.getDamage() * 1.05);
                }
            }
        }
    }

    public boolean isInPhase1(UUID playerUUID) {
        return !hasTransformed.contains(playerUUID);
    }
}