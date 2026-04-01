package dev.gdataa.hyvel.minas.manager;

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import dev.gdataa.hyvel.MinasMain;
import dev.gdataa.hyvel.minas.model.Mina;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MinaManager {

    private final MinasMain plugin;
    private final SlimeLoader customLoader;
    private final AdvancedSlimePaperAPI asp;

    private final Map<UUID, Mina> minasCache = new HashMap<>();
    private final Map<UUID, ItemStack[]> inventariosSalvos = new HashMap<>();
    private final Map<UUID, ItemStack[]> armadurasSalvas = new HashMap<>();

    private final String NOME_MAPA_BASE = "Mina";

    public MinaManager(MinasMain plugin, SlimeLoader customLoader) {
        this.plugin = plugin;
        this.customLoader = customLoader;
        this.asp = AdvancedSlimePaperAPI.instance();
    }

    public void acessarMina(Player player) {
        UUID uuid = player.getUniqueId();
        Mina mina = minasCache.computeIfAbsent(uuid, k -> new Mina(uuid));

        if (mina.getBukkitWorld() != null) {
            salvarInventario(player);
            prepararInventarioMina(player, mina);
            player.teleport(mina.getSpawnLocation());

            player.setHealth(20.0);
            player.setFoodLevel(20);

            player.sendMessage("§aTeleportado para sua mina!");
            return;
        }

        player.sendMessage("§eCarregando sua mina, aguarde...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                SlimePropertyMap props = new SlimePropertyMap();
                props.setValue(SlimeProperties.DIFFICULTY, "normal");

                SlimeWorld mundoMina;
                boolean mundoExisteNoDisco = customLoader.worldExists(mina.getNomeMundo());

                if (mundoExisteNoDisco) {
                    mundoMina = asp.readWorld(customLoader, mina.getNomeMundo(), false, props);
                } else {
                    SlimeWorld template = asp.readWorld(customLoader, NOME_MAPA_BASE, true, props);
                    mundoMina = template.clone(mina.getNomeMundo(), customLoader);
                }

                mina.setSlimeWorld(mundoMina);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    Logger logger = Logger.getLogger("Minecraft");
                    Level prevLogLevel = logger.getLevel();
                    logger.setLevel(Level.WARNING);

                    asp.loadWorld(mundoMina, true);

                    logger.setLevel(prevLogLevel);

                    World bukkitWorld = mina.getBukkitWorld();
                    if (bukkitWorld != null) {
                        bukkitWorld.setAutoSave(false);
                        bukkitWorld.setKeepSpawnInMemory(false);

                        // Configurações do Mundo
                        bukkitWorld.setDifficulty(Difficulty.PEACEFUL);
                        bukkitWorld.setTime(6000L);
                        bukkitWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                        bukkitWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
                        bukkitWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);

                        preencherMina(mina);
                        salvarInventario(player);
                        prepararInventarioMina(player, mina);

                        player.teleport(mina.getSpawnLocation());

                        player.setHealth(20.0);
                        player.setFoodLevel(20);

                        player.sendMessage("§aMina carregada e preparada com sucesso!");
                    }
                });

            } catch (Exception e) {
                plugin.getLogger().severe("Erro ao carregar a mina de " + player.getName());
                e.printStackTrace();
            }
        });
    }

    public void descarregarMina(Player player) {
        Mina mina = minasCache.get(player.getUniqueId());
        if (mina == null || mina.getBukkitWorld() == null) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            World bukkitWorld = mina.getBukkitWorld();

            if (bukkitWorld != null) {
                for (Player p : bukkitWorld.getPlayers()) {
                    p.sendMessage("§eA mina foi fechada pois o dono saiu do servidor.");
                    p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                }

                Bukkit.unloadWorld(bukkitWorld, true);
                mina.setSlimeWorld(null);
            }
        }, 20L);
    }

    public void preencherMina(Mina mina) {
        World world = mina.getBukkitWorld();
        if (world == null) return;

        String faixa = "1";
        if (plugin.getConfig().getConfigurationSection("progresso") != null) {
            for (String key : plugin.getConfig().getConfigurationSection("progresso").getKeys(false)) {
                int nivelRequisitado = Integer.parseInt(key);
                if (mina.getNivel() >= nivelRequisitado) {
                    faixa = key;
                }
            }
        }

        Map<Material, Double> minerios = new HashMap<>();
        double totalPeso = 0.0;
        String path = "progresso." + faixa + ".minerios";

        if (plugin.getConfig().getConfigurationSection(path) != null) {
            for (String key : plugin.getConfig().getConfigurationSection(path).getKeys(false)) {
                Material mat = Material.matchMaterial(key);
                if (mat != null) {
                    double chance = plugin.getConfig().getDouble(path + "." + key);
                    minerios.put(mat, chance);
                    totalPeso += chance;
                }
            }
        }

        if (minerios.isEmpty()) return;

        int minX = plugin.getConfig().getInt("regiao.minX");
        int minY = plugin.getConfig().getInt("regiao.minY");
        int minZ = plugin.getConfig().getInt("regiao.minZ");
        int maxX = plugin.getConfig().getInt("regiao.maxX");
        int maxY = plugin.getConfig().getInt("regiao.maxY");
        int maxZ = plugin.getConfig().getInt("regiao.maxZ");

        int totalBlocos = 0;
        for (int x = Math.min(minX, maxX); x <= Math.max(minX, maxX); x++) {
            for (int y = Math.min(minY, maxY); y <= Math.max(minY, maxY); y++) {
                for (int z = Math.min(minZ, maxZ); z <= Math.max(minZ, maxZ); z++) {
                    Material sorteado = sortearMinerio(minerios, totalPeso);
                    world.getBlockAt(x, y, z).setType(sorteado, false);
                    totalBlocos++;
                }
            }
        }
        mina.setBlocosTotais(totalBlocos);
        mina.resetarContagem();
    }

    private Material sortearMinerio(Map<Material, Double> minerios, double totalPeso) {
        double random = Math.random() * totalPeso;
        double atual = 0.0;
        for (Map.Entry<Material, Double> entry : minerios.entrySet()) {
            atual += entry.getValue();
            if (random <= atual) {
                return entry.getKey();
            }
        }
        return Material.STONE;
    }

    public void resetarMina(Mina mina, Player player) {
        player.sendMessage("§eA tua mina está a ser reabastecida!");
        player.teleport(mina.getSpawnLocation());
        preencherMina(mina);
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 1f, 1f);
    }

    private void salvarInventario(Player player) {
        UUID uuid = player.getUniqueId();
        if (inventariosSalvos.containsKey(uuid)) return;

        inventariosSalvos.put(uuid, player.getInventory().getContents());
        armadurasSalvas.put(uuid, player.getInventory().getArmorContents());

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
    }

    public void restaurarInventario(Player player) {
        UUID uuid = player.getUniqueId();

        if (inventariosSalvos.containsKey(uuid)) {
            player.getInventory().clear();

            player.getInventory().setContents(inventariosSalvos.get(uuid));
            player.getInventory().setArmorContents(armadurasSalvas.get(uuid));

            inventariosSalvos.remove(uuid);
            armadurasSalvas.remove(uuid);

            player.updateInventory();
        }
    }

    public void sairMina(Player player) {
        Mina mina = minasCache.get(player.getUniqueId());

        if (mina != null) {
            ItemStack picaretaAtual = player.getInventory().getItem(4);
            if (picaretaAtual != null) {
                mina.setPicaretaBase64(dev.gdataa.hyvel.utils.ItemSerializer.toBase64(picaretaAtual));
            }
        }

        restaurarInventario(player);
        player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        player.sendMessage("§aSaíste da mina e o teu inventário foi restaurado!");
    }

    public void prepararInventarioMina(Player player, Mina mina) {
        player.getInventory().clear();

        ItemStack picareta;
        if (mina.getPicaretaBase64() != null && !mina.getPicaretaBase64().isEmpty()) {
            picareta = dev.gdataa.hyvel.utils.ItemSerializer.fromBase64(mina.getPicaretaBase64());
        } else {
            picareta = new ItemStack(Material.DIAMOND_PICKAXE);
            ItemMeta metaPick = picareta.getItemMeta();
            if (metaPick != null) {
                metaPick.setDisplayName(dev.gdataa.hyvel.utils.ColorUtil.formatar("<#eab543>&lPɪᴄᴀʀᴇᴛᴀ ᴅᴇ Pɪʀᴀᴛᴀ"));

                metaPick.addEnchant(Enchantment.EFFICIENCY, 100, true);
                metaPick.setUnbreakable(true);
                metaPick.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

                NamespacedKey key = new NamespacedKey(plugin, "item_mina");
                metaPick.getPersistentDataContainer().set(key, PersistentDataType.STRING, "picareta");

                picareta.setItemMeta(metaPick);

                // Constrói a Lore automaticamente com os níveis padrão via PickaxeUtils
                picareta = dev.gdataa.hyvel.utils.PickaxeUtils.reconstruirLore(picareta, plugin);
            }
        }

        ItemStack sair = new ItemStack(Material.BARRIER);
        ItemMeta metaSair = sair.getItemMeta();
        if (metaSair != null) {
            metaSair.setDisplayName("§cSair da Mina");
            metaSair.setLore(Arrays.asList("§7Clique direito para voltar", "§7para o spawn principal."));

            NamespacedKey key = new NamespacedKey(plugin, "item_mina");
            metaSair.getPersistentDataContainer().set(key, PersistentDataType.STRING, "sair_item");

            sair.setItemMeta(metaSair);
        }

        player.getInventory().setItem(4, picareta);
        player.getInventory().setItem(8, sair);
        player.getInventory().setHeldItemSlot(4);
    }

    public void processarRecompensa(Player player, Mina mina, Material materialQuebrado) {
        if (materialQuebrado == Material.AIR) return;

        ItemStack picareta = player.getInventory().getItem(4);
        if (picareta == null || picareta.getType() == Material.AIR) return;

        // ==========================================
        // 1. SKILLS ATIVAS ÉPICAS (Enxame / Golfinho)
        // ==========================================
        if (Math.random() < 0.001) {
            int lvlEnxame = dev.gdataa.hyvel.utils.PickaxeUtils.getEnchantLevel(picareta, dev.gdataa.hyvel.utils.PickaxeUtils.ENC_ENXAME, plugin);
            int lvlGolfinho = dev.gdataa.hyvel.utils.PickaxeUtils.getEnchantLevel(picareta, dev.gdataa.hyvel.utils.PickaxeUtils.ENC_GOLFINHO, plugin);

            if (lvlEnxame > 0 && Math.random() < 0.5) {
                new dev.gdataa.hyvel.minas.skills.EnxameSkill(plugin, player, mina, lvlEnxame).runTaskTimer(plugin, 0L, 1L);
            } else if (lvlGolfinho > 0) {
                if (!dev.gdataa.hyvel.minas.skills.GolfinhoSkill.bonusAtivo) {
                    new dev.gdataa.hyvel.minas.skills.GolfinhoSkill(player, mina).runTaskTimer(plugin, 0L, 1L);
                }
            }
        }

        // ==========================================
        // 2. DAR XP À PICARETA
        // ==========================================
        dev.gdataa.hyvel.utils.PickaxeUtils.adicionarXP(picareta, 1.0, plugin, player);

        // ==========================================
        // 3. KEY FINDER
        // ==========================================
        int lvlKeys = dev.gdataa.hyvel.utils.PickaxeUtils.getEnchantLevel(picareta, dev.gdataa.hyvel.utils.PickaxeUtils.ENC_KEYS, plugin);
        if (lvlKeys > 0) {
            double chanceChave = 0.0015 + (lvlKeys * 0.0015);
            if (Math.random() * 100 <= chanceChave) {
                player.sendMessage(dev.gdataa.hyvel.utils.ColorUtil.formatar("<#00d2d3>§lSorte! §7Encontraste uma chave nos escombros! (Key Finder)"));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
            }
        }

        // ==========================================
        // 4. ECONOMIA (Dinheiro e Tokens)
        // ==========================================
        double precoFinal = 0.0;
        double multLuckPerms = dev.gdataa.hyvel.utils.HyvelHooks.getMultiplicador(player);

        // -- FORTUNA (Coins) --
        String faixa = "1";
        if (plugin.getConfig().getConfigurationSection("progresso") != null) {
            int maiorNivel = 1;
            for (String key : plugin.getConfig().getConfigurationSection("progresso").getKeys(false)) {
                try {
                    int nivelReq = Integer.parseInt(key);
                    if (mina.getNivel() >= nivelReq && nivelReq >= maiorNivel) {
                        faixa = key;
                        maiorNivel = nivelReq;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        double precoBase = plugin.getConfig().getDouble("progresso." + faixa + ".precos." + materialQuebrado.name(), 0.0);

        if (precoBase > 0) {
            int lvlFortuna = dev.gdataa.hyvel.utils.PickaxeUtils.getEnchantLevel(picareta, dev.gdataa.hyvel.utils.PickaxeUtils.ENC_FORTUNA, plugin);
            double bonusFortuna = 1.0 + (lvlFortuna * 0.0005); // O multiplicador estava "100.0", corrigido!

            precoFinal = (precoBase * bonusFortuna) * multLuckPerms;

            if (dev.gdataa.hyvel.utils.HyvelHooks.getEconomy() != null) {
                dev.gdataa.hyvel.utils.HyvelHooks.getEconomy().depositPlayer(player, precoFinal);
            }
        }

        // -- TOKINATOR (Tokens) --
        int lvlTokinator = dev.gdataa.hyvel.utils.PickaxeUtils.getEnchantLevel(picareta, dev.gdataa.hyvel.utils.PickaxeUtils.ENC_TOKENS, plugin);
        double tokensBase = 1.0;
        double multiplicadorTokinator = 1.0 + (lvlTokinator * 0.01);

        int tokensFinais = (int) Math.ceil(tokensBase * multiplicadorTokinator);
        mina.adicionarTokens(tokensFinais);

        // ==========================================
        // 5. ACTION BAR UNIFICADA (Tudo na mesma linha)
        // ==========================================
        String moneyStr = dev.gdataa.hyvel.utils.NumberUtil.formatar(precoFinal);
        String multStr = String.format("%.1f", multLuckPerms).replace(",", ".");

        String msgActionBar = "<#2ed573>+$" + moneyStr + " &8| <#f1c40f>+" + tokensFinais + " Tokens &8| &7x" + multStr + " Rank";
        player.sendActionBar(dev.gdataa.hyvel.utils.ColorUtil.formatar(msgActionBar));

        // ==========================================
        // 6. PROGRESSO DA MINA E RESET
        // ==========================================
        mina.adicionarBlocoQuebrado();

        if (mina.getPercentagemQuebrada() >= 80.0) {
            resetarMina(mina, player);
        }
    }

    public Map<UUID, Mina> getMinasCache() {
        return minasCache;
    }
}