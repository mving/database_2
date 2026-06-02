package org.ecommerce.queries;

public class CassandraQueries {
    public static final String LISTAR_TABLAS = "SELECT keyspace_name, table_name FROM system_schema.tables WHERE keyspace_name = 'ecommerce';";

    // ---------------------------------------------------------
    // 3.1 Registro de eventos de comportamiento
    // ---------------------------------------------------------
    public static final String INSERT_EVENTO_USUARIO = 
        "-- 3.1 Inserción de un evento de usuario\n" +
        "-- Registra un evento (ej. view, click) asociado a un usuario y producto.\n" +
        "INSERT INTO ecommerce.eventos_usuario (\n" +
        "    usuario_id, timestamp, tipo_evento, producto_id, contexto, dispositivo, termino\n" +
        ") VALUES (\n" +
        "    69d2e841-49c1-178b-8d35-784400000000,\n" +
        "    toTimestamp(now()),\n" +
        "    'view',\n" +
        "    69d2e2a8-c083-b3f0-1236-f4b000000000,\n" +
        "    'product_page',\n" +
        "    'mobile',\n" +
        "    null\n" +
        ");";

    public static final String EVENTOS_USUARIO_24H = 
        "-- 3.1.a Eventos de un usuario en las últimas 24 horas\n" +
        "-- Recupera todos los eventos de un usuario ordenados cronológicamente inverso.\n" +
        "SELECT timestamp, tipo_evento, producto_id, termino, dispositivo, contexto\n" +
        "FROM ecommerce.eventos_usuario\n" +
        "WHERE usuario_id = 69d2e841-49c1-178b-8d35-784400000000\n" +
        "  AND timestamp >= toTimestamp(now()) - 86400s;";

    public static final String ULTIMOS_100_EVENTOS_PRODUCTO = 
        "-- 3.1.b Últimos 100 eventos sobre un producto específico\n" +
        "-- Requiere tabla auxiliar eventos_producto particionada por producto_id.\n" +
        "SELECT timestamp, tipo_evento, usuario_id, dispositivo, contexto\n" +
        "FROM ecommerce.eventos_producto\n" +
        "WHERE producto_id = 69d2e2a8-c083-b3f0-1236-f4b000000000\n" +
        "LIMIT 100;";

    public static final String TIEMPO_PROMEDIO_SESION = 
        "-- 3.1.c Tiempo promedio de sesión de un usuario en el último mes\n" +
        "-- Trae todos los eventos del mes. La agrupación por sesión se resuelve en Java.\n" +
        "SELECT timestamp, contexto\n" +
        "FROM ecommerce.eventos_usuario\n" +
        "WHERE usuario_id = 69d2e841-49c1-178b-8d35-784400000000\n" +
        "  AND timestamp >= toTimestamp(now()) - 2592000s;";

    public static final String PRODUCTOS_MAS_VISTOS_2H = 
        "-- 3.1.d Productos más vistos en las últimas 2 horas\n" +
        "-- Se consulta sobre metricas_diarias. Si cruza medianoche, se consultan ambos días.\n" +
        "-- El ranking final se ensambla en Java.\n" +
        "SELECT producto_id, hora, vistas\n" +
        "FROM ecommerce.metricas_diarias\n" +
        "WHERE producto_id = 69d2e2a8-c083-b3f0-1236-f4b000000000\n" +
        "  AND fecha = '2026-05-31'\n" +
        "  AND hora >= 13;";

    // ---------------------------------------------------------
    // 3.2 Análisis de sesiones
    // ---------------------------------------------------------
    public static final String CREAR_SESION_TTL = 
        "-- 3.2.a Crear o reactivar una sesión de usuario con TTL\n" +
        "-- La sesión expira automáticamente en 30 minutos (1800s).\n" +
        "INSERT INTO ecommerce.sesiones_activas (\n" +
        "    sesion_id, usuario_id, carrito, ultima_actividad\n" +
        ") VALUES (\n" +
        "    e97f5b1c-7235-438b-807d-304700000032,\n" +
        "    a3b073c4-6449-4bd4-8461-182300000032,\n" +
        "    '{\"items\": [{\"producto\": \"remera\", \"qty\": 1}]}',\n" +
        "    toTimestamp(now())\n" +
        ") USING TTL 1800;";

    public static final String ACTUALIZAR_CARRITO_TTL = 
        "-- 3.2.b Actualizar el contenido del carrito\n" +
        "-- Renueva la vida útil de la sesión con USING TTL 1800.\n" +
        "UPDATE ecommerce.sesiones_activas\n" +
        "USING TTL 1800\n" +
        "SET\n" +
        "    usuario_id = a3b073c4-6449-4bd4-8461-182300000032,\n" +
        "    carrito = '{\"items\": [{\"producto\": \"remera\", \"qty\": 1}, {\"producto\": \"zapatillas\", \"qty\": 2}]}',\n" +
        "    ultima_actividad = toTimestamp(now())\n" +
        "WHERE sesion_id = e97f5b1c-7235-438b-807d-304700000032;";

    public static final String RECUPERAR_CARRITO_SESION = 
        "-- 3.2.c Recuperar el carrito de una sesión activa\n" +
        "SELECT sesion_id, usuario_id, carrito, ultima_actividad\n" +
        "FROM ecommerce.sesiones_activas\n" +
        "WHERE sesion_id = e97f5b1c-7235-438b-807d-304700000032;";

    public static final String VERIFICAR_TTL_SESION = 
        "-- 3.2.d Verificación del TTL automático\n" +
        "-- Comprueba el tiempo de vida restante que Cassandra gestiona.\n" +
        "SELECT sesion_id, TTL(carrito) AS ttl_carrito, TTL(usuario_id) AS ttl_usuario\n" +
        "FROM ecommerce.sesiones_activas\n" +
        "WHERE sesion_id = e97f5b1c-7235-438b-807d-304700000032;";

    // ---------------------------------------------------------
    // 3.3 Métricas de rendimiento de productos
    // ---------------------------------------------------------
    public static final String INSERT_METRICA = 
        "-- 3.3 Inserción de una métrica\n" +
        "INSERT INTO ecommerce.metricas_diarias (\n" +
        "    producto_id, fecha, hora, vistas, clics, conversiones, revenue_por_hora\n" +
        ") VALUES (\n" +
        "    69d2e2a8-c083-b3f0-1236-f4b000000000, '2026-05-31', 17, 10, 3, 1, 89990\n" +
        ");";

    public static final String METRICAS_HORA_POR_HORA = 
        "-- 3.3.a Métricas hora por hora de un producto en un día\n" +
        "SELECT hora, vistas, clics, conversiones, revenue_por_hora\n" +
        "FROM ecommerce.metricas_diarias\n" +
        "WHERE producto_id = 69d2e2a8-c083-b3f0-1236-f4b000000000\n" +
        "  AND fecha = '2026-05-31';";

    public static final String TASA_CONVERSION_SEMANA = 
        "-- 3.3.b Tasa de conversión en la última semana\n" +
        "-- Se deben recuperar las vistas y conversiones para los días deseados.\n" +
        "-- El cálculo (compras/vistas * 100) se procesa en la aplicación.\n" +
        "SELECT fecha, hora, vistas, conversiones\n" +
        "FROM ecommerce.metricas_diarias\n" +
        "WHERE producto_id = 69d2e2a8-c083-b3f0-1236-f4b000000000\n" +
        "  AND fecha >= '2026-05-25' AND fecha <= '2026-05-31' ALLOW FILTERING;";

    public static final String CRECIMIENTO_VISTAS = 
        "-- 3.3.c Crecimiento de vistas: últimas 6h vs 6h anteriores\n" +
        "-- Para un producto, trae las horas requeridas y la app suma y compara.\n" +
        "SELECT hora, vistas\n" +
        "FROM ecommerce.metricas_diarias\n" +
        "WHERE producto_id = 69d2e2a8-c083-b3f0-1236-f4b000000000\n" +
        "  AND fecha = '2026-05-20'\n" +
        "  AND hora >= 6;";

    // ---------------------------------------------------------
    // Legacy Queries (Utilizadas por código existente)
    // ---------------------------------------------------------
    public static final String METRICAS_REPORTE = 
        "SELECT producto_id, fecha, vistas, conversiones FROM ecommerce.metricas_diarias " +
        "WHERE producto_id = %s AND fecha = '%s';";

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
        "INSERT INTO ecommerce.busquedas_por_termino (termino, timestamp, usuario_id, convertido, productos_devueltos) " +
        "VALUES ('%s', toTimestamp(now()), %s, false, %s);";
    public static final String HISTORICO_CONVERSIONES = 
        "SELECT productos_devueltos, convertido FROM ecommerce.busquedas_por_termino WHERE termino = '%s';";

    // OP-4
    public static final String SESIONES_ACTIVAS = 
        "SELECT sesion_id, usuario_id, carrito, ultima_actividad FROM ecommerce.sesiones_activas;";
    public static final String VALIDAR_COMPRA = 
        "SELECT tipo_evento FROM ecommerce.eventos_usuario WHERE usuario_id = %s AND timestamp >= '%s';";
}
