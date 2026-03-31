package dev.gdataa.hyvel.minas.listener;

import dev.gdataa.hyvel.MinasMain;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ConexaoListener implements Listener {

    private final MinasMain plugin;

    public ConexaoListener(MinasMain plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Quando entra, carrega o mundo da mina (mas não necessariamente teleporta ele na hora,
        // deixamos isso para o comando /mina, ou você pode colocar o teleporte automático aqui)
        plugin.getMinaManager().acessarMina(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Quando sai, desliga o mundo para não consumir memória
        plugin.getMinaManager().descarregarMina(player);
    }
}