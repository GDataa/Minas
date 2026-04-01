package dev.gdataa.hyvel.minas.model;

import com.infernalsuite.asp.api.world.SlimeWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Mina {
    private final UUID owner;
    private SlimeWorld slimeWorld;

    // Novas variáveis de progresso
    private int nivel = 1;
    private int prestigio = 0;
    private int blocosQuebradosTotal = 0;
    private int tokens = 0; // <-- NOVA VARIÁVEL

    // Variáveis de controlo da mina (reset)
    private int blocosTotais = 0;
    private int blocosQuebradosNaSessao = 0;

    private String picaretaBase64 = null;

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

        // O cálculo é acumulativo: Nível 1 precisa de 700 total, Nível 2 precisa de 1400...
        int blocosNecessarios = nivel * 700;

        if (blocosQuebradosTotal >= blocosNecessarios) {
            int nivelAntigo = nivel; // Guarda o nível antigo antes de subir
            nivel++; // Sobe o nível

            if (Bukkit.getPlayer(owner) != null) {
                Player p = Bukkit.getPlayer(owner);

                // Formato: UP! | 1 -> 2
                p.sendTitle("§a§l🆙", "§7§m" + nivelAntigo + "§f ➡ §a" + nivel, 10, 50, 15);
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);

                if (nivel == 100) {
                    p.sendMessage("§e§lAVISO: §7Chegaste ao nível máximo! Usa §6/prestigio §7para evoluir.");
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

    public int getTokens() { return tokens; }
    public void setTokens(int tokens) { this.tokens = tokens; }
    public void adicionarTokens(int quantidade) { this.tokens += quantidade; }

    public String getNomeMundo() { return "mina_" + owner.toString(); }
    public World getBukkitWorld() { return Bukkit.getWorld(getNomeMundo()); }
    public Location getSpawnLocation() { return new Location(getBukkitWorld(), 0.5, 65, 0.5, 180f, 0f); }

    public void setSlimeWorld(SlimeWorld sw) { this.slimeWorld = sw; }
    public void setBlocosTotais(int t) { this.blocosTotais = t; }
    public void resetarContagem() { this.blocosQuebradosNaSessao = 0; }
    public double getPercentagemQuebrada() {
        if (blocosTotais == 0) return 0;
        return ((double) blocosQuebradosNaSessao / blocosTotais) * 100;
    }

    public UUID getDono() {
        return this.owner;
    }

    public String getPicaretaBase64() { return picaretaBase64; }
    public void setPicaretaBase64(String base64) { this.picaretaBase64 = base64; }
}