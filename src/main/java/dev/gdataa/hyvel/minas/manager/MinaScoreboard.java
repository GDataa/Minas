package dev.gdataa.hyvel.minas.manager;

import dev.gdataa.hyvel.MinasMain;
import dev.gdataa.hyvel.minas.model.Mina;
import dev.gdataa.hyvel.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class MinaScoreboard extends BukkitRunnable {

    private final MinasMain plugin;

    public MinaScoreboard(MinasMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().getName().startsWith("mina_")) {
                if (player.getScoreboard().getObjective("MinaBoard") != null) {
                    player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                }
                continue;
            }

            Mina mina = plugin.getMinaManager().getMinasCache().get(player.getUniqueId());
            if (mina == null) continue;

            Scoreboard board = player.getScoreboard();
            Objective obj = board.getObjective("MinaBoard");

            if (obj == null) {
                board = Bukkit.getScoreboardManager().getNewScoreboard();

                String titulo = ColorUtil.formatar("⓶");
                obj = board.registerNewObjective("MinaBoard", "dummy", titulo);
                obj.setDisplaySlot(DisplaySlot.SIDEBAR);

                // Organizei as posições (11 a 0) para encaixar tudo com os espaçamentos
                obj.getScore(ColorUtil.formatar("&1")).setScore(11);
                obj.getScore(ColorUtil.formatar("&2")).setScore(8);
                obj.getScore(ColorUtil.formatar("&3")).setScore(5);
                obj.getScore(ColorUtil.formatar("&4")).setScore(1);

                // Rodapé
                obj.getScore(ColorUtil.formatar("<#eab543>    Hʏᴠᴇʟ.ɴᴇᴛ")).setScore(0);

                player.setScoreboard(board);
            }

            // --- DADOS ---
            String cargo = dev.gdataa.hyvel.utils.HyvelHooks.getCargo(player);
            double multNum = dev.gdataa.hyvel.utils.HyvelHooks.getMultiplicador(player);
            String multiplicador = multNum + "x";

            double coins = 0.0;
            if (dev.gdataa.hyvel.utils.HyvelHooks.getEconomy() != null) {
                coins = dev.gdataa.hyvel.utils.HyvelHooks.getEconomy().getBalance(player);
            }

            int nivel = mina.getNivel();
            int prestigio = mina.getPrestigio();
            int tokens = mina.getTokens();
            int blocos = mina.getBlocosQuebradosTotal();

            // --- ATUALIZAÇÃO DINÂMICA ---

            atualizarLinha(board, "cargo",
                    ColorUtil.formatar("<#eab543>Cᴀʀɢᴏ: "),
                    ColorUtil.formatar(cargo), "§a", 10);

            atualizarLinha(board, "mult",
                    ColorUtil.formatar("<#eab543>ᴍᴜʟᴛɪᴘʟɪᴄᴀᴅᴏʀ:  "),
                    ColorUtil.formatar("<#eab543>" + multiplicador), "§b", 9);

            atualizarLinha(board, "nivel",
                    ColorUtil.formatar("<#eab543>ɴíᴠᴇʟ: "),
                    ColorUtil.formatar("<#eab543>" + nivel), "§c", 7);

            atualizarLinha(board, "prestigio",
                    ColorUtil.formatar("<#eab543>ᴘʀᴇꜱᴛíɢɪᴏ: "),
                    ColorUtil.formatar("<#eab543>" + prestigio), "§d", 6);

            atualizarLinha(board, "coins",
                    ColorUtil.formatar("<#ff793f>Cᴏɪɴs: "),
                    ColorUtil.formatar("§2$" + dev.gdataa.hyvel.utils.NumberUtil.formatar(coins)), "§e", 4);

            atualizarLinha(board, "tokens",
                    ColorUtil.formatar("<#ff793f>Tᴏᴋᴇɴs: "),
                    ColorUtil.formatar("§6" + dev.gdataa.hyvel.utils.NumberUtil.formatar(tokens)), "§5", 3);

            atualizarLinha(board, "blocos",
                    ColorUtil.formatar("<#ff793f>ʙʟᴏᴄᴏꜱ ǫ.: "),
                    ColorUtil.formatar("§f" + blocos), "§f", 2);
        }
    }

    private void atualizarLinha(Scoreboard board, String teamName, String prefix, String suffix, String entryId, int scoreIndex) {
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
            team.addEntry(entryId);
            board.getObjective("MinaBoard").getScore(entryId).setScore(scoreIndex);
        }
        team.setPrefix(prefix);
        team.setSuffix(suffix);
    }
}