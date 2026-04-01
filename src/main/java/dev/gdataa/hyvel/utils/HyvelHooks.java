package dev.gdataa.hyvel.utils;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class HyvelHooks {

    private static Economy econ = null;

    // Conecta a Economia do servidor (neste caso o yEconomias através do Vault)
    public static boolean setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEconomy() {
        return econ;
    }

    // Puxa o Prefix (Cargo) do jogador no LuckPerms
    public static String getCargo(Player player) {
        try {
            User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                String prefix = user.getCachedData().getMetaData().getPrefix();
                if (prefix != null && !prefix.isEmpty()) {
                    return prefix; // Retorna o prefixo (ex: &c[Admin])
                }
                // Se não tiver prefixo, puxa o nome do grupo e mete a primeira letra maiúscula
                String group = user.getPrimaryGroup();
                return group.substring(0, 1).toUpperCase() + group.substring(1);
            }
        } catch (Exception ignored) {}
        return "Membro";
    }

    // Puxa o Multiplicador (Meta) configurado no LuckPerms
    public static double getMultiplicador(Player player) {
        try {
            User user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                // Vai procurar pela chave "multiplicador"
                String metaValue = user.getCachedData().getMetaData().getMetaValue("multiplicador");
                if (metaValue != null) {
                    return Double.parseDouble(metaValue.replace(",", "."));
                }
            }
        } catch (Exception ignored) {}
        return 1.0; // Se o jogador não tiver multiplicador, o ganho é normal (1x)
    }
}