package soys.autosellforplayer;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Main extends JavaPlugin implements Listener {
    public static final String DATA_NAME="AutoSellForPlayer-pickUp";
    public static final String AUTO_SELL_PERMISSION="AutoSellForPlayer.AUTO_SELL_PERMISSION";
    public static final long DELAY_TIME=60000;
    public static Economy ECON;
    public static Main plugin;
    public static boolean testMod=false;
    @Override
    public void onEnable() {
        plugin=this;
        loadEconomy();
        Bukkit.getPluginManager().registerEvents(this,this);
        sendPluginText("拾取自动出售 事件注册完毕");
        saveDefaultConfig();
        sendPluginText("成功加载配置文件");
        SellItemStack.loadSellItemStack();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length==0 && sender instanceof Player){
            Player player=(Player)sender;
            if(!player.hasPermission(AUTO_SELL_PERMISSION)){
                sendColorText(sender,"&4缺少权限!您需要 &e"+AUTO_SELL_PERMISSION+" &4权限才能执行该指令");
                return true;
            }
            if(player.hasMetadata(DATA_NAME)){
                player.removeMetadata(DATA_NAME,plugin);
                sendColorText(sender,"&4关闭&2拾取掉落自动出售功能");
            }else {
                player.setMetadata(DATA_NAME,new FixedMetadataValue(plugin, System.currentTimeMillis()+DELAY_TIME));
                sendColorText(sender,"&2开启拾取掉落自动出售功能");
            }
        }
        if(!sender.isOp()){
            return true;
        }
        if(args.length==1){
            if(args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                SellItemStack.loadSellItemStack();
                sendColorText(sender,"&2成功加载配置文件与 自动出售物品 ");
            }
            if(args[0].equalsIgnoreCase("testmod")){
                testMod=!testMod;
                sendColorText(sender,"&2测试模式信息输出至后台,测试模式修改为 :" +testMod);
            }
        }
        if(args.length==3 && sender instanceof Player && args[0].equalsIgnoreCase("autosell")){
            Player player=(Player)sender;
            ItemStack hand=player.getItemInHand();
            if(hand==null || hand.getType()==Material.AIR){
                sendColorText(sender,"&4玩家手持物品为空,无法设置为 拾取自动售出物品");
                return true;
            }
            ItemStack copyHand=hand.clone();
            copyHand.setAmount(1);
            if(SellItemStack.getSellItemHashMap().containsKey(copyHand)){
                sendColorText(sender,"&4已存在相同的 拾取自动售出物品,若想修改出售价格请前往配置文件修改");
                return true;
            }
            String minString=args[1];
            String maxString=args[2];
            double min;
            double max;
            try{
                min=Double.parseDouble(minString);
                max=Double.parseDouble(maxString);
            }catch (NumberFormatException e){
                sendColorText(sender,"&4"+minString+" 和 "+maxString+" 必须为有效的数字");
                return true;
            }
            ConfigurationSection section=new YamlConfiguration();

            section.set("item",copyHand);
            section.set("sell.min",Math.min(min,max));
            section.set("sell.max",Math.max(min,max));
            getConfig().set("autoSell."+ SellItemStack.getSellItemHashMap().size(),section);
            try {
                getConfig().save(new File(getDataFolder(),"config.yml"));
            } catch (IOException e) {
                sendColorText(sender,"&4保存配置文件时发生严重异常");
                throw new RuntimeException(e);
            }
            SellItemStack.getSellItemHashMap().put(copyHand,new SellItemStack(section));
            sendColorText(sender,"&2成功添加拾取自动售出物品");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ArrayList<String> tabs=new ArrayList<>();
        if(sender.isOp()){
            if(args.length==1) {
                tabs.add("autosell");
                tabs.add("reload");
                tabs.add("testmod");
            }
        }
        return tabs;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPickupItem(PlayerPickupItemEvent event){
        /*
        LivingEntity livingEntity=event.getEntity();
        if(!(livingEntity instanceof Player)){
            return;
        }
        Player player=(Player)livingEntity;
        */
        Player player=event.getPlayer();
        sendTestModPluginText("玩家"+player.getName()+"拾取事件触发");
        if(!player.hasMetadata(DATA_NAME)){
            sendTestModPluginText("玩家"+player.getName()+"未开启 拾取自动出售");
            return;
        }
        ItemStack itemStack=event.getItem().getItemStack();
        if(itemStack==null || itemStack.getType()== Material.AIR){
            sendTestModPluginText("玩家拾取物品为 空气");
            return;
        }
        int amount= itemStack.getAmount();
        ItemStack copyItemStack=itemStack.clone();
        copyItemStack.setAmount(1);
        SellItemStack sellItemStack=SellItemStack.getSellItemHashMap().get(copyItemStack);
        if(sellItemStack==null){
            sendTestModPluginText(itemStack +" 未匹配到 拾取自动售出");
            return;
        }
        sendTestModPluginText("玩家"+player.getName()+" 满足自动出售条件,正在自动出售");
        /*
        List<MetadataValue> metadataValueList=player.getMetadata(DATA_NAME);
        metadataValueList.forEach(metadataValue -> {
            if(metadataValue.getOwningPlugin()==plugin){
                long time=(long)metadataValue.value();
                if(time>System.currentTimeMillis()){

                }
            }
        });
        */
        double randomMoney=sellItemStack.getRandomValue()*amount;
        getEconomy().depositPlayer(player,randomMoney);
        sendPlayerMessage(itemStack,player,randomMoney);
        event.setCancelled(true);
        event.getItem().remove();
        //getConfig().isConfigurationSection()

    }

    public static void sendPlayerMessage(ItemStack itemStack,Player player,double money){
        boolean enable=plugin.getConfig().getBoolean("autoSellMessage.enable",true);
        if(!enable){
            return;
        }
        String message=ChatColor.translateAlternateColorCodes('&',plugin.getConfig().getString("autoSellMessage.message","&e[asfp]&2拾取物品 @ITEMNAME@ 自动出售,获得&6 @MONEY@ &2元"));
        message=message.replaceAll("@ITEMNAME@",itemStack.getType().name());
        message=message.replaceAll("@MONEY@", String.valueOf(money));
        player.sendMessage(message);
    }

    public static Economy getEconomy() {
        return ECON;
    }

    public void loadEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            sendPluginText("你的服务端没有安装Vault 未启用经济相关功能.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            sendPluginText("你的服务端没有安装Vault 未启用经济相关功能.");
            return;
        }
        ECON = rsp.getProvider();
        sendPluginText("已成功加载Vault 启用经济相关功能");
    }

    public static void sendTestModPluginText(String message){
        if(testMod) {
            Bukkit.getLogger().info("[" + plugin.getName() + "][TESTMOD] -> " + message);
        }
    }

    public static void sendColorText(CommandSender sender,String message){
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&e["+plugin.getName()+"]&f"+message));
    }

    public static void sendPluginText(String message){
        Bukkit.getLogger().info("["+plugin.getName()+"]"+message);
    }
}
