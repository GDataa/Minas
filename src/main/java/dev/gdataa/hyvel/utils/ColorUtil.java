package dev.gdataa.hyvel.utils;

import net.md_5.bungee.api.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    // Deteta o padrão <#HEX> (ex: <#eab543>)
    private static final Pattern HEX_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");

    public static String formatar(String mensagem) {
        if (mensagem == null) return "";

        Matcher matcher = HEX_PATTERN.matcher(mensagem);
        StringBuffer buffer = new StringBuffer();

        // Substitui todos os <#HEX> pela cor real do Minecraft
        while (matcher.find()) {
            String hex = matcher.group(1);
            ChatColor cor = ChatColor.of("#" + hex);
            matcher.appendReplacement(buffer, cor.toString());
        }
        matcher.appendTail(buffer);

        // AQUI ESTAVA O ERRO! Removemos o .replace("§", "&")
        // Agora o código traduz o '&' para o símbolo '§' que o Minecraft usa para colorir.
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
}