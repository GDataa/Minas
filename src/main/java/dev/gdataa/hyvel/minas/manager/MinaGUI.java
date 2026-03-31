package dev.gdataa.hyvel.minas.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

public class MinaGUI implements Listener {

    private static final String MENU_TITLE = "§8Mundo das Minas";

    // Método que passaste para criar cabeças Custom (mantive intacto)
    public static ItemStack createCustomHead(String base64Value, String name, String... lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(Arrays.asList(lore));

            UUID uuid = UUID.nameUUIDFromBytes(base64Value.getBytes());
            PlayerProfile profile = Bukkit.createPlayerProfile(uuid, null);
            PlayerTextures textures = profile.getTextures();

            try {
                String decoded = new String(Base64.getDecoder().decode(base64Value));
                String url = decoded.substring(decoded.indexOf("url\":\"") + 6, decoded.lastIndexOf("\""));
                textures.setSkin(new URL(url));
                profile.setTextures(textures);
            } catch (Exception e) {
                Bukkit.getLogger().warning("Erro ao carregar textura Base64 para o item: " + name);
            }

            meta.setOwnerProfile(profile);
            head.setItemMeta(meta);
        }
        return head;
    }

    // Método para abrir o Menu para o jogador
    public void openMenu(Player player) {
        // Criamos um inventário de 3 linhas (27 slots)
        Inventory inv = Bukkit.createInventory(null, 27, MENU_TITLE);

        // 1. Skull do Player (Usamos a cabeça REAL do jogador para ser mais interativo)
        ItemStack playerSkull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerMeta = (SkullMeta) playerSkull.getItemMeta();
        if (playerMeta != null) {
            playerMeta.setOwningPlayer(player);
            playerMeta.setDisplayName("§a§lSeu Perfil");
            // AQUI: Substitui pelos valores reais da tua API/Database
            playerMeta.setLore(Arrays.asList(
                    "§7Nível: §f10",
                    "§7Blocos Partidos: §f1.540",
                    "§7Prestígio: §fI"
            ));
            playerSkull.setItemMeta(playerMeta);
        }

        // 2. Botão: Mina Ir (Base64 de uma Seta Verde)
        String setaVerdeBase64 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2RjOWU0ZGNmYTQyMjFhMWZhZGMxYjViMmIxMWQ4YmVlYjU3ODc5YWYxYzQyMzYyMTQyYmFlMWVkZDUifX19";
        ItemStack minaIr = createCustomHead(setaVerdeBase64, "§a§lMina Ir",
                "§7Clica aqui para seres",
                "§7teletransportado para a tua mina.");

        // 3. Botão: Minas Públicas e Privadas (Base64 de um Globo)
        String globoBase64 = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2RkNDhhMjI5Y2E3NjY0YmI1NGNlN2RhODZlYzU0ZDI4MjU4MzU1NzU0N2MxOWVkNmZhZjk1NjlkYzM5ZWZhMSJ9fX0=";
        ItemStack minasList = createCustomHead(globoBase64, "§e§lMinas da Comunidade",
                "§c§l[BREVEMENTE]",
                "§7Explora minas públicas e",
                "§7privadas de outros jogadores.");

        // Vidros cinza para decoração do menu (opcional, mas fica bonito)
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        org.bukkit.inventory.meta.ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" "); // Nome vazio para não incomodar
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        // Posicionar os itens nos slots corretos (11, 13 e 15 ficam centrados e bonitos)
        inv.setItem(11, minaIr);
        inv.setItem(13, playerSkull);
        inv.setItem(15, minasList);

        // Abrir inventário ao jogador
        player.openInventory(inv);
    }

    // Evento para cancelar cliques e dar as ações aos botões
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Verifica se o menu é o nosso
        if (event.getView().getTitle().equals(MENU_TITLE)) {
            event.setCancelled(true); // Impede que o jogador roube os itens do menu

            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();

            // Clicou no slot 11 (Mina Ir)
            if (event.getRawSlot() == 11) {
                player.closeInventory();
                // Executa o comando ou coloca aqui a tua lógica direta de teleporte
                player.performCommand("mina ir");
            }

            // Clicou no slot 15 (Minas Público/Privado)
            if (event.getRawSlot() == 15) {
                player.sendMessage("§cEste sistema de minas públicas e privadas chega BREVEMENTE!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
    }
}
