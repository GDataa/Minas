package dev.gdataa.hyvel.minas.manager;

import dev.gdataa.hyvel.MinasMain;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class GerenciarUpgradeGUI implements InventoryHolder {

    private final MinasMain plugin;
    private final Player player;
    private final Inventory inventory;
    private final UpgradeGUI.EncantamentoData enc;

    private static final int[] SLOTS_PAINEIS = {11, 12, 13, 14, 15};
    private static final int SLOT_ARROW = 18;
    private static final int SLOT_BOTAO = 29;
    private static final int SLOT_COMPARADOR = 33;

    // Guardar os valores dinâmicos para sabermos quantos níveis dar ao clicar
    private int niveisPainelMetade;
    private int niveisPainelTudo;

    public GerenciarUpgradeGUI(MinasMain plugin, Player player, UpgradeGUI.EncantamentoData enc) {
        this.plugin = plugin;
        this.player = player;
        this.enc = enc;
        this.inventory = Bukkit.createInventory(this, 36, "§8Gerenciar: " + enc.nome);

        renderizar();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void abrir() {
        player.openInventory(inventory);
    }

    public void renderizar() {
        inventory.clear();

        inventory.setItem(SLOT_ARROW, criarItem(Material.ARROW, "§aVoltar", "§7Clique para voltar ao menu de Upgrades."));
        inventory.setItem(SLOT_BOTAO, criarItem(Material.OAK_BUTTON, "§eDefinir Quantidade Customizada",
                "§7Clique para digitar no chat", "§7quantos níveis queres evoluir."));

        // Calculos dinâmicos baseados no limite de níveis
        int niveisRestantes = enc.nivelMaximo - enc.nivelAtual;
        this.niveisPainelMetade = Math.max(1, niveisRestantes / 2);
        this.niveisPainelTudo = niveisRestantes;

        // Painéis: +1, +5, +10, +Metade (X), +Tudo (Y)
        gerarPainel(SLOTS_PAINEIS[0], 1);
        gerarPainel(SLOTS_PAINEIS[1], 5);
        gerarPainel(SLOTS_PAINEIS[2], 10);
        gerarPainel(SLOTS_PAINEIS[3], niveisPainelMetade);
        gerarPainel(SLOTS_PAINEIS[4], niveisPainelTudo);
    }

    private void gerarPainel(int slot, int niveisAAumentar) {
        if (niveisAAumentar <= 0 || enc.nivelAtual >= enc.nivelMaximo) {
            inventory.setItem(slot, criarItem(Material.BARRIER, "§cMÁXIMO ALCANÇADO", "§7Este encantamento já está no nível máximo."));
            return;
        }

        int niveisReais = Math.min(niveisAAumentar, enc.nivelMaximo - enc.nivelAtual);
        int nivelFinal = enc.nivelAtual + niveisReais;

        double chanceNova = enc.chanceAtual + (enc.chancePorNivel * niveisReais);
        String moedaStr = (enc.tipoMoeda == UpgradeGUI.Filtro.MONEY) ? "Coins" : "Tokens";

        // =====================================
        // MODO DE TESTE - TUDO GRÁTIS
        // =====================================
        double custoTotal = 0.0;

        // Formatação limpa das chances
        String chanceAtualFormat = String.format("%.2f", enc.chanceAtual).replace(",", ".");
        String chanceNovaFormat = String.format("%.2f", chanceNova).replace(",", ".");

        // O Título agora mostra o número real: "Evoluir X [0 ➟ X]"
        String titulo = "§cEvoluir " + niveisReais + " [" + enc.nivelAtual + " §f➟ §c" + nivelFinal + "]";

        ItemStack painel = criarItem(Material.LIME_STAINED_GLASS_PANE, titulo,
                "",
                "§7Custo: §fGRÁTIS (Teste) " + moedaStr,
                "§7Chance: §a" + chanceAtualFormat + "% §f➟ §a" + chanceNovaFormat + "%",
                "",
                "§aClique para evoluir!"
        );

        inventory.setItem(slot, painel);
    }

    public void processarClique(int slot) {
        if (slot == SLOT_ARROW) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            new UpgradeGUI(plugin, player).abrir();
        }
        else if (slot == SLOT_BOTAO) {
            player.closeInventory();
            player.sendMessage("§eEscreve no chat a quantidade que queres evoluir (ou digita 'cancelar').");
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }
        else {
            // Verifica quantos níveis vamos comprar dependendo do slot clicado
            int niveisComprados = 0;
            if (slot == SLOTS_PAINEIS[0]) niveisComprados = 1;
            else if (slot == SLOTS_PAINEIS[1]) niveisComprados = 5;
            else if (slot == SLOTS_PAINEIS[2]) niveisComprados = 10;
            else if (slot == SLOTS_PAINEIS[3]) niveisComprados = niveisPainelMetade;
            else if (slot == SLOTS_PAINEIS[4]) niveisComprados = niveisPainelTudo;

            if (niveisComprados > 0) {
                // Segurança extra para não passar o máximo
                niveisComprados = Math.min(niveisComprados, enc.nivelMaximo - enc.nivelAtual);
                if (niveisComprados <= 0) return;

                // =====================================
                // APLICAR UPGRADE REAL NA PICARETA
                // =====================================

                // 1. Pega a picareta correta
                ItemStack picareta = player.getInventory().getItemInMainHand();
                int slotPicareta = player.getInventory().getHeldItemSlot();
                if (picareta == null || picareta.getType() == Material.AIR) {
                    picareta = player.getInventory().getItem(4);
                    slotPicareta = 4;
                }

                if (picareta != null && picareta.getType() != Material.AIR) {

                    // 2. Converte o ID do menu para o ID do PickaxeUtils (ex: "FORTUNA" -> "enc_fortuna")
                    String nomeInterno = "enc_" + enc.id.toLowerCase();

                    // 3. Aplica os N níveis de uma vez usando a PickaxeUtils
                    ItemStack picaretaAtualizada = dev.gdataa.hyvel.utils.PickaxeUtils.upgradeEnchant(picareta, nomeInterno, plugin, niveisComprados);

                    // 4. Salva a picareta de volta no inventário do jogador!
                    player.getInventory().setItem(slotPicareta, picaretaAtualizada);

                    // 5. Atualiza o objeto do menu visualmente
                    enc.nivelAtual += niveisComprados;
                    enc.chanceAtual += (enc.chancePorNivel * niveisComprados);

                    player.sendMessage("§a[!] Evoluíste §f" + niveisComprados + " §aníveis de " + enc.nome + "!");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);

                    renderizar(); // Atualiza a janela em tempo real
                }
            }
        }
    }

    private ItemStack criarItem(Material material, String nome, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(nome);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}