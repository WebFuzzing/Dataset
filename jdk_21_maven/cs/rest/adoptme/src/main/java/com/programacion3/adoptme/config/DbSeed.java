package com.programacion3.adoptme.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.neo4j.core.Neo4jClient;

@Configuration
public class DbSeed {

    @Bean
    CommandLineRunner seed(Neo4jClient neo4j) {
        return args -> seedIfEmpty(neo4j);
    }

    @Transactional
    void seedIfEmpty(Neo4jClient neo4j) {
        // Check if data is complete (15 shelters expected)
        long shelterCount = neo4j.query("MATCH (s:Shelter) RETURN count(s) AS c")
                .fetchAs(Long.class)
                .mappedBy((t,r) -> r.get("c").asLong())
                .one()
                .orElse(0L);

        long dogCount = neo4j.query("MATCH (d:Dog) RETURN count(d) AS c")
                .fetchAs(Long.class)
                .mappedBy((t,r) -> r.get("c").asLong())
                .one()
                .orElse(0L);

        long adopterCount = neo4j.query("MATCH (a:Adopter) RETURN count(a) AS c")
                .fetchAs(Long.class)
                .mappedBy((t,r) -> r.get("c").asLong())
                .one()
                .orElse(0L);

        // Check if database has complete test data
        boolean hasCompleteData = shelterCount == 15 && dogCount == 40 && adopterCount == 15;

        if (hasCompleteData) {
            System.out.println("[SEED] Database already has complete test data:");
            System.out.println("[SEED]   - 15 Shelters ✓");
            System.out.println("[SEED]   - 40 Dogs ✓");
            System.out.println("[SEED]   - 15 Adopters ✓");
            return;
        }

        // Database is incomplete or empty, re-seed everything
        System.out.println("[SEED] Database incomplete or empty. Current state:");
        System.out.println("[SEED]   - Shelters: " + shelterCount + "/15");
        System.out.println("[SEED]   - Dogs: " + dogCount + "/40");
        System.out.println("[SEED]   - Adopters: " + adopterCount + "/15");
        System.out.println("[SEED] Cleaning and re-seeding database...");

        neo4j.query("MATCH (n) DETACH DELETE n").run();

        // ========== SHELTERS (15 total) ==========
        neo4j.query("""
CREATE (a:Shelter {id:'A', name:'Refugio A', capacity:20}),
       (b:Shelter {id:'B', name:'Refugio B', capacity:15}),
       (c:Shelter {id:'C', name:'Refugio C', capacity:10}),
       (h:Shelter {id:'H', name:'Central Hub', capacity:40}),
       (d:Shelter {id:'D', name:'Refugio D', capacity:25}),
       (e:Shelter {id:'E', name:'Refugio E', capacity:18}),
       (f:Shelter {id:'F', name:'Refugio F', capacity:12}),
       (g:Shelter {id:'G', name:'Refugio G', capacity:30}),
       (i:Shelter {id:'I', name:'Refugio I', capacity:22}),
       (j:Shelter {id:'J', name:'Refugio J', capacity:16}),
       (k:Shelter {id:'K', name:'Refugio K', capacity:14}),
       (l:Shelter {id:'L', name:'Refugio L', capacity:28}),
       (m:Shelter {id:'M', name:'Refugio M', capacity:20}),
       (n:Shelter {id:'N', name:'Refugio N', capacity:24}),
       (o:Shelter {id:'O', name:'Refugio O', capacity:35})

""").run();

        // ========== NETWORK CONNECTIONS (35+ edges) ==========
        neo4j.query("""
MATCH (a:Shelter {id:'A'}),(b:Shelter {id:'B'}),(c:Shelter {id:'C'}),(h:Shelter {id:'H'}),
      (d:Shelter {id:'D'}),(e:Shelter {id:'E'}),(f:Shelter {id:'F'}),(g:Shelter {id:'G'}),
      (i:Shelter {id:'I'}),(j:Shelter {id:'J'}),(k:Shelter {id:'K'}),(l:Shelter {id:'L'}),
      (m:Shelter {id:'M'}),(n:Shelter {id:'N'}),(o:Shelter {id:'O'})
CREATE
    // Original connections
    (h)-[:NEAR {distKm:5,  timeMin:10}]->(a),
    (h)-[:NEAR {distKm:7,  timeMin:15}]->(b),
    (h)-[:NEAR {distKm:9,  timeMin:18}]->(c),
    (a)-[:NEAR {distKm:6,  timeMin:12}]->(b),
    (b)-[:NEAR {distKm:8,  timeMin:16}]->(c),
    (a)-[:NEAR {distKm:10, timeMin:20}]->(c),
    (c)-[:NEAR {distKm:14, timeMin:25}]->(h),

    // Hub connections to new shelters
    (h)-[:NEAR {distKm:8,  timeMin:16}]->(d),
    (h)-[:NEAR {distKm:12, timeMin:24}]->(g),
    (h)-[:NEAR {distKm:11, timeMin:22}]->(l),
    (h)-[:NEAR {distKm:15, timeMin:30}]->(o),

    // Regional cluster 1: A-D-E-I
    (a)-[:NEAR {distKm:7,  timeMin:14}]->(d),
    (d)-[:NEAR {distKm:9,  timeMin:18}]->(e),
    (e)-[:NEAR {distKm:6,  timeMin:12}]->(i),
    (i)-[:NEAR {distKm:11, timeMin:22}]->(a),

    // Regional cluster 2: B-F-J-K
    (b)-[:NEAR {distKm:5,  timeMin:10}]->(f),
    (f)-[:NEAR {distKm:8,  timeMin:16}]->(j),
    (j)-[:NEAR {distKm:7,  timeMin:14}]->(k),
    (k)-[:NEAR {distKm:9,  timeMin:18}]->(b),

    // Regional cluster 3: C-G-M-N
    (c)-[:NEAR {distKm:6,  timeMin:12}]->(g),
    (g)-[:NEAR {distKm:10, timeMin:20}]->(m),
    (m)-[:NEAR {distKm:8,  timeMin:16}]->(n),
    (n)-[:NEAR {distKm:12, timeMin:24}]->(c),

    // Cluster 4: L-O
    (l)-[:NEAR {distKm:13, timeMin:26}]->(o),

    // Inter-cluster connections
    (d)-[:NEAR {distKm:15, timeMin:30}]->(g),
    (e)-[:NEAR {distKm:10, timeMin:20}]->(f),
    (f)-[:NEAR {distKm:14, timeMin:28}]->(m),
    (i)-[:NEAR {distKm:16, timeMin:32}]->(j),
    (j)-[:NEAR {distKm:11, timeMin:22}]->(n),
    (k)-[:NEAR {distKm:13, timeMin:26}]->(l),
    (l)-[:NEAR {distKm:9,  timeMin:18}]->(m),
    (n)-[:NEAR {distKm:17, timeMin:34}]->(o),
    (e)-[:NEAR {distKm:18, timeMin:36}]->(o),

    // Additional shortcuts
    (a)-[:NEAR {distKm:20, timeMin:40}]->(g),
    (d)-[:NEAR {distKm:22, timeMin:44}]->(l)
""").run();
        // ========== DOGS (40 total) - varied characteristics for testing ==========
        neo4j.query("""
CREATE
      (d1:Dog {id:'D1', name:'Luna',  size:'SMALL',  weightKg:8,  age:2, energy:'LOW',    goodWithKids:true,  specialNeeds:false, priority:4}),
      (d2:Dog {id:'D2', name:'Toto',   size:'MEDIUM', weightKg:18, age:3, energy:'HIGH',   goodWithKids:true,  specialNeeds:false, priority:6}),
      (d3:Dog {id:'D3', name:'Rex', size:'LARGE',  weightKg:25, age:5, energy:'MEDIUM', goodWithKids:false, specialNeeds:true,  priority:8}),
      (d4:Dog {id:'D4', name:'Miranda',  size:'SMALL',  weightKg:10, age:1, energy:'HIGH',   goodWithKids:true,  specialNeeds:false, priority:5}),
      (d5:Dog {id:'D5', name:'Perchita',  size:'MEDIUM', weightKg:15, age:4, energy:'MEDIUM', goodWithKids:true,  specialNeeds:false, priority:3}),
      (d6:Dog {id:'D6', name:'Lina', size:'LARGE',  weightKg:30, age:6, energy:'LOW',    goodWithKids:false, specialNeeds:true,  priority:7}),

      // New dogs for extensive testing
      (d7:Dog {id:'D7', name:'Buddy', size:'MEDIUM', weightKg:20, age:4, energy:'HIGH',   goodWithKids:true,  specialNeeds:false, priority:6}),
      (d8:Dog {id:'D8', name:'Max', size:'LARGE',  weightKg:28, age:7, energy:'LOW',    goodWithKids:false, specialNeeds:false, priority:5}),
      (d9:Dog {id:'D9', name:'Bella', size:'SMALL',  weightKg:7,  age:1, energy:'MEDIUM', goodWithKids:true,  specialNeeds:false, priority:9}),
      (d10:Dog {id:'D10', name:'Charlie', size:'MEDIUM', weightKg:16, age:3, energy:'HIGH',   goodWithKids:true,  specialNeeds:false, priority:7}),
      (d11:Dog {id:'D11', name:'Daisy', size:'SMALL',  weightKg:9,  age:2, energy:'LOW',    goodWithKids:true,  specialNeeds:true,  priority:8}),
      (d12:Dog {id:'D12', name:'Rocky', size:'LARGE',  weightKg:32, age:6, energy:'MEDIUM', goodWithKids:false, specialNeeds:false, priority:4}),
      (d13:Dog {id:'D13', name:'Lucy', size:'SMALL',  weightKg:6,  age:1, energy:'HIGH',   goodWithKids:true,  specialNeeds:false, priority:10}),
      (d14:Dog {id:'D14', name:'Cooper', size:'MEDIUM', weightKg:19, age:5, energy:'MEDIUM', goodWithKids:true,  specialNeeds:false, priority:5}),
      (d15:Dog {id:'D15', name:'Sadie', size:'LARGE',  weightKg:27, age:4, energy:'LOW',    goodWithKids:false, specialNeeds:true,  priority:6}),
      (d16:Dog {id:'D16', name:'Duke', size:'LARGE',  weightKg:35, age:8, energy:'LOW',    goodWithKids:false, specialNeeds:true,  priority:3}),
      (d17:Dog {id:'D17', name:'Lola', size:'SMALL',  weightKg:8,  age:2, energy:'MEDIUM', goodWithKids:true,  specialNeeds:false, priority:7}),
      (d18:Dog {id:'D18', name:'Bailey', size:'MEDIUM', weightKg:17, age:3, energy:'HIGH',   goodWithKids:true,  specialNeeds:false, priority:8}),
      (d19:Dog {id:'D19', name:'Maggie', size:'SMALL',  weightKg:10, age:6, energy:'LOW',    goodWithKids:true,  specialNeeds:false, priority:4}),
      (d20:Dog {id:'D20', name:'Bear', size:'LARGE',  weightKg:33, age:7, energy:'MEDIUM', goodWithKids:false, specialNeeds:false, priority:5}),
      (d21:Dog {id:'D21', name:'Molly', size:'MEDIUM', weightKg:16, age:2, energy:'HIGH',   goodWithKids:true,  specialNeeds:false, priority:9}),
      (d22:Dog {id:'D22', name:'Jack', size:'SMALL',  weightKg:7,  age:1, energy:'HIGH',   goodWithKids:true,  specialNeeds:false, priority:6}),
      (d23:Dog {id:'D23', name:'Sophie', size:'MEDIUM', weightKg:18, age:4, energy:'MEDIUM', goodWithKids:true,  specialNeeds:true,  priority:7}),
      (d24:Dog {id:'D24', name:'Zeus', size:'LARGE',  weightKg:34, age:5, energy:'HIGH',   goodWithKids:false, specialNeeds:false, priority:8}),
      (d25:Dog {id:'D25', name:'Chloe', size:'SMALL',  weightKg:9,  age:3, energy:'LOW',    goodWithKids:true,  specialNeeds:false, priority:5}),
      (d26:Dog {id:'D26', name:'Bentley', size:'MEDIUM', weightKg:21, age:4, energy:'MEDIUM', goodWithKids:false, specialNeeds:false, priority:6}),
      (d27:Dog {id:'D27', name:'Zoe', size:'SMALL',  weightKg:6,  age:1, energy:'HIGH',   goodWithKids:true,  specialNeeds:false, priority:10}),
      (d28:Dog {id:'D28', name:'Gus', size:'LARGE',  weightKg:29, age:6, energy:'LOW',    goodWithKids:false, specialNeeds:true,  priority:4}),
      (d29:Dog {id:'D29', name:'Penny', size:'MEDIUM', weightKg:17, age:2, energy:'HIGH',   goodWithKids:true,  specialNeeds:false, priority:8}),
      (d30:Dog {id:'D30', name:'Milo', size:'SMALL',  weightKg:8,  age:3, energy:'MEDIUM', goodWithKids:true,  specialNeeds:false, priority:7}),
      (d31:Dog {id:'D31', name:'Ruby', size:'LARGE',  weightKg:26, age:5, energy:'LOW',    goodWithKids:false, specialNeeds:false, priority:5}),
      (d32:Dog {id:'D32', name:'Oscar', size:'MEDIUM', weightKg:19, age:4, energy:'HIGH',   goodWithKids:true,  specialNeeds:true,  priority:9}),
      (d33:Dog {id:'D33', name:'Stella', size:'SMALL',  weightKg:7,  age:2, energy:'LOW',    goodWithKids:true,  specialNeeds:false, priority:6}),
      (d34:Dog {id:'D34', name:'Tucker', size:'LARGE',  weightKg:31, age:7, energy:'MEDIUM', goodWithKids:false, specialNeeds:false, priority:4}),
      (d35:Dog {id:'D35', name:'Rosie', size:'SMALL',  weightKg:9,  age:1, energy:'HIGH',   goodWithKids:true,  specialNeeds:false, priority:8}),
      (d36:Dog {id:'D36', name:'Harley', size:'MEDIUM', weightKg:20, age:5, energy:'MEDIUM', goodWithKids:false, specialNeeds:true,  priority:5}),
      (d37:Dog {id:'D37', name:'Ellie', size:'SMALL',  weightKg:6,  age:2, energy:'LOW',    goodWithKids:true,  specialNeeds:false, priority:7}),
      (d38:Dog {id:'D38', name:'Leo', size:'LARGE',  weightKg:30, age:6, energy:'HIGH',   goodWithKids:false, specialNeeds:false, priority:6}),
      (d39:Dog {id:'D39', name:'Lily', size:'MEDIUM', weightKg:18, age:3, energy:'MEDIUM', goodWithKids:true,  specialNeeds:false, priority:8}),
      (d40:Dog {id:'D40', name:'Ace', size:'LARGE',  weightKg:28, age:4, energy:'LOW',    goodWithKids:false, specialNeeds:true,  priority:7})
""").run();
        
        // ========== SHELTER-DOG ASSIGNMENTS (40 dogs distributed across 15 shelters) ==========
        neo4j.query("""
MATCH
      (a:Shelter {id:'A'}), (b:Shelter {id:'B'}), (c:Shelter {id:'C'}), (h:Shelter {id:'H'}),
      (d:Shelter {id:'D'}), (e:Shelter {id:'E'}), (f:Shelter {id:'F'}), (g:Shelter {id:'G'}),
      (i:Shelter {id:'I'}), (j:Shelter {id:'J'}), (k:Shelter {id:'K'}), (l:Shelter {id:'L'}),
      (m:Shelter {id:'M'}), (n:Shelter {id:'N'}), (o:Shelter {id:'O'}),
      (d1:Dog {id:'D1'}), (d2:Dog {id:'D2'}), (d3:Dog {id:'D3'}), (d4:Dog {id:'D4'}), (d5:Dog {id:'D5'}),
      (d6:Dog {id:'D6'}), (d7:Dog {id:'D7'}), (d8:Dog {id:'D8'}), (d9:Dog {id:'D9'}), (d10:Dog {id:'D10'}),
      (d11:Dog {id:'D11'}), (d12:Dog {id:'D12'}), (d13:Dog {id:'D13'}), (d14:Dog {id:'D14'}), (d15:Dog {id:'D15'}),
      (d16:Dog {id:'D16'}), (d17:Dog {id:'D17'}), (d18:Dog {id:'D18'}), (d19:Dog {id:'D19'}), (d20:Dog {id:'D20'}),
      (d21:Dog {id:'D21'}), (d22:Dog {id:'D22'}), (d23:Dog {id:'D23'}), (d24:Dog {id:'D24'}), (d25:Dog {id:'D25'}),
      (d26:Dog {id:'D26'}), (d27:Dog {id:'D27'}), (d28:Dog {id:'D28'}), (d29:Dog {id:'D29'}), (d30:Dog {id:'D30'}),
      (d31:Dog {id:'D31'}), (d32:Dog {id:'D32'}), (d33:Dog {id:'D33'}), (d34:Dog {id:'D34'}), (d35:Dog {id:'D35'}),
      (d36:Dog {id:'D36'}), (d37:Dog {id:'D37'}), (d38:Dog {id:'D38'}), (d39:Dog {id:'D39'}), (d40:Dog {id:'D40'})
CREATE
      // Shelter A (3 dogs)
      (a)-[:HAS_DOG]->(d1),
      (a)-[:HAS_DOG]->(d2),
      (a)-[:HAS_DOG]->(d9),

      // Shelter B (3 dogs)
      (b)-[:HAS_DOG]->(d3),
      (b)-[:HAS_DOG]->(d10),
      (b)-[:HAS_DOG]->(d17),

      // Shelter C (3 dogs)
      (c)-[:HAS_DOG]->(d4),
      (c)-[:HAS_DOG]->(d11),
      (c)-[:HAS_DOG]->(d18),

      // Shelter H - Central Hub (4 dogs)
      (h)-[:HAS_DOG]->(d5),
      (h)-[:HAS_DOG]->(d6),
      (h)-[:HAS_DOG]->(d7),
      (h)-[:HAS_DOG]->(d8),

      // Shelter D (3 dogs)
      (d)-[:HAS_DOG]->(d12),
      (d)-[:HAS_DOG]->(d13),
      (d)-[:HAS_DOG]->(d19),

      // Shelter E (3 dogs)
      (e)-[:HAS_DOG]->(d14),
      (e)-[:HAS_DOG]->(d15),
      (e)-[:HAS_DOG]->(d20),

      // Shelter F (2 dogs)
      (f)-[:HAS_DOG]->(d16),
      (f)-[:HAS_DOG]->(d21),

      // Shelter G (3 dogs)
      (g)-[:HAS_DOG]->(d22),
      (g)-[:HAS_DOG]->(d23),
      (g)-[:HAS_DOG]->(d24),

      // Shelter I (3 dogs)
      (i)-[:HAS_DOG]->(d25),
      (i)-[:HAS_DOG]->(d26),
      (i)-[:HAS_DOG]->(d27),

      // Shelter J (2 dogs)
      (j)-[:HAS_DOG]->(d28),
      (j)-[:HAS_DOG]->(d29),

      // Shelter K (2 dogs)
      (k)-[:HAS_DOG]->(d30),
      (k)-[:HAS_DOG]->(d31),

      // Shelter L (3 dogs)
      (l)-[:HAS_DOG]->(d32),
      (l)-[:HAS_DOG]->(d33),
      (l)-[:HAS_DOG]->(d34),

      // Shelter M (3 dogs)
      (m)-[:HAS_DOG]->(d35),
      (m)-[:HAS_DOG]->(d36),
      (m)-[:HAS_DOG]->(d37),

      // Shelter N (2 dogs)
      (n)-[:HAS_DOG]->(d38),
      (n)-[:HAS_DOG]->(d39),

      // Shelter O (2 dogs)
      (o)-[:HAS_DOG]->(d40),
      (o)-[:HAS_DOG]->(d1)
""").run();
        
        // ========== ADOPTERS (15 total) - varied profiles for matching algorithms ==========
        neo4j.query("""
CREATE
      (p1:Adopter {id:'P1', name:'Camila', budget:25000, hasYard:true,  hasKids:true,  maxDogs:2}),
      (p2:Adopter {id:'P2', name:'Lucas', budget:18000, hasYard:false, hasKids:false, maxDogs:1}),
      (p3:Adopter {id:'P3', name:'Daniela',  budget:30000, hasYard:true,  hasKids:false, maxDogs:3}),

      // New adopters with varied profiles
      (p4:Adopter {id:'P4', name:'Martin', budget:22000, hasYard:true,  hasKids:true,  maxDogs:2}),
      (p5:Adopter {id:'P5', name:'Sofia', budget:15000, hasYard:false, hasKids:false, maxDogs:1}),
      (p6:Adopter {id:'P6', name:'Roberto', budget:35000, hasYard:true,  hasKids:false, maxDogs:4}),
      (p7:Adopter {id:'P7', name:'Ana', budget:20000, hasYard:true,  hasKids:true,  maxDogs:1}),
      (p8:Adopter {id:'P8', name:'Diego', budget:28000, hasYard:false, hasKids:false, maxDogs:2}),
      (p9:Adopter {id:'P9', name:'Julia', budget:40000, hasYard:true,  hasKids:true,  maxDogs:3}),
      (p10:Adopter {id:'P10', name:'Carlos', budget:12000, hasYard:false, hasKids:false, maxDogs:1}),
      (p11:Adopter {id:'P11', name:'Valeria', budget:32000, hasYard:true,  hasKids:false, maxDogs:5}),
      (p12:Adopter {id:'P12', name:'Pedro', budget:24000, hasYard:true,  hasKids:true,  maxDogs:2}),
      (p13:Adopter {id:'P13', name:'Laura', budget:16000, hasYard:false, hasKids:true,  maxDogs:1}),
      (p14:Adopter {id:'P14', name:'Andres', budget:45000, hasYard:true,  hasKids:false, maxDogs:4}),
      (p15:Adopter {id:'P15', name:'Monica', budget:27000, hasYard:true,  hasKids:true,  maxDogs:3})
""").run();
        
        // ========== INITIAL ADOPTIONS (10 adoptions, 30 dogs still available) ==========
        neo4j.query("""
MATCH
      (p1:Adopter {id:'P1'}), (p2:Adopter {id:'P2'}), (p3:Adopter {id:'P3'}),
      (p4:Adopter {id:'P4'}), (p5:Adopter {id:'P5'}), (p6:Adopter {id:'P6'}),
      (d1:Dog {id:'D1'}), (d2:Dog {id:'D2'}), (d3:Dog {id:'D3'}), (d4:Dog {id:'D4'}), (d5:Dog {id:'D5'}),
      (d13:Dog {id:'D13'}), (d22:Dog {id:'D22'}), (d27:Dog {id:'D27'}), (d35:Dog {id:'D35'}), (d9:Dog {id:'D9'})
CREATE
      // P1: Camila adopts 2 dogs (has kids, needs goodWithKids dogs)
      (p1)-[:ADOPTS]->(d1),
      (p1)-[:ADOPTS]->(d2),

      // P2: Lucas adopts 1 dog (no kids, can handle any)
      (p2)-[:ADOPTS]->(d3),

      // P3: Daniela adopts 2 dogs (no kids, prefers low maintenance)
      (p3)-[:ADOPTS]->(d4),
      (p3)-[:ADOPTS]->(d5),

      // P4: Martin adopts 1 dog (has kids)
      (p4)-[:ADOPTS]->(d9),

      // P5: Sofia adopts 1 dog (single, small space)
      (p5)-[:ADOPTS]->(d13),

      // P6: Roberto adopts 3 dogs (large budget, no kids)
      (p6)-[:ADOPTS]->(d22),
      (p6)-[:ADOPTS]->(d27),
      (p6)-[:ADOPTS]->(d35)
""").run();
        
        System.out.println("[SEED] ===== DATABASE SEEDED SUCCESSFULLY =====");
        System.out.println("[SEED] - 15 Shelters (A-O) with varied capacities");
        System.out.println("[SEED] - 40 Dogs with diverse characteristics for algorithm testing");
        System.out.println("[SEED] - 15 Adopters with varied profiles (budget, yard, kids, maxDogs)");
        System.out.println("[SEED] - 39 NEAR relationships creating a dense shelter network");
        System.out.println("[SEED] - 40 HAS_DOG relationships distributing dogs across shelters");
        System.out.println("[SEED] - 10 initial ADOPTS relationships (30 dogs available for matching)");
        System.out.println("[SEED] =========================================");
    }
}
