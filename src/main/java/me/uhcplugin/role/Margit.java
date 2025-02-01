package me.uhcplugin.role;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Margit implements Listener {
    private final Player player;
    private static final Material ABILITY_ITEM = Material.STICK; // Bâton standard
    private static final String ITEM_NAME = ChatColor.GOLD + "Marteau de Margit";

    public Margit(Player player) {
        this.player = player;
        giveAbilityItem();
    }

    private void giveAbilityItem() {
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("UHCPlugin"), () -> {
            ItemStack item = new ItemStack(ABILITY_ITEM);
            ItemMeta meta = item.getItemMeta();
            
            if (meta != null) {
                meta.setDisplayName(ITEM_NAME);
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
            
            player.getInventory().addItem(item);
            player.sendMessage(ChatColor.GREEN + "Debug : Item reçu !");
        }, 200L); // 10 secondes
    }

    @EventHandler
    public void onAbilityUse(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();

        // Vérification identique à la boussole
        if (item.getType() == ABILITY_ITEM 
            && item.hasItemMeta()
            && item.getItemMeta().getDisplayName().equals(ITEM_NAME)) {
            
            p.sendMessage(ChatColor.GREEN + "Debug : Interaction détectée !");
            event.setCancelled(true); // Bloque l'action normale
            
            // Ajouter ici le cooldown et la compétence plus tard
        }
    }
}