package org.ecommerce.queries;

public class Neo4jQueries {
    public static final String LISTAR_PRODUCTOS = "MATCH (p:Producto) RETURN p.id, p.nombre, p.pagerank LIMIT 25;";
    public static final String CENTRALIDAD_ADIDAS = 
        "MATCH (p:Producto)\n" +
        "WHERE p.producto_id = \"69d2e2a8c083b3f01236f4b0\" OR p.nombre CONTAINS 'Adidas'\n" +
        "RETURN p.producto_id, p.nombre, p.pagerank;\n";
        
    public static final String REPORTE_PRODUCTO = 
        "MATCH (p:Producto) WHERE p.producto_id = $id OR p.nombre CONTAINS 'Adidas' " +
        "RETURN p.producto_id AS id, p.nombre AS nombre, p.pagerank AS pagerank";

    // OP-1
    public static final String TOP_COCOMPRAS = 
        "MATCH (p:Producto {producto_id: $id})-[:COMPRADO_CON]->(other:Producto) " +
        "RETURN other.producto_id AS id, other.nombre AS nombre ORDER BY other.pagerank DESC LIMIT 5";

    // OP-2
    public static final String COLABORATIVO_HOMEPAGE = 
        "MATCH (u:Usuario {usuario_id: $uid})-[:COMPRO]->(p:Producto)<-[:COMPRO]-()-[:COMPRO]->(rec:Producto) " +
        "WHERE NOT (u)-[:COMPRO]->(rec) RETURN rec.producto_id AS id, rec.nombre AS nombre LIMIT 5";
}
