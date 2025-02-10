package me.uhcplugin.roles;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import me.uhcplugin.Main;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class RanniRole implements Listener, CommandExecutor {
    private final Main plugin;
    private final HashMap<UUID, UUID> partners = new HashMap<>();
    private final Set<UUID> sentWarningRecently = new HashSet<>();


    public RanniRole(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private final Map<UUID, ItemStack[]> savedInventories = new HashMap<>(); // Sauvegarde des inventaires

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "❌ Seuls les joueurs peuvent exécuter cette commande !");
            return true;
        }

        Player player = (Player) sender;

        // ✅ Vérifie que le joueur est bien Ranni
        String role = plugin.getRoleManager().getRole(player);
        if (!role.equalsIgnoreCase("Ranni")) {
            player.sendMessage(ChatColor.RED + "❌ Seule Ranni peut utiliser cette commande !");
            return true;
        }

        // ✅ Vérifie qu’un joueur a été spécifié en argument
        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "❌ Utilisation : /lecture <joueur>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        // ✅ Vérifie que le joueur cible est bien en ligne et dans la partie
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "❌ Le joueur " + args[0] + " n’est pas en ligne !");
            return true;
        }

        // ✅ Vérifie que la cible a bien un rôle attribué
        String realRole = plugin.getRoleManager().getRole(target);
        if (realRole == null) {
            player.sendMessage(ChatColor.RED + "❌ Impossible d’analyser ce joueur !");
            return true;
        }

        // ✅ Sélectionne un rôle aléatoire qui n’est pas "Ranni"
        List<String> activeRoles = new ArrayList<>(plugin.getRoleManager().getPlayerRoles().values());
        activeRoles.remove("Ranni"); // Exclut Ranni de la sélection

        if (activeRoles.isEmpty()) {
            player.sendMessage(ChatColor.RED + "❌ Aucun rôle supplémentaire disponible !");
            return true;
        }

        Collections.shuffle(activeRoles);
        String fakeRole = activeRoles.get(0); // Premier rôle aléatoire de la liste

        // ✅ Mélange l'affichage des rôles pour ne pas indiquer le vrai rôle
        List<String> displayedRoles = Arrays.asList(realRole, fakeRole);
        Collections.shuffle(displayedRoles);

        // ✅ Envoie le résultat à Ranni
        player.sendMessage(ChatColor.AQUA + "🔮 Lecture Astrale...");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "✨ Le joueur " + ChatColor.WHITE + target.getName() + ChatColor.LIGHT_PURPLE + " est l’un des rôles suivants :");
        player.sendMessage(ChatColor.YELLOW + "🎭 " + displayedRoles.get(0));
        player.sendMessage(ChatColor.YELLOW + "🎭 " + displayedRoles.get(1));

        return true;
    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) return; // Si ce n'est pas un joueur qui a tué, on ignore.

        String killerRole = plugin.getRoleManager().getRole(killer);
        if (killerRole.equalsIgnoreCase("Ranni") && !partners.containsKey(killer.getUniqueId())) {
            // 🔹 Sauvegarde l'inventaire du joueur AVANT sa mort
            savedInventories.put(victim.getUniqueId(), victim.getInventory().getContents().clone());

            // 🔹 Empêche le drop du stuff du joueur
            event.getDrops().clear();

            // 🔹 Stocke la victime comme partenaire
            partners.put(killer.getUniqueId(), victim.getUniqueId());
            plugin.getConfig().set("partners." + killer.getUniqueId().toString(), victim.getUniqueId().toString());
            plugin.saveConfig();

            // 🔹 Ressuscite le joueur immédiatement
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                victim.spigot().respawn();
                victim.teleport(killer.getLocation()); // TP au killer
                victim.sendMessage(ChatColor.LIGHT_PURPLE + "🌙 Une étrange puissance lunaire t’enveloppe...");
                victim.sendMessage(ChatColor.AQUA + "✨ Ranni t’a lié à son destin, vous êtes désormais unis !");
                victim.sendMessage(ChatColor.GOLD + "⚔ Ton objectif : Remporter la victoire à ses côtés !");

                // 🔹 Restaure son inventaire après la résurrection
                if (savedInventories.containsKey(victim.getUniqueId())) {
                    victim.getInventory().setContents(savedInventories.get(victim.getUniqueId()));
                    savedInventories.remove(victim.getUniqueId()); // Nettoie après la restauration
                }

                // 🔹 Préserve le rôle du joueur ressuscité
                String victimRole = plugin.getRoleManager().getRole(victim);
                plugin.getRoleManager().setRole(victim, victimRole);
            }, 20L); // 20 ticks après pour éviter les conflits
        }
    }

    public boolean hasPartner(Player ranni) {
        return partners.containsKey(ranni.getUniqueId());
    }

    public Player getPartner(Player ranni) {
        UUID partnerUUID = partners.get(ranni.getUniqueId());
        return partnerUUID != null ? Bukkit.getPlayer(partnerUUID) : null;
    }

    public static ItemStack getRanniArtifact() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "⚡ Fragment de Lune Noire");
            meta.setLore(Collections.singletonList(ChatColor.GRAY + "Utilise cet artefact pour invoquer ton domaine."));
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void giveArtifactToRanni(Player player) {
        if (plugin.getRoleManager().getRole(player).equalsIgnoreCase("Ranni")) {
            player.getInventory().addItem(getRanniArtifact());
            player.sendMessage(ChatColor.AQUA + "✨ Un étrange artefact apparaît dans ton inventaire...");
        }
    }

    private final Set<UUID> manaBuffer = new HashSet<>();

    @EventHandler
    public void onArtifactUse(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return; // ✅ Évite les appels de la main secondaire
        Player player = event.getPlayer();

        // ✅ Vérifie que l'event vient bien d'un clic droit et de la main principale
        if (!event.getAction().toString().contains("RIGHT_CLICK")) return; // Ignore les clics gauches

        // ✅ Vérifie que l'objet est bien la Nether Star de Ranni
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.NETHER_STAR || !item.hasItemMeta() ||
                !item.getItemMeta().getDisplayName().equals(ChatColor.LIGHT_PURPLE + "⚡ Fragment de Lune Noire")) {
            return;
        }

        String role = plugin.getRoleManager().getRole(player);

        // ✅ Vérifie que c'est bien Ranni
        if (!role.equalsIgnoreCase("Ranni")) return;

        // ✅ Vérifie si Ranni a un partenaire (empêche le spam)
        if (!hasPartner(player)) {
            if (!sentWarningRecently.contains(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "❌ Tu n’as pas encore de partenaire, tu ne peux pas activer ton pouvoir !");
                sentWarningRecently.add(player.getUniqueId());
                Bukkit.getScheduler().runTaskLater(plugin, () -> sentWarningRecently.remove(player.getUniqueId()), 20L);
            }
            return;
        }

        // ✅ Ajoute temporairement le joueur au buffer pour éviter le message "Pas assez de mana" juste après
        manaBuffer.add(player.getUniqueId());
        Bukkit.getScheduler().runTaskLater(plugin, () -> manaBuffer.remove(player.getUniqueId()), 20L); // 1s d'attente

        // ✅ Annule immédiatement l'event pour éviter les doubles appels
        event.setCancelled(true);

        // ✅ Vérifie d'abord le mana AVANT d'aller plus loin
        if (!plugin.getManaManager().consumeMana(player, 100)) {
            return;
        }

        // ✅ Met à jour le scoreboard APRES avoir confirmé l'utilisation du mana
        plugin.getManaManager().updateManaDisplay(player);

        // ✅ Ensuite, on active la capacité
        createLunarZone(player);
    }

    public void createLunarZone(Player ranni) {
        // ✅ Vérifie si le monde "Ranni" existe bien
        World ranniWorld = Bukkit.getWorld("Ranni");
        if (ranniWorld == null) {
            ranni.sendMessage(ChatColor.RED + "❌ La dimension Ranni n'existe pas ! Contactez un administrateur.");
            return;
        }

        // 📌 Détermine la position de téléportation (ex: 0, 50, 0 dans la dimension Ranni)
        Location teleportLocation = new Location(ranniWorld, 0, -59, 0);

        // 🔹 Liste pour stocker les joueurs à téléporter
        List<Player> playersToTeleport = new ArrayList<>();

        // 🔹 Vérifie chaque joueur en ligne
        for (Player online : Bukkit.getOnlinePlayers()) {
            double distance = online.getLocation().distance(ranni.getLocation()); // Distance par rapport à Ranni

            if (distance <= 20) { // ✅ Vérifie si le joueur est dans le rayon de 20 blocs
                playersToTeleport.add(online); // 🔹 Ajoute le joueur à la liste
            }
        }

        // ✅ Téléporte chaque joueur de la liste (évite les erreurs)
        for (Player p : playersToTeleport) {
            p.teleport(teleportLocation);
            p.sendMessage(ChatColor.LIGHT_PURPLE + "✨ Une force mystérieuse t’a transporté dans le domaine de Ranni...");
        }

        // ✅ Applique les effets
        for (Player online : playersToTeleport) {
            if (!online.equals(ranni)) {
                online.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 60, 0)); // Slowness I (1 min)
            } else {
                online.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 60, 0)); // Résistance I (1 min)
            }
        }

        // ⏳ Supprime la zone après 1 minute et renvoie les joueurs
        Bukkit.getScheduler().runTaskLater(plugin, () -> removeLunarZone(ranniWorld, playersToTeleport), 20 * 60);
    }

    public void removeLunarZone(World ranniWorld, List<Player> teleportedPlayers) {
        // 🔹 Téléporte tous les joueurs du monde Ranni vers un endroit aléatoire dans l’UHC
        World uhcWorld = Bukkit.getWorld("uhc");

        if (uhcWorld != null) {
            WorldBorder border = uhcWorld.getWorldBorder();
            Location spawn = uhcWorld.getSpawnLocation();
            int borderSize = (int) border.getSize() / 2; // Taille de la demi-bordure

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getWorld().getName().equals("Ranni")) { // Vérifie qu'ils sont bien dans le monde Ranni

                    // 📌 Coordonnées aléatoires dans la bordure
                    int x = spawn.getBlockX() + (int) (Math.random() * borderSize * 2) - borderSize;
                    int z = spawn.getBlockZ() + (int) (Math.random() * borderSize * 2) - borderSize;

                    // 📌 Trouver un sol solide
                    Location safeLocation = new Location(uhcWorld, x, uhcWorld.getHighestBlockYAt(x, z) + 1, z);

                    // 📌 TP sécurisé
                    online.teleport(safeLocation);
                    online.sendMessage(ChatColor.LIGHT_PURPLE + "✨ L'éclipse lunaire s’est dissipée... Tu as été téléporté dans l'Entre-terre !");
                }
            }
        }
    }

    private boolean nightMessageSent = false; // ✅ Évite le spam du message
    private boolean dayMessageSent = false;  // ✅ Évite le spam du message

    public void startNightResistanceTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (plugin.getRoleManager().getRole(player).equalsIgnoreCase("Ranni")) {
                    long time = player.getWorld().getTime();

                    if (time >= 13000 && time <= 23000) { // 🌙 C'est la nuit
                        // ✅ Applique la résistance pour **6 secondes** toutes les **5 secondes** (chevauchement)
                        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 120, 0, false, false));

                        if (!nightMessageSent) {
                            player.sendMessage(ChatColor.LIGHT_PURPLE + "✨ La nuit renforce ton corps... Résistance activée !");
                            nightMessageSent = true;
                            dayMessageSent = false; // Réinitialise pour le jour
                        }

                    } else { // ☀️ C'est le jour
                        if (player.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
                            player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                        }

                        if (!dayMessageSent) {
                            player.sendMessage(ChatColor.GRAY + "☀️ Le jour se lève... Tu perds ta résistance.");
                            dayMessageSent = true;
                            nightMessageSent = false; // Réinitialise pour la prochaine nuit
                        }
                    }
                }
            }
        }, 0L, 100L); // ✅ Vérifie toutes les **5 secondes** (100 ticks)
    }

    public boolean isInManaBuffer(Player player) {
        return manaBuffer.contains(player.getUniqueId());
    }
}