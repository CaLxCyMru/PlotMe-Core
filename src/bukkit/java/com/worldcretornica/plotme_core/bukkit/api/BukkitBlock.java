package com.worldcretornica.plotme_core.bukkit.api;

import com.worldcretornica.plotme_core.api.IBlock;
import com.worldcretornica.plotme_core.api.IWorld;
import com.worldcretornica.plotme_core.api.Location;
import com.worldcretornica.plotme_core.api.Vector;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

public class BukkitBlock implements IBlock {

    private final Block block;

    public BukkitBlock(Block block) {
        this.block = block;
    }

    @Override
    public Location getLocation() {
        return new Location(getWorld(), getPosition());
    }

    @Override
    public IWorld getWorld() {
        return new BukkitWorld(block.getWorld());
    }

    @Override
    public int getX() {
        return block.getX();
    }

    @Override
    public int getY() {
        return block.getY();
    }

    @Override
    public int getZ() {
        return block.getZ();
    }

    @Deprecated
    @SuppressWarnings("deprecation")
    @Override
    public int getTypeId() {
        return block.getTypeId();
    }

    @Deprecated
    @Override
    public String getBiome() {

        return block.getBiome().toString();
    }

    @Deprecated
    @Override
    public void setBiome(Biome plains) {
        block.setBiome(plains);
    }

    @Deprecated
    @SuppressWarnings("deprecation")
    @Override
    public boolean setTypeIdAndData(short id, byte data, boolean applyPhysics) {
        return block.setTypeIdAndData(id, data, applyPhysics);
    }

    @Deprecated
    @SuppressWarnings("deprecation")
    @Override
    public byte getData() {
        return block.getData();
    }

    @Deprecated
    @SuppressWarnings("deprecation")
    @Override
    public void setTypeId(int id, boolean applyPhysics) {
        block.setTypeId(id, applyPhysics);
    }

    @Deprecated
    @Override
    public void setType(Material material, boolean applyPhyics) {
        block.setType(material, applyPhyics);
    }

    @Deprecated
    @Override
    public Material getType() {
        return block.getType();
    }

    @Deprecated
    @Override
    public BlockState getState() {
        return block.getState();
    }

    @Override
    public void setData(byte b, boolean b1) {
        block.setData(b, b1);
    }

    @Override
    public String toString() {
        return "Bukkit Block: " + getTypeId() + ":" + getData();
    }

    public Vector getPosition() {
        return new Vector(block.getX(), block.getY(), block.getZ());
    }
}
