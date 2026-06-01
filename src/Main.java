import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Query;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Value;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.net.ssl.SSLContext;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new EcommerceDatabaseUi().setVisible(true);
        });
    }
}

class EcommerceDatabaseUi extends JFrame {
    private final CassandraPanel cassandraPanel = new CassandraPanel();
    private final MongoPanel mongoPanel = new MongoPanel();
    private final Neo4jPanel neo4jPanel = new Neo4jPanel();

    EcommerceDatabaseUi() {
        setTitle("E-Commerce NoSQL - Consultas TP2");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1060, 720));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 18, 18));
        root.setBackground(new Color(245, 247, 250));

        JLabel title = new JLabel("Consultas de datos - Cassandra, MongoDB y Neo4j");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        root.add(title, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Reporte TP", new ReportPanel(cassandraPanel, mongoPanel, neo4jPanel));
        tabs.addTab("Cassandra", cassandraPanel);
        tabs.addTab("MongoDB", mongoPanel);
        tabs.addTab("Neo4j", neo4jPanel);
        root.add(tabs, BorderLayout.CENTER);

        setContentPane(root);
        pack();
    }
}

class AppConfig {
    private AppConfig() {
    }

    static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}

abstract class QueryPanel extends JPanel {
    protected final JLabel status = new JLabel("Sin conectar");
    protected final JTextArea queryArea = new JTextArea(8, 80);
    protected final JTable resultTable = new JTable(new DefaultTableModel());

    QueryPanel() {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(Color.WHITE);

        queryArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    }

    protected JPanel baseConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Conexion"));
        return panel;
    }

    protected JTextField addText(JPanel panel, String label, String value, int row, int col) {
        JTextField field = new JTextField(value);
        addField(panel, label, field, row, col);
        return field;
    }

    protected JPasswordField addPassword(JPanel panel, String label, String value, int row, int col) {
        JPasswordField field = new JPasswordField(value);
        addField(panel, label, field, row, col);
        return field;
    }

    private void addField(JPanel panel, String label, JTextField field, int row, int col) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 6, 5, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridy = row;
        c.gridx = col * 2;
        c.weightx = 0;
        JLabel jLabel = new JLabel(label);
        jLabel.setFont(jLabel.getFont().deriveFont(Font.BOLD));
        panel.add(jLabel, c);

        c.gridx = col * 2 + 1;
        c.weightx = 1;
        field.setPreferredSize(new Dimension(210, 30));
        panel.add(field, c);
    }

    protected JSplitPane queryAndResults(JPanel topPanel) {
        JPanel queryPanel = new JPanel(new BorderLayout(0, 8));
        queryPanel.setBackground(Color.WHITE);
        queryPanel.add(topPanel, BorderLayout.NORTH);
        queryPanel.add(new JScrollPane(queryArea), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, queryPanel, new JScrollPane(resultTable));
        split.setResizeWeight(0.44);
        split.setBorder(BorderFactory.createEmptyBorder());
        return split;
    }

    protected JPanel actionBar(JButton executeButton) {
        JPanel actions = new JPanel(new BorderLayout());
        actions.setBackground(Color.WHITE);
        status.setForeground(new Color(89, 96, 105));
        actions.add(status, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setBackground(Color.WHITE);
        buttons.add(executeButton);
        actions.add(buttons, BorderLayout.EAST);
        return actions;
    }

    protected void setRows(List<Map<String, Object>> rows) {
        DefaultTableModel model = new DefaultTableModel();
        if (rows.isEmpty()) {
            model.addColumn("Resultado");
            model.addRow(new Object[]{"Sin resultados"});
            resultTable.setModel(model);
            return;
        }

        List<String> columns = new ArrayList<>(rows.get(0).keySet());
        for (String column : columns) {
            model.addColumn(column);
        }
        for (Map<String, Object> row : rows) {
            Object[] values = new Object[columns.size()];
            for (int i = 0; i < columns.size(); i++) {
                values[i] = row.get(columns.get(i));
            }
            model.addRow(values);
        }
        resultTable.setModel(model);
    }

    protected void setStatusOk(String text) {
        status.setText(text);
        status.setForeground(new Color(28, 111, 73));
    }

    protected void setStatusError(String text) {
        status.setText(text);
        status.setForeground(new Color(173, 54, 49));
    }

    protected String password(JPasswordField field) {
        return new String(field.getPassword());
    }
}

class CassandraPanel extends QueryPanel {
    private final JTextField hostField;
    private final JTextField portField;
    private final JTextField datacenterField;
    private final JTextField keyspaceField;
    private final JTextField userField;
    private final JPasswordField passwordField;
    private CqlSession session;

    CassandraPanel() {
        JPanel config = baseConfigPanel();
        hostField = addText(config, "Host", AppConfig.env("CASSANDRA_HOST", "cassandra.us-east-1.amazonaws.com"), 0, 0);
        portField = addText(config, "Puerto", AppConfig.env("CASSANDRA_PORT", "9142"), 0, 1);
        datacenterField = addText(config, "Datacenter", AppConfig.env("CASSANDRA_DATACENTER", "us-east-1"), 1, 0);
        keyspaceField = addText(config, "Keyspace", AppConfig.env("CASSANDRA_KEYSPACE", ""), 1, 1);
        userField = addText(config, "Usuario", AppConfig.env("CASSANDRA_USER", ""), 2, 0);
        passwordField = addPassword(config, "Contrasena", AppConfig.env("CASSANDRA_PASSWORD", ""), 2, 1);

        JComboBox<String> templates = new JComboBox<>(new String[]{
                "Metricas del producto Adidas",
                "Listar tablas",
                "Consulta libre"
        });
        templates.addActionListener(e -> applyTemplate((String) templates.getSelectedItem()));
        applyTemplate("Metricas del producto Adidas");

        JButton connect = new JButton("Conectar");
        connect.addActionListener(e -> connectAsync());
        JButton execute = new JButton("Ejecutar CQL");
        execute.addActionListener(e -> executeAsync());

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setBackground(Color.WHITE);
        top.add(config, BorderLayout.NORTH);
        top.add(templateBar(templates, connect), BorderLayout.SOUTH);

        add(queryAndResults(top), BorderLayout.CENTER);
        add(actionBar(execute), BorderLayout.SOUTH);
    }

    private JPanel templateBar(JComboBox<String> templates, JButton connect) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.setBackground(Color.WHITE);
        panel.add(new JLabel("Plantilla"));
        panel.add(templates);
        panel.add(connect);
        return panel;
    }

    private void applyTemplate(String template) {
        if ("Listar tablas".equals(template)) {
            queryArea.setText("SELECT keyspace_name, table_name FROM system_schema.tables WHERE keyspace_name = 'ecommerce';");
        } else if ("Metricas del producto Adidas".equals(template)) {
            queryArea.setText("""
                    SELECT producto_id, fecha, vistas, conversiones
                    FROM ecommerce.metricas_diarias
                    WHERE producto_id = 69d2e2a8-c083-b3f0-1236-f4b000000000 AND fecha = '2026-05-30';
                    """);
        }
    }

    void connectAsync() {
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() {
                close();
                CqlSessionBuilder builder = CqlSession.builder()
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
                    setStatusOk("Cassandra conectada");
                } catch (Exception ex) {
                    setStatusError("Error Cassandra: " + Errors.rootMessage(ex));
                }
            }
        }.execute();
    }

    void executeAsync() {
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

    List<Map<String, Object>> executeForReport(String cql) {
        ensureConnected();
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

class MongoPanel extends QueryPanel {
    private final JTextField uriField;
    private final JTextField databaseField;
    private final JTextField collectionField;
    private MongoClient client;
    private MongoDatabase database;

    MongoPanel() {
        JPanel config = baseConfigPanel();
        uriField = addText(config, "URI", AppConfig.env("MONGO_URI", ""), 0, 0);
        databaseField = addText(config, "Base", AppConfig.env("MONGO_DATABASE", "ecommerce"), 0, 1);
        collectionField = addText(config, "Coleccion", AppConfig.env("MONGO_COLLECTION", "productos"), 1, 0);

        JComboBox<String> templates = new JComboBox<>(new String[]{
                "Ficha producto Adidas",
                "Primeros productos",
                "Filtro libre"
        });
        templates.addActionListener(e -> applyTemplate((String) templates.getSelectedItem()));
        applyTemplate("Ficha producto Adidas");

        JButton connect = new JButton("Conectar");
        connect.addActionListener(e -> connectAsync());
        JButton execute = new JButton("Ejecutar find");
        execute.addActionListener(e -> executeAsync());

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setBackground(Color.WHITE);
        top.add(config, BorderLayout.NORTH);
        top.add(templateBar(templates, connect), BorderLayout.SOUTH);

        add(queryAndResults(top), BorderLayout.CENTER);
        add(actionBar(execute), BorderLayout.SOUTH);
    }

    private JPanel templateBar(JComboBox<String> templates, JButton connect) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.setBackground(Color.WHITE);
        panel.add(new JLabel("Plantilla"));
        panel.add(templates);
        panel.add(connect);
        return panel;
    }

    private void applyTemplate(String template) {
        if ("Primeros productos".equals(template)) {
            queryArea.setText("{}");
        } else if ("Ficha producto Adidas".equals(template)) {
            queryArea.setText("{ \"_id\": ObjectId(\"69d2e2a8c083b3f01236f4b0\") }");
        }
    }

    void connectAsync() {
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() {
                close();
                client = MongoClients.create(uriField.getText().trim());
                database = client.getDatabase(databaseField.getText().trim());
                database.runCommand(new Document("ping", 1));
                return null;
            }

            protected void done() {
                try {
                    get();
                    setStatusOk("MongoDB conectada");
                } catch (Exception ex) {
                    setStatusError("Error MongoDB: " + Errors.rootMessage(ex));
                }
            }
        }.execute();
    }

    void executeAsync() {
        new SwingWorker<List<Map<String, Object>>, Void>() {
            protected List<Map<String, Object>> doInBackground() {
                return findDocuments(collectionField.getText().trim(), queryArea.getText(), 100);
            }

            protected void done() {
                try {
                    List<Map<String, Object>> rows = get();
                    setRows(rows);
                    setStatusOk("MongoDB: " + rows.size() + " documento(s)");
                } catch (Exception ex) {
                    setStatusError("Error MongoDB: " + Errors.rootMessage(ex));
                }
            }
        }.execute();
    }

    List<Map<String, Object>> findDocuments(String collectionName, String filterText, int limit) {
        ensureConnected();
        MongoCollection<Document> collection = database.getCollection(collectionName);
        Document filter = Document.parse(normalizeMongoFilter(filterText));
        FindIterable<Document> docs = collection.find(filter).limit(limit);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Document doc : docs) {
            rows.add(flattenDocument(doc));
        }
        return rows;
    }

    private Map<String, Object> flattenDocument(Document doc) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : doc.entrySet()) {
            row.put(entry.getKey(), entry.getValue());
        }
        return row;
    }

    private String normalizeMongoFilter(String text) {
        Pattern pattern = Pattern.compile("ObjectId\\(\"([0-9a-fA-F]{24})\"\\)");
        Matcher matcher = pattern.matcher(text.trim());
        StringBuilder normalized = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(normalized, Matcher.quoteReplacement("{\"$oid\":\"" + matcher.group(1) + "\"}"));
        }
        matcher.appendTail(normalized);
        return normalized.toString();
    }

    private void ensureConnected() {
        if (database == null) {
            throw new IllegalStateException("Primero conectate a MongoDB");
        }
    }

    private void close() {
        if (client != null) {
            client.close();
        }
    }
}

class Neo4jPanel extends QueryPanel {
    private final JTextField uriField;
    private final JTextField databaseField;
    private final JTextField userField;
    private final JPasswordField passwordField;
    private Driver driver;

    Neo4jPanel() {
        JPanel config = baseConfigPanel();
        uriField = addText(config, "URI", AppConfig.env("NEO4J_URI", "neo4j+s://323c8a8e.databases.neo4j.io"), 0, 0);
        databaseField = addText(config, "Base", AppConfig.env("NEO4J_DATABASE", ""), 0, 1);
        userField = addText(config, "Usuario", AppConfig.env("NEO4J_USER", ""), 1, 0);
        passwordField = addPassword(config, "Contrasena", AppConfig.env("NEO4J_PASSWORD", ""), 1, 1);

        JComboBox<String> templates = new JComboBox<>(new String[]{
                "Centralidad Adidas",
                "Listar productos",
                "Cypher libre"
        });
        templates.addActionListener(e -> applyTemplate((String) templates.getSelectedItem()));
        applyTemplate("Centralidad Adidas");

        JButton connect = new JButton("Conectar");
        connect.addActionListener(e -> connectAsync());
        JButton execute = new JButton("Ejecutar Cypher");
        execute.addActionListener(e -> executeAsync());

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setBackground(Color.WHITE);
        top.add(config, BorderLayout.NORTH);
        top.add(templateBar(templates, connect), BorderLayout.SOUTH);

        add(queryAndResults(top), BorderLayout.CENTER);
        add(actionBar(execute), BorderLayout.SOUTH);
    }

    private JPanel templateBar(JComboBox<String> templates, JButton connect) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.setBackground(Color.WHITE);
        panel.add(new JLabel("Plantilla"));
        panel.add(templates);
        panel.add(connect);
        return panel;
    }

    private void applyTemplate(String template) {
        if ("Listar productos".equals(template)) {
            queryArea.setText("MATCH (p:Producto) RETURN p.id, p.nombre, p.pagerank LIMIT 25;");
        } else if ("Centralidad Adidas".equals(template)) {
            queryArea.setText("""
                    MATCH (p:Producto)
                    WHERE p.id = "69d2e2a8c083b3f01236f4b0" OR p.nombre CONTAINS 'Adidas'
                    RETURN p.id, p.nombre, p.pagerank;
                    """);
        }
    }

    void connectAsync() {
        new SwingWorker<Void, Void>() {
            protected Void doInBackground() {
                close();
                driver = GraphDatabase.driver(uriField.getText().trim(), AuthTokens.basic(userField.getText().trim(), password(passwordField)));
                driver.verifyConnectivity();
                return null;
            }

            protected void done() {
                try {
                    get();
                    setStatusOk("Neo4j conectada");
                } catch (Exception ex) {
                    setStatusError("Error Neo4j: " + Errors.rootMessage(ex));
                }
            }
        }.execute();
    }

    void executeAsync() {
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

    List<Map<String, Object>> runCypher(String cypher, Map<String, Object> parameters) {
        ensureConnected();
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

class ReportPanel extends JPanel {
    private final CassandraPanel cassandra;
    private final MongoPanel mongo;
    private final Neo4jPanel neo4j;
    private final JTextField cassandraIdField = new JTextField("69d2e2a8-c083-b3f0-1236-f4b000000000");
    private final JTextField mongoNeoIdField = new JTextField("69d2e2a8c083b3f01236f4b0");
    private final JTextField dateField = new JTextField("2026-05-30");
    private final JTextArea output = new JTextArea();

    ReportPanel(CassandraPanel cassandra, MongoPanel mongo, Neo4jPanel neo4j) {
        this.cassandra = cassandra;
        this.mongo = mongo;
        this.neo4j = neo4j;

        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(Color.WHITE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createTitledBorder("Reporte integrado del producto"));
        addReportField(form, "ID Cassandra", cassandraIdField, 0);
        addReportField(form, "ID Mongo/Neo4j", mongoNeoIdField, 1);
        addReportField(form, "Fecha", dateField, 2);

        JButton connectAll = new JButton("Conectar todo");
        connectAll.addActionListener(e -> {
            cassandra.connectAsync();
            mongo.connectAsync();
            neo4j.connectAsync();
            JOptionPane.showMessageDialog(this, "Se iniciaron las conexiones. Espera los estados en cada pestaña antes de generar el reporte.");
        });
        JButton report = new JButton("Generar reporte");
        report.addActionListener(e -> generateReport());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setBackground(Color.WHITE);
        buttons.add(connectAll);
        buttons.add(report);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(output), BorderLayout.CENTER);
    }

    private void addReportField(JPanel panel, String label, JTextField field, int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        panel.add(new JLabel(label), c);

        c.gridx = 1;
        c.weightx = 1;
        field.setPreferredSize(new Dimension(420, 30));
        panel.add(field, c);
    }

    private void generateReport() {
        output.setText("Generando reporte...\n");
        new SwingWorker<String, Void>() {
            protected String doInBackground() {
                String cassandraId = cassandraIdField.getText().trim();
                String productId = mongoNeoIdField.getText().trim();
                String date = dateField.getText().trim();

                String cql = "SELECT producto_id, fecha, vistas, conversiones FROM ecommerce.metricas_diarias "
                        + "WHERE producto_id = " + cassandraId + " AND fecha = '" + date + "';";
                List<Map<String, Object>> metrics = cassandra.executeForReport(cql);
                List<Map<String, Object>> products = mongo.findDocuments("productos", "{ \"_id\": ObjectId(\"" + productId + "\") }", 1);
                List<Map<String, Object>> graphRows = neo4j.runCypher(
                        "MATCH (p:Producto) WHERE p.id = $id OR p.nombre CONTAINS 'Adidas' RETURN p.id AS id, p.nombre AS nombre, p.pagerank AS pagerank",
                        Map.of("id", productId)
                );

                StringBuilder text = new StringBuilder();
                text.append("CONSULTA CASSANDRA\n").append(cql).append("\n\n");
                appendRows(text, metrics);
                text.append("\nCONSULTA MONGODB\nColeccion productos, filtro por _id ").append(productId).append("\n\n");
                appendRows(text, products);
                text.append("\nCONSULTA NEO4J\nMATCH por id/nombre Adidas y pagerank\n\n");
                appendRows(text, graphRows);
                text.append("\nRESUMEN\n");
                appendSummary(text, metrics, products, graphRows);
                return text.toString();
            }

            protected void done() {
                try {
                    output.setText(get());
                } catch (Exception ex) {
                    output.setText("Error generando reporte: " + Errors.rootMessage(ex));
                }
            }
        }.execute();
    }

    private void appendRows(StringBuilder text, List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            text.append("Sin resultados\n");
            return;
        }
        for (Map<String, Object> row : rows) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                text.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            text.append("\n");
        }
    }

    private void appendSummary(StringBuilder text, List<Map<String, Object>> metrics, List<Map<String, Object>> products, List<Map<String, Object>> graphRows) {
        Object views = firstValue(metrics, "vistas");
        Object conversions = firstValue(metrics, "conversiones");
        Object name = firstValue(products, "nombre");
        Object brand = firstValue(products, "marca");
        Object price = firstValue(products, "precio_base");
        Object pagerank = firstValue(graphRows, "pagerank");

        text.append("Producto: ").append(name != null ? name : "sin dato").append("\n");
        text.append("Marca: ").append(brand != null ? brand : "sin dato").append("\n");
        text.append("Precio: ").append(price != null ? price : "sin dato").append("\n");
        text.append("Vistas: ").append(views != null ? views : "sin dato").append("\n");
        text.append("Conversiones: ").append(conversions != null ? conversions : "sin dato").append("\n");
        text.append("Pagerank: ").append(pagerank != null ? pagerank : "sin dato").append("\n");
        if (views instanceof Number && conversions instanceof Number && ((Number) views).doubleValue() > 0) {
            double rate = ((Number) conversions).doubleValue() * 100.0 / ((Number) views).doubleValue();
            text.append("Tasa conversion: ").append(String.format("%.2f%%", rate)).append("\n");
        }
    }

    private Object firstValue(List<Map<String, Object>> rows, String key) {
        if (rows.isEmpty()) {
            return null;
        }
        return rows.get(0).get(key);
    }
}

class Errors {
    private Errors() {
    }

    static String rootMessage(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
