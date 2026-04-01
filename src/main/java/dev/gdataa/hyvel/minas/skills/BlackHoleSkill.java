package dev.gdataa.hyvel.minas.skills;

import dev.gdataa.hyvel.MinasMain;
import dev.gdataa.hyvel.minas.model.Mina;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class BlackHoleSkill extends BukkitRunnable {

    private final MinasMain plugin;
    private final Player player;
    private final Mina mina;
    private final Location centerLoc;

    private final Random random = new Random();

    // Lista de Blocos Fantasmas que estão a ser sugados
    private class BlocoSugado {
        BlockDisplay display;
        Material material;
        int ticksRestantes;

        public BlocoSugado(BlockDisplay display, Material material, int ticksRestantes) {
            this.display = display;
            this.material = material;
            this.ticksRestantes = ticksRestantes;
        }
    }
    private final List<BlocoSugado> blocosSugados = new ArrayList<>();

    // Lista de Blocos do Planeta (Buraco Negro)
    private class PlanetaBlock {
        BlockDisplay display;
        Vector baseOffset;

        public PlanetaBlock(BlockDisplay display, Vector baseOffset) {
            this.display = display;
            this.baseOffset = baseOffset;
        }
    }
    private final List<PlanetaBlock> blocosDoPlaneta = new ArrayList<>();

    private int ticksDeVida = 0;
    private final int MAX_TICKS = 200; // 10 segundos
    private double currentAngle = 0;

    private final long tempoAntigo;
    private boolean tinhaVisaoNoturna = false;
    private int duracaoNV = 0;
    private int amplificadorNV = 0;

    public BlackHoleSkill(MinasMain plugin, Player player, Mina mina) {
        this.plugin = plugin;
        this.player = player;
        this.mina = mina;

        int minX = plugin.getConfig().getInt("regiao.minX");
        int maxX = plugin.getConfig().getInt("regiao.maxX");
        int minZ = plugin.getConfig().getInt("regiao.minZ");
        int maxZ = plugin.getConfig().getInt("regiao.maxZ");
        int maxY = plugin.getConfig().getInt("regiao.maxY");

        int centroX = (minX + maxX) / 2;
        int centroZ = (minZ + maxZ) / 2;
        this.centerLoc = new Location(mina.getBukkitWorld(), centroX, maxY + 15, centroZ);

        if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            PotionEffect nv = player.getPotionEffect(PotionEffectType.NIGHT_VISION);
            duracaoNV = nv.getDuration();
            amplificadorNV = nv.getAmplifier();
            tinhaVisaoNoturna = true;
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }

        this.tempoAntigo = centerLoc.getWorld().getTime();
        centerLoc.getWorld().setTime(18000);
        construirPlaneta();

        player.playSound(centerLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);
        player.playSound(centerLoc, Sound.AMBIENT_CAVE, 2.0f, 0.1f);
    }

    private void construirPlaneta() {
        int raioEsfera = 3;
        for (int x = -raioEsfera; x <= raioEsfera; x++) {
            for (int y = -raioEsfera; y <= raioEsfera; y++) {
                for (int z = -raioEsfera; z <= raioEsfera; z++) {
                    if (x * x + y * y + z * z <= raioEsfera * raioEsfera + 1) {
                        Material mat = random.nextBoolean() ? Material.OBSIDIAN : Material.CRYING_OBSIDIAN;
                        spawnDisplayBlock(mat, new Vector(x, y, z));
                    }
                }
            }
        }

        int raioInterno = 6;
        int raioExterno = 8;
        for (int x = -raioExterno; x <= raioExterno; x++) {
            for (int z = -raioExterno; z <= raioExterno; z++) {
                double dist = x * x + z * z;
                if (dist >= (raioInterno * raioInterno) && dist <= (raioExterno * raioExterno)) {
                    Material mat = random.nextBoolean() ? Material.YELLOW_STAINED_GLASS : Material.ORANGE_STAINED_GLASS;
                    spawnDisplayBlock(mat, new Vector(x, 0, z));
                }
            }
        }
    }

    private void spawnDisplayBlock(Material mat, Vector offset) {
        Location loc = centerLoc.clone().add(offset);
        BlockDisplay display = centerLoc.getWorld().spawn(loc, BlockDisplay.class, bd -> {
            bd.setBlock(mat.createBlockData());
            bd.setPersistent(false); // Impede o servidor de guardar no disco (Previne lag)
            bd.setTeleportDuration(1);

            Transformation t = bd.getTransformation();
            t.getTranslation().set(-0.5f, -0.5f, -0.5f);
            bd.setTransformation(t);
        });
        blocosDoPlaneta.add(new PlanetaBlock(display, offset));
    }

    @Override
    public void run() {
        ticksDeVida++;

        // ==========================================
        // 1. FINALIZAÇÃO DA SKILL
        // ==========================================
        if (ticksDeVida > MAX_TICKS) {
            for (PlanetaBlock pb : blocosDoPlaneta) pb.display.remove();
            for (BlocoSugado bs : blocosSugados) bs.display.remove();

            centerLoc.getWorld().setTime(tempoAntigo);
            if (tinhaVisaoNoturna) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, duracaoNV, amplificadorNV, false, false, false));
            }

            centerLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, centerLoc, 5);
            centerLoc.getWorld().playSound(centerLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            this.cancel();
            return;
        }

        // ==========================================
        // 2. ROTAÇÃO DO PLANETA (Igual ao anterior)
        // ==========================================
        double progress = (double) ticksDeVida / MAX_TICKS;
        double speed = 0.05 + (progress * progress * 0.5);
        currentAngle += speed;

        float scale = 1.0f;
        int ticksParaFim = MAX_TICKS - ticksDeVida;
        if (ticksParaFim <= 40) {
            scale = (float) ticksParaFim / 40.0f;
        }

        for (PlanetaBlock pb : blocosDoPlaneta) {
            Vector v = pb.baseOffset.clone();
            double cos = Math.cos(currentAngle);
            double sin = Math.sin(currentAngle);
            double nx = v.getX() * cos - v.getZ() * sin;
            double nz = v.getX() * sin + v.getZ() * cos;

            v.setX(nx);
            v.setZ(nz);
            v.multiply(scale);

            pb.display.teleport(centerLoc.clone().add(v));

            Transformation t = pb.display.getTransformation();
            t.getScale().set(scale, scale, scale);
            t.getTranslation().set(-0.5f * scale, -0.5f * scale, -0.5f * scale);
            pb.display.setInterpolationDelay(0);
            pb.display.setInterpolationDuration(1);
            pb.display.setTransformation(t);
        }

        // ==========================================
        // 3. GERIR OS BLOCOS SUGADOS SEM LAG
        // ==========================================
        if (ticksDeVida < MAX_TICKS - 30) {
            for (int i = 0; i < 5; i++) sugarBlocoAleatorio();
        }

        // Apenas decrementamos um contador de ticks. Zero teleportes e zero física!
        Iterator<BlocoSugado> it = blocosSugados.iterator();
        while (it.hasNext()) {
            BlocoSugado bs = it.next();
            bs.ticksRestantes--;

            // O bloco "chegou" ao buraco negro!
            if (bs.ticksRestantes <= 0) {
                // Dá a recompensa
                plugin.getMinaManager().processarRecompensa(player, mina, bs.material);

                // Apaga a entidade visual
                bs.display.remove();
                it.remove();
            }
        }

        centerLoc.getWorld().spawnParticle(Particle.PORTAL, centerLoc, 50, 4, 1, 4, 0.2);
    }

    private void sugarBlocoAleatorio() {
        int minX = plugin.getConfig().getInt("regiao.minX");
        int maxX = plugin.getConfig().getInt("regiao.maxX");
        int minY = plugin.getConfig().getInt("regiao.minY");
        int maxY = plugin.getConfig().getInt("regiao.maxY");
        int minZ = plugin.getConfig().getInt("regiao.minZ");
        int maxZ = plugin.getConfig().getInt("regiao.maxZ");

        int rx = Math.min(minX, maxX) + random.nextInt(Math.abs(maxX - minX) + 1);
        int ry = Math.min(minY, maxY) + random.nextInt(Math.abs(maxY - minY) + 1);
        int rz = Math.min(minZ, maxZ) + random.nextInt(Math.abs(maxZ - minZ) + 1);

        Location loc = new Location(mina.getBukkitWorld(), rx, ry, rz);
        Material mat = loc.getBlock().getType();

        if (mat != Material.AIR && mat != Material.BEDROCK) {
            // Remove o bloco do chão
            loc.getBlock().setType(Material.AIR, false);

            // A MAGIA DA INTERPOLAÇÃO ACONTECE AQUI
            int tempoDeVoo = 15; // O bloco demora 15 ticks a chegar ao buraco negro

            BlockDisplay display = loc.getWorld().spawn(loc, BlockDisplay.class, bd -> {
                bd.setBlock(mat.createBlockData());
                bd.setPersistent(false); // À prova de lag no SlimePaper
                bd.setGlowing(true);

                // Define que a animação visual vai durar 15 ticks
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(tempoDeVoo);

                // Calcula o vetor de movimento desde a posição original até ao Buraco Negro
                Vector viagem = centerLoc.toVector().subtract(loc.toVector());

                Transformation t = bd.getTransformation();
                // Move o bloco para o centro
                t.getTranslation().set((float) viagem.getX(), (float) viagem.getY(), (float) viagem.getZ());
                // Encolhe o bloco para ele "desaparecer" quando chega ao buraco
                t.getScale().set(0.1f, 0.1f, 0.1f);

                bd.setTransformation(t);
            });

            blocosSugados.add(new BlocoSugado(display, mat, tempoDeVoo));
        }
    }
}