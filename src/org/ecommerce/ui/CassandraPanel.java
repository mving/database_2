package org.ecommerce.ui;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import org.ecommerce.config.AppConfig;
import org.ecommerce.queries.CassandraQueries;
import org.ecommerce.util.Errors;

import javax.net.ssl.SSLContext;
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
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CassandraPanel extends QueryPanel {
    private final JTextField hostField;
    private final JTextField portField;
    private final JTextField datacenterField;
    private final JTextField keyspaceField;
    private final JTextField userField;
    private final JPasswordField passwordField;
    private CqlSession session;
    private final JLabel indicator = new JLabel("● Cassandra");

    public CassandraPanel() {
        JPanel config = baseConfigPanel();
        hostField = addText(config, "Host", AppConfig.env("GRUPO4_CASSANDRA_HOST", "cassandra.us-east-1.amazonaws.com"), 0, 0);
        portField = addText(config, "Puerto", AppConfig.env("GRUPO4_CASSANDRA_PORT", "9142"), 0, 1);
        datacenterField = addText(config, "Datacenter", AppConfig.env("GRUPO4_CASSANDRA_DATACENTER", "us-east-1"), 1, 0);
        keyspaceField = addText(config, "Keyspace", AppConfig.env("GRUPO4_CASSANDRA_KEYSPACE", ""), 1, 1);
        userField = addText(config, "Usuario", AppConfig.env("GRUPO4_CASSANDRA_USER", ""), 2, 0);
        passwordField = addPassword(config, "Contraseña", AppConfig.env("GRUPO4_CASSANDRA_PASSWORD", ""), 2, 1);
        passwordField.setEchoChar('*');
        passwordField.setEnabled(false);

        JCheckBox editPassCheck = new JCheckBox("Editar contraseña");
        editPassCheck.setBackground(Color.WHITE);
        GridBagConstraints cPass = new GridBagConstraints();
        cPass.gridx = 0; cPass.gridy = 3; cPass.gridwidth = 4; cPass.anchor = GridBagConstraints.WEST; cPass.insets = new Insets(5, 6, 5, 6);
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
                "Metricas del producto Adidas",
                "Listar tablas",
                "Consulta libre"
        });
        templates.addActionListener(e -> applyTemplate((String) templates.getSelectedItem()));
        applyTemplate("Metricas del producto Adidas");

        indicator.setForeground(Color.RED);
        JButton connect = new JButton("Conectar");
        connect.addActionListener(e -> connectAsync());
        JButton execute = new JButton("Ejecutar CQL");
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
        if ("Listar tablas".equals(template)) {
            queryArea.setText(CassandraQueries.LISTAR_TABLAS);
        } else if ("Metricas del producto Adidas".equals(template)) {
            queryArea.setText(CassandraQueries.METRICAS_ADIDAS);
        }
    }

    public SwingWorker<Void, Void> connectAsync() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            protected Void doInBackground() {
                close();
                DriverConfigLoader loader = DriverConfigLoader.programmaticBuilder()
                        .withString(DefaultDriverOption.REQUEST_CONSISTENCY, "LOCAL_QUORUM")
                        .build();

                CqlSessionBuilder builder = CqlSession.builder()
                        .withConfigLoader(loader)
                        .addContactPoint(new InetSocketAddress(hostField.getText().trim(), Integer.parseInt(portField.getText().trim())))
                        .withLocalDatacenter(datacenterField.getText().trim())
                        .withSslContext(defaultSslContext())
                        .withAuthCredentials(userField.getText().trim(), password(passwordField));
                String keyspace = keyspaceField.getText().trim();
                if (!keyspace.isEmpty()) {
                    builder.withKeyspace(keyspace);
                }
                session = builder.build();
                return null;
            }

            protected void done() {
                try {
                    get();
                    setStatusOk("");
                    indicator.setForeground(new Color(0, 153, 0));
                } catch (Exception ex) {
                    setStatusError("Error Cassandra: " + Errors.rootMessage(ex));
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
                ensureConnected();
                ResultSet rs = session.execute(queryArea.getText());
                List<Map<String, Object>> rows = new ArrayList<>();
                for (Row row : rs) {
                    Map<String, Object> values = new LinkedHashMap<>();
                    for (ColumnDefinition column : row.getColumnDefinitions()) {
                        String name = column.getName().asInternal();
                        values.put(name, row.getObject(name));
                    }
                    rows.add(values);
                }
                return rows;
            }

            protected void done() {
                try {
                    List<Map<String, Object>> rows = get();
                    setRows(rows);
                    setStatusOk("Cassandra: " + rows.size() + " fila(s)");
                } catch (Exception ex) {
                    setStatusError("Error CQL: " + Errors.rootMessage(ex));
                }
            }
        }.execute();
    }

    private void ensureConnected() {
        if (session == null || session.isClosed()) {
            throw new IllegalStateException("Primero conectate a Cassandra");
        }
    }

    public List<Map<String, Object>> executeForReport(String cql) {
        ensureConnected();
        try {
            ResultSet rs = session.execute(cql);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Row row : rs) {
                Map<String, Object> values = new LinkedHashMap<>();
                for (ColumnDefinition column : row.getColumnDefinitions()) {
                    String name = column.getName().asInternal();
                    values.put(name, row.getObject(name));
                }
                rows.add(values);
            }
            return rows;
        } catch (Exception ex) {
            throw new RuntimeException("Cassandra - " + Errors.rootMessage(ex));
        }
    }

    private void close() {
        if (session != null) {
            session.close();
        }
    }

    private SSLContext defaultSslContext() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, null, null);
            return context;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo inicializar TLS para Cassandra", ex);
        }
    }
}
