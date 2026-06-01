package org.ecommerce.queries;

public class MongoQueries {
    public static final String PRIMEROS_PRODUCTOS = "{}";
    public static final String FICHA_ADIDAS = "{ \"_id\": ObjectId(\"69d2e2a8c083b3f01236f4b0\") }";
    
    // Uses String.format for productId
    public static final String PRODUCTO_REPORTE = "{ \"_id\": ObjectId(\"%s\") }";
}
