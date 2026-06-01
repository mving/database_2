package org.ecommerce.ui;

import org.bson.Document;
import org.ecommerce.queries.CassandraQueries;
import org.ecommerce.queries.MongoQueries;
import org.ecommerce.queries.Neo4jQueries;
import org.ecommerce.util.Errors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.Dialog;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ReportPanel extends JPanel {
    private final CassandraPanel cassandra;
    private final MongoPanel mongo;
    private final Neo4jPanel neo4j;
    
    private final JComboBox<String> operationBox;
    private final JPanel cards;
    private final JTextArea output = new JTextArea();
    
    private final JLabel rpMongo = new JLabel("● MongoDB");
    private final JLabel rpNeo = new JLabel("● Neo4j");
    private final JLabel rpCass = new JLabel("● Cassandra");

    // Fields OP-1
    private final JTextField op1ProductIdField = new JTextField("69d2e2a8c083b3f01236f4b0");
    private final JTextField op1CassandraIdField = new JTextField("69d2e2a8-c083-b3f0-1236-f4b000000000");

    // Fields OP-5
    private final JTextField op5ProductIdField = new JTextField("69d2e2a8c083b3f01236f4b0");
    private final JTextField op5CassandraIdField = new JTextField("69d2e2a8-c083-b3f0-1236-f4b000000000");

    // Fields OP-2
    private final JTextField op2UserIdField = new JTextField("69d2e841-49c1-178b-8d35-784400000000");

    // Fields OP-3
    private final JTextField op3TermField = new JTextField("Adidas");
    private final JTextField op3UserIdField = new JTextField("69d2e841-49c1-178b-8d35-784400000000");

    // Fields OP-4
    // None needed, runs as batch

    public ReportPanel(CassandraPanel cassandra, MongoPanel mongo, Neo4jPanel neo4j) {
        this.cassandra = cassandra;
        this.mongo = mongo;
        this.neo4j = neo4j;

        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(Color.WHITE);

        String[] ops = {
            "OP-1: Panel de Marketing (Producto)",
            "OP-2: Recomendación Homepage (Usuario)",
            "OP-3: Búsqueda con Intención (Catálogo)",
            "OP-4: Detección Abandono Carrito (Daemon)",
            "OP-5: Reporte Performance (Existente)"
        };
        operationBox = new JComboBox<>(ops);
        cards = new JPanel(new CardLayout());
        cards.setBackground(Color.WHITE);

        // Build Cards
        cards.add(buildOp1Card(), ops[0]);
        cards.add(buildOp2Card(), ops[1]);
        cards.add(buildOp3Card(), ops[2]);
        cards.add(buildOp4Card(), ops[3]);
        cards.add(buildOp5Card(), ops[4]);

        String[] summaries = {
            "OP-1: Vista consolidada de marketing para un producto. Junta catálogo (Mongo), top 5 co-compras (Neo4j) y métricas recientes de 24h (Cassandra).",
            "OP-2: Recomendación homepage. Busca últimos 10 eventos (Cassandra), cruza candidatos por filtrado colaborativo (Neo4j), excluye lo ya comprado (Mongo) y registra sugerencia (Cassandra).",
            "OP-3: Búsqueda con intención. Loguea la búsqueda en Cassandra, usa su historial para rankear resultados, y busca por texto completo con filtros en MongoDB.",
            "OP-4: Abandono de carrito. Escanea sesiones sin comprar (Cassandra) y extrae detalles de contacto del cliente (MongoDB) de forma batch.",
            "OP-5: Reporte performance. Genera un reporte consolidado con vistas (Cassandra), datos de ventas (MongoDB) y relevancia de co-compra (Neo4j)."
        };

        JTextArea summaryArea = new JTextArea(3, 120);
        summaryArea.setEditable(false);
        summaryArea.setLineWrap(true);
        summaryArea.setWrapStyleWord(true);
        summaryArea.setBackground(new Color(245, 245, 245));
        summaryArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        summaryArea.setText(summaries[0]);

        operationBox.addActionListener(e -> {
            int idx = operationBox.getSelectedIndex();
            summaryArea.setText(summaries[idx]);
            CardLayout cl = (CardLayout) (cards.getLayout());
            cl.show(cards, (String) operationBox.getSelectedItem());
        });

        JPanel form = new JPanel(new BorderLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createTitledBorder("Operaciones Políglotas"));
        
        JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        comboPanel.setBackground(Color.WHITE);
        comboPanel.add(new JLabel("Operación: "));
        comboPanel.add(operationBox);

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setBackground(Color.WHITE);
        topSection.add(comboPanel, BorderLayout.NORTH);
        
        JPanel summaryWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT));
        summaryWrapper.setBackground(Color.WHITE);
        summaryWrapper.add(summaryArea);
        topSection.add(summaryWrapper, BorderLayout.CENTER);

        form.add(topSection, BorderLayout.NORTH);
        form.add(cards, BorderLayout.CENTER);

        JButton connectAll = new JButton("Conectar Motores");
        connectAll.addActionListener(e -> {
            JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Conectando...", Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setLayout(new BorderLayout(10, 10));
            dialog.setSize(350, 200);
            dialog.setLocationRelativeTo(this);

            JLabel label = new JLabel("Conectando a Mongo, Neo4j y Cassandra...", SwingConstants.CENTER);
            JProgressBar progress = new JProgressBar();
            progress.setIndeterminate(true);

            JLabel lCass = new JLabel("● Cassandra");
            lCass.setForeground(Color.RED);
            JLabel lMongo = new JLabel("● MongoDB");
            lMongo.setForeground(Color.RED);
            JLabel lNeo = new JLabel("● Neo4j");
            lNeo.setForeground(Color.RED);

            JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
            statusPanel.add(lMongo);
            statusPanel.add(lNeo);
            statusPanel.add(lCass);
            
            SwingWorker<Void, Void> cw = cassandra.connectAsync();
            SwingWorker<Void, Void> mw = mongo.connectAsync();
            SwingWorker<Void, Void> nw = neo4j.connectAsync();

            cw.addPropertyChangeListener(evt -> {
                if ("state".equals(evt.getPropertyName()) && SwingWorker.StateValue.DONE == evt.getNewValue()) {
                    try { cw.get(); lCass.setForeground(new Color(0, 153, 0)); rpCass.setForeground(new Color(0, 153, 0)); } catch (Exception ignore) {}
                }
            });
            mw.addPropertyChangeListener(evt -> {
                if ("state".equals(evt.getPropertyName()) && SwingWorker.StateValue.DONE == evt.getNewValue()) {
                    try { mw.get(); lMongo.setForeground(new Color(0, 153, 0)); rpMongo.setForeground(new Color(0, 153, 0)); } catch (Exception ignore) {}
                }
            });
            nw.addPropertyChangeListener(evt -> {
                if ("state".equals(evt.getPropertyName()) && SwingWorker.StateValue.DONE == evt.getNewValue()) {
                    try { nw.get(); lNeo.setForeground(new Color(0, 153, 0)); rpNeo.setForeground(new Color(0, 153, 0)); } catch (Exception ignore) {}
                }
            });

            JButton cancelBtn = new JButton("Cancelar");
            cancelBtn.addActionListener(ev -> {
                cw.cancel(true);
                mw.cancel(true);
                nw.cancel(true);
                dialog.dispose();
            });
            
            JPanel center = new JPanel(new BorderLayout(0, 10));
            center.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            center.add(label, BorderLayout.NORTH);
            center.add(progress, BorderLayout.CENTER);
            center.add(statusPanel, BorderLayout.SOUTH);
            
            JPanel bottom = new JPanel();
            bottom.add(cancelBtn);
            
            dialog.add(center, BorderLayout.CENTER);
            dialog.add(bottom, BorderLayout.SOUTH);

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    while (!cw.isDone() || !mw.isDone() || !nw.isDone()) {
                        if (isCancelled()) break;
                        Thread.sleep(100);
                    }
                    return null;
                }
                @Override
                protected void done() {
                    dialog.dispose();
                    if (!isCancelled() && !cw.isCancelled() && !mw.isCancelled() && !nw.isCancelled()) {
                        JOptionPane.showMessageDialog(ReportPanel.this, "Se conectaron los motores correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }.execute();

            dialog.setVisible(true);
        });

        rpMongo.setForeground(Color.RED);
        rpNeo.setForeground(Color.RED);
        rpCass.setForeground(Color.RED);

        JPanel indicators = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        indicators.setBackground(Color.WHITE);
        indicators.add(rpMongo);
        indicators.add(rpNeo);
        indicators.add(rpCass);

        JButton executeBtn = new JButton("Ejecutar Operación");
        executeBtn.addActionListener(e -> executeOperation());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setBackground(Color.WHITE);
        buttons.add(connectAll);
        buttons.add(executeBtn);

        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(Color.WHITE);
        bottomBar.add(indicators, BorderLayout.WEST);
        bottomBar.add(buttons, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.add(form, BorderLayout.CENTER);

        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(output), BorderLayout.CENTER);
        add(bottomBar, BorderLayout.SOUTH);
    }

    private JPanel buildOp1Card() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        addReportField(p, "ID Mongo/Neo4j (Ej: 69d2e2a8...)", op1ProductIdField, 0);
        addReportField(p, "ID Cassandra (UUID)", op1CassandraIdField, 1);
        return p;
    }

    private JPanel buildOp5Card() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        addReportField(p, "ID Mongo/Neo4j (Ej: 69d2e2a8...)", op5ProductIdField, 0);
        addReportField(p, "ID Cassandra (UUID)", op5CassandraIdField, 1);
        return p;
    }

    private JPanel buildOp2Card() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        addReportField(p, "Usuario ID (Cassandra/Neo4j UUID)", op2UserIdField, 0);
        return p;
    }

    private JPanel buildOp3Card() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        addReportField(p, "Término de búsqueda", op3TermField, 0);
        addReportField(p, "Usuario ID (Auditoría)", op3UserIdField, 1);
        return p;
    }

    private JPanel buildOp4Card() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.setBackground(Color.WHITE);
        p.add(new JLabel("Esta operación simula un proceso en batch. No requiere parámetros."));
        return p;
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

    private void executeOperation() {
        int index = operationBox.getSelectedIndex();
        output.setText("Ejecutando operación...\n");
        new SwingWorker<String, Void>() {
            protected String doInBackground() throws Exception {
                try {
                    return switch (index) {
                        case 0 -> ejecutarOP1();
                        case 1 -> ejecutarOP2();
                        case 2 -> ejecutarOP3();
                        case 3 -> ejecutarOP4();
                        case 4 -> ejecutarOP5();
                        default -> "Operación no implementada.";
                    };
                } catch (Exception ex) {
                    throw ex;
                }
            }

            protected void done() {
                try {
                    output.setText(get());
                } catch (Exception ex) {
                    String msg = Errors.rootMessage(ex);
                    output.setText("Error en operación políglota: " + msg);
                    JOptionPane.showMessageDialog(ReportPanel.this, 
                        "Error conectando a bases de datos:\n" + msg, 
                        "Fallo de Operación", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private String ejecutarOP1() {
        String mongoId = op1ProductIdField.getText().trim();
        String cassId = op1CassandraIdField.getText().trim();
        
        StringBuilder sb = new StringBuilder("=== OP-1: PANEL DE MARKETING ===\n\n");
        
        sb.append("1. MONGODB (Maestro y Reviews)\n");
        String mQuery = String.format(MongoQueries.PRODUCTO_REPORTE, mongoId);
        List<Map<String, Object>> mData = mongo.findDocuments("productos", mQuery, 1);
        appendRows(sb, mData);
        
        sb.append("\n2. NEO4J (Top 5 co-compras)\n");
        List<Map<String, Object>> nData = neo4j.runCypher(Neo4jQueries.TOP_COCOMPRAS, Map.of("id", mongoId));
        appendRows(sb, nData);

        sb.append("\n3. CASSANDRA (Métricas 24hs)\n");
        String today = java.time.LocalDate.now().toString();
        // Para simplificar, traemos las métricas del día actual desde la hora 0
        String cQuery = String.format(CassandraQueries.METRICAS_HORARIAS, cassId, today, 0);
        List<Map<String, Object>> cData = cassandra.executeForReport(cQuery);
        appendRows(sb, cData);

        return sb.toString();
    }

    private String ejecutarOP2() {
        String userId = op2UserIdField.getText().trim();
        StringBuilder sb = new StringBuilder("=== OP-2: RECOMENDACIÓN HOMEPAGE ===\n\n");

        sb.append("1. CASSANDRA (Últimos 10 eventos - 30 min)\n");
        String limitDate = Instant.now().minus(30, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS).toString();
        String cQuery = String.format(CassandraQueries.ULTIMOS_EVENTOS, userId, limitDate);
        List<Map<String, Object>> cData = cassandra.executeForReport(cQuery);
        appendRows(sb, cData);

        sb.append("\n2. NEO4J (Filtro colaborativo a 2 grados)\n");
        List<Map<String, Object>> nData = neo4j.runCypher(Neo4jQueries.COLABORATIVO_HOMEPAGE, Map.of("uid", userId));
        appendRows(sb, nData);

        sb.append("\n3. MONGODB (Filtro catálogo, excluyendo ya comprados)\n");
        List<String> recIds = nData.stream().map(m -> (String)m.get("id")).collect(Collectors.toList());
        if (recIds.isEmpty()) {
            sb.append("Sin recomendaciones en Neo4j.\n");
        } else {
            String inList = recIds.stream().map(id -> "ObjectId(\""+id+"\")").collect(Collectors.joining(","));
            String mQuery = "{ \"_id\": { \"$in\": [" + inList + "] } }";
            List<Map<String, Object>> mData = mongo.findDocuments("productos", mQuery, 5);
            appendRows(sb, mData);
        }

        sb.append("\n4. CASSANDRA (Registrar 'recommendation_shown')\n");
        String insertRec = String.format(CassandraQueries.INSERT_RECOMENDACION, userId);
        cassandra.executeForReport(insertRec);
        sb.append("Evento registrado correctamente.\n");

        return sb.toString();
    }

    private String ejecutarOP3() {
        String term = op3TermField.getText().trim();
        String userId = op3UserIdField.getText().trim();
        StringBuilder sb = new StringBuilder("=== OP-3: BÚSQUEDA CON INTENCIÓN ===\n\n");

        sb.append("1. CASSANDRA (Registro de auditoría)\n");
        String insert = String.format(CassandraQueries.INSERT_BUSQUEDA, term, userId);
        cassandra.executeForReport(insert); // Ejecutar inserción
        sb.append("Evento guardado correctamente.\n");

        sb.append("\n2. CASSANDRA (Histórico de conversiones)\n");
        String histQuery = String.format(CassandraQueries.HISTORICO_CONVERSIONES, term);
        List<Map<String, Object>> histData = cassandra.executeForReport(histQuery);
        appendRows(sb, histData);

        sb.append("\n3. MONGODB (Búsqueda textual + stock > 0)\n");
        String mQuery = "{ \"$text\": { \"$search\": \"" + term + "\" }, \"stock\": { \"$gt\": 0 } }";
        List<Map<String, Object>> mData = mongo.findDocuments("productos", mQuery, 10);
        appendRows(sb, mData);

        return sb.toString();
    }

    private String ejecutarOP4() {
        StringBuilder sb = new StringBuilder("=== OP-4: ABANDONO DE CARRITO ===\n\n");
        
        sb.append("1. CASSANDRA (Sesiones inactivas)\n");
        List<Map<String, Object>> sesiones = cassandra.executeForReport(CassandraQueries.SESIONES_ACTIVAS);
        
        if (sesiones.isEmpty()) {
            sb.append("No hay sesiones activas.\n");
            return sb.toString();
        }
        
        sb.append("Analizando " + sesiones.size() + " sesiones...\n");
        for (Map<String, Object> s : sesiones) {
            String uid = s.get("usuario_id").toString();
            String limitDate = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS).toString();
            String checkCompra = String.format(CassandraQueries.VALIDAR_COMPRA, uid, limitDate);
            List<Map<String, Object>> eventos = cassandra.executeForReport(checkCompra);
            
            boolean tieneCompra = eventos.stream().anyMatch(e -> "purchase".equals(e.get("tipo_evento")));
            
            if (!tieneCompra) {
                sb.append("\n=> Abandono detectado para UUID: ").append(uid).append("\n");
                String idLimpio = uid.replace("-", "").substring(0, 24);
                String mQuery = String.format(MongoQueries.PRODUCTO_REPORTE, idLimpio); // Reusa filter de ID
                List<Map<String, Object>> clienteList = mongo.findDocuments("clientes", mQuery, 1);
                
                if (clienteList.isEmpty()) {
                    sb.append("   [!] Cliente no encontrado en MongoDB.\n");
                } else {
                    Map<String, Object> c = clienteList.get(0);
                    sb.append("   👤 Cliente: ").append(c.get("nombre")).append("\n");
                    sb.append("   ✉️ Email: ").append(c.get("email")).append("\n");
                    Object dirObj = c.get("direccion_envio");
                    if (dirObj instanceof org.bson.Document) {
                        org.bson.Document dir = (org.bson.Document) dirObj;
                        String calle = dir.getString("calle");
                        String num = dir.getString("numero");
                        String prov = dir.getString("provincia");
                        String cp = dir.getString("codigo_postal");
                        sb.append("   📍 Dirección: ").append(calle).append(" ").append(num)
                          .append(", ").append(prov).append(" (CP: ").append(cp).append(")\n");
                    } else if (dirObj != null) {
                        sb.append("   📍 Dirección: ").append(dirObj.toString()).append("\n");
                    }
                    
                    Object carrito = s.get("carrito");
                    if (carrito != null) {
                        sb.append("   🛒 Carrito abandonado: ").append(carrito).append("\n");
                    }
                    
                    sb.append("   >> ACCIÓN: Cola de envío para 'Email de Recuperación' activada.\n");
                }
            }
        }
        return sb.toString();
    }

    private String ejecutarOP5() {
        String mongoId = op5ProductIdField.getText().trim();
        String cassId = op5CassandraIdField.getText().trim();
        String date = "2026-05-30";

        StringBuilder sb = new StringBuilder("=== OP-5: PERFORMANCE CATÁLOGO ===\n\n");
        
        String cql = String.format(CassandraQueries.METRICAS_REPORTE, cassId, date);
        List<Map<String, Object>> metrics = cassandra.executeForReport(cql);
        
        String mQuery = String.format(MongoQueries.PRODUCTO_REPORTE, mongoId);
        List<Map<String, Object>> products = mongo.findDocuments("productos", mQuery, 1);
        
        List<Map<String, Object>> graphRows = neo4j.runCypher(Neo4jQueries.REPORTE_PRODUCTO, Map.of("id", mongoId));

        sb.append("CONSULTA CASSANDRA\n");
        appendRows(sb, metrics);
        sb.append("\nCONSULTA MONGODB\n");
        appendRows(sb, products);
        sb.append("\nCONSULTA NEO4J\n");
        appendRows(sb, graphRows);
        return sb.toString();
    }

    private void appendRows(StringBuilder text, List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            text.append("Sin resultados\n");
            return;
        }
        for (Map<String, Object> row : rows) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                text.append(entry.getKey()).append(": ").append(entry.getValue()).append(" | ");
            }
            text.append("\n");
        }
    }
}
