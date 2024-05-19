package me.MnMaxon.AutoPickup;

import java.util.ArrayList;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class MainListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (p.hasPermission(Permissions.INFINITE_PICK) && p.getItemInHand().getType().name().contains("PICK"))
            p.getItemInHand().setDurability((short) -1);
        if (p.hasPermission(Permissions.AUTO_SMELT) &&
                p.getItemInHand().containsEnchantment(Enchantment.LOOT_BONUS_BLOCKS)) {
            Material material;
            if (e.getBlock().getType().equals(Material.IRON_ORE)) {
                material = Material.IRON_INGOT;
            } else if (e.getBlock().getType().equals(Material.GOLD_ORE)) {
                material = Material.GOLD_INGOT;
            } else {
                return;
            }
            int i = p.getItemInHand().getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS);
            int j = (new Random()).nextInt(i);
            ItemStack finalItem = new ItemStack(material);
            finalItem.setAmount(j);
            Location loc = e.getBlock().getLocation().add(0.5D, 0.5D, 0.5D);
            loc.getWorld().dropItemNaturally(loc, finalItem);
        }
        p.giveExp(e.getExpToDrop());
        e.setExpToDrop(0);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent e) {
        if (!NoPickup.canPickup(e.getEntity().getItemStack())) {
            e.getEntity().setItemStack(NoPickup.remove(e.getEntity().getItemStack()));
            return;
        }
        Player p = null;
        if (p == null) {
            p = getNearby(e.getEntity(), 0.5D);
            if (p == null) {
                p = getNearby(e.getEntity(), 1.0D);
                if (p == null) {
                    p = getNearby(e.getEntity(), 2.0D);
                    if (p == null) {
                        p = getNearby(e.getEntity(), 3.0D);
                        if (p == null) {
                            p = getNearby(e.getEntity(), 4.0D);
                            if (p == null) {
                                p = getNearby(e.getEntity(), 5.0D);
                                if (p == null) {
                                    p = getNearby(e.getEntity(), 6.0D);
                                    if (p == null)
                                        return;
                                }
                            }
                        }
                    }
                }
            }
        }
        ArrayList<ItemStack> finalItems = new ArrayList<>();
        finalItems.add(e.getEntity().getItemStack());
        if(p.hasMetadata("AutoSellForPlayer-pickUp")){
            Plugin plugin= Bukkit.getServer().getPluginManager().getPlugin("AutoSellForPlayer");
            if(plugin!=null){
                soys.autosellforplayer.Main AutoSell=(soys.autosellforplayer.Main)plugin;
                for (ItemStack is : finalItems) {
                    AutoSell.onPickupItem(new PlayerPickupItemEvent(p,e.getEntity(),is.getAmount()));
                    if(e.getEntity().isDead()){
                        finalItems.remove(is);
                    }
                }
            }
        }

        if (p.hasPermission(Permissions.AUTO_SMELT)) {
            for (ItemStack is : finalItems) {
                if (Main.blocksToSmelt.containsKey(is.getType())) {
                    is.setType(Main.blocksToSmelt.get(is.getType()).getNewType());
                }
            }
        }
        finalItems = Main.addToInventory(p, finalItems);
        if (p.hasPermission(Permissions.AUTO_BLOCK)) {
            ItemStack[] newInvCont = Main.convertToBlocks(p.getInventory().getContents());
            if (!p.getInventory().getContents().equals(newInvCont)) {
                p.getInventory().setContents(newInvCont);
                p.updateInventory();
            }
        }
        if (!finalItems.isEmpty())
            p.sendMessage(Messages.FULL_INVENTORY);
        e.getEntity().remove();
    }

    private Player getNearby(Item item, double range) {
        for (Entity e : item.getNearbyEntities(range, range, range)) {
            if (e instanceof Player)
                return (Player) e;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent e) {
        ArrayList<ItemStack> drops = new ArrayList<>(e.getDrops());
        e.getDrops().clear();
        Player killer = e.getEntity().getKiller();
        if (e.getEntity() instanceof Player) {
            for (ItemStack is : drops) {
                is = NoPickup.add(is);
                e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), is);
            }
        } else if (killer != null && killer.isValid()) {
            for (ItemStack is : drops)
                killer.getWorld().dropItemNaturally(killer.getLocation(), is);
        }
        if (killer != null && !(e.getEntity() instanceof Player)) {
            killer.giveExp(e.getDroppedExp());
            e.setDroppedExp(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDrop(PlayerDropItemEvent e) {
        if (!e.isCancelled())
            e.getItemDrop().setItemStack(NoPickup.add(e.getItemDrop().getItemStack()));
    }
}


