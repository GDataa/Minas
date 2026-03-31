package dev.gdataa.hyvel.minas.model;

import com.infernalsuite.asp.api.world.SlimeWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public class Mina {

    private final UUID dono;
    private final String nomeMundo;
    private SlimeWorld slimeWorld;

    public Mina(UUID dono) {
        this.dono = dono;
        this.nomeMundo = "mina_" + dono.toString();
    }

    public UUID getDono() { return dono; }
    public String getNomeMundo() { return nomeMundo; }

    public SlimeWorld getSlimeWorld() { return slimeWorld; }
    public void setSlimeWorld(SlimeWorld slimeWorld) { this.slimeWorld = slimeWorld; }

    public World getBukkitWorld() {
        return Bukkit.getWorld(this.nomeMundo);
    }

    public Location getSpawnLocation() {
        World world = getBukkitWorld();
        if (world != null) {
            // Atualizado com as coordenadas exatas da sua print!
            // X, Y, Z, Yaw (rotação pros lados), Pitch (rotação pra cima/baixo)
            return new Location(world, 0.5, 62.0, 0.5, -180f, 0f);
        }
        return null;
    }
}