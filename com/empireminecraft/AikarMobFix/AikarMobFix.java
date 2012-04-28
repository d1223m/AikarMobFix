package com.empireminecraft.AikarMobFix;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.plugin.java.JavaPlugin;


public class AikarMobFix extends JavaPlugin implements Listener {
	Logger log;
		
	private class ChunkCountCache {
		public int count;
		public long checkTime;
		public long lastChecked;
	}
	
	public Map<Chunk, ChunkCountCache> chunkCountCache = new HashMap<Chunk, ChunkCountCache>();
	
	public void onEnable() {
		log = this.getLogger();
		
		saveDefaultConfig();
		
		getServer().getPluginManager().registerEvents(this, this);
		
		long cacheMaxLifeCheckInterval = getConfig().getLong("cacheMaxLifeCheckInterval");
		
		if( cacheMaxLifeCheckInterval > 0 ) { 
			getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
				public void run() { 
					int preSize = chunkCountCache.size();
					log.info("clearing cache entries "+chunkCountCache.size());
					long now = System.currentTimeMillis();
					long cacheMaxLife = getConfig().getLong("cacheMaxLife");
					Iterator<ChunkCountCache> iter = chunkCountCache.values().iterator();
					while( iter.hasNext() ) {
						ChunkCountCache cache = iter.next();
						if( now - cache.lastChecked > cacheMaxLife ) {
							iter.remove();
						}
					}
					log.info("cleared cache entries "+preSize+ " -> "+chunkCountCache.size());
				}
			}, cacheMaxLifeCheckInterval, cacheMaxLifeCheckInterval);
		}
	}

	private int countMobsInChunk(Chunk chunk) {
		int count = 0;
		for( Entity entity : chunk.getEntities()) {
			if( entity instanceof Monster ) {
				count ++;
			}
		}
		return count;
	}
	
	private int getMobsInChunk(Chunk chunk) {
		long cacheTime = getConfig().getLong("countCacheTime");
		if( cacheTime == 0 ) { return countMobsInChunk(chunk); }
		ChunkCountCache cache = chunkCountCache.get(chunk);
		long now = System.currentTimeMillis();
		if( cache == null ) { 
			log.info("no cache found for "+chunk);
			cache = new ChunkCountCache();
			cache.checkTime = now;
			cache.count = countMobsInChunk(chunk);
			cache.lastChecked = now;
			chunkCountCache.put(chunk, cache);
			return cache.count;
		}
		//log.info("found cache item for chunk "+chunk+" old: "+(now - cache.checkTime)+" cache time "+cacheTime+" last count "+cache.count);
		if( now - cache.checkTime > cacheTime ) { 
			cache.count = countMobsInChunk(chunk);
			cache.checkTime = now;
		}
		cache.lastChecked = now;
		return cache.count;
	}
	
	@SuppressWarnings("unused")
	@EventHandler
	private void entitySpawn(CreatureSpawnEvent event) {
		if( event.getSpawnReason() == SpawnReason.SPAWNER 
		|| (getConfig().getBoolean("limitNatural") && event.getSpawnReason() == SpawnReason.NATURAL)
		) { 
			Chunk chunk = event.getLocation().getChunk();
			
			int mobCount =getMobsInChunk(chunk);
			if( getConfig().getBoolean("countSurrounding") ) { 
				World world = chunk.getWorld();
				mobCount +=getMobsInChunk(world.getChunkAt(chunk.getX()-1, chunk.getZ()-1));
				mobCount +=getMobsInChunk(world.getChunkAt(chunk.getX()  , chunk.getZ()-1));
				mobCount +=getMobsInChunk(world.getChunkAt(chunk.getX()+1, chunk.getZ()-1));
				
				mobCount +=getMobsInChunk(world.getChunkAt(chunk.getX()-1, chunk.getZ()  ));
				//mobCount +=getMobsInChunk(world.getChunkAt(chunk.getX()  , chunk.getZ()-1));
				mobCount +=getMobsInChunk(world.getChunkAt(chunk.getX()+1, chunk.getZ()  ));
	
				mobCount +=getMobsInChunk(world.getChunkAt(chunk.getX()-1, chunk.getZ()+1));
				mobCount +=getMobsInChunk(world.getChunkAt(chunk.getX()  , chunk.getZ()+1));
				mobCount +=getMobsInChunk(world.getChunkAt(chunk.getX()+1, chunk.getZ()+1));
	
			}
			
			log.info("mob spawn at "+chunk+" mob count is "+mobCount + "/"+getConfig().getInt("mobCap"));
			
			if( mobCount > getConfig().getInt("mobCap") ) {
				event.setCancelled(true);
			}
		}
	}
}
