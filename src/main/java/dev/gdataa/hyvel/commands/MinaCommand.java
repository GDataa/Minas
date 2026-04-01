package dev.gdataa.hyvel.commands;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import dev.gdataa.hyvel.MinasMain;
import dev.gdataa.hyvel.minas.manager.MinaGUI;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class MinaCommand implements CommandExecutor {

    private final MinasMain plugin;
    private final MinaGUI minaGUI;

    public MinaCommand(MinasMain plugin) {
        this.plugin = plugin;
        this.minaGUI = new MinaGUI(plugin);

        // Regista os eventos do menu principal diretamente no construtor
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

        // Se o jogador digitar "/mina admin" -> Menu de Administração
        if (args[0].equalsIgnoreCase("admin")) {
            if (!player.hasPermission("minas.admin")) {
                player.sendMessage("§cSem permissão.");
                return true;
            }

            // --- COMANDO: /mina admin criar ---
            if (args.length > 1 && args[1].equalsIgnoreCase("criar")) {
                LocalSession session = WorldEdit.getInstance().getSessionManager().get(BukkitAdapter.adapt(player));
                try {
                    Region selecao = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
                    BlockVector3 min = selecao.getMinimumPoint();
                    BlockVector3 max = selecao.getMaximumPoint();

                    plugin.getConfig().set("regiao.minX", min.x());
                    plugin.getConfig().set("regiao.minY", min.y());
                    plugin.getConfig().set("regiao.minZ", min.z());
                    plugin.getConfig().set("regiao.maxX", max.x());
                    plugin.getConfig().set("regiao.maxY", max.y());
                    plugin.getConfig().set("regiao.maxZ", max.z());
                    plugin.saveConfig();

                    player.sendMessage("§aÁrea base da mina salva com sucesso usando o WorldEdit!");
                    player.sendMessage("§7A partir de agora, as minas geradas vão usar o sistema de Progressão da config.yml.");

                } catch (IncompleteRegionException e) {
                    player.sendMessage("§cVocê precisa selecionar a região da mina com o Machado do WorldEdit primeiro!");
                }
                return true;
            }

            // --- COMANDO: /mina admin setnivel <jogador> <nível> ---
            if (args.length > 1 && args[1].equalsIgnoreCase("setnivel")) {
                if (args.length < 4) {
                    player.sendMessage("§cUso correto: /mina admin setnivel <jogador> <nível>");
                    return true;
                }

                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    player.sendMessage("§cJogador não encontrado ou offline.");
                    return true;
                }

                int novoNivel;
                try {
                    novoNivel = Integer.parseInt(args[3]);
                    if (novoNivel < 1) novoNivel = 1;
                } catch (NumberFormatException e) {
                    player.sendMessage("§cO nível deve ser um número válido!");
                    return true;
                }

                dev.gdataa.hyvel.minas.model.Mina targetMina = plugin.getMinaManager().getMinasCache().get(target.getUniqueId());
                if (targetMina == null) {
                    player.sendMessage("§cOs dados da mina desse jogador ainda não estão carregados.");
                    return true;
                }

                ItemStack picareta = null;
                boolean targetNaMina = target.getWorld().equals(targetMina.getBukkitWorld());

                // 1. Encontrar a picareta! (Se ele estiver na mina é o item físico, se estiver fora é a Base64)
                if (targetNaMina) {
                    picareta = target.getInventory().getItem(4);
                    if (picareta == null || !picareta.getType().name().contains("PICKAXE")) {
                        picareta = target.getInventory().getItemInMainHand();
                    }
                } else {
                    if (targetMina.getPicaretaBase64() != null && !targetMina.getPicaretaBase64().isEmpty()) {
                        picareta = dev.gdataa.hyvel.utils.ItemSerializer.fromBase64(targetMina.getPicaretaBase64());
                    }
                }

                if (picareta == null || !picareta.getType().name().contains("PICKAXE")) {
                    player.sendMessage("§cNão foi possível encontrar a picareta de " + target.getName() + ".");
                    return true;
                }

                // 2. Modificar o nível no PersistentDataContainer
                ItemMeta meta = picareta.getItemMeta();
                if (meta != null) {
                    NamespacedKey keyNivel = new NamespacedKey(plugin, dev.gdataa.hyvel.utils.PickaxeUtils.KEY_NIVEL);
                    NamespacedKey keyXp = new NamespacedKey(plugin, dev.gdataa.hyvel.utils.PickaxeUtils.KEY_XP);

                    meta.getPersistentDataContainer().set(keyNivel, PersistentDataType.INTEGER, novoNivel);
                    meta.getPersistentDataContainer().set(keyXp, PersistentDataType.DOUBLE, 0.0); // Reseta o XP atual
                    picareta.setItemMeta(meta);

                    // 3. Atualizar a Lore visual
                    picareta = dev.gdataa.hyvel.utils.PickaxeUtils.reconstruirLore(picareta, plugin);

                    // 4. Devolver a picareta ao lugar certo
                    if (!targetNaMina) {
                        targetMina.setPicaretaBase64(dev.gdataa.hyvel.utils.ItemSerializer.toBase64(picareta));
                    }

                    player.sendMessage("§aNível da picareta de " + target.getName() + " alterado para " + novoNivel + " com sucesso!");
                    target.sendMessage("§e⛏ §lPICARETA §8▸ §7O nível da tua picareta foi alterado para §f" + novoNivel + " §7por um Administrador!");
                    target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }
                return true;
            }

            player.sendMessage("§cUso: /mina admin <criar | setnivel>");
            return true;
        }

        // Mensagem de fallback caso digite argumentos inválidos
        player.sendMessage("§cUso incorreto. Usa: /mina, /mina ir ou /mina admin");
        return true;
    }
}