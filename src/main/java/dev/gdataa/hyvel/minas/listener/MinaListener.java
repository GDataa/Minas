package dev.gdataa.hyvel.minas.listener;

import dev.gdataa.hyvel.MinasMain;
import dev.gdataa.hyvel.minas.model.Mina; // Usa o import correto do teu modelo
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class MinaListener implements Listener {

    private final MinasMain plugin;

    public MinaListener(MinasMain plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void aoQuebrarBloco(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location loc = block.getLocation();

        // 1. Obtém a mina do cache. Se o player não tiver mina carregada, ignora.
        Mina mina = plugin.getMinaManager().getMinasCache().get(player.getUniqueId());
        if (mina == null || !loc.getWorld().getName().equals(mina.getNomeMundo())) {
            return; // Não está no mundo da mina dele
        }

        // Se for um Admin em modo criativo, deixamos partir os blocos de decoração para construir
        if (player.getGameMode() == GameMode.CREATIVE && player.hasPermission("minas.admin")) {
            return;
        }

        // 2. Verifica se o bloco partido está DENTRO da zona delimitada no config
        int minX = plugin.getConfig().getInt("regiao.minX");
        int minY = plugin.getConfig().getInt("regiao.minY");
        int minZ = plugin.getConfig().getInt("regiao.minZ");
        int maxX = plugin.getConfig().getInt("regiao.maxX");
        int maxY = plugin.getConfig().getInt("regiao.maxY");
        int maxZ = plugin.getConfig().getInt("regiao.maxZ");

        boolean dentroDaMina =
                loc.getBlockX() >= Math.min(minX, maxX) && loc.getBlockX() <= Math.max(minX, maxX) &&
                        loc.getBlockY() >= Math.min(minY, maxY) && loc.getBlockY() <= Math.max(minY, maxY) &&
                        loc.getBlockZ() >= Math.min(minZ, maxZ) && loc.getBlockZ() <= Math.max(minZ, maxZ);

        if (!dentroDaMina) {
            event.setCancelled(true);
            player.sendMessage("§cSó podes minerar dentro da área da mina!");
            return;
        }

        // 3. O jogador partiu um minério válido!
        event.setDropItems(false); // Impede que o bloco caia no chão (evita lag)

        // Dá o bloco direto para o inventário (Auto-Pickup)
        player.getInventory().addItem(new ItemStack(block.getType()));

        // Transforma o bloco em Ar
        block.setType(Material.AIR);

        // 4. Lógica de Reset Automático
        mina.adicionarBlocoQuebrado();

        // Se a mina estiver 80% (ou mais) vazia, dá trigger ao reset!
        if (mina.getPercentagemQuebrada() >= 80.0) {
            plugin.getMinaManager().resetarMina(mina, player);
        }
    }
}