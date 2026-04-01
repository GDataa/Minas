package dev.gdataa.hyvel.minas.listener;

import dev.gdataa.hyvel.minas.manager.GerenciarUpgradeGUI;
import dev.gdataa.hyvel.minas.manager.UpgradeGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuiListener implements Listener {

    @EventHandler
    public void aoClicarNoMenu(InventoryClickEvent event) {

        // Verifica Menu 1: Lista de Upgrades Principais
        if (event.getInventory().getHolder() instanceof UpgradeGUI) {
            event.setCancelled(true); // Cancela para o jogador não roubar os itens!

            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getInventory())) {
                UpgradeGUI menu = (UpgradeGUI) event.getInventory().getHolder();
                menu.processarClique(event.getSlot()); // Passa a ação para a classe que criámos!
            }
        }

        // Verifica Menu 2: Gestão Específica do Encantamento (O Novo Menu!)
        else if (event.getInventory().getHolder() instanceof GerenciarUpgradeGUI) {
            event.setCancelled(true); // Evita o roubo de itens neste menu também

            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getInventory())) {
                GerenciarUpgradeGUI menu = (GerenciarUpgradeGUI) event.getInventory().getHolder();
                menu.processarClique(event.getSlot()); // Passa a ação para a classe de gestão
            }
        }
    }
}