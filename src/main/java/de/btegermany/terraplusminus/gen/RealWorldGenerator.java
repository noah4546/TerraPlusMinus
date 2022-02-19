package de.btegermany.terraplusminus.gen;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import de.btegermany.terraplusminus.data.TerraConnector;
import io.papermc.lib.PaperLib;
import lombok.SneakyThrows;
import net.buildtheearth.terraminusminus.generator.CachedChunkData;
import net.buildtheearth.terraminusminus.generator.ChunkDataLoader;
import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.substitutes.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


public class RealWorldGenerator extends ChunkGenerator {
    private Location spawnLocation = null;

    EarthGeneratorSettings settings = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS);
    ChunkDataLoader loader;

    Plugin plugin;
    public LoadingCache<ChunkPos, CompletableFuture<CachedChunkData>> cache = null;


    public RealWorldGenerator(Plugin pPlugin){
        plugin = pPlugin;
        this.loader = new ChunkDataLoader(settings);
    }


    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int x, int z, @NotNull ChunkData chunkData) {

    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        return new CustomBiomeProvider();
    }


    public void generateSurface(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        final int minY = worldInfo.getMinHeight();
        final int maxY = worldInfo.getMaxHeight();


       // getChunkAsync(Bukkit.getWorld(worldInfo.getUID()),chunkX, chunkZ).whenComplete((ignored, throwable) -> {


            try {
                CachedChunkData terraData = this.loader.load(new ChunkPos(chunkX,chunkZ)).get();//this.loader.load(new ChunkPos(chunkX, chunkZ)).get();//new ChunkPos(chunkX, chunkZ)
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {

                        int groundY = terraData.groundHeight(x, z);
                        int waterY = terraData.waterHeight(x, z);
                        BlockState state = terraData.surfaceBlock(x, z);

                        Material material = Material.GRASS_BLOCK;
                        // Generates mountains over 1700m only from stone
                        int randomizer = (int) Math.floor(Math.random()*(1700-1695+1)+1695);
                        if(groundY >= randomizer) {
                            material = Material.STONE;
                        }
                        //--------------------------------------------------------


                        for (int y = minY; y < Math.min(maxY, groundY); y++) chunkData.setBlock(x, y, z, Material.STONE);

                        if (groundY < maxY) {
                            if(state != null){

                                //System.out.println(state.getBlock().toString());
                                switch (state.getBlock().toString()) {
                                    case "minecraft:dirt_path":
                                        chunkData.setBlock(x, groundY, z, Material.MOSS_BLOCK);
                                        break;
                                    default:
                                        chunkData.setBlock(x, groundY, z, BukkitBindings.getAsBlockData(state));
                                        break;
                                }

                            } else {
                                chunkData.setBlock(x, groundY, z, material);
                            }

                        }
                        for (int y = groundY + 1; y < Math.min(maxY, waterY); y++) chunkData.setBlock(x, y, z, Material.WATER);

                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
     //   });
    }
/*
    public CompletableFuture<Void> getChunkAsync(World world, int x, int z) {
        return CompletableFuture.allOf(PaperLib.getChunkAtAsync(world, x, z));
    }

    private ChunkPos getChunkPos(World world,int chunkX, int chunkZ) throws ExecutionException, InterruptedException {
        CompletableFuture<Chunk> chunk = PaperLib.getChunkAtAsync(world, chunkX, chunkZ);
        chunk.thenAccept(marked -> {Bukkit.getServer().getWorld(String.valueOf(world)).setChunkForceLoaded(chunkX, chunkZ, true); });

        ChunkPos pos = new ChunkPos(chunk.get().getX(),chunk.get().getZ());
        this.cache = CacheBuilder.newBuilder()
                .expireAfterAccess(5L, TimeUnit.MINUTES)
                .softValues()
                .build(new ChunkDataLoader(this.settings));
        CompletableFuture<CachedChunkData> future = this.cache.get(pos);
        return pos;
    }*/

    public void generateBedrock(@NotNull WorldInfo worldInfo, @NotNull Random random, int x, int z, @NotNull ChunkGenerator.ChunkData chunkData) {
        // no bedrock, because bedrock bad
    }

    public void generateCaves(@NotNull WorldInfo worldInfo, @NotNull Random random, int x, int z, @NotNull ChunkGenerator.ChunkData chunkData) {
        // no caves, because caves scary
    }



    public int getBaseHeight(@NotNull WorldInfo worldInfo, @NotNull Random random, int x, int z, @NotNull HeightMap heightMap) {
        throw new UnsupportedOperationException("Not implemented");
    }


    @NotNull
    @Deprecated
    public ChunkGenerator.ChunkData generateChunkData(@NotNull World world, @NotNull Random random, int x, int z, @NotNull ChunkGenerator.BiomeGrid biome) {

        throw new UnsupportedOperationException("Custom generator " + getClass().getName() + " is missing required method generateChunkData");
    }


    public boolean canSpawn(@NotNull World world, int x, int z) {
        Block highest = world.getBlockAt(x, world.getHighestBlockYAt(x, z), z);

        switch (world.getEnvironment()) {
            case NETHER:
                return true;
            case THE_END:
                return highest.getType() != Material.AIR && highest.getType() != Material.WATER && highest.getType() != Material.LAVA;
            case NORMAL:
            default:
                return highest.getType() == Material.SAND || highest.getType() == Material.GRAVEL;
        }
    }

    @NotNull
    public List<BlockPopulator> getDefaultPopulators(@NotNull World world) {
        return new ArrayList<BlockPopulator>();
    }

    @Nullable
    public Location getFixedSpawnLocation(@NotNull World world, @NotNull Random random) {
        if (spawnLocation == null)
            spawnLocation = new Location(world, 3517417, 58,-5288234);
        return spawnLocation;
    }

    @Deprecated
    public boolean isParallelCapable() {
        return false;
    }


    public boolean shouldGenerateNoise() {
        return false;
    }


    public boolean shouldGenerateSurface() {
        return false;
    }


    public boolean shouldGenerateBedrock() {
        return false;
    }


    public boolean shouldGenerateCaves() {
        return false;
    }


    public boolean shouldGenerateDecorations() {
        return false;
    }


    public boolean shouldGenerateMobs() {
        return false;
    }

    public boolean shouldGenerateStructures() {
        return false;
    }


    /*@NotNull
    public ChunkGenerator.ChunkData createVanillaChunkData(@NotNull World world, int x, int z) {
        var chunk = Bukkit.getServer().createChunkData(world);

        Field maxHeightField = null;
        try {
            maxHeightField = chunk.getClass().getDeclaredField("maxHeight");
            maxHeightField.setAccessible(true);
            maxHeightField.set(chunk, 2032);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return chunk;

    }*/
    // Paper
}
