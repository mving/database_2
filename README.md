# UI E-Commerce NoSQL

Aplicacion Java Swing para ejecutar consultas piloto sobre tres motores NoSQL usados en el TP:

- Cassandra / Amazon Keyspaces
- MongoDB Atlas
- Neo4j Aura

## Modalidad elegida

La modalidad elegida es una interfaz grafica simple de escritorio. Se eligio esta opcion porque permite ejecutar las consultas del TP sin modificar codigo fuente, cargar parametros de conexion en campos visibles y mostrar resultados o errores de forma directa.

## Requisitos

- JDK 17 o superior
- Maven
- Acceso de red a las bases configuradas

El proyecto incluye las dependencias necesarias en `pom.xml`:

- DataStax Java Driver para Cassandra
- MongoDB Java Driver
- Neo4j Java Driver

## Configuracion

Las conexiones se pueden cargar manualmente desde la interfaz o mediante variables de entorno.

### Cassandra / Amazon Keyspaces

```text
CASSANDRA_HOST
CASSANDRA_PORT
CASSANDRA_DATACENTER
CASSANDRA_KEYSPACE
CASSANDRA_USER
CASSANDRA_PASSWORD
```

Notas:

- Para Amazon Keyspaces se usa TLS y el puerto habitual es `9142`.
- El keyspace puede quedar vacio para probar primero la conexion.

### MongoDB

```text
MONGO_URI
MONGO_DATABASE
MONGO_COLLECTION
```

### Neo4j

```text
NEO4J_URI
NEO4J_DATABASE
NEO4J_USER
NEO4J_PASSWORD
```
## Ejecucion

Desde la raiz del proyecto:

```powershell
mvn exec:java
```

Si se usa el Maven embebido de IntelliJ:

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.1\plugins\maven\lib\maven3\bin\mvn.cmd' exec:java
```

Tambien puede ejecutarse desde IntelliJ corriendo la clase `Main`.

## Operaciones piloto

La aplicacion permite ejecutar estas operaciones sin modificar el codigo fuente:

1. Consultar metricas diarias de un producto en Cassandra.
2. Listar tablas disponibles de un keyspace en Cassandra.
3. Consultar la ficha comercial de un producto por `_id` en MongoDB.
4. Listar documentos de la coleccion de productos en MongoDB.
5. Consultar centralidad o relevancia de un producto en Neo4j con Cypher.

Ademas incluye una pestaña de reporte integrado que combina:

- metricas transaccionales desde Cassandra
- metadatos del catalogo desde MongoDB
- pagerank o relevancia estructural desde Neo4j

## Manejo de errores

La interfaz muestra mensajes descriptivos en la parte inferior de cada pestaña. Los errores se resumen para evitar mostrar stack traces crudos al usuario.

Ejemplos:

```text
Primero conectate a Cassandra
Error MongoDB: ...
Error Cypher: ...
```

## Seguridad

Las credenciales reales no deben subirse al repositorio. Por eso el codigo usa variables de entorno y el `.gitignore` excluye archivos locales como `.env` y `local.properties`.
