package org.ecommerce.ui;

import org.ecommerce.config.AppConfig;
import org.ecommerce.queries.Neo4jQueries;
import org.ecommerce.util.Errors;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Query;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Value;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Neo4jPanel extends QueryPanel {
    private final JTextField uriField;
    private final JTextField databaseField;
    private final JTextField userField;
    private final JPasswordField passwordField;
    private Driver driver;
    private final JLabel indicator = new JLabel("● Neo4j");

    public Neo4jPanel() {
        JPanel config = baseConfigPanel();
        uriField = addText(config, "URI", AppConfig.env("GRUPO4_NEO4J_URI", "neo4j+s://323c8a8e.databases.neo4j.io"), 0, 0);
        databaseField = addText(config, "Base", AppConfig.env("GRUPO4_NEO4J_DATABASE", ""), 0, 1);
        userField = addText(config, "Usuario", AppConfig.env("GRUPO4_NEO4J_USER", ""), 1, 0);
        passwordField = addPassword(config, "Contraseña", AppConfig.env("GRUPO4_NEO4J_PASSWORD", ""), 1, 1);
        passwordField.setEchoChar('*');
        passwordField.setEnabled(false);

        JCheckBox editPassCheck = new JCheckBox("Editar contraseña");
        editPassCheck.setBackground(Color.WHITE);
        GridBagConstraints cPass = new GridBagConstraints();
        cPass.gridx = 0; cPass.gridy = 2; cPass.gridwidth = 4; cPass.anchor = GridBagConstraints.WEST; cPass.insets = new Insets(5, 6, 5, 6);
        config.add(editPassCheck, cPass);

        editPassCheck.addActionListener(e -> {
            if (editPassCheck.isSelected()) {
                int result = JOptionPane.showConfirmDialog(this,
                        "¿Estás seguro de que quieres editar tu contraseña?\nEditar esta contraseña revelará tus credenciales.",
                        "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    passwordField.setEnabled(true);
                } else {
                    editPassCheck.setSelected(false);
                }
            } else {
                passwordField.setEnabled(false);
                passwordField.setEchoChar('*');
            }
        });

        passwordField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (passwordField.isEnabled()) {
                    passwordField.setEchoChar((char) 0);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                passwordField.setEchoChar('*');
            }
        });

        JComboBox<String> templates = new JComboBox<>(new String[]{
                "Centralidad Adidas",
                "Listar productos",
                "Cypher libre"
        });
        templates.addActionListener(e -> applyTemplate((String) templates.getSelectedItem()));
        applyTemplate("Centralidad Adidas");

        indicator.setForeground(Color.RED);
        JButton connect = new JButton("Conectar");
        connect.addActionListener(e -> connectAsync());
        JButton execute = new JButton("Ejecutar Cypher");
        execute.addActionListener(e -> executeAsync());

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setBackground(Color.WHITE);
        top.add(config, BorderLayout.NORTH);
        top.add(templateBar(templates), BorderLayout.SOUTH);

        add(queryAndResults(top), BorderLayout.CENTER);
        add(actionBar(connect, execute, indicator), BorderLayout.SOUTH);
    }

    private JPanel templateBar(JComboBox<String> templates) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.setBackground(Color.WHITE);
        panel.add(new JLabel("Plantilla"));
        panel.add(templates);
        return panel;
    }

    private void applyTemplate(String template) {
        if ("Listar productos".equals(template)) {
            queryArea.setText(Neo4jQueries.LISTAR_PRODUCTOS);
        } else if ("Centralidad Adidas".equals(template)) {
            queryArea.setText(Neo4jQueries.CENTRALIDAD_ADIDAS);
        }
    }

    public SwingWorker<Void, Void> connectAsync() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            protected Void doInBackground() {
                close();
                driver = GraphDatabase.driver(uriField.getText().trim(), AuthTokens.basic(userField.getText().trim(), password(passwordField)));
                driver.verifyConnectivity();
                return null;
            }

            protected void done() {
                try {
                    get();
                    setStatusOk("");
                    indicator.setForeground(new Color(0, 153, 0));
                } catch (Exception ex) {
                    setStatusError("Error Neo4j: " + Errors.rootMessage(ex));
                    indicator.setForeground(Color.RED);
                }
            }
        };
        worker.execute();
        return worker;
    }

    public void executeAsync() {
        new SwingWorker<List<Map<String, Object>>, Void>() {
            protected List<Map<String, Object>> doInBackground() {
                return runCypher(queryArea.getText(), Map.of());
            }

            protected void done() {
                try {
                    List<Map<String, Object>> rows = get();
                    setRows(rows);
                    setStatusOk("Neo4j: " + rows.size() + " registro(s)");
                } catch (Exception ex) {
                    setStatusError("Error Cypher: " + Errors.rootMessage(ex));
                }
            }
        }.execute();
    }

    public List<Map<String, Object>> runCypher(String cypher, Map<String, Object> parameters) {
        ensureConnected();
        try {
            String databaseName = databaseField.getText().trim();
            SessionConfig config = databaseName.isEmpty()
                    ? SessionConfig.defaultConfig()
                    : SessionConfig.builder().withDatabase(databaseName).build();
            try (Session session = driver.session(config)) {
                Result result = session.run(new Query(cypher, parameters));
                List<Map<String, Object>> rows = new ArrayList<>();
                while (result.hasNext()) {
                    org.neo4j.driver.Record record = result.next();
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (String key : record.keys()) {
                        Value value = record.get(key);
                        row.put(key, value.isNull() ? null : value.asObject());
                    }
                    rows.add(row);
                }
                return rows;
            }
        } catch (Exception ex) {
            throw new RuntimeException("Neo4j - " + Errors.rootMessage(ex));
        }
    }

    private void ensureConnected() {
        if (driver == null) {
            throw new IllegalStateException("Primero conectate a Neo4j");
        }
    }

    private void close() {
        if (driver != null) {
            driver.close();
        }
    }
}
