package dev.gdataa.hyvel.minas.skills;

import dev.gdataa.hyvel.MinasMain;
import dev.gdataa.hyvel.minas.model.Mina;
import dev.gdataa.hyvel.utils.ColorUtil;
import dev.gdataa.hyvel.utils.NumberUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.trait.SkinTrait;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class PirataSkill extends BukkitRunnable {

    private final MinasMain plugin;
    private final Player player;
    private final Mina mina;

    private class NpcPirata {
        NPC npc;
        Block alvo;
        boolean escavandoTunel = false;
        int ticksCavando = 0;
        int falhasAoChegar = 0;

        public NpcPirata(NPC npc) {
            this.npc = npc;
        }
    }

    private final List<NpcPirata> bandoDePiratas = new ArrayList<>();
    private static final Set<Block> blocosReservados = new HashSet<>();

    private int ticksDeVida = 0;
    private final int MAX_TICKS = 300; // 15 segundos
    private final Random random = new Random();

    private double dinheiroGanho = 0.0;
    private int blocosQuebradosTotal = 0;

    private final int minX, maxX, minY, maxY, minZ, maxZ;

    public PirataSkill(MinasMain plugin, Player player, Mina mina, int nivelSkill) {
        this.plugin = plugin;
        this.player = player;
        this.mina = mina;

        this.minX = Math.min(plugin.getConfig().getInt("regiao.minX"), plugin.getConfig().getInt("regiao.maxX"));
        this.maxX = Math.max(plugin.getConfig().getInt("regiao.minX"), plugin.getConfig().getInt("regiao.maxX"));
        this.minY = Math.min(plugin.getConfig().getInt("regiao.minY"), plugin.getConfig().getInt("regiao.maxY"));
        this.maxY = Math.max(plugin.getConfig().getInt("regiao.minY"), plugin.getConfig().getInt("regiao.maxY"));
        this.minZ = Math.min(plugin.getConfig().getInt("regiao.minZ"), plugin.getConfig().getInt("regiao.maxZ"));
        this.maxZ = Math.max(plugin.getConfig().getInt("regiao.minZ"), plugin.getConfig().getInt("regiao.maxZ"));

        int maxPiratas = Math.min(5, 1 + (nivelSkill / 20));

        String val = "ewogICJ0aW1lc3RhbXAiIDogMTc3NTUxODMzOTIwNCwKICAicHJvZmlsZUlkIiA6ICI2MzA1MDhmN2YyODg0N2E1OTA0ZTk2Mjc3NmFjM2ExMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJkeWxhbmNhdHMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjk2MDdkZTQ0Y2IxMmU4OTYxOTZmZTUxZWI4ODI0ZDU0NmU0MDhmNjQyZjc2MTZiMTU5MzIwOGUwNGRhYmUwMSIKICAgIH0KICB9Cn0=";
        String sig = "VjfYTFdnQdKXelbbj/gADISq4XKKqE4LTiGh/1yer7hGfD7wDPm9LusLf48G8CqHGRr3WA4ru4Sf6vrIYf67eHHtHXpo6IdKNQjEfIrps6UnbPJW35mRTuvOv3eyVNRU9FOItT3en5qMWWs500S8mIKctK61nHq4BwgxmFRXXGo/twKNPFUzgtbC8BiLGM7AxIem86WcnAhUiprJKRbaoQpkNmEI0Y4o+FriprzVYMRb0sR8+uzfNha1E4FrO3owdCoD7MY85V+LijpxswM6R2bTZ5dXxvx7D+MOOtqSo0iXwFUJG1JKHXcpXcrvyBTBterhwkiW3xOXVfIq1acp/S5RpC+C3fJNreq4oWlBBsMRn6wjMZTrt3552EhtuIz7z+T5YwA7LmJeICEGo5oTqnKfL3O1h/s/DnE46QfCOfjeri5CNu6eE2kJEJe4ja06kuNLgwA+82z6EKeZYxD6drCR2AgNdhfZnGGpAxy8NxUMezoN/ODexiD2DSXwEl8c8JvsDWIgx23XbRgF2XXOsSSxk7P9TU/1qusVOsmK3Cq/26QyoInNwfUoYnyueY613zV5hj6ePkaczdJignv2TcYGB5jSy5dCeF8ZWzzofsBcfX/XPh5guoAMcEOaaUHw3mCMedDSBvyatOG6iH8tMQzLCUwypDTAmFL7NAPEKjE=";

        Location spawnLoc = new Location(mina.getBukkitWorld(), 0.591, 63, -87.314, 0f, 0f);

        for (int i = 0; i < maxPiratas; i++) {
            NPC npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "§cPirata #" + (i+1));
            npc.setProtected(true);

            SkinTrait skin = npc.getOrAddTrait(SkinTrait.class);
            skin.setSkinPersistent("Pirata_" + i, sig, val);

            Equipment equip = npc.getOrAddTrait(Equipment.class);
            equip.set(Equipment.EquipmentSlot.HAND, new ItemStack(Material.GOLDEN_PICKAXE));

            npc.getNavigator().getDefaultParameters().speedModifier(1.8f); // Ainda mais rápidos a correr
            npc.spawn(spawnLoc);

            bandoDePiratas.add(new NpcPirata(npc));
        }

        player.sendMessage(ColorUtil.formatar("<#eab543>§lYARRR! §7Os piratas começaram a perfurar a mina!"));
    }

    @Override
    public void run() {
        ticksDeVida++;

        // FIM DA SKILL
        if (ticksDeVida > MAX_TICKS) {
            for (NpcPirata p : bandoDePiratas) {
                if (p.npc.isSpawned()) {
                    p.npc.getEntity().getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, p.npc.getEntity().getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.05);
                }
                p.npc.destroy();
            }
            blocosReservados.clear();

            player.sendMessage("");
            player.sendMessage(ColorUtil.formatar("<#eab543>§lSAQUE FINALIZADO!"));
            player.sendMessage(ColorUtil.formatar("§7A tripulação escavou §f" + blocosQuebradosTotal + " §7blocos."));
            player.sendMessage(ColorUtil.formatar("§7Ganhos: <#2ed573>+$" + NumberUtil.formatar(dinheiroGanho)));
            player.sendMessage("");
            this.cancel();
            return;
        }

        for (NpcPirata pirata : bandoDePiratas) {
            if (!pirata.npc.isSpawned()) continue;
            Player npcEntity = (Player) pirata.npc.getEntity();

            // 1. SE NÃO TEM ALVO E NÃO ESTÁ A CAVAR, PROCURAR UM NOVO PONTO NA SUPERFÍCIE
            if (pirata.alvo == null && !pirata.escavandoTunel) {
                pirata.alvo = buscarSuperficieExclusiva();
                if (pirata.alvo != null) {
                    blocosReservados.add(pirata.alvo);
                    // Manda-os correr para o topo do bloco
                    pirata.npc.getNavigator().setTarget(pirata.alvo.getLocation().add(0.5, 1, 0.5));
                }
                continue;
            }

            // 2. CORRENDO PARA O ALVO
            if (!pirata.escavandoTunel && pirata.alvo != null) {
                Location alvoLoc = pirata.alvo.getLocation().add(0.5, 1, 0.5);
                double dist = npcEntity.getLocation().distance(alvoLoc);

                if (dist <= 2.5) {
                    // Chegou! Ativa o modo Túnel.
                    pirata.npc.getNavigator().cancelNavigation();
                    pirata.escavandoTunel = true;
                    pirata.ticksCavando = 0;
                } else {
                    pirata.falhasAoChegar++;
                    // ANTI-PRESO: Demorou mais de 1.5s? Teleporta direto para o bloco com um fuminho ninja!
                    if (pirata.falhasAoChegar > 30) {
                        npcEntity.getWorld().spawnParticle(Particle.CLOUD, npcEntity.getLocation(), 10, 0.2, 0.5, 0.2, 0.05);
                        pirata.npc.teleport(alvoLoc, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
                        pirata.escavandoTunel = true;
                        pirata.ticksCavando = 0;
                        pirata.falhasAoChegar = 0;
                    }
                }
            }

            // 3. MODO ESCAVADORA DE TÚNEIS (CAVANDO PARA BAIXO)
            else if (pirata.escavandoTunel) {
                pirata.ticksCavando++;

                // Animação constante: Balança a picareta loucamente e olha para baixo
                if (pirata.ticksCavando % 2 == 0) {
                    npcEntity.swingMainHand();
                    pirata.npc.faceLocation(npcEntity.getLocation().add(0, -2, 0)); // Olha para baixo
                }

                // A cada 5 ticks (0.25 segundos), destrói a camada onde está os pés dele e a de baixo
                if (pirata.ticksCavando % 5 == 0) {
                    Location centro = npcEntity.getLocation();
                    boolean quebrouAlgoNestaCamada = false;

                    // Escava um buraco 3x3 nas duas camadas por baixo dele
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            for (int y = 0; y >= -2; y--) {
                                Block b = centro.clone().add(x, y, z).getBlock();

                                if (b.getX() >= minX && b.getX() <= maxX && b.getY() >= minY && b.getY() <= maxY && b.getZ() >= minZ && b.getZ() <= maxZ) {
                                    Material mat = b.getType();
                                    if (mat != Material.AIR && mat != Material.BEDROCK) {
                                        b.setType(Material.AIR);
                                        b.getWorld().spawnParticle(Particle.BLOCK, b.getLocation().add(0.5, 0.5, 0.5), 4, 0.2, 0.2, 0.2, mat.createBlockData());
                                        calcularRecompensa(mat);
                                        blocosQuebradosTotal++;
                                        quebrouAlgoNestaCamada = true;
                                    }
                                }
                            }
                        }
                    }

                    if (quebrouAlgoNestaCamada) {
                        npcEntity.getWorld().playSound(centro, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.4f, 1.5f);
                    } else {
                        // Se não quebrou nada, ou chegou ao fundo da mina (MinY) ou caiu no vazio.
                        // Cancela o túnel e procura novo bloco na superfície!
                        npcEntity.getWorld().spawnParticle(Particle.CLOUD, npcEntity.getLocation(), 15, 0.5, 1, 0.5, 0.05);
                        if (pirata.alvo != null) blocosReservados.remove(pirata.alvo);
                        pirata.alvo = null;
                        pirata.escavandoTunel = false;
                        pirata.falhasAoChegar = 0;
                    }
                }
            }
        }
    }

    private Block buscarSuperficieExclusiva() {
        for (int i = 0; i < 30; i++) {
            int rx = minX + random.nextInt((maxX - minX) + 1);
            int rz = minZ + random.nextInt((maxZ - minZ) + 1);

            // Escaneia de cima para baixo até achar o primeiro bloco da superfície
            for (int ry = maxY; ry >= minY; ry--) {
                Block b = mina.getBukkitWorld().getBlockAt(rx, ry, rz);
                if (b.getType() != Material.AIR && b.getType() != Material.BEDROCK) {
                    // Verifica se não tem um pirata já a ir para aí
                    if (!blocosReservados.contains(b)) {
                        return b;
                    }
                }
            }
        }
        return null;
    }

    private void calcularRecompensa(Material mat) {
        plugin.getMinaManager().processarRecompensa(player, mina, mat);

        String faixa = "1";
        if (plugin.getConfig().getConfigurationSection("progresso") != null) {
            for (String key : plugin.getConfig().getConfigurationSection("progresso").getKeys(false)) {
                if (mina.getNivel() >= Integer.parseInt(key)) faixa = key;
            }
        }
        double preco = plugin.getConfig().getDouble("progresso." + faixa + ".precos." + mat.name(), 0.0);
        dinheiroGanho += (preco * dev.gdataa.hyvel.utils.HyvelHooks.getMultiplicador(player));
    }
}