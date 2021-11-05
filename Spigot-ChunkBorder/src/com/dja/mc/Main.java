package com.dja.mc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class Main extends JavaPlugin{

	private static final Material 	BORDER_MATERIAL = Material.CYAN_STAINED_GLASS;
	private static final BlockData 	BORDER_BLOCK = BORDER_MATERIAL.createBlockData();
	private static final int 		GLASS_BORDER = 3;
	private static final Material[] BLOCKS_TO_CONSUME = 
		{
		 Material.AIR, 
		 Material.GRASS, Material.LILY_PAD, Material.COBWEB, Material.FERN, Material.DEAD_BUSH, Material.SEAGRASS, 
		 Material.TALL_GRASS, Material.WATER, Material.LAVA, Material.OAK_SAPLING, Material.SPRUCE_SAPLING, Material.BIRCH_SAPLING,
		 Material.JUNGLE_SAPLING, Material.ACACIA_SAPLING, Material.DARK_OAK_SAPLING,Material.VINE, Material.FLOWERING_AZALEA,
		 Material.AZALEA, Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.LILY_OF_THE_VALLEY, 
		 Material.ALLIUM, Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP, Material.WHITE_TULIP,
		 Material.PINK_TULIP, Material.OXEYE_DAISY, Material.CORNFLOWER, Material.WITHER_ROSE, Material.SPORE_BLOSSOM,
		 Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.CRIMSON_FUNGUS, Material.WARPED_FUNGUS, Material.CRIMSON_ROOTS,
		 Material.WARPED_ROOTS, Material.NETHER_SPROUTS, Material.WEEPING_VINES, Material.TWISTING_VINES, Material.SUGAR_CANE, 
		 Material.HANGING_ROOTS,Material.BIG_DRIPLEAF, Material.SMALL_DRIPLEAF, Material.BAMBOO, Material.BAMBOO_SAPLING, 
		 Material.SUNFLOWER, Material.LILAC, Material.ROSE_BUSH, Material.PEONY, Material.LARGE_FERN
		};
	
	private static final String 	PLUGIN_NAME = "[WORLD_BORDER_PLUGIN]";
	private static final int 		BORDER_RADIUS = 10;	// 8 = Chunk
	
	private static final HashMap<UUID, WorldBorderPlayer> playerMap = new HashMap<>();
	
	private static Location spawn;
	private static Main p;
	private static int northX = 0;
	private static int southX = 0;
	private static int northZ = 0;
	private static int southZ = 0;
	
	@Override
	public void onEnable()
	{
		System.out.println(Main.PLUGIN_NAME + " has started!");
		
		p = this;
		
		spawn = Bukkit.getWorld("world").getSpawnLocation();
		Main.northX = Math.abs(spawn.getBlockX())+Main.BORDER_RADIUS;
		Main.northZ = Math.abs(spawn.getBlockZ())+Main.BORDER_RADIUS;
		Main.southX = Math.abs(spawn.getBlockX())-Main.BORDER_RADIUS;
		Main.southZ = Math.abs(spawn.getBlockZ())-Main.BORDER_RADIUS;
		
		Bukkit.getOnlinePlayers().forEach((p)->{
				Main.playerMap.put(p.getUniqueId(), new WorldBorderPlayer(p));
		});
		
		Bukkit.getPluginManager().registerEvents(new WorldBorderEventHandler(), this);
		
	}
	
	@Override
	public void onDisable()
	{
		Main.playerMap.values().forEach((p)->{p.clean(true);});
		System.out.println(Main.PLUGIN_NAME + " has disabled!");
	}
	
	private static boolean invalidLocation(Location x)
	{
		final int currX = Math.abs(x.getBlockX());
		final int currZ = Math.abs(x.getBlockZ());
		
		boolean xTrue = currX >= Main.southX && currX <= Main.northX;
		boolean zTrue = currZ >= Main.southZ && currZ <= Main.northZ;
		
		return !xTrue || !zTrue || x.getBlockY() >= 255;
	}
	
	private class WorldBorderPlayer
	{
		private static final int TASK_MAX_INCREMENTS = 5;
		private final List<Block> blocks;
		private final Player player;
		private BukkitTask task;
		private int taskIncrements;
		
		WorldBorderPlayer(Player p)
		{
			this.player = p;
			this.taskIncrements = TASK_MAX_INCREMENTS;
			this.blocks = new ArrayList<>();
		}
		
		public void add(Block b)
		{
			this.blocks.add(b);
		}
		
		public void clean(boolean clearAll)
		{
			this.blocks.removeIf((blk)->
			{
				boolean res = clearAll || player.getLocation().distance(blk.getLocation()) >= 5;
				if(res) player.sendBlockChange(blk.getLocation(), blk.getBlockData());
				return res;
			});
		}
		
		public void clean()
		{
			this.clean(false);
		}
		
		public void startTask()
		{
			
			this.task = new BukkitRunnable() {
				
				@Override
				public void run() {

			    	if(!player.isOnline()) return;
			    	
			    	if(taskIncrements == 0)
			    	{
			    		player.setHealth(0);
			    		Bukkit.broadcastMessage(ChatColor.RED + "[WARNING] " + player.getDisplayName() + " is cheating! They have been killed.");
			    		stopTask();
			    	}
			    	else if(taskIncrements > 0)
			    	{
				    	Bukkit.broadcastMessage(ChatColor.RED + "[WARNING] " + player.getDisplayName() + " is cheating! They will be killed in " + taskIncrements-- + " seconds.");
				    	
				    	double toTake = 20 / TASK_MAX_INCREMENTS;
				    	double health = toTake * taskIncrements;

				    	if(health < player.getHealth())
			    		{
				    		if(player.getHealth() <= toTake)
				    		{
				    			player.damage(player.getHealth()-1);
				    			player.setHealth(2);
				    		}
				    		else
				    		{
				    			player.damage(toTake);
				    		}
			    		}
				    	
			    	}
					
				}
				
			}.runTaskTimer(Main.p, 0, 20);
			
		}
		
		public void stopTask()
		{
	    	this.task.cancel();
			reset();
		}
		
		private void reset()
		{
			this.taskIncrements = TASK_MAX_INCREMENTS;
		}
		
		public boolean taskActive()
		{
			return this.taskIncrements != TASK_MAX_INCREMENTS;
		}
		
	}
	
	private class WorldBorderEventHandler implements Listener
	{
	
		@EventHandler
		public void playerJoin(PlayerJoinEvent e)
		{
			UUID id = e.getPlayer().getUniqueId();
			if(!Main.playerMap.containsKey(id))
			{
				Main.playerMap.put(id, new WorldBorderPlayer(e.getPlayer()));
			}
		}
		
		@EventHandler
		public void playerInt(PlayerInteractEvent e)
		{

			if (e.getClickedBlock() != null) {

				Block b = e.getClickedBlock();
								
				if(Main.invalidLocation(b.getLocation()))
				{
					if(b.getLocation().distance(spawn) > (Main.BORDER_RADIUS*2))
						e.setCancelled(true);
				}
			}
		}
		
		@EventHandler
		public void death(PlayerDeathEvent e)
		{
			WorldBorderPlayer wbp = Main.playerMap.get(e.getEntity().getUniqueId());
			if(wbp.taskActive())
			{
				wbp.stopTask();
			}
		}
		
		@EventHandler
		public void respawn(PlayerRespawnEvent e)
		{
			WorldBorderPlayer wbp = Main.playerMap.get(e.getPlayer().getUniqueId());
			if(wbp.taskActive())
			{
				wbp.stopTask();
			}
		}
		
		@EventHandler
		public void move(PlayerMoveEvent e)
		{

			Location l = e.getPlayer().getLocation();
			Player p = e.getPlayer();
			WorldBorderPlayer wbp = Main.playerMap.get(p.getUniqueId());
			
			if(Main.invalidLocation(e.getPlayer().getLocation()))
			{
				if(!wbp.taskActive())
				{
					wbp.startTask();
				}
			}
			else
			{
				if(wbp.taskActive())
				{
					wbp.stopTask();
				}
			}
			
			
			for(int x = l.getBlockX()-Main.GLASS_BORDER-2; x < l.getBlockX()+Main.GLASS_BORDER+2; x++)
			{
				for(int y = l.getBlockY()-Main.GLASS_BORDER-3; y < l.getBlockY()+(Main.GLASS_BORDER+3); y++)
				{
					for(int z = l.getBlockZ()-Main.GLASS_BORDER; z < l.getBlockZ()+Main.GLASS_BORDER; z++)
					{
						if(checkBlock(p, p.getWorld().getBlockAt(x, y, z).getLocation()))
						{
							Main.playerMap.get(p.getUniqueId()).add(p.getWorld().getBlockAt(x, y, z));
						}
					}
				}
			}
			
			Main.playerMap.get(p.getUniqueId()).clean();
		}
	
		private boolean checkBlock(Player p, Location blockToSpoof)
		{
			if(Main.invalidLocation(blockToSpoof) && p.getLocation().distance(blockToSpoof) <= 3)
			{
				if(Arrays.stream(BLOCKS_TO_CONSUME).anyMatch((m) -> {return blockToSpoof.getBlock().getType()==m;} ))
				{
					p.sendBlockChange(blockToSpoof, BORDER_BLOCK);
					return true;
				}
			}
			return false;
		}
		
	}
		
}
