package dev.gdataa.hyvel.minas.skills;

import dev.gdataa.hyvel.MinasMain;
import dev.gdataa.hyvel.minas.model.Mina;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Bee;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EnxameSkill extends BukkitRunnable {

    private final MinasMain plugin;
    private final Player player;
    private final Mina mina;

    private class AbelhaKamikaze {
        Bee entidade;
        Location alvo;
        Vector direcao;
    }

    private final List<AbelhaKamikaze> enxame = new ArrayList<>();
    private int ticksDeVida = 0;
    private final Random random = new Random();

    private final int minX, maxX, minY, maxY, minZ, maxZ;

    public EnxameSkill(MinasMain plugin, Player player, Mina mina, int nivel) {
        this.plugin = plugin;
        this.player = player;
        this.mina = mina;

        this.minY = plugin.getConfig().getInt("regiao.minY");
        this.maxY = plugin.getConfig().getInt("regiao.maxY");
        this.minX = Math.min(plugin.getConfig().getInt("regiao.minX"), plugin.getConfig().getInt("regiao.maxX"));
        this.maxX = Math.max(plugin.getConfig().getInt("regiao.minX"), plugin.getConfig().getInt("regiao.maxX"));
        this.minZ = Math.min(plugin.getConfig().getInt("regiao.minZ"), plugin.getConfig().getInt("regiao.maxZ"));
        this.maxZ = Math.max(plugin.getConfig().getInt("regiao.minZ"), plugin.getConfig().getInt("regiao.maxZ"));

        // A quantidade de abelhas depende do nível (Mínimo 3, Máximo 10)
        int qtdAbelhas = Math.min(10, 3 + (nivel / 10));

        player.playSound(player.getLocation(), Sound.ENTITY_BEE_LOOP, 1f, 1.5f);
        player.sendMessage("§e§lBZZZZ! §7O enxame explosivo foi libertado!");

        // Nascer as abelhas no topo da mina
        for (int i = 0; i < qtdAbelhas; i++) {
            Location spawn = new Location(mina.getBukkitWorld(),
                    minX + random.nextInt((maxX - minX) + 1), maxY + 2, minZ + random.nextInt((maxZ - minZ) + 1));

            Bee bee = (Bee) mina.getBukkitWorld().spawnEntity(spawn, EntityType.BEE);
            bee.setAI(false); // Nós controlamos o voo!
            bee.setInvulnerable(true);
            bee.setAnger(100); // Ficam com os olhos vermelhos de raiva

            AbelhaKamikaze ak = new AbelhaKamikaze();
            ak.entidade = bee;
            definirNovoAlvo(ak);
            enxame.add(ak);
        }
    }

    private void definirNovoAlvo(AbelhaKamikaze ak) {
        int alvoX = minX + random.nextInt((maxX - minX) + 1);
        int alvoY = minY + random.nextInt((maxY - minY) + 1); // Pode atingir qualquer profundidade
        int alvoZ = minZ + random.nextInt((maxZ - minZ) + 1);

        ak.alvo = new Location(mina.getBukkitWorld(), alvoX, alvoY, alvoZ);
        ak.direcao = ak.alvo.toVector().subtract(ak.entidade.getLocation().toVector()).normalize().multiply(0.8);
        ak.entidade.getLocation().setDirection(ak.direcao);
    }

    @Override
    public void run() {
        ticksDeVida++;

        if (ticksDeVida > 100) { // 5 segundos de caos
            for (AbelhaKamikaze ak : enxame) {
                if (ak.entidade != null && !ak.entidade.isDead()) ak.entidade.remove();
            }
            this.cancel();
            return;
        }

        for (AbelhaKamikaze ak : enxame) {
            if (ak.entidade.isDead()) continue;

            Location loc = ak.entidade.getLocation();
            loc.add(ak.direcao);
            ak.entidade.teleport(loc);

            // Rasto de fumo
            loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 1, 0, 0, 0, 0);

            // Se bater no alvo (ou perto) EXPLODE!
            if (loc.distance(ak.alvo) < 1.5) {
                explodirArea(loc);
                definirNovoAlvo(ak); // Define novo alvo e continua!
            }
        }
    }

    private void explodirArea(Location centro) {
        centro.getWorld().spawnParticle(Particle.EXPLOSION, centro, 1);
        centro.getWorld().playSound(centro, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.5f);

        // Quebra 3x3x3
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block b = centro.clone().add(x, y, z).getBlock();
                    if (b.getX() >= minX && b.getX() <= maxX && b.getY() >= minY && b.getY() <= maxY && b.getZ() >= minZ && b.getZ() <= maxZ) {
                        Material mat = b.getType();
                        if (mat != Material.AIR && mat != Material.BEDROCK) {
                            b.setType(Material.AIR);
                            b.getWorld().spawnParticle(Particle.BLOCK, b.getLocation(), 2, mat.createBlockData());
                            plugin.getMinaManager().processarRecompensa(player, mina, mat);
                        }
                    }
                }
            }
        }
    }
}
