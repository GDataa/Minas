package dev.gdataa.hyvel.commands;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import dev.gdataa.hyvel.MinasMain;
import dev.gdataa.hyvel.minas.manager.MenuAdmin;
import dev.gdataa.hyvel.minas.manager.MinaGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MinaCommand implements CommandExecutor {

    private final MinasMain plugin;
    private final MenuAdmin menuAdmin;
    private final MinaGUI minaGUI;

    public MinaCommand(MinasMain plugin) {
        this.plugin = plugin;
        this.menuAdmin = new MenuAdmin(plugin);
        this.minaGUI = new MinaGUI(plugin);

        // Regista os eventos de ambos os menus diretamente no construtor
        plugin.getServer().getPluginManager().registerEvents(this.menuAdmin, plugin);
        plugin.getServer().getPluginManager().registerEvents(this.minaGUI, plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando.");
            return true;
        }

        Player player = (Player) sender;

        // Se o jogador digitar apenas "/mina" -> Abre o novo menu principal
        if (args.length == 0) {
            minaGUI.openMenu(player);
            return true;
        }

        // Se o jogador digitar "/mina ir" -> Lógica de acessar à mina (Antigo /mina)
        if (args[0].equalsIgnoreCase("ir")) {
            plugin.getMinaManager().acessarMina(player);
            return true;
        }

        // Se o jogador digitar "/mina admin" -> Mantemos a lógica original de Admin
        if (args[0].equalsIgnoreCase("admin")) {
            if (!player.hasPermission("minas.admin")) {
                player.sendMessage("§cSem permissão.");
                return true;
            }

            if (args.length > 1 && args[1].equalsIgnoreCase("criar")) {

                // 1. Tenta pegar a seleção do WorldEdit do jogador
                LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
                try {
                    Region selecao = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
                    BlockVector3 min = selecao.getMinimumPoint();
                    BlockVector3 max = selecao.getMaximumPoint();

                    // 2. Salva as coordenadas X, Y e Z da região na config
                    plugin.getConfig().set("regiao.minX", min.x());
                    plugin.getConfig().set("regiao.minY", min.y());
                    plugin.getConfig().set("regiao.minZ", min.z());
                    plugin.getConfig().set("regiao.maxX", max.x());
                    plugin.getConfig().set("regiao.maxY", max.y());
                    plugin.getConfig().set("regiao.maxZ", max.z());
                    plugin.saveConfig();

                    player.sendMessage("§aÁrea da mina salva com sucesso usando o WorldEdit!");

                } catch (IncompleteRegionException e) {
                    player.sendMessage("§cVocê precisa selecionar a região da mina com o Machado do WorldEdit primeiro!");
                    return true;
                }

                // 3. Abre o menu para ele configurar as porcentagens
                menuAdmin.abrirMenu(player);
                return true;
            }

            player.sendMessage("§cUso: /mina admin criar (Selecione com WorldEdit antes)");
            return true;
        }

        // Mensagem de fallback caso digite argumentos inválidos
        player.sendMessage("§cUso incorreto. Usa: /mina, /mina ir ou /mina admin");
        return true;
    }
}