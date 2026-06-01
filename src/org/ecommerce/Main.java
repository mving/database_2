package org.ecommerce;

import org.ecommerce.ui.EcommerceDatabaseUi;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        // Silenciar logs de diagnóstico por defecto de los drivers
        System.setProperty("org.slf4j.simpleLogger.log.org.mongodb.driver", "warn");
        System.setProperty("org.slf4j.simpleLogger.log.com.datastax.oss", "warn");
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new EcommerceDatabaseUi().setVisible(true);
        });
    }
}
