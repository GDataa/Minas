package dev.gdataa.hyvel.commands;

import dev.gdataa.hyvel.MinasMain;
import dev.gdataa.hyvel.minas.model.Mina;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PrestigioCommand implements CommandExecutor {

    private final MinasMain plugin;

    public PrestigioCommand(MinasMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        Mina mina = plugin.getMinaManager().getMinasCache().get(player.getUniqueId());

        if (mina == null) {
            player.sendMessage("§cPrecisas de ter uma mina ativa para usar este comando.");
            return true;
        }

        if (mina.getNivel() < 100) {
            player.sendMessage("§cPrecisas de atingir o Nível 100 para dar prestígio!");
            return true;
        }

        // Lógica de Prestígio (Rebirth)
        mina.setPrestigio(mina.getPrestigio() + 1);
        mina.setNivel(1);
        mina.setBlocosQuebradosTotal(0);

        player.sendMessage("");
        player.sendMessage("§e§l⭐ PRESTÍGIO ⭐");
        player.sendMessage("§7Parabéns! Evoluíste para o Prestígio §e" + mina.getPrestigio());
        player.sendMessage("§7O teu nível foi resetado e agora és mais poderoso!");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

        // Opcional: Podes dar uma recompensa aqui (Coins, multiplicadores, etc)

        return true;
    }
}