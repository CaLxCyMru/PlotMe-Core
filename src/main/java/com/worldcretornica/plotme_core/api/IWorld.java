package com.worldcretornica.plotme_core.api;


import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.util.List;
import java.util.UUID;

public interface IWorld {

    /**
     * Get the name of the world
     *
     * @return world name
     */
    String getName();

    File getWorldFolder();

    UUID getUUID();

    void refreshChunk(int x, int z);

    IBlock getBlockAt(int x, int y, int z);

    IBlock getBlockAt(Vector add);

    void getBiome(Vector position);

    List<IEntity> getEntities();

    Entity spawnEntity(Location etloc, EntityType entitytype);
}
