package dev.gdataa.hyvel.minas.listener;

import dev.gdataa.hyvel.MinasMain;
import dev.gdataa.hyvel.minas.model.Mina;
import dev.gdataa.hyvel.minas.skills.BlackHoleSkill;
import dev.gdataa.hyvel.minas.skills.DragaoSkill;
import dev.gdataa.hyvel.minas.skills.MagnetismoSkill;
import dev.gdataa.hyvel.minas.skills.PirataSkill;
import dev.gdataa.hyvel.utils.ColorUtil;
import dev.gdataa.hyvel.utils.PickaxeUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class MinaListener implements Listener {

    private final MinasMain plugin;
    private final NamespacedKey tagMina;

    public MinaListener(MinasMain plugin) {
        this.plugin = plugin;
        this.tagMina = new NamespacedKey(plugin, "item_mina");
    }

    @EventHandler
    public void aoQuebrarBloco(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location loc = block.getLocation();
        ItemStack picaretaNaMao = player.getInventory().getItemInMainHand();
        boolean temPicaretaDaMina = picaretaNaMao.hasItemMeta() && picaretaNaMao.getItemMeta().getPersistentDataContainer().has(tagMina, PersistentDataType.STRING);

        Mina mina = plugin.getMinaManager().getMinasCache().get(player.getUniqueId());

        // 1. O JOGADOR ESTÁ FORA DA MINA
        if (mina == null || !loc.getWorld().getName().equals(mina.getNomeMundo())) {
            if (temPicaretaDaMina) {
                event.setCancelled(true);
                player.sendMessage("§cNão podes usar a picareta da mina no mundo normal!");
            }
            return;
        }

        // 2. O JOGADOR ESTÁ DENTRO DA MINA
        if (player.getGameMode() == GameMode.CREATIVE && player.hasPermission("minas.admin")) {
            return; // Admin a construir
        }

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
        event.setDropItems(false);
        Material materialQuebrado = block.getType();
        block.setType(Material.AIR);

        // --- SKILL: MAGNETISMO (Visual) ---
        if (temPicaretaDaMina) {
            int lvlMag = PickaxeUtils.getEnchantLevel(picaretaNaMao, PickaxeUtils.ENC_MAGNETISMO, plugin);
            if (lvlMag > 0) {
                // Ativa a animação visual de puxar o bloco
                MagnetismoSkill.disparar(player, loc, materialQuebrado);

                // Usamos ActionBar para o Magnetismo não destruir o chat do jogador com spam!
                player.sendActionBar(ColorUtil.formatar("<#a29bfe>§lMAGNETISMO §7A puxar blocos..."));
            }
        }

        // Processa a recompensa do bloco principal (dinheiro e tokens base/fortuna geridos no MinaManager)
        plugin.getMinaManager().processarRecompensa(player, mina, materialQuebrado);

        // ==========================================
        // 4. LÓGICA DAS OUTRAS SKILLS DA PICARETA
        // ==========================================
        if (temPicaretaDaMina) {

            // --- SKILL: CAMADA (Destruição em massa) ---
            int lvlCamada = PickaxeUtils.getEnchantLevel(picaretaNaMao, PickaxeUtils.ENC_CAMADA, plugin);
            if (lvlCamada > 0) {
                // 0.05% de chance por nível
                if (Math.random() <= (lvlCamada * 0.0005)) {
                    destruirCamada(player, mina, loc.getBlockY());
                    player.sendMessage(ColorUtil.formatar("<#ff793f>§lCAMADA! §7Uma camada inteira foi vaporizada!"));
                }
            }

            // --- SKILL: BURACO NEGRO ---
            int nivelBuracoNegro = PickaxeUtils.getEnchantLevel(picaretaNaMao, PickaxeUtils.ENC_BURACO_NEGRO, plugin);
            if (nivelBuracoNegro > 0) {
                // 0.1% de chance por cada nível (Ex: Nível 100 = 10% de chance)
                double chanceBuraco = nivelBuracoNegro * 0.001;
                if (Math.random() <= chanceBuraco) {
                    player.sendMessage(ColorUtil.formatar("<#5f27cd>§l✧ O Buraco Negro despertou!"));
                    new BlackHoleSkill(plugin, player, mina).runTaskTimer(plugin, 0L, 1L);
                }
            }

            // --- SKILL: PIRATA ---
            int nivelPirata = PickaxeUtils.getEnchantLevel(picaretaNaMao, PickaxeUtils.ENC_PIRATA, plugin);
            if (nivelPirata > 0) {
                // 0.05% de chance por nível
                double chancePirata = nivelPirata * 0.0005;
                if (Math.random() <= chancePirata) {
                    new PirataSkill(plugin, player, mina, nivelPirata).runTaskTimer(plugin, 0L, 1L);
                }
            }

            // --- SKILL: DRAGÃO (Prestígio 10) ---
            int nivelDragao = PickaxeUtils.getEnchantLevel(picaretaNaMao, PickaxeUtils.ENC_DRAGAO, plugin);
            if (nivelDragao > 0) {
                // 0.02% de chance por nível (É tão forte que deve ser mais raro!)
                double chanceDragao = nivelDragao * 0.0002;
                if (Math.random() <= chanceDragao) {
                    new DragaoSkill(plugin, player, mina).runTaskTimer(plugin, 0L, 1L);
                }
            }
        }
    }

    // Método auxiliar para destruir a camada inteira
    private void destruirCamada(Player player, Mina mina, int y) {
        int minX = plugin.getConfig().getInt("regiao.minX");
        int maxX = plugin.getConfig().getInt("regiao.maxX");
        int minZ = plugin.getConfig().getInt("regiao.minZ");
        int maxZ = plugin.getConfig().getInt("regiao.maxZ");

        World world = mina.getBukkitWorld();

        for (int x = Math.min(minX, maxX); x <= Math.max(minX, maxX); x++) {
            for (int z = Math.min(minZ, maxZ); z <= Math.max(minZ, maxZ); z++) {
                Block b = world.getBlockAt(x, y, z);
                Material type = b.getType();
                if (type != Material.AIR && type != Material.BEDROCK) {
                    b.setType(Material.AIR, false);
                    // Dá a recompensa de cada bloco destruído na camada
                    plugin.getMinaManager().processarRecompensa(player, mina, type);
                }
            }
        }
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
    }

    // ==========================================
    // SISTEMA DE PROTEÇÃO DE ITENS E MENUS
    // ==========================================

    @EventHandler
    public void aoInteragirComItens(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            String valor = item.getItemMeta().getPersistentDataContainer().get(tagMina, PersistentDataType.STRING);

            if (valor != null) {
                if (valor.equals("sair_item")) {
                    event.setCancelled(true);
                    plugin.getMinaManager().sairMina(player);
                } else if (valor.equals("picareta")) {
                    Mina mina = plugin.getMinaManager().getMinasCache().get(player.getUniqueId());
                    if (mina != null) {
                        // Instancia e abre o NOVO menu Top Xuxa!
                        dev.gdataa.hyvel.minas.manager.UpgradeGUI menu = new dev.gdataa.hyvel.minas.manager.UpgradeGUI(plugin, player);
                        menu.abrir();
                    }
                }
            }
        }
    }

    @EventHandler
    public void aoDropar(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (item == null || !item.hasItemMeta()) return;

        if (item.getItemMeta().getPersistentDataContainer().has(tagMina, PersistentDataType.STRING)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cVocê não pode jogar fora itens da mina!");
        }
    }

    @EventHandler
    public void aoMover(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(tagMina, PersistentDataType.STRING)) {
            event.setCancelled(true);
            return;
        }

        if (event.getClick() == ClickType.NUMBER_KEY) {
            ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
            if (hotbarItem != null && hotbarItem.hasItemMeta() && hotbarItem.getItemMeta().getPersistentDataContainer().has(tagMina, PersistentDataType.STRING)) {
                event.setCancelled(true);
            }
        }
    }

    // ==========================================
    // PROTEÇÃO CONTRA CRASHES / LOGOUTS
    // ==========================================

    @EventHandler
    public void aoSairDoServidor(PlayerQuitEvent event) {
        plugin.getMinaManager().restaurarInventario(event.getPlayer());
    }

    @EventHandler
    public void aoMudarDeMundo(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String mundoAnterior = event.getFrom().getName();
        String mundoAtual = player.getWorld().getName();

        if (mundoAnterior.startsWith("mina_") && !mundoAtual.startsWith("mina_")) {
            plugin.getMinaManager().restaurarInventario(player);
        }
    }

    // ==========================================
    // IMUNIDADE E PROTEÇÃO NA MINA
    // ==========================================

    @EventHandler
    public void aoPerderFome(org.bukkit.event.entity.FoodLevelChangeEvent event) {
        // Se a entidade está no mundo da mina, bloqueia a perda de fome e enche a barra!
        if (event.getEntity().getWorld().getName().startsWith("mina_")) {
            event.setCancelled(true);
            ((Player) event.getEntity()).setFoodLevel(20);
        }
    }

    @EventHandler
    public void aoTomarDano(org.bukkit.event.entity.EntityDamageEvent event) {
        // Se for um jogador a levar dano no mundo da mina, cancela instantaneamente
        if (event.getEntity() instanceof Player) {
            if (event.getEntity().getWorld().getName().startsWith("mina_")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void aoEntrarNoServidor(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.getWorld().getName().startsWith("mina_")) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(tagMina, PersistentDataType.STRING)) {
                    player.getInventory().remove(item);
                }
            }
        }
    }
}