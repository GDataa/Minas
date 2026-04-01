package dev.gdataa.hyvel;

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.loaders.file.FileLoader;
import dev.gdataa.hyvel.commands.MinaCommand;
import dev.gdataa.hyvel.commands.PrestigioCommand;
import dev.gdataa.hyvel.minas.listener.ConexaoListener;
import dev.gdataa.hyvel.minas.listener.GuiListener;
import dev.gdataa.hyvel.minas.listener.MinaListener;
import dev.gdataa.hyvel.minas.manager.MinaManager;
import dev.gdataa.hyvel.minas.model.Mina;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class MinasMain extends JavaPlugin {

    private MinaManager minaManager;
    private SlimeLoader loaderCustomizado;
    private dev.gdataa.hyvel.minas.database.DatabaseManager databaseManager;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        this.databaseManager = new dev.gdataa.hyvel.minas.database.DatabaseManager(this);

        if (!dev.gdataa.hyvel.utils.HyvelHooks.setupEconomy()) {
            getLogger().severe("Vault não encontrado! A economia das minas não vai funcionar.");
        } else {
            getLogger().info("Conectado ao yEconomias (Vault) com sucesso!");
        }

        // Verifica se a API do ASP está a rodar
        try {
            AdvancedSlimePaperAPI.instance();
        } catch (NoClassDefFoundError e) {
            getLogger().severe("API do AdvancedSlimePaper não encontrada! Desativando...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 1. Cria a pasta "plugins/Minas/mundos" e inicializa o nosso Loader
        File pastaMundos = criarPastaMundos();
        this.loaderCustomizado = new FileLoader(pastaMundos);

        // 2. Inicia o gerenciador e passa o nosso loader
        this.minaManager = new MinaManager(this, this.loaderCustomizado);

        new dev.gdataa.hyvel.minas.manager.MinaScoreboard(this).runTaskTimer(this, 20L, 20L);

        // 3. Registar eventos independentes (como o de entrar/sair do servidor)
        getServer().getPluginManager().registerEvents(new ConexaoListener(this), this);
        getServer().getPluginManager().registerEvents(new MinaListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(), this);

        // 4. Registar o Comando Principal
        // NOTA: O MinaCommand já instancia e regista os eventos da MinaGUI e MenuAdmin lá dentro!
        getCommand("mina").setExecutor(new MinaCommand(this));
        getCommand("prestigio").setExecutor(new PrestigioCommand(this));

        getLogger().info("Plugin ativado: Salvando minas em pasta customizada!");

        // Sistema de Auto-Save a cada 5 minutos (6000 ticks)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            getLogger().info("Executando auto-save das minas...");
            for (Player online : Bukkit.getOnlinePlayers()) {
                Mina mina = minaManager.getMinasCache().get(online.getUniqueId());
                if (mina != null) {
                    // Se o jogador estiver na mina dele, atualizamos o Base64 do item que está na mão dele
                    Bukkit.getScheduler().runTask(this, () -> {
                        ItemStack item = online.getInventory().getItem(4);
                        if (item != null && item.getType().name().contains("PICKAXE")) {
                            mina.setPicaretaBase64(dev.gdataa.hyvel.utils.ItemSerializer.toBase64(item));
                            databaseManager.salvarJogador(mina); // Salva de forma asíncrona
                        }
                    });
                }
            }
        }, 6000L, 6000L);
    }

    @Override
    public void onDisable() {
        getLogger().info("A salvar dados de todos os jogadores antes de desligar...");

        if (minaManager != null && databaseManager != null) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                Mina mina = minaManager.getMinasCache().get(online.getUniqueId());

                if (mina != null) {
                    // 1. Procurar a picareta no inventário (Slot 4 ou mão)
                    ItemStack picareta = online.getInventory().getItem(4);
                    if (picareta == null || picareta.getType() == Material.AIR) {
                        picareta = online.getInventory().getItemInMainHand();
                    }

                    // 2. Se encontrou a picareta, atualiza o Base64 na memória da Mina
                    if (picareta != null && picareta.getType().name().contains("PICKAXE")) {
                        mina.setPicaretaBase64(dev.gdataa.hyvel.utils.ItemSerializer.toBase64(picareta));
                    }

                    // 3. Salva de forma SÍNCRONA (bloqueia o desligamento até terminar de salvar)
                    databaseManager.salvarJogadorSincrono(mina);
                }
            }
        }

        if (databaseManager != null) {
            databaseManager.fecharConexao();
        }
    }

    /**
     * Cria a pasta onde os arquivos .slime das minas ficarão salvos
     */
    private File criarPastaMundos() {
        File pastaPlugin = getDataFolder();
        if (!pastaPlugin.exists()) {
            pastaPlugin.mkdirs();
        }

        File pastaMundos = new File(pastaPlugin, "mundos");
        if (!pastaMundos.exists()) {
            pastaMundos.mkdirs();
        }
        return pastaMundos;
    }

    public dev.gdataa.hyvel.minas.database.DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public MinaManager getMinaManager() {
        return minaManager;
    }
}