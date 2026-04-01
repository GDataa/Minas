package dev.gdataa.hyvel.utils;

import java.text.DecimalFormat;

public class NumberUtil {

    // A lista de sufixos desde Mil (K) até Octilião (Oc)
    private static final String[] SUFIXOS = new String[]{
            "", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc"
    };

    // Formatador para deixar sempre com máximo de 2 casas decimais (ex: 1.52K)
    private static final DecimalFormat FORMATADOR = new DecimalFormat("#,##0.00");

    public static String formatar(double valor) {
        // Se for menos de mil, apenas mostra o dinheiro normal (ex: 500.50)
        if (valor < 1000.0) {
            return FORMATADOR.format(valor);
        }

        int index = 0;
        // Divide por mil até o número ficar pequeno o suficiente ou até acabarem os sufixos
        while (valor >= 1000.0 && index < SUFIXOS.length - 1) {
            valor /= 1000.0;
            index++;
        }

        // Devolve o número formatado juntamente com a letra correspondente
        return FORMATADOR.format(valor) + SUFIXOS[index];
    }
}