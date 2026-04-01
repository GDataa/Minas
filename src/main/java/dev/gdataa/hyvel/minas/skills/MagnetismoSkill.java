package dev.gdataa.hyvel.minas.skills;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

public class MagnetismoSkill {

    public static void disparar(Player player, Location blocoLoc, Material mat) {
        // O bloco voa para a localização do jogador (um pouco acima dos pés)
        Location target = player.getLocation().add(0, 1.2, 0);
        int tempoVoo = 8; // 8 ticks = 0.4 segundos (rápido e satisfatório)

        blocoLoc.getWorld().spawn(blocoLoc, BlockDisplay.class, bd -> {
            bd.setBlock(mat.createBlockData());
            bd.setPersistent(false);

            // Configura a interpolação para o cliente fazer o movimento suave
            bd.setInterpolationDelay(0);
            bd.setInterpolationDuration(tempoVoo);

            // Vetor da viagem
            Vector v = target.toVector().subtract(blocoLoc.toVector());

            Transformation t = bd.getTransformation();
            t.getTranslation().set((float) v.getX(), (float) v.getY(), (float) v.getZ());
            t.getScale().set(0.2f, 0.2f, 0.2f); // Encolhe ao chegar ao player

            bd.setTransformation(t);

            // Remove a entidade após o tempo de voo
            org.bukkit.Bukkit.getScheduler().runTaskLater(dev.gdataa.hyvel.MinasMain.getPlugin(dev.gdataa.hyvel.MinasMain.class), bd::remove, tempoVoo);
        });
    }
}
