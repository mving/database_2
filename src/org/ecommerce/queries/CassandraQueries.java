package org.ecommerce.queries;

public class CassandraQueries {
    public static final String LISTAR_TABLAS = "SELECT keyspace_name, table_name FROM system_schema.tables WHERE keyspace_name = 'ecommerce';";
    public static final String METRICAS_ADIDAS = 
        "SELECT producto_id, fecha, vistas, conversiones\n" +
        "FROM ecommerce.metricas_diarias\n" +
        "WHERE producto_id = 69d2e2a8-c083-b3f0-1236-f4b000000000 AND fecha = '2026-05-30';\n";
    
    // Uses String.format for cassandraId and date
    public static final String METRICAS_REPORTE = 
        "SELECT producto_id, fecha, vistas, conversiones FROM ecommerce.metricas_diarias " +
        "WHERE producto_id = %s AND fecha = '%s';";


    // OP-1
    public static final String METRICAS_HORARIAS = 
        "SELECT vistas, clics, conversiones, revenue_por_hora FROM ecommerce.metricas_diarias " +
        "WHERE producto_id = %s AND fecha = '%s' AND hora >= %d;";

    // OP-2
    public static final String ULTIMOS_EVENTOS = 
        "SELECT producto_id, tipo_evento, contexto FROM ecommerce.eventos_usuario " +
        "WHERE usuario_id = %s AND timestamp >= '%s' LIMIT 10;";
    public static final String INSERT_RECOMENDACION = 
        "INSERT INTO ecommerce.eventos_usuario (usuario_id, timestamp, tipo_evento, producto_id) " +
        "VALUES (%s, toTimestamp(now()), 'recommendation_shown', null);";

    // OP-3
    public static final String INSERT_BUSQUEDA = 
        "INSERT INTO ecommerce.busquedas_por_termino (termino, timestamp, usuario_id, convertido) " +
        "VALUES ('%s', toTimestamp(now()), %s, false);";
    public static final String HISTORICO_CONVERSIONES = 
        "SELECT productos_devueltos, convertido FROM ecommerce.busquedas_por_termino WHERE termino = '%s';";

    // OP-4
    public static final String SESIONES_ACTIVAS = 
        "SELECT sesion_id, usuario_id, carrito, ultima_actividad FROM ecommerce.sesiones_activas;";
    public static final String VALIDAR_COMPRA = 
        "SELECT tipo_evento FROM ecommerce.eventos_usuario WHERE usuario_id = %s AND timestamp >= '%s';";
}
