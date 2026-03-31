package dev.gdataa.hyvel.minas.manager;

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import dev.gdataa.hyvel.MinasMain;
import dev.gdataa.hyvel.minas.model.Mina;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MinaManager {

    private final MinasMain plugin;
    private final SlimeLoader customLoader;
    private final AdvancedSlimePaperAPI asp;

    private final Map<UUID, Mina> minasCache = new HashMap<>();

    // O arquivo .slime do mapa base precisa estar dentro da pasta plugins/Minas/mundos/
    private final String NOME_MAPA_BASE = "Mina";

    public MinaManager(MinasMain plugin, SlimeLoader customLoader) {
        this.plugin = plugin;
        this.customLoader = customLoader;
        this.asp = AdvancedSlimePaperAPI.instance();
    }

    public void acessarMina(Player player) {
        UUID uuid = player.getUniqueId();
        Mina mina = minasCache.computeIfAbsent(uuid, k -> new Mina(uuid));

        if (mina.getBukkitWorld() != null) {
            player.teleport(mina.getSpawnLocation());
            player.sendMessage("§aTeleportado para sua mina!");
            return;
        }

        player.sendMessage("§eCarregando sua mina, aguarde...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                SlimePropertyMap props = new SlimePropertyMap();
                props.setValue(SlimeProperties.DIFFICULTY, "normal");
                props.setValue(SlimeProperties.PVP, false);

                SlimeWorld mundoMina;

                // Usa o NOSSO customLoader (pasta do plugin) para checar e ler
                if (customLoader.worldExists(mina.getNomeMundo())) {
                    mundoMina = asp.readWorld(customLoader, mina.getNomeMundo(), false, props);
                } else {
                    // Importante: O mapa base "Mina.slime" deve estar dentro da pasta plugins/Minas/mundos/
                    SlimeWorld template = asp.readWorld(customLoader, NOME_MAPA_BASE, true, props);
                    mundoMina = template.clone(mina.getNomeMundo(), customLoader);
                }

                mina.setSlimeWorld(mundoMina);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    Logger logger = Logger.getLogger("Minecraft");
                    Level prevLogLevel = logger.getLevel();
                    logger.setLevel(Level.WARNING);

                    asp.loadWorld(mundoMina, true);

                    logger.setLevel(prevLogLevel);

                    World bukkitWorld = mina.getBukkitWorld();
                    if (bukkitWorld != null) {
                        bukkitWorld.setAutoSave(true);
                        bukkitWorld.setDifficulty(Difficulty.NORMAL);

                        player.teleport(mina.getSpawnLocation());
                        player.sendMessage("§aMina carregada com sucesso!");
                    }
                });

            } catch (Exception e) {
                plugin.getLogger().severe("Erro ao carregar a mina de " + player.getName());
                e.printStackTrace();
            }
        });
    }

    public void descarregarMina(Player player) {
        Mina mina = minasCache.get(player.getUniqueId());
        if (mina == null || mina.getBukkitWorld() == null) return;

        // Atrasamos o descarregamento em 20 ticks (1 segundo) para garantir
        // que o dono já desconectou 100% e o Bukkit salvou os dados dele em paz.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            World bukkitWorld = mina.getBukkitWorld();

            if (bukkitWorld != null) {
                // Se sobrou algum jogador lá dentro (ex: um amigo visitando), manda pro spawn
                for (Player p : bukkitWorld.getPlayers()) {
                    p.sendMessage("§eA mina foi fechada pois o dono saiu do servidor.");
                    p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                }

                // Agora sim, descarregamos o mundo com segurança
                Bukkit.unloadWorld(bukkitWorld, true);
                mina.setSlimeWorld(null);
            }
        }, 20L);
    }
}