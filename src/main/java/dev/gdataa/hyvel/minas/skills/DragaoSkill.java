package dev.gdataa.hyvel.minas.skills;

import dev.gdataa.hyvel.MinasMain;
import dev.gdataa.hyvel.minas.model.Mina;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

public class DragaoSkill extends BukkitRunnable {

    private final MinasMain plugin;
    private final Player player;
    private final Mina mina;

    private Location currentLoc;
    private Vector direcaoVoo;
    private int ticksDeVida = 0;
    private double angulo = 0;

    private final int minY;
    private final int minX, maxX, minZ, maxZ;
    private final Random random = new Random();

    // Agora usamos o Phantom escalado! A entidade perfeita para "Baby Dragon".
    private Phantom bebeDragao;

    public DragaoSkill(MinasMain plugin, Player player, Mina mina) {
        this.plugin = plugin;
        this.player = player;
        this.mina = mina;

        this.minY = plugin.getConfig().getInt("regiao.minY");
        int maxY = plugin.getConfig().getInt("regiao.maxY");
        this.minX = Math.min(plugin.getConfig().getInt("regiao.minX"), plugin.getConfig().getInt("regiao.maxX"));
        this.maxX = Math.max(plugin.getConfig().getInt("regiao.minX"), plugin.getConfig().getInt("regiao.maxX"));
        this.minZ = Math.min(plugin.getConfig().getInt("regiao.minZ"), plugin.getConfig().getInt("regiao.maxZ"));
        this.maxZ = Math.max(plugin.getConfig().getInt("regiao.minZ"), plugin.getConfig().getInt("regiao.maxZ"));

        // 1. Ponto de Spawn (Topo da mina)
        int spawnX = minX + random.nextInt((maxX - minX) + 1);
        int spawnZ = minZ + random.nextInt((maxZ - minZ) + 1);
        this.currentLoc = new Location(mina.getBukkitWorld(), spawnX, maxY, spawnZ);

        // 2. Alvo (Fundo da mina)
        int alvoX = minX + random.nextInt((maxX - minX) + 1);
        int alvoZ = minZ + random.nextInt((maxZ - minZ) + 1);
        Location alvoLoc = new Location(mina.getBukkitWorld(), alvoX, minY, alvoZ);

        this.direcaoVoo = alvoLoc.toVector().subtract(currentLoc.toVector()).normalize().multiply(0.6);

        // ==========================================
        // O NASCIMENTO DO BABY DRAGON (PHANTOM)
        // ==========================================
        this.bebeDragao = (Phantom) currentLoc.getWorld().spawnEntity(currentLoc, EntityType.PHANTOM);

        bebeDragao.setAI(false); // Totalmente submisso ao nosso código
        bebeDragao.setInvulnerable(true);
        bebeDragao.setSilent(true);
        bebeDragao.setCustomName("§5§lBaby Dragon");
        bebeDragao.setCustomNameVisible(true);

        // Aumentamos o Phantom para 180% do tamanho normal! Fica assustador!
        if (bebeDragao.getAttribute(Attribute.SCALE) != null) {
            bebeDragao.getAttribute(Attribute.SCALE).setBaseValue(1.8);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.8f);
    }

    @Override
    public void run() {
        ticksDeVida++;

        // Chegou ao fundo da mina ou atingiu o tempo limite de segurança
        if (currentLoc.getY() <= minY || ticksDeVida > 300) {
            if (bebeDragao != null && !bebeDragao.isDead()) {
                bebeDragao.remove();
            }

            currentLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, currentLoc, 3);
            currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.5f);
            this.cancel();
            return;
        }

        // Move o centro da espiral para a frente
        currentLoc.add(direcaoVoo);

        // Matemática do voo em espiral
        angulo += 0.5;
        double raio = 3.0;
        double xOffset = Math.cos(angulo) * raio;
        double zOffset = Math.sin(angulo) * raio;

        Location cabecaDragao = currentLoc.clone().add(xOffset, 0, zOffset);

        // Vira a cara do bicho para a direção onde ele está a ir
        cabecaDragao.setDirection(direcaoVoo);

        // Teleporta o nosso dragão
        if (bebeDragao != null && !bebeDragao.isDead()) {
            bebeDragao.teleport(cabecaDragao);
        }

        // Partículas de "Bafo de Dragão" e som de asas
        cabecaDragao.getWorld().spawnParticle(Particle.DRAGON_BREATH, cabecaDragao, 15, 0.2, 0.2, 0.2, 0.05);
        if (ticksDeVida % 5 == 0) {
            cabecaDragao.getWorld().playSound(cabecaDragao, Sound.ENTITY_PHANTOM_FLAP, 1.5f, 0.5f); // Som mais grave
        }

        // ==========================================
        // DESTRUIÇÃO CONTROLADA DA MINA (Área 5x5x5)
        // ==========================================
        int raioDestruicao = 2;
        int cx = cabecaDragao.getBlockX();
        int cy = cabecaDragao.getBlockY();
        int cz = cabecaDragao.getBlockZ();

        for (int x = cx - raioDestruicao; x <= cx + raioDestruicao; x++) {
            for (int y = cy - raioDestruicao; y <= cy + raioDestruicao; y++) {
                for (int z = cz - raioDestruicao; z <= cz + raioDestruicao; z++) {

                    if (x >= minX && x <= maxX && y >= minY && z >= minZ && z <= maxZ) {
                        Block b = cabecaDragao.getWorld().getBlockAt(x, y, z);
                        Material mat = b.getType();

                        if (mat != Material.AIR && mat != Material.BEDROCK) {
                            b.setType(Material.AIR, false);

                            // Efeito visual de pedras a saltar
                            b.getWorld().spawnParticle(Particle.BLOCK, b.getLocation().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, mat.createBlockData());

                            // Processa o dinheiro e tokens
                            plugin.getMinaManager().processarRecompensa(player, mina, mat);
                        }
                    }
                }
            }
        }
    }
}