package dev.gdataa.hyvel.minas.skills;

import dev.gdataa.hyvel.MinasMain;
import dev.gdataa.hyvel.minas.model.Mina;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Dolphin;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class GolfinhoSkill extends BukkitRunnable {

    private final Player player;
    private final Mina mina;
    private final Dolphin golfinho;

    private int ticksDeVida = 0;
    private final int MAX_TICKS = 300; // 15 Segundos de Bónus

    // Variável estática ou gerida pelo MinaManager para saber se o bónus está ativo
    public static boolean bonusAtivo = false;

    public GolfinhoSkill(Player player, Mina mina) {
        this.player = player;
        this.mina = mina;

        Location topoDaMina = mina.getSpawnLocation().clone().add(0, 5, 0);

        this.golfinho = (Dolphin) topoDaMina.getWorld().spawnEntity(topoDaMina, EntityType.DOLPHIN);
        this.golfinho.setAI(false);
        this.golfinho.setInvulnerable(true);
        this.golfinho.setCustomName("§3§lGolfinho Sortudo (+XP)");
        this.golfinho.setCustomNameVisible(true);

        bonusAtivo = true;

        player.playSound(player.getLocation(), Sound.ENTITY_DOLPHIN_SPLASH, 1f, 1f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        player.sendMessage("§3§lSPLAASH! §7O Golfinho Sortudo chegou! Bónus de XP de Rank Ativo!");
    }

    @Override
    public void run() {
        ticksDeVida++;

        if (ticksDeVida > MAX_TICKS) {
            bonusAtivo = false;
            if (golfinho != null && !golfinho.isDead()) {
                golfinho.getWorld().spawnParticle(Particle.SPLASH, golfinho.getLocation(), 50, 0.5, 0.5, 0.5);
                golfinho.remove();
            }
            player.sendMessage("§7O Golfinho Sortudo mergulhou de volta. Bónus terminado.");
            this.cancel();
            return;
        }

        // Faz o golfinho rodar devagarinho e deitar partículas de água e corações
        Location loc = golfinho.getLocation();
        loc.setYaw(loc.getYaw() + 5);
        golfinho.teleport(loc);

        if (ticksDeVida % 5 == 0) {
            golfinho.getWorld().spawnParticle(Particle.FALLING_WATER, golfinho.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);
        }
        if (ticksDeVida % 20 == 0) {
            golfinho.getWorld().spawnParticle(Particle.HEART, golfinho.getLocation().add(0, 1, 0), 1);
            golfinho.getWorld().playSound(golfinho.getLocation(), Sound.ENTITY_DOLPHIN_AMBIENT, 0.5f, 1f);
        }
    }
}
