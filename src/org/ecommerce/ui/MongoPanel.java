package org.ecommerce.ui;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.ecommerce.config.AppConfig;
import org.ecommerce.queries.MongoQueries;
import org.ecommerce.util.Errors;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MongoPanel extends QueryPanel {
    private final JTextField uriField;
    private final JTextField databaseField;
    private final JTextField collectionField;
    private final JCheckBox editUriCheck;
    private String realMongoUri;
    private MongoClient client;
    private MongoDatabase database;
    private final JLabel indicator = new JLabel("● MongoDB");

    public MongoPanel() {
        JPanel config = baseConfigPanel();
        realMongoUri = AppConfig.env("GRUPO4_MONGO_URI", "");
        uriField = addText(config, "URI", maskMongoUri(realMongoUri), 0, 0);
        uriField.setEnabled(false);
        databaseField = addText(config, "Base", AppConfig.env("GRUPO4_MONGO_DATABASE", "ecommerce"), 0, 1);
        collectionField = addText(config, "Coleccion", AppConfig.env("GRUPO4_MONGO_COLLECTION", "productos"), 1, 0);

        editUriCheck = new JCheckBox("Editar string de conexion");
        editUriCheck.setBackground(Color.WHITE);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 2; c.gridwidth = 4; c.anchor = GridBagConstraints.WEST; c.insets = new Insets(5, 6, 5, 6);
        config.add(editUriCheck, c);

        editUriCheck.addActionListener(e -> {
            if (editUriCheck.isSelected()) {
                int result = JOptionPane.showConfirmDialog(this,
                        "¿Estás seguro de que quieres editar tu string de conexión?\nEditar este string revelará tus credenciales.",
                        "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    uriField.setEnabled(true);
                } else {
                    editUriCheck.setSelected(false);
                }
            } else {
                uriField.setEnabled(false);
                String text = uriField.getText().trim();
                if (!text.isEmpty() && !text.contains("*****")) {
                    realMongoUri = text;
                }
                uriField.setText(maskMongoUri(realMongoUri));
            }
        });

        uriField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (uriField.isEnabled()) {
                    uriField.setText(realMongoUri);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                String text = uriField.getText().trim();
                if (!text.isEmpty() && !text.contains("*****")) {
                    realMongoUri = text;
                }
                uriField.setText(maskMongoUri(realMongoUri));
            }
        });

        JComboBox<String> templates = new JComboBox<>(new String[]{
                "Ficha producto Adidas",
                "Primeros productos",
                "Filtro libre"
        });
        templates.addActionListener(e -> applyTemplate((String) templates.getSelectedItem()));
        applyTemplate("Ficha producto Adidas");

        indicator.setForeground(Color.RED);
        JButton connect = new JButton("Conectar");
        connect.addActionListener(e -> connectAsync());
        JButton execute = new JButton("Ejecutar find");
        execute.addActionListener(e -> executeAsync());

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setBackground(Color.WHITE);
        top.add(config, BorderLayout.NORTH);
        top.add(templateBar(templates), BorderLayout.SOUTH);

        add(queryAndResults(top), BorderLayout.CENTER);
        add(actionBar(connect, execute, indicator), BorderLayout.SOUTH);
    }

    private String maskMongoUri(String uri) {
        if (uri == null) return "";
        return uri.replaceAll("(mongodb(?:\\+srv)?://[^:]+:)([^@]+)(@.*)", "$1*****$3");
    }

    private JPanel templateBar(JComboBox<String> templates) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.setBackground(Color.WHITE);
        panel.add(new JLabel("Plantilla"));
        panel.add(templates);
        return panel;
    }

    private void applyTemplate(String template) {
        if ("Primeros productos".equals(template)) {
            queryArea.setText(MongoQueries.PRIMEROS_PRODUCTOS);
        } else if ("Ficha producto Adidas".equals(template)) {
            queryArea.setText(MongoQueries.FICHA_ADIDAS);
        }
    }

    public SwingWorker<Void, Void> connectAsync() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            protected Void doInBackground() {
                close();
                String text = uriField.getText().trim();
                if (!text.isEmpty() && !text.contains("*****")) {
                    realMongoUri = text;
                }
                client = MongoClients.create(realMongoUri);
                database = client.getDatabase(databaseField.getText().trim());
                database.runCommand(new Document("ping", 1));
                return null;
            }

            protected void done() {
                try {
                    get();
                    setStatusOk("");
                    indicator.setForeground(new Color(0, 153, 0));
                } catch (Exception ex) {
                    setStatusError("Error MongoDB: " + Errors.rootMessage(ex));
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

    public List<Map<String, Object>> findDocuments(String collectionName, String filterText, int limit) {
        ensureConnected();
        try {
            MongoCollection<Document> collection = database.getCollection(collectionName);
            Document filter = Document.parse(normalizeMongoFilter(filterText));
            FindIterable<Document> docs = collection.find(filter).limit(limit);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Document doc : docs) {
                rows.add(flattenDocument(doc));
            }
            return rows;
        } catch (Exception ex) {
            throw new RuntimeException("MongoDB - " + Errors.rootMessage(ex));
        }
    }

    private Map<String, Object> flattenDocument(Document doc) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : doc.entrySet()) {
            row.put(entry.getKey(), entry.getValue());
        }
        return row;
    }

    public void insertDocument(String collectionName, Document doc) {
        ensureConnected();
        try {
            database.getCollection(collectionName).insertOne(doc);
        } catch (Exception ex) {
            throw new RuntimeException("MongoDB Insert - " + Errors.rootMessage(ex));
        }
    }

    public void updateDocument(String collectionName, Document filter, Document update) {
        ensureConnected();
        try {
            database.getCollection(collectionName).updateOne(filter, new Document("$set", update));
        } catch (Exception ex) {
            throw new RuntimeException("MongoDB Update - " + Errors.rootMessage(ex));
        }
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

    public MongoDatabase getDatabase() {
        ensureConnected();
        return database;
    }
}
