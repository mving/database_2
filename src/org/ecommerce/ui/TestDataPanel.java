package org.ecommerce.ui;

import org.bson.Document;
import org.bson.types.ObjectId;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;

public class TestDataPanel extends JPanel {
    private final CassandraPanel cassandra;
    private final MongoPanel mongo;
    private final Neo4jPanel neo4j;
    
    private final JTextArea logArea = new JTextArea();
    private final JButton btnGenerar = new JButton("Generar Datos de Prueba (Conectar Motores Primero)");

    public TestDataPanel(CassandraPanel cassandra, MongoPanel mongo, Neo4jPanel neo4j) {
        this.cassandra = cassandra;
        this.mongo = mongo;
        this.neo4j = neo4j;

        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(Color.WHITE);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBackground(Color.WHITE);
        top.add(btnGenerar);

        btnGenerar.addActionListener(e -> generarDatos());

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(logArea), BorderLayout.CENTER);
    }

    private void log(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void generarDatos() {
        btnGenerar.setEnabled(false);
        logArea.setText("Iniciando generación de datos de prueba...\n\n");

        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    publish("1. Obteniendo 3 productos reales de MongoDB...");
                    List<Map<String, Object>> productos = mongo.findDocuments("productos", "{}", 3);
                    if (productos.size() < 3) {
                        publish("Error: Se necesitan al menos 3 productos en la colección 'productos' de MongoDB.");
                        return null;
                    }

                    // Extract Mongo IDs and convert to Cassandra UUID format
                    String[] mongoIds = new String[3];
                    String[] cassIds = new String[3];
                    String[] nombres = new String[3];

                    for (int i = 0; i < 3; i++) {
                        Map<String, Object> p = productos.get(i);
                        mongoIds[i] = p.get("_id").toString();
                        nombres[i] = p.get("nombre") != null ? p.get("nombre").toString() : "Producto " + i;
                        // Format to standard UUID using mongoId + padding
                        String oid = mongoIds[i];
                        cassIds[i] = String.format("%s-%s-%s-%s-%s00000000",
                            oid.substring(0, 8), oid.substring(8, 12),
                            oid.substring(12, 16), oid.substring(16, 20),
                            oid.substring(20, 24));
                        publish("   Producto " + (i+1) + ": " + nombres[i] + " (" + cassIds[i] + ")");
                    }

                    publish("\n2. Creando 3 usuarios de prueba (usando IDs por defecto del panel)...");
                    String[] userCassIds = new String[]{
                        "69d2e841-49c1-178b-8d35-784400000000", // Default en OP-2 y OP-3
                        "a3b073c4-6449-4bd4-8461-182300000032",
                        "b4c184d5-7550-5ce5-9572-293400000043"
                    };
                    String[] userMongoIds = new String[]{
                        "69d2e84149c1178b8d357844",
                        "a3b073c464494bd484611823",
                        "b4c184d575505ce595722934"
                    };

                    for (int i = 0; i < 3; i++) {
                        String oid = userMongoIds[i];
                        
                        // Intentar borrar antes por si ya existe para evitar errores
                        try {
                            mongo.getDatabase().getCollection("clientes").deleteOne(new Document("_id", new ObjectId(oid)));
                        } catch (Exception ignored) {}

                        // Crear cliente en Mongo
                        Document doc = new Document("_id", new ObjectId(oid))
                            .append("nombre", "Usuario de Prueba " + (i+1))
                            .append("email", "test" + i + "@example.com")
                            .append("direccion_envio", new Document("calle", "Falsa").append("numero", "123").append("provincia", "Buenos Aires").append("codigo_postal", "1000"));
                        mongo.insertDocument("clientes", doc);
                        publish("   Usuario " + (i+1) + " creado en Mongo (" + userMongoIds[i] + ")");
                    }

                    publish("\n3. Inyectando OP-1 y OP-5 (Métricas de Marketing y Neo4j)");
                    String today = java.time.LocalDate.now().toString();
                    int currentHour = java.time.LocalTime.now().getHour();
                    
                    // Asegurar nodos de productos en Neo4j
                    for (int i = 0; i < 3; i++) {
                        String mergeCypher = "MERGE (p:Producto {producto_id: $id}) SET p.nombre = $nombre, p.pagerank = $pr";
                        neo4j.runCypher(mergeCypher, Map.of("id", mongoIds[i], "nombre", nombres[i], "pr", (3-i)*1.5));
                        publish("   [Neo4j] Nodo :Producto creado/actualizado → id=" + mongoIds[i] + ", nombre=" + nombres[i] + ", pagerank=" + ((3-i)*1.5));
                    }
                    
                    // Relaciones Neo4j (Co-compras)
                    neo4j.runCypher("MATCH (a:Producto {producto_id: $idA}), (b:Producto {producto_id: $idB}) MERGE (a)-[:COMPRADO_CON]->(b)", 
                        Map.of("idA", mongoIds[0], "idB", mongoIds[1]));
                    publish("   [Neo4j] Relación creada → (" + nombres[0] + ")-[:COMPRADO_CON]->(" + nombres[1] + ")");
                    neo4j.runCypher("MATCH (a:Producto {producto_id: $idA}), (b:Producto {producto_id: $idB}) MERGE (a)-[:COMPRADO_CON]->(b)", 
                        Map.of("idA", mongoIds[0], "idB", mongoIds[2]));
                    publish("   [Neo4j] Relación creada → (" + nombres[0] + ")-[:COMPRADO_CON]->(" + nombres[2] + ")");

                    // Cassandra Métricas
                    for (int i = 0; i < 3; i++) {
                        for (int h = 0; h <= currentHour; h++) {
                            String insertMetrics = String.format(
                                "INSERT INTO ecommerce.metricas_diarias (producto_id, fecha, hora, vistas, clics, conversiones, revenue_por_hora) " +
                                "VALUES (%s, '%s', %d, %d, %d, %d, %d)",
                                cassIds[i], today, h, (100-h*2), 20, 5, 5000
                            );
                            cassandra.executeForReport(insertMetrics);
                        }
                        publish("   [Cassandra] metricas_diarias → producto=" + nombres[i] + ", fecha=" + today + ", horas 0-" + currentHour + " (" + (currentHour+1) + " filas), vistas=" + (100) + "→" + (100-currentHour*2) + ", clics=20, conv=5, rev=$5000/h");
                    }

                    publish("\n4. Inyectando OP-2 (Recomendación / Historial de eventos)");
                    // Asegurar Nodos de Usuario en Neo4j
                    for (int i = 0; i < 3; i++) {
                        neo4j.runCypher("MERGE (u:Usuario {usuario_id: $uid})", Map.of("uid", userCassIds[i]));
                        publish("   [Neo4j] Nodo :Usuario creado/actualizado → id=" + userCassIds[i]);
                    }
                    
                    // Relaciones COMPRO en Neo4j para filtrado colaborativo
                    neo4j.runCypher("MATCH (u:Usuario {usuario_id: $uid}), (p:Producto {producto_id: $pid}) MERGE (u)-[:COMPRO]->(p)", 
                        Map.of("uid", userCassIds[0], "pid", mongoIds[1])); // U1 compró P2
                    publish("   [Neo4j] Relación creada → (Usuario 1)-[:COMPRO]->(" + nombres[1] + ")");
                    neo4j.runCypher("MATCH (u:Usuario {usuario_id: $uid}), (p:Producto {producto_id: $pid}) MERGE (u)-[:COMPRO]->(p)", 
                        Map.of("uid", userCassIds[1], "pid", mongoIds[1])); // U2 compró P2
                    publish("   [Neo4j] Relación creada → (Usuario 2)-[:COMPRO]->(" + nombres[1] + ")");
                    neo4j.runCypher("MATCH (u:Usuario {usuario_id: $uid}), (p:Producto {producto_id: $pid}) MERGE (u)-[:COMPRO]->(p)", 
                        Map.of("uid", userCassIds[1], "pid", mongoIds[2])); // U2 compró P3
                    publish("   [Neo4j] Relación creada → (Usuario 2)-[:COMPRO]->(" + nombres[2] + ")");
                        
                    // Cassandra Eventos Recientes (Últimos 30 mins)
                    for (int i = 0; i < 3; i++) {
                        for(int ev = 0; ev < 10; ev++) {
                            String eventTime = Instant.now().minus(ev * 2, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS).toString();
                            
                            // Insertar en eventos_usuario
                            String insertEventU = String.format(
                                "INSERT INTO ecommerce.eventos_usuario (usuario_id, timestamp, tipo_evento, producto_id, contexto) " +
                                "VALUES (%s, '%s', 'view', %s, 'homepage')",
                                userCassIds[i], eventTime, cassIds[0]
                            );
                            cassandra.executeForReport(insertEventU);
                            
                            // Doble escritura en eventos_producto
                            String insertEventP = String.format(
                                "INSERT INTO ecommerce.eventos_producto (producto_id, timestamp, usuario_id, contexto, dispositivo, tipo_evento) " +
                                "VALUES (%s, '%s', %s, 'homepage', 'mobile', 'view')",
                                cassIds[0], eventTime, userCassIds[i]
                            );
                            cassandra.executeForReport(insertEventP);
                        }
                        publish("   [Cassandra] eventos_usuario + eventos_producto → 10 eventos 'view' para Usuario " + (i+1) + " sobre " + nombres[0] + " (últimos 20 min, cada 2 min)");
                    }

                    publish("\n5. Inyectando OP-3 (Búsqueda Histórica)");
                    for (int i = 0; i < 3; i++) {
                        String termino = nombres[i].split(" ")[0]; // Primer palabra del nombre
                        String insertBusqueda = String.format(
                            "INSERT INTO ecommerce.busquedas_por_termino (termino, timestamp, usuario_id, convertido, productos_devueltos) " +
                            "VALUES ('%s', toTimestamp(now()), %s, false, ['%s'])",
                            termino, userCassIds[i], cassIds[i]
                        );
                        cassandra.executeForReport(insertBusqueda);
                        publish("   [Cassandra] busquedas_por_termino → término=\"" + termino + "\", usuario=" + (i+1) + ", convertido=false, producto devuelto=" + nombres[i]);
                    }

                    publish("\n6. Inyectando OP-4 (Abandono de Sesiones)");
                    for (int i = 0; i < 3; i++) {
                        String sesionId = UUID.randomUUID().toString();
                        // Actividad hace 2.5 horas (Inactiva, abandonada)
                        String pastTime = Instant.now().minus(150, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS).toString();
                        String insertSesion = String.format(
                            "INSERT INTO ecommerce.sesiones_activas (sesion_id, usuario_id, carrito, ultima_actividad) " +
                            "VALUES (%s, %s, '{\"items\":[{\"producto\":\"%s\",\"qty\":1}]}', '%s')",
                            sesionId, userCassIds[i], nombres[i], pastTime
                        );
                        cassandra.executeForReport(insertSesion);
                        publish("   [Cassandra] sesiones_activas → sesion_id=" + sesionId + ", usuario=" + (i+1) + ", carrito=[" + nombres[i] + " x1], ultima_actividad=" + pastTime + " (hace 2.5h, SIN TTL → abandonada)");
                    }

                    publish("\n¡Generación Completa!");
                    publish("---------------------------------------------------------");
                    publish("Datos de prueba:");
                    publish("ID Mongo: " + mongoIds[0]);
                    publish("ID Cassandra (Producto): " + cassIds[0]);
                    publish("ID Cassandra (Usuario): " + userCassIds[0]);
                    publish("Término de búsqueda: " + nombres[0].split(" ")[0]);
                    
                } catch (Exception ex) {
                    publish("\n[ERROR] Ocurrió un error: " + ex.getMessage());
                    ex.printStackTrace();
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String c : chunks) {
                    log(c);
                }
            }

            @Override
            protected void done() {
                btnGenerar.setEnabled(true);
            }
        }.execute();
    }
}
