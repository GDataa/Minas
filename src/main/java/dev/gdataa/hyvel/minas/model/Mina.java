package dev.gdataa.hyvel.minas.model;

import com.infernalsuite.asp.api.world.SlimeWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import java.util.UUID;

public class Mina {
    private final UUID owner;
    private SlimeWorld slimeWorld;

    // Novas variáveis de progresso
    private int nivel = 1;
    private int prestigio = 0;
    private int blocosQuebradosTotal = 0;

    // Variáveis de controlo da mina (reset)
    private int blocosTotais = 0;
    private int blocosQuebradosNaSessao = 0;

    public Mina(UUID owner) {
        this.owner = owner;
    }

    // --- Lógica de Nível ---

    public void adicionarBlocoQuebrado() {
        this.blocosQuebradosNaSessao++; // Para o reset da mina (80%)
        this.blocosQuebradosTotal++;    // Para o nível global

        checkLevelUp();
    }

    private void checkLevelUp() {
        if (nivel >= 100) return; // Bloqueia no 100 até dar prestígio

        // Exemplo simples: cada nível pede (nível atual * 100) blocos
        // Nível 1 -> 2: 100 blocos | Nível 2 -> 3: 200 blocos...
        int blocosNecessarios = nivel * 100;

        // Se quiseres algo fixo: if (blocosQuebradosTotal >= 500) ...
        if (blocosQuebradosTotal >= blocosNecessarios) {
            nivel++;
            blocosQuebradosTotal = 0; // Reinicia o progresso para o próximo nível

            if (Bukkit.getPlayer(owner) != null) {
                Bukkit.getPlayer(owner).sendMessage("§a§lUP! §7Subiste para o nível §f" + nivel + "§7!");
                if (nivel == 100) {
                    Bukkit.getPlayer(owner).sendMessage("§e§lAVISO: §7Chegaste ao nível máximo! Usa §6/prestigio §7para evoluir.");
                }
            }
        }
    }

    // --- Getters e Setters ---

    public int getNivel() { return nivel; }
    public void setNivel(int nivel) { this.nivel = nivel; }

    public int getPrestigio() { return prestigio; }
    public void setPrestigio(int prestigio) { this.prestigio = prestigio; }

    public int getBlocosQuebradosTotal() { return blocosQuebradosTotal; }
    public void setBlocosQuebradosTotal(int blocos) { this.blocosQuebradosTotal = blocos; }

    // (Mantém os outros métodos que já tínhamos: getBukkitWorld, getNomeMundo, etc)
    public String getNomeMundo() { return "mina_" + owner.toString(); }
    public World getBukkitWorld() { return Bukkit.getWorld(getNomeMundo()); }
    public Location getSpawnLocation() { return new Location(getBukkitWorld(), 0.5, 65, 0.5); }

    public void setSlimeWorld(SlimeWorld sw) { this.slimeWorld = sw; }
    public void setBlocosTotais(int t) { this.blocosTotais = t; }
    public void resetarContagem() { this.blocosQuebradosNaSessao = 0; }
    public double getPercentagemQuebrada() {
        if (blocosTotais == 0) return 0;
        return ((double) blocosQuebradosNaSessao / blocosTotais) * 100;
    }
}