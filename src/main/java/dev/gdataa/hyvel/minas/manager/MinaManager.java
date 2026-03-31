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
import org.bukkit.Material;
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

                SlimeWorld mundoMina;
                // Variável para saber se devemos preencher os minérios depois
                boolean novaMina = false;

                if (customLoader.worldExists(mina.getNomeMundo())) {
                    mundoMina = asp.readWorld(customLoader, mina.getNomeMundo(), false, props);
                } else {
                    SlimeWorld template = asp.readWorld(customLoader, NOME_MAPA_BASE, true, props);
                    mundoMina = template.clone(mina.getNomeMundo(), customLoader);
                    novaMina = true; // Acabamos de clonar, então é nova!
                }

                mina.setSlimeWorld(mundoMina);

                // Variável final para podermos usar dentro do runTask abaixo
                final boolean devePreencher = novaMina;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    Logger logger = Logger.getLogger("Minecraft");
                    Level prevLogLevel = logger.getLevel();
                    logger.setLevel(Level.WARNING);

                    asp.loadWorld(mundoMina, true);

                    logger.setLevel(prevLogLevel);

                    World bukkitWorld = mina.getBukkitWorld();
                    if (bukkitWorld != null) {
                        bukkitWorld.setAutoSave(false);
                        bukkitWorld.setDifficulty(Difficulty.NORMAL);

                        // SE FOR UMA MINA NOVA, GERA OS MINÉRIOS NELA!
                        if (devePreencher) {
                            preencherMina(mina);
                        }

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

    // Método que preenche a mina com os blocos sorteados
    public void preencherMina(Mina mina) {
        World world = mina.getBukkitWorld();
        if (world == null) return;

        // Verifica se o Admin já configurou a região e os minérios
        if (!plugin.getConfig().contains("regiao.minX") || !plugin.getConfig().contains("minerios")) {
            plugin.getLogger().warning("A mina " + mina.getNomeMundo() + " não foi preenchida porque as coordenadas ou minérios não foram configurados no /mina admin criar!");
            return;
        }

        // Pega as coordenadas da caixa do WorldEdit salvas na config
        int minX = plugin.getConfig().getInt("regiao.minX");
        int minY = plugin.getConfig().getInt("regiao.minY");
        int minZ = plugin.getConfig().getInt("regiao.minZ");
        int maxX = plugin.getConfig().getInt("regiao.maxX");
        int maxY = plugin.getConfig().getInt("regiao.maxY");
        int maxZ = plugin.getConfig().getInt("regiao.maxZ");

        // Prepara a "roleta" com os pesos (porcentagens)
        Map<Material, Double> minerios = new HashMap<>();
        double totalPeso = 0.0;
        for (String key : plugin.getConfig().getConfigurationSection("minerios").getKeys(false)) {
            Material mat = Material.matchMaterial(key);
            if (mat != null) {
                double chance = plugin.getConfig().getDouble("minerios." + key);
                minerios.put(mat, chance);
                totalPeso += chance;
            }
        }

        if (minerios.isEmpty()) return;

        // Percorre cada bloco dentro da caixa selecionada e coloca um minério sorteado
        for (int x = Math.min(minX, maxX); x <= Math.max(minX, maxX); x++) {
            for (int y = Math.min(minY, maxY); y <= Math.max(minY, maxY); y++) {
                for (int z = Math.min(minZ, maxZ); z <= Math.max(minZ, maxZ); z++) {
                    Material sorteado = sortearMinerio(minerios, totalPeso);
                    // O 'false' no final evita atualizações de física (ex: areia caindo), o que reduz muito o lag
                    world.getBlockAt(x, y, z).setType(sorteado, false);
                }
            }
        }
    }

    // A matemática da roleta que decide qual bloco colocar
    private Material sortearMinerio(Map<Material, Double> minerios, double totalPeso) {
        double random = Math.random() * totalPeso;
        double atual = 0.0;
        for (Map.Entry<Material, Double> entry : minerios.entrySet()) {
            atual += entry.getValue();
            if (random <= atual) {
                return entry.getKey();
            }
        }
        return Material.STONE; // Bloco padrão caso algo dê muito errado
    }
}