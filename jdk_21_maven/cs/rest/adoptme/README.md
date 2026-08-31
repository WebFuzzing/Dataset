# 🐶 AdoptMe – Trabajo Práctico Final (Programación 3 – UADE)

Aplicación desarrollada en **Spring Boot + Neo4j**, que simula un sistema de adopción de perros entre refugios y adoptantes.  
El proyecto incluye lógica de grafos para encontrar caminos entre refugios (BFS) y próximamente otros algoritmos de búsqueda y priorización.

---

## 🚀 Tecnologías utilizadas

- **Java 21**
- **Spring Boot 3.5.x**
- **Neo4j 5.x (base de datos de grafos)**
- **Maven Wrapper (`mvnw`)**
- **Lombok** (para reducir código repetitivo)
- **IntelliJ IDEA / VS Code / Eclipse**

---

## 📂 Estructura del proyecto

```
src/
 └── main/java/com/programacion3/adoptme
     ├── AdoptMApplication.java
     ├── config/
     │    └── DbSeed.java              # Semilla inicial de la base (Shelters, Dogs, Adopters)
     ├── controller/                   # Controladores REST
     ├── domain/                       # Entidades (Shelter, Dog, Adopter)
     ├── dto/                          # Clases para respuestas personalizadas (PathResponse)
     ├── repo/                         # Repositorios Neo4j
     ├── service/                      # Lógica de negocio y algoritmos (BFS, etc)
     └── util/                         # Clases auxiliares (Edge, etc)
```


---

## ⚙️ Requisitos previos

### 1️⃣ Java y Maven
Verificar instalación:
```
java -version
mvn -version
```
Si no los tenés:
- Java 21: https://adoptium.net
- Maven: https://maven.apache.org/download.cgi

---

### 2️⃣ Neo4j Desktop

1. Descargar desde 👉 https://neo4j.com/download/
2. Crear una base local llamada adoptme
3. Configurar:
    - Usuario: neo4j
    - Contraseña: neo4j123
    - Puerto Bolt: 7687
4. Iniciar la base
5. Abrir Neo4j Browser y probar:
   RETURN 1 AS ok;

---

## 🔧 Configuración de Spring Boot

Archivo: src/main/resources/application.yml
```
spring:
neo4j:
uri: bolt://localhost:7687
authentication:
username: neo4j
password: neo4j123
data:
neo4j:
database: neo4j

server:
port: 8080
```
---

## ▶️ Ejecución del proyecto

### En IntelliJ
1. Abrir el proyecto (File → Open → pom.xml)
2. Esperar a que Maven descargue dependencias
3. Verificar que Neo4j esté corriendo
4. Click derecho sobre AdoptMApplication → Run

### Desde terminal
```
./mvnw.cmd spring-boot:run   # Windows

./mvnw spring-boot:run       # Linux/Mac
```
---

## 🌱 Base de datos inicial (Seed)

Al iniciar la aplicación, se ejecuta automáticamente la clase DbSeed.java, que:

- Limpia la base (MATCH (n) DETACH DELETE n)
- Crea refugios (A, B, C) conectados con relaciones :NEAR
- Crea perros (Luna, Toto, Rex) ubicados en refugios
- Crea adoptantes (Carla, Leo)

Podés visualizarlo en Neo4j Browser con:

MATCH (n) RETURN n;

---

## 🧪 Endpoints principales

Método | URL | Descripción
-------|-----|-------------
GET | /ping | Verifica conexión
GET | /shelters | Lista los refugios
GET | /dogs | Lista los perros
GET | /dogs/sort?criteria=priority&algorithm=mergesort | Ordena perros
GET | /adopters | Lista los adoptantes
GET | /graph/reachable?from=A&to=C&method=bfs | Ejecuta BFS/DFS entre refugios
GET | /routes/shortest?from=A&to=C | Calcula camino más corto (Dijkstra)
GET | /routes/tsp/bnb?nodes=A,B,C | Calcula ruta TSP óptima
GET | /network/mst?algorithm=kruskal | Calcula MST (Kruskal/Prim)
GET | /adoptions/greedy?adopterId=P1 | Matching greedy de perros
GET | /adoptions/constraints/backtracking | Asignación por backtracking
GET | /transport/optimal-dp?capacityKg=50 | Optimización de transporte (Knapsack)

Ejemplo de respuesta:

{
"exists": true,
"path": ["A", "B", "C"]
}

---

## 🎨 Frontend Web

El proyecto incluye un **frontend web interactivo** ubicado en la carpeta `/frontend`.

### Características
- ✅ Interfaz moderna y responsiva (sin color violeta)
- ✅ Dashboard con estadísticas en tiempo real
- ✅ Prueba de todos los algoritmos implementados
- ✅ Datos precargados para testing
- ✅ Visualización clara de resultados

### Cómo usar el Frontend

1. **Iniciar el backend** (Spring Boot debe estar corriendo en `http://localhost:8080`)

2. **Abrir el frontend** de alguna de estas formas:

   **Opción A - Servidor HTTP Simple (Python):**
   ```bash
   cd frontend
   python3 -m http.server 3000
   ```
   Luego abrir: http://localhost:3000

   **Opción B - Directamente en el navegador:**
   ```bash
   cd frontend
   # Abrir index.html con tu navegador preferido
   ```

   **Opción C - VS Code Live Server:**
   - Instalar extensión "Live Server"
   - Click derecho en `index.html` → "Open with Live Server"

3. **Explorar las pestañas:**
   - **Dashboard**: Ver refugios, perros y adoptantes
   - **Rutas & Grafos**: BFS, DFS, Dijkstra, TSP
   - **Redes & MST**: Kruskal, Prim
   - **Matching**: Greedy, Backtracking
   - **Ordenamiento**: MergeSort, QuickSort
   - **Transporte**: Knapsack (Programación Dinámica)

### Datos de Prueba Precargados

El sistema incluye datos de prueba en Neo4j:
- **4 Refugios**: A, B, C, H (Hub)
- **6 Perros**: Luna, Toto, Rex, Miranda, Perchita, Lina
- **3 Adoptantes**: Camila (P1), Lucas (P2), Daniela (P3)

Todos los algoritmos pueden probarse directamente desde la interfaz web.

---

## 🧠 Algoritmos implementados

### Algoritmos de Grafos
- **BFS (Breadth-First Search)**: Encuentra caminos entre refugios por búsqueda en anchura
- **DFS (Depth-First Search)**: Encuentra caminos entre refugios por búsqueda en profundidad
- **Dijkstra**: Calcula el camino más corto considerando distancias ponderadas
- **TSP Branch & Bound**: Resuelve el Problema del Viajante para encontrar la ruta óptima

### Algoritmos de Redes
- **Kruskal**: Calcula el Árbol de Expansión Mínima (MST)
- **Prim**: Calcula el MST usando un enfoque alternativo

### Algoritmos de Asignación
- **Greedy**: Selección voraz de perros para un adoptante basado en compatibilidad
- **Backtracking**: Asignación de múltiples perros a múltiples adoptantes con restricciones

### Algoritmos de Ordenamiento
- **MergeSort (TimSort)**: Ordenamiento eficiente de perros por prioridad, edad o peso
- **QuickSort**: Ordenamiento divide y conquista

### Programación Dinámica
- **Knapsack 0/1**: Optimiza el transporte de perros maximizando prioridad dentro de capacidad

---

## 💡 Tips y resolución de errores

- Si la app tira error de conexión a Neo4j:
    - Verificá que esté corriendo en el puerto 7687
    - Revisá usuario y contraseña en application.yml

- Si el seed falla:
    - Borrar manualmente todos los nodos:
      MATCH (n) DETACH DELETE n;
    - Luego reiniciar la app

- Si IntelliJ no reconoce lombok:
    - Instalar el plugin desde Settings → Plugins → Lombok
    - Habilitar Annotation Processing

---

## 🧍‍♀️ Integrantes del grupo

Nombre | Legajo   
--------|----------
Daniela Melian | 198423   
Lucas Vitale | 1192520 
Camila Di Laudo| 1193552

---

## 🧾 Licencia

Proyecto académico – Universidad Argentina de la Empresa (UADE)
Materia: Programación 3 – 2C 2025
