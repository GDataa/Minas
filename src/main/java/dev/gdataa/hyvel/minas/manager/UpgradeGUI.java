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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UpgradeGUI implements InventoryHolder {

    private final MinasMain plugin;
    private final Player player;
    private final Inventory inventory;

    // --- CONFIGURAÇÃO DOS SLOTS ---
    private static final int SLOT_PICARETA = 9;
    private static final int SLOT_SKIN = 27;

    private static final int SLOT_PAG_ANTERIOR = 20;
    private static final int SLOT_PAG_SEGUINTE = 26;

    private static final int SLOT_FILTRO_TODOS = 49;
    private static final int SLOT_FILTRO_MONEY = 50;
    private static final int SLOT_FILTRO_TOKENS = 51;

    // A Grelha 3x5 para os encantamentos (15 slots)
    private static final int[] SLOTS_ENCANTAMENTOS = {
            12, 13, 14, 15, 16,
            21, 22, 23, 24, 25,
            30, 31, 32, 33, 34
    };

    // --- VARIÁVEIS DE ESTADO ---
    private int paginaAtual = 1;
    private Filtro filtroAtual = Filtro.TODOS;

    public enum Filtro {
        TODOS, MONEY, TOKENS
    }

    // Classe para representar um encantamento no menu
    public static class EncantamentoData {
        public String id;
        public String nome;
        public Material icone;
        public Filtro tipoMoeda;

        public int nivelAtual;
        public int nivelMaximo;
        public double chanceAtual;
        public double chancePorNivel;
        public List<String> lorePadrao;

        public EncantamentoData(String id, String nome, Material icone, Filtro tipoMoeda) {
            this.id = id;
            this.nome = nome;
            this.icone = icone;
            this.tipoMoeda = tipoMoeda;
        }
    }

    public UpgradeGUI(MinasMain plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, "§8Evoluir Picareta");

        renderizar();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void abrir() {
        player.openInventory(inventory);
    }

    // ==========================================
    // RENDERIZAÇÃO DA INTERFACE
    // ==========================================
    public void renderizar() {
        inventory.clear();

        ItemStack vidro = criarItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, vidro);
        }

        ItemStack picaretaJogador = player.getInventory().getItemInMainHand();
        if (picaretaJogador != null && picaretaJogador.getType() != Material.AIR) {
            inventory.setItem(SLOT_PICARETA, picaretaJogador);
        }

        inventory.setItem(SLOT_SKIN, criarItem(Material.NAME_TAG, "§e§lSkins da Picareta",
                "§7Clica para aplicares uma skin", "§7e obteres multiplicadores extra!"));

        inventory.setItem(SLOT_FILTRO_TODOS, criarItem(Material.NETHER_STAR, "§a§lTodos os Encantamentos",
                (filtroAtual == Filtro.TODOS ? "§a▶ Selecionado" : "§7Clica para ver todos.")));

        inventory.setItem(SLOT_FILTRO_MONEY, criarItem(Material.GOLD_INGOT, "§e§lEncantamentos por Coins",
                (filtroAtual == Filtro.MONEY ? "§a▶ Selecionado" : "§7Clica para filtrar.")) );

        inventory.setItem(SLOT_FILTRO_TOKENS, criarItem(Material.SUNFLOWER, "§6§lEncantamentos por Tokens",
                (filtroAtual == Filtro.TOKENS ? "§a▶ Selecionado" : "§7Clica para filtrar.")));

        List<EncantamentoData> listaFiltrada = obterListaFiltrada();
        int maxPaginas = (int) Math.ceil((double) listaFiltrada.size() / SLOTS_ENCANTAMENTOS.length);
        if (maxPaginas == 0) maxPaginas = 1;

        if (paginaAtual > maxPaginas) paginaAtual = maxPaginas;

        if (paginaAtual > 1) {
            inventory.setItem(SLOT_PAG_ANTERIOR, criarItem(Material.ARROW, "§aPágina Anterior", "§7Página: " + (paginaAtual - 1)));
        } else {
            inventory.setItem(SLOT_PAG_ANTERIOR, criarItem(Material.BARRIER, "§cSem página anterior"));
        }

        if (paginaAtual < maxPaginas) {
            inventory.setItem(SLOT_PAG_SEGUINTE, criarItem(Material.ARROW, "§aPágina Seguinte", "§7Página: " + (paginaAtual + 1)));
        } else {
            inventory.setItem(SLOT_PAG_SEGUINTE, criarItem(Material.BARRIER, "§cÚltima página"));
        }

        int startIndex = (paginaAtual - 1) * SLOTS_ENCANTAMENTOS.length;

        for (int i = 0; i < SLOTS_ENCANTAMENTOS.length; i++) {
            int listIndex = startIndex + i;

            if (listIndex < listaFiltrada.size()) {
                EncantamentoData enc = listaFiltrada.get(listIndex);
                ItemStack itemEnc = criarItemLoreLista(enc.icone, enc.nome, enc.lorePadrao);
                inventory.setItem(SLOTS_ENCANTAMENTOS[i], itemEnc);
            } else {
                inventory.setItem(SLOTS_ENCANTAMENTOS[i], criarItem(Material.GRAY_STAINED_GLASS_PANE, " "));
            }
        }
    }

    // ==========================================
    // LÓGICA DE CLIQUES
    // ==========================================
    public void processarClique(int slot) {
        if (slot == SLOT_PAG_ANTERIOR) {
            if (paginaAtual > 1) {
                paginaAtual--;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                renderizar();
            }
        }
        else if (slot == SLOT_PAG_SEGUINTE) {
            int maxPaginas = (int) Math.ceil((double) obterListaFiltrada().size() / SLOTS_ENCANTAMENTOS.length);
            if (paginaAtual < maxPaginas) {
                paginaAtual++;
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                renderizar();
            }
        }
        else if (slot == SLOT_FILTRO_TODOS) mudarFiltro(Filtro.TODOS);
        else if (slot == SLOT_FILTRO_MONEY) mudarFiltro(Filtro.MONEY);
        else if (slot == SLOT_FILTRO_TOKENS) mudarFiltro(Filtro.TOKENS);
        else if (slot == SLOT_SKIN) {
            player.sendMessage("§a[!] A abrir o menu de Skins... (Em breve)");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
        }
        else {
            for (int i = 0; i < SLOTS_ENCANTAMENTOS.length; i++) {
                if (slot == SLOTS_ENCANTAMENTOS[i]) {
                    int listIndex = ((paginaAtual - 1) * SLOTS_ENCANTAMENTOS.length) + i;
                    List<EncantamentoData> listaFiltrada = obterListaFiltrada();

                    if (listIndex < listaFiltrada.size()) {
                        EncantamentoData encClicado = listaFiltrada.get(listIndex);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

                        new dev.gdataa.hyvel.minas.manager.GerenciarUpgradeGUI(plugin, player, encClicado).abrir();
                    }
                    break;
                }
            }
        }
    }

    private void mudarFiltro(Filtro novoFiltro) {
        if (this.filtroAtual != novoFiltro) {
            this.filtroAtual = novoFiltro;
            this.paginaAtual = 1;
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
            renderizar();
        }
    }

    // ==========================================
    // SISTEMA DE DADOS E LORE DINÂMICA
    // ==========================================
    private List<EncantamentoData> obterListaFiltrada() {
        List<EncantamentoData> todos = new ArrayList<>();

        ItemStack picareta = player.getInventory().getItemInMainHand();
        if (picareta == null || picareta.getType() == Material.AIR) {
            picareta = player.getInventory().getItem(4);
        }

        // 1. Pega o Nível atual da Picareta
        int nivelPic = dev.gdataa.hyvel.utils.PickaxeUtils.getPicaretaNivel(picareta, plugin);

        int lvlFortuna = dev.gdataa.hyvel.utils.PickaxeUtils.getEnchantLevel(picareta, dev.gdataa.hyvel.utils.PickaxeUtils.ENC_FORTUNA, plugin);
        int maxFortuna = dev.gdataa.hyvel.utils.PickaxeUtils.getMaxLevel(dev.gdataa.hyvel.utils.PickaxeUtils.ENC_FORTUNA, nivelPic);
        int reqFortuna = dev.gdataa.hyvel.utils.PickaxeUtils.getNivelNecessario(dev.gdataa.hyvel.utils.PickaxeUtils.ENC_FORTUNA);

        int lvlTokinator = dev.gdataa.hyvel.utils.PickaxeUtils.getEnchantLevel(picareta, dev.gdataa.hyvel.utils.PickaxeUtils.ENC_TOKENS, plugin);
        int maxTokinator = dev.gdataa.hyvel.utils.PickaxeUtils.getMaxLevel(dev.gdataa.hyvel.utils.PickaxeUtils.ENC_TOKENS, nivelPic);
        int reqTokinator = dev.gdataa.hyvel.utils.PickaxeUtils.getNivelNecessario(dev.gdataa.hyvel.utils.PickaxeUtils.ENC_TOKENS);

        int lvlKeys = dev.gdataa.hyvel.utils.PickaxeUtils.getEnchantLevel(picareta, dev.gdataa.hyvel.utils.PickaxeUtils.ENC_KEYS, plugin);
        int maxKeys = dev.gdataa.hyvel.utils.PickaxeUtils.getMaxLevel(dev.gdataa.hyvel.utils.PickaxeUtils.ENC_KEYS, nivelPic);
        int reqKeys = dev.gdataa.hyvel.utils.PickaxeUtils.getNivelNecessario(dev.gdataa.hyvel.utils.PickaxeUtils.ENC_KEYS);

        int lvlMagnetismo = dev.gdataa.hyvel.utils.PickaxeUtils.getEnchantLevel(picareta, dev.gdataa.hyvel.utils.PickaxeUtils.ENC_MAGNETISMO, plugin);
        int maxMagnetismo = dev.gdataa.hyvel.utils.PickaxeUtils.getMaxLevel(dev.gdataa.hyvel.utils.PickaxeUtils.ENC_MAGNETISMO, nivelPic);
        int reqMagnetismo = dev.gdataa.hyvel.utils.PickaxeUtils.getNivelNecessario(dev.gdataa.hyvel.utils.PickaxeUtils.ENC_MAGNETISMO);

        int lvlGolfinho = dev.gdataa.hyvel.utils.PickaxeUtils.getEnchantLevel(picareta, dev.gdataa.hyvel.utils.PickaxeUtils.ENC_GOLFINHO, plugin);
        int maxGolfinho = dev.gdataa.hyvel.utils.PickaxeUtils.getMaxLevel(dev.gdataa.hyvel.utils.PickaxeUtils.ENC_GOLFINHO, nivelPic);
        int reqGolfinho = dev.gdataa.hyvel.utils.PickaxeUtils.getNivelNecessario(dev.gdataa.hyvel.utils.PickaxeUtils.ENC_GOLFINHO);

        int lvlCamada = dev.gdataa.hyvel.utils.PickaxeUtils.getEnchantLevel(picareta, dev.gdataa.hyvel.utils.PickaxeUtils.ENC_CAMADA, plugin);
        int maxCamada = dev.gdataa.hyvel.utils.PickaxeUtils.getMaxLevel(dev.gdataa.hyvel.utils.PickaxeUtils.ENC_CAMADA, nivelPic);
        int reqCamada = dev.gdataa.hyvel.utils.PickaxeUtils.getNivelNecessario(dev.gdataa.hyvel.utils.PickaxeUtils.ENC_CAMADA);

        int lvlEnxame = dev.gdataa.hyvel.utils.PickaxeUtils.getEnchantLevel(picareta, dev.gdataa.hyvel.utils.PickaxeUtils.ENC_ENXAME, plugin);
        int maxEnxame = dev.gdataa.hyvel.utils.PickaxeUtils.getMaxLevel(dev.gdataa.hyvel.utils.PickaxeUtils.ENC_ENXAME, nivelPic);
        int reqEnxame = dev.gdataa.hyvel.utils.PickaxeUtils.getNivelNecessario(dev.gdataa.hyvel.utils.PickaxeUtils.ENC_ENXAME);

        int lvlBuraco = dev.gdataa.hyvel.utils.PickaxeUtils.getEnchantLevel(picareta, dev.gdataa.hyvel.utils.PickaxeUtils.ENC_BURACO_NEGRO, plugin);
        int maxBuraco = dev.gdataa.hyvel.utils.PickaxeUtils.getMaxLevel(dev.gdataa.hyvel.utils.PickaxeUtils.ENC_BURACO_NEGRO, nivelPic);
        int reqBuraco = dev.gdataa.hyvel.utils.PickaxeUtils.getNivelNecessario(dev.gdataa.hyvel.utils.PickaxeUtils.ENC_BURACO_NEGRO);

        int lvlPirata = dev.gdataa.hyvel.utils.PickaxeUtils.getEnchantLevel(picareta, dev.gdataa.hyvel.utils.PickaxeUtils.ENC_PIRATA, plugin);
        int maxPirata = dev.gdataa.hyvel.utils.PickaxeUtils.getMaxLevel(dev.gdataa.hyvel.utils.PickaxeUtils.ENC_PIRATA, nivelPic);
        int reqPirata = dev.gdataa.hyvel.utils.PickaxeUtils.getNivelNecessario(dev.gdataa.hyvel.utils.PickaxeUtils.ENC_PIRATA);

        int lvlDragao = dev.gdataa.hyvel.utils.PickaxeUtils.getEnchantLevel(picareta, dev.gdataa.hyvel.utils.PickaxeUtils.ENC_DRAGAO, plugin);
        int maxDragao = dev.gdataa.hyvel.utils.PickaxeUtils.getMaxLevel(dev.gdataa.hyvel.utils.PickaxeUtils.ENC_DRAGAO, nivelPic);
        int reqDragao = dev.gdataa.hyvel.utils.PickaxeUtils.getNivelNecessario(dev.gdataa.hyvel.utils.PickaxeUtils.ENC_DRAGAO);

        // Adiciona à lista (Passa o Nível da Picareta e o Nível Requerido)
        todos.add(criarEncData("FORTUNA", "§aFortuna", Material.EMERALD, Filtro.MONEY, "Aumenta a quantia de coins ganhos.", lvlFortuna, maxFortuna, lvlFortuna * 100.0, 100.0, nivelPic, reqFortuna));
        todos.add(criarEncData("TOKENS", "§6Tokinator", Material.HORN_CORAL, Filtro.MONEY, "Aumenta a quantia de tokens.", lvlTokinator, maxTokinator, lvlTokinator * 100.0, 100.0, nivelPic, reqTokinator));
        todos.add(criarEncData("KEYS", "§bKey Finder", Material.TRIPWIRE_HOOK, Filtro.TOKENS, "Aumenta a chance de achar chaves.", lvlKeys, maxKeys, lvlKeys * 0.01, 0.01, nivelPic, reqKeys));
        todos.add(criarEncData("ENXAME", "§eEnxame Explosivo", Material.BEE_SPAWN_EGG, Filtro.TOKENS, "Invoca abelhas explosivas.", lvlEnxame, maxEnxame, lvlEnxame * 0.3, 0.3, nivelPic, reqEnxame));
        todos.add(criarEncData("GOLFINHO", "§3Golfinho Sortudo", Material.DOLPHIN_SPAWN_EGG, Filtro.TOKENS, "Dá bónus de XP de Rank.", lvlGolfinho, maxGolfinho, lvlGolfinho * 0.5, 0.5, nivelPic, reqGolfinho));
        todos.add(criarEncData("MAGNETISMO", "§5Magnetismo", Material.HOPPER, Filtro.TOKENS, "Puxa os blocos partidos.", lvlMagnetismo, maxMagnetismo, lvlMagnetismo * 1.0, 1.0, nivelPic, reqMagnetismo));
        todos.add(criarEncData("CAMADA", "§eCamada", Material.TNT, Filtro.TOKENS, "Vaporiza uma camada inteira.", lvlCamada, maxCamada, lvlCamada * 0.5, 0.5, nivelPic, reqCamada));
        todos.add(criarEncData("BURACO_NEGRO", "§8Buraco Negro", Material.OBSIDIAN, Filtro.TOKENS, "Engole blocos em massa.", lvlBuraco, maxBuraco, lvlBuraco * 0.1, 0.1, nivelPic, reqBuraco));
        todos.add(criarEncData("PIRATA", "§cPirata", Material.BONE, Filtro.TOKENS, "Uma tripulação pirata saqueia a mina.", lvlPirata, maxPirata, lvlPirata * 0.5, 0.5, nivelPic, reqPirata));
        todos.add(criarEncData("DRAGAO", "§4Dragão", Material.DRAGON_HEAD, Filtro.TOKENS, "O Ender Dragon devasta a tua mina.", lvlDragao, maxDragao, lvlDragao * 0.2, 0.2, nivelPic, reqDragao));

        if (filtroAtual == Filtro.TODOS) return todos;

        List<EncantamentoData> filtrados = new ArrayList<>();
        for (EncantamentoData enc : todos) {
            if (enc.tipoMoeda == filtroAtual) filtrados.add(enc);
        }
        return filtrados;
    }

    private EncantamentoData criarEncData(String id, String nome, Material icone, Filtro moeda, String desc, int nivAtual, int nivMax, double chanceAtual, double chancePorNivel, int picaretaNivel, int reqNivel) {
        EncantamentoData enc = new EncantamentoData(id, nome, icone, moeda);
        enc.nivelAtual = nivAtual;
        enc.nivelMaximo = nivMax;
        enc.chanceAtual = chanceAtual;
        enc.chancePorNivel = chancePorNivel;

        String cor = "§a";
        if (nome.length() >= 2 && (nome.startsWith("§") || nome.startsWith("&"))) {
            cor = nome.substring(0, 2);
        }

        String chanceAtualFormat = String.format("%.2f", chanceAtual).replace(",", ".");
        String chancePorNivelFormat = String.format("%.2f", chancePorNivel).replace(",", ".");

        // ==========================================
        // SE A PICARETA NÃO TIVER NÍVEL SUFICIENTE
        // ==========================================
        if (picaretaNivel < reqNivel) {
            enc.icone = Material.BARRIER; // Fica como Barreira Vermelha
            enc.lorePadrao = Arrays.asList(
                    "§cEncantamento Bloqueado",
                    "",
                    "§7A tua picareta não é forte",
                    "§7o suficiente para suportar este poder.",
                    "",
                    "§cDesbloqueia no Nível §l" + reqNivel,
                    "§7Nível atual da Picareta: §f" + picaretaNivel
            );
            return enc;
        }

        // ==========================================
        // SE ESTIVER DESBLOQUEADO
        // ==========================================
        enc.lorePadrao = Arrays.asList(
                "§8Encantamento Desbloqueado",
                "",
                cor + desc,
                "",
                cor + "Informações:",
                cor + "▸ §fNível: " + cor + dev.gdataa.hyvel.utils.NumberUtil.formatar(nivAtual) + "§8/" + cor + dev.gdataa.hyvel.utils.NumberUtil.formatar(nivMax),
                cor + "▸ §fChance base: " + cor + chancePorNivelFormat + "%",
                cor + "▸ §fChance atual: " + cor + chanceAtualFormat + "%",
                "",
                "§7Clique para gerenciar."
        );
        return enc;
    }

    private ItemStack criarItem(Material material, String nome, String... lore) {
        return criarItemLoreLista(material, nome, Arrays.asList(lore));
    }

    private ItemStack criarItemLoreLista(Material material, String nome, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(nome);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}