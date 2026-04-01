package dev.gdataa.hyvel.utils;

import dev.gdataa.hyvel.MinasMain;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class PickaxeUtils {

    // Nomes internos da Picareta
    public static final String KEY_NIVEL = "picareta_nivel";
    public static final String KEY_XP = "picareta_xp";

    // Nomes internos dos encantamentos
    public static final String ENC_FORTUNA = "enc_fortuna";
    public static final String ENC_TOKENS = "enc_tokens";
    public static final String ENC_KEYS = "enc_keys";
    public static final String ENC_GOLFINHO = "enc_golfinho";
    public static final String ENC_MAGNETISMO = "enc_magnetismo";
    public static final String ENC_CAMADA = "enc_camada";
    public static final String ENC_BURACO_NEGRO = "enc_buraco_negro";
    public static final String ENC_PIRATA = "enc_pirata";
    public static final String ENC_DRAGAO = "enc_dragao";
    public static final String ENC_ENXAME = "enc_enxame";

    // --- REQUISITOS DE NÍVEL DA PICARETA ---
    public static int getNivelNecessario(String enchant) {
        switch (enchant) {
            case ENC_PIRATA: return 50;
            case ENC_MAGNETISMO: return 25;
            case ENC_ENXAME: return 10;
            case ENC_BURACO_NEGRO: return 60;
            case ENC_GOLFINHO: return 20;
            case ENC_DRAGAO: return 100;
            case ENC_CAMADA: return 80;
            default: return 1; // Fortuna, Tokens, Keys, Camada vêm logo no Nível 1
        }
    }

    // --- LIMITES BASEADOS NO NÍVEL DA PICARETA ---
    public static int getMaxLevel(String enchant, int picaretaNivel) {
        switch (enchant) {
            case ENC_FORTUNA:
            case ENC_TOKENS:
            case ENC_KEYS:
                // Limite INFINITO! Escala com o nível da picareta (Ex: Nível 1 = 5.000 | Nível 100 = 10.000)
                return 5000 + (picaretaNivel * 50);

            case ENC_MAGNETISMO:
            case ENC_BURACO_NEGRO:
            case ENC_CAMADA:
            case ENC_PIRATA:
            case ENC_DRAGAO:
            case ENC_ENXAME:
            case ENC_GOLFINHO:
                return 100; // As skills de % ficam cravadas no 100 (para dar 100% chance no máximo)

            default: return 0;
        }
    }

    // --- LER DADOS DA PICARETA ---
    public static int getPicaretaNivel(ItemStack item, MinasMain plugin) {
        if (item == null || !item.hasItemMeta()) return 1;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, KEY_NIVEL), PersistentDataType.INTEGER, 1);
    }

    public static double getPicaretaXP(ItemStack item, MinasMain plugin) {
        if (item == null || !item.hasItemMeta()) return 0.0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, KEY_XP), PersistentDataType.DOUBLE, 0.0);
    }

    public static int getEnchantLevel(ItemStack item, String enchant, MinasMain plugin) {
        if (item == null || !item.hasItemMeta()) return 0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, enchant), PersistentDataType.INTEGER, 0);
    }

    public static double getXPNecessario(int nivelAtual) {
        // Fórmula infinita: Nível atual * 500 (Nvl 1 precisa 500, Nvl 2 precisa 1000...)
        return nivelAtual * 500.0;
    }

    // --- SISTEMA DE DAR XP À PICARETA ---
    public static void adicionarXP(ItemStack item, double quantidade, MinasMain plugin, Player player) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        int nivel = meta.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, KEY_NIVEL), PersistentDataType.INTEGER, 1);
        int nivelAntigo = nivel; // Guardamos o nível antigo aqui!

        double xpAtual = meta.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, KEY_XP), PersistentDataType.DOUBLE, 0.0);

        xpAtual += quantidade;
        double xpNecessario = getXPNecessario(nivel);

        boolean subiuNivel = false;
        while (xpAtual >= xpNecessario) {
            xpAtual -= xpNecessario;
            nivel++;
            subiuNivel = true;
            xpNecessario = getXPNecessario(nivel);
        }

        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, KEY_NIVEL), PersistentDataType.INTEGER, nivel);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, KEY_XP), PersistentDataType.DOUBLE, xpAtual);
        item.setItemMeta(meta);

        // Atualiza a Lore visualmente
        reconstruirLore(item, plugin);

        if (subiuNivel) {
            // Envia o Title gigante para o ecrã do jogador (FadeIn: 10 ticks, Fica: 40 ticks, FadeOut: 10 ticks)
            player.sendTitle("§e⛏ §lPICARETA", "§7§m" + nivelAntigo + "§f ➟ §a" + nivel, 10, 40, 10);

            // Mensagem no chat a acompanhar
            player.sendMessage(ColorUtil.formatar("<#f1c40f>§lUPGRADE! §7A tua Picareta subiu para o Nível §f" + nivel + "§7!"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }
    }

    // --- APLICAR UPGRADE AOS ENCANTAMENTOS ---
    public static ItemStack upgradeEnchant(ItemStack item, String enchant, MinasMain plugin, int niveisAAdicionar) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        NamespacedKey key = new NamespacedKey(plugin, enchant);
        int currentLevel = meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, 0);
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, currentLevel + niveisAAdicionar);
        item.setItemMeta(meta);

        return reconstruirLore(item, plugin);
    }

    // --- CONSTRUIR A LORE COM O NÍVEL DA PICARETA ---
    public static ItemStack reconstruirLore(ItemStack item, MinasMain plugin) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        int nivel = meta.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, KEY_NIVEL), PersistentDataType.INTEGER, 1);
        double xpAtual = meta.getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin, KEY_XP), PersistentDataType.DOUBLE, 0.0);
        double xpNecessario = getXPNecessario(nivel);

        List<String> lore = new ArrayList<>();
        lore.add("§7Uma ferramenta ancestral ligada à tua alma.");
        lore.add("");
        lore.add("§8[ §e⭐ Nível da Picareta: §f" + nivel + " §8]");
        lore.add("§8[ §aXP: §f" + String.format("%.0f", xpAtual) + "§8/§a" + String.format("%.0f", xpNecessario) + " §8]");
        lore.add("");
        lore.add("§e(Clique Direito para abrir os Upgrades)");
        lore.add("");
        lore.add("§6§lENCANTAMENTOS:");

        lore.add(" §b▸ §bEficiência ♾");

        int fortLvl = getEnchantLevel(item, ENC_FORTUNA, plugin);
        if (fortLvl > 0) lore.add(" §a▸ §aFortuna Lvl. " +fortLvl);

        int tokLvl = getEnchantLevel(item, ENC_TOKENS, plugin);
        if (tokLvl > 0) lore.add(" §e▸ §eTokinator Lvl. " + tokLvl);

        int keysLvl = getEnchantLevel(item, ENC_KEYS, plugin);
        if (keysLvl > 0) lore.add(" §b▸ §bKey Finder Lvl. " + keysLvl);

        int magLvl = getEnchantLevel(item, ENC_MAGNETISMO, plugin);
        if (magLvl > 0) lore.add(" §5▸ §5Magnetismo Lvl. " + magLvl);

        int golfLvl = getEnchantLevel(item, ENC_GOLFINHO, plugin);
        if (golfLvl > 0) lore.add(" §3▸ §3Golfinho Sortudo Lvl. " + golfLvl);

        int camadaLvl = getEnchantLevel(item, ENC_CAMADA, plugin);
        if (camadaLvl > 0) lore.add(" §e▸ §eCamada Lvl. " + camadaLvl);

        int enxameLvl = getEnchantLevel(item, ENC_ENXAME, plugin);
        if (enxameLvl > 0) lore.add(" §e▸ §eEnxame Explosivo Lvl. " + enxameLvl);

        int burLvl = getEnchantLevel(item, ENC_BURACO_NEGRO, plugin);
        if (burLvl > 0) lore.add(" §8▸ §8Buraco Negro Lvl. " + burLvl);

        int pirataLvl = getEnchantLevel(item, ENC_PIRATA, plugin);
        if (pirataLvl > 0) lore.add(" §c▸ §cPirata Lvl. " + pirataLvl);

        int dragaoLvl = getEnchantLevel(item, ENC_DRAGAO, plugin);
        if (dragaoLvl > 0) lore.add(" §4▸ §4§lDragão Lvl. " + dragaoLvl);

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}