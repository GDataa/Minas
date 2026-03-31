package dev.gdataa.hyvel;

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.loaders.file.FileLoader;
import dev.gdataa.hyvel.commands.MinaCommand;
import dev.gdataa.hyvel.commands.PrestigioCommand;
import dev.gdataa.hyvel.minas.listener.ConexaoListener;
import dev.gdataa.hyvel.minas.manager.MinaManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class MinasMain extends JavaPlugin {

    private MinaManager minaManager;
    private SlimeLoader loaderCustomizado;

    @Override
    public void onEnable() {

        saveDefaultConfig();

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

        // 3. Registar eventos independentes (como o de entrar/sair do servidor)
        getServer().getPluginManager().registerEvents(new ConexaoListener(this), this);

        // 4. Registar o Comando Principal
        // NOTA: O MinaCommand já instancia e regista os eventos da MinaGUI e MenuAdmin lá dentro!
        getCommand("mina").setExecutor(new MinaCommand(this));
        getCommand("prestigio").setExecutor(new PrestigioCommand(this));

        getLogger().info("Plugin ativado: Salvando minas em pasta customizada!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Desativando plugin e salvando minas abertas...");
        if (minaManager != null) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                minaManager.descarregarMina(online);
            }
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

    public MinaManager getMinaManager() {
        return minaManager;
    }
}