package dev.gdataa.hyvel.minas.manager;

import dev.gdataa.hyvel.MinasMain;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MenuAdmin implements Listener {

    private final MinasMain plugin;
    private final String TITULO_MENU = "§8Configurar Minérios";

    public MenuAdmin(MinasMain plugin) {
        this.plugin = plugin;
    }

    public void abrirMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITULO_MENU);

        // Carrega os blocos que já estão salvos na config
        if (plugin.getConfig().contains("minerios")) {
            for (String materialName : plugin.getConfig().getConfigurationSection("minerios").getKeys(false)) {
                Material mat = Material.matchMaterial(materialName);
                if (mat != null) {
                    double chance = plugin.getConfig().getDouble("minerios." + materialName);
                    inv.addItem(criarIcone(mat, chance));
                }
            }
        }

        player.openInventory(inv);
        player.sendMessage("§eAdicione blocos do seu inventário. \n§aEsquerdo: +1% \n§cDireito: -1% \n§4Shift: Remover");
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITULO_MENU)) return;

        event.setCancelled(true); // Bloqueia pegar o item

        Player player = (Player) event.getWhoClicked();
        ItemStack itemClicado = event.getCurrentItem();
        Inventory inv = event.getInventory();

        if (itemClicado == null || itemClicado.getType() == Material.AIR) return;

        // Se clicou no inventário de baixo (do próprio jogador), adiciona ao menu com 1%
        if (event.getClickedInventory().equals(event.getView().getBottomInventory())) {
            if (!itemClicado.getType().isBlock()) {
                player.sendMessage("§cApenas blocos podem ser adicionados!");
                return;
            }
            // Verifica se já tem no menu
            for (ItemStack item : inv.getContents()) {
                if (item != null && item.getType() == itemClicado.getType()) return;
            }
            inv.addItem(criarIcone(itemClicado.getType(), 1.0));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            return;
        }

        // Se clicou no menu de cima (configurando as porcentagens)
        if (event.getClickedInventory().equals(inv)) {
            Material mat = itemClicado.getType();
            double chanceAtual = pegarChanceDoLore(itemClicado);

            if (event.getClick() == ClickType.LEFT) {
                chanceAtual = Math.min(100.0, chanceAtual + 1.0); // Máximo 100%
            } else if (event.getClick() == ClickType.RIGHT) {
                chanceAtual = Math.max(0.1, chanceAtual - 1.0); // Mínimo 0.1%
            } else if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                event.getInventory().setItem(event.getSlot(), null); // Remove
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_BREAK, 1f, 1f);
                return;
            }

            event.getInventory().setItem(event.getSlot(), criarIcone(mat, chanceAtual));
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(TITULO_MENU)) return;

        // Limpa a config atual e salva o que está no menu
        plugin.getConfig().set("minerios", null);

        double total = 0.0;
        for (ItemStack item : event.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                double chance = pegarChanceDoLore(item);
                plugin.getConfig().set("minerios." + item.getType().name(), chance);
                total += chance;
            }
        }

        plugin.saveConfig();
        Player player = (Player) event.getPlayer();
        player.sendMessage("§aMinérios salvos com sucesso! Total: §e" + total + "%");

        if (total != 100.0) {
            player.sendMessage("§cAtenção: A soma das porcentagens não é 100%!");
        }
    }

    // --- Utilitários Visuais ---

    private ItemStack criarIcone(Material material, double chance) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e" + material.name());
            List<String> lore = new ArrayList<>();
            lore.add("§7Chance atual: §b" + chance + "%");
            lore.add("");
            lore.add("§a[Botão Esquerdo] +1%");
            lore.add("§c[Botão Direito] -1%");
            lore.add("§4[Shift] Remover");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private double pegarChanceDoLore(ItemStack item) {
        try {
            String linha = item.getItemMeta().getLore().get(0);
            // Extrai apenas os números da lore "Chance atual: X%"
            String numero = linha.replaceAll("[^0-9.]", "");
            return Double.parseDouble(numero);
        } catch (Exception e) {
            return 1.0;
        }
    }
}