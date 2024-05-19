package soys.autosellforplayer;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Set;

public class SellItemStack {
    private static HashMap<ItemStack,SellItemStack> sellItemHashMap=new HashMap<>();
    public static void loadSellItemStack(){
        ConfigurationSection autoSell=Main.plugin.getConfig().getConfigurationSection("autoSell");
        Set<String> stringSet=autoSell.getKeys(false);
        stringSet.forEach(sell -> {
            ConfigurationSection autoSellConfigurationSection=autoSell.getConfigurationSection(sell);
            ItemStack itemStack=autoSellConfigurationSection.getItemStack("item");
            if(itemStack==null){
                Main.sendTestModPluginText(autoSellConfigurationSection.getCurrentPath()+" 配置的物品无法被正常获取,请重新设置");
                return;
            }
            if(getSellItemHashMap().containsKey(itemStack)){
                Main.sendTestModPluginText(autoSellConfigurationSection.getCurrentPath()+" 配置的物品被检测到重复,将保留原有检测,请自行前往配置文件删除重复项");
                return;
            }
            getSellItemHashMap().put(itemStack,new SellItemStack(autoSellConfigurationSection));
        });
    }

    public static HashMap<ItemStack, SellItemStack> getSellItemHashMap() {
        return sellItemHashMap;
    }

    public ConfigurationSection section;
    public SellItemStack(ConfigurationSection section){
        this.section=section;
    }

    public double getRandomValue(){
        double min=section.getDouble("sell.min");
        double max=section.getDouble("sell.max");
        if(min==max){
            return min;
        }
        double money=Math.random()*(max-min)+min;
        String str = String.format("%.2f",money);
        return Double.parseDouble(str);
    }
}
