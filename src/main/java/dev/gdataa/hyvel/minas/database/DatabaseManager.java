package dev.gdataa.hyvel.minas.database;

import dev.gdataa.hyvel.MinasMain;
import dev.gdataa.hyvel.minas.model.Mina;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class DatabaseManager {

    private final MinasMain plugin;
    private Connection connection;
    private final String tipo;

    public DatabaseManager(MinasMain plugin) {
        this.plugin = plugin;
        this.tipo = plugin.getConfig().getString("database.tipo", "sqlite").toLowerCase();
        conectar();
        criarTabela();
    }

    private void conectar() {
        try {
            if (tipo.equals("mysql")) {
                String host = plugin.getConfig().getString("database.host");
                int porta = plugin.getConfig().getInt("database.porta");
                String banco = plugin.getConfig().getString("database.banco");
                String usuario = plugin.getConfig().getString("database.usuario");
                String senha = plugin.getConfig().getString("database.senha");

                String url = "jdbc:mysql://" + host + ":" + porta + "/" + banco + "?autoReconnect=true&useSSL=false";
                connection = DriverManager.getConnection(url, usuario, senha);
                plugin.getLogger().info("Conectado ao MySQL com sucesso!");

            } else {
                File file = new File(plugin.getDataFolder(), "database.db");
                String url = "jdbc:sqlite:" + file.getAbsolutePath();
                connection = DriverManager.getConnection(url);
                plugin.getLogger().info("Conectado ao SQLite com sucesso!");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro ao conectar na base de dados!");
            e.printStackTrace();
        }
    }

    private void criarTabela() {
        if (connection == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS hyvel_minas (" +
                            "uuid VARCHAR(36) PRIMARY KEY, " +
                            "nivel INT NOT NULL, " +
                            "prestigio INT NOT NULL, " +
                            "blocos_quebrados INT NOT NULL, " +
                            "tokens INT NOT NULL, " +
                            "picareta_data TEXT" +
                            ");"
            )) {
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void carregarJogador(UUID uuid) {
        if (connection == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM hyvel_minas WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();

                Mina mina = plugin.getMinaManager().getMinasCache().computeIfAbsent(uuid, k -> new Mina(uuid));

                if (rs.next()) {
                    mina.setNivel(rs.getInt("nivel"));
                    mina.setPrestigio(rs.getInt("prestigio"));
                    mina.setBlocosQuebradosTotal(rs.getInt("blocos_quebrados"));
                    mina.setTokens(rs.getInt("tokens"));
                    mina.setPicaretaBase64(rs.getString("picareta_data"));
                } else {
                    salvarJogadorSincrono(mina);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void salvarJogadorSincrono(Mina mina) {
        if (connection == null) return;

        String query = tipo.equals("mysql")
                ? "INSERT INTO hyvel_minas (uuid, nivel, prestigio, blocos_quebrados, tokens, picareta_data) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE nivel = ?, prestigio = ?, blocos_quebrados = ?, tokens = ?, picareta_data = ?;"
                : "INSERT OR REPLACE INTO hyvel_minas (uuid, nivel, prestigio, blocos_quebrados, tokens, picareta_data) VALUES (?, ?, ?, ?, ?, ?);";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, mina.getDono().toString());
            ps.setInt(2, mina.getNivel());
            ps.setInt(3, mina.getPrestigio());
            ps.setInt(4, mina.getBlocosQuebradosTotal()); // Aqui ele salva o total acumulado
            ps.setInt(5, mina.getTokens());
            ps.setString(6, mina.getPicaretaBase64());

            if (tipo.equals("mysql")) {
                ps.setInt(7, mina.getNivel());
                ps.setInt(8, mina.getPrestigio());
                ps.setInt(9, mina.getBlocosQuebradosTotal());
                ps.setInt(10, mina.getTokens());
                ps.setString(11, mina.getPicaretaBase64());
            }

            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("ERRO ao salvar dados de " + mina.getDono());
            e.printStackTrace();
        }
    }

    public void salvarJogador(Mina mina) {
        if (connection == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> salvarJogadorSincrono(mina));
    }

    public void fecharConexao() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}