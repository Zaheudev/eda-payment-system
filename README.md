# 🏦 Event-Driven Payment Processing System

## 📚 Proiect de Licență - Universitate

**Titlu:** Dezvoltarea unei aplicații Java orientată pe principiile arhitecturilor bazate pe evenimente (EDA)

**Autor:** Zaharia Iulian  
**Status:** 🚧 **Work in Progress** - Proiect în desfășurare  
**Dată Inițiere:** Februarie 2026

---

## 📋 Descriere Proiect

Acest proiect implementează un **sistem de procesare a plăților asincron, bazat pe arhitectura event-driven (EDA)**, utilizând cele mai moderne tehnologii Java pentru a demonstra principiile de design și best practices în construirea sistemelor distribuite și scalabile.

### 🎯 Obiective Principale

- ✅ Implementarea unei arhitecturi event-driven cu **Apache Kafka** pentru event streaming
- ✅ Utilizarea **Apache Avro** pentru schema management și serializare
- ✅ Persistență în **PostgreSQL** cu ORM-ul Spring Data JPA
- ✅ Comunicație asincronă și decuplată între module
- ✅ Demonstrarea best practices în arhitecturi distriburite
- ✅ Scalabilitate și fault-tolerance prin event sourcing

---

## 🛠️ Tehnologii Utilizate

| Componenta | Versiune | Rol |
|------------|----------|-----|
| **Java** | 21 LTS | Limbaj de programare |
| **Spring Boot** | 3.2.2 | Framework principal |
| **Spring Data JPA** | 3.2.2 | ORM și persistență |
| **Spring Kafka** | 3.2.2 | Integrare Kafka |
| **Apache Kafka** | 7.6.0 | Event streaming broker |
| **Confluent Schema Registry** | 7.6.0 | Avro schema management |
| **Apache Avro** | 1.12.1 | Serializare și schema |
| **PostgreSQL** | 16 | Bază de date relațională |
| **Lombok** | 1.18.38 | Reducere boilerplate code |
| **Maven** | 3.13.0+ | Build tool |

---

## 📦 Structura Proiectului

```
demo/
├── pom.xml                          # Parent POM - Maven configuration
├── docker-compose.yml               # Docker stack (Kafka, PostgreSQL, Schema Registry)
│
├── shared/                          # Module - Shared components
│   ├── pom.xml
│   ├── src/main/java/com/zaheudev/shared/
│   │   └── avro/
│   │       ├── PaymentEvent.java    # Generated from Avro schema
│   │       └── PaymentStatus.java   # Enum for payment status
│   └── src/main/resources/avro/
│       └── payment.avsc             # Avro schema definition
│
└── payment-gateway/                 # Spring Boot Application Module
    ├── pom.xml
    ├── src/main/java/com/zaheudev/demo/
    │   ├── DemoApplication.java     # Spring Boot entry point
    │   ├── entity/                  # JPA entities
    │   ├── service/                 # Business logic
    │   ├── kafka/                   # Kafka producers/consumers
    │   └── controller/              # REST endpoints
    └── src/main/resources/
        └── application.properties   # Configuration
```

---

## 🚀 Quick Start

### Prerequisite-uri

- **Java 21 LTS** - [Download](https://www.oracle.com/java/technologies/downloads/#java21)
- **Maven 3.13.0+** - [Download](https://maven.apache.org/download.cgi)
- **Docker & Docker Compose** - [Download](https://www.docker.com/products/docker-desktop)

### 1️⃣ Setup Infrastrukturii (Kafka, PostgreSQL, Schema Registry)

```bash
# Din root-ul proiectului
docker-compose up -d
```

**Servicii disponibile:**
- 🐳 **Kafka**: `localhost:29092`
- 🗄️ **PostgreSQL**: `localhost:5433` (user: postgres, pass: postgres, db: demo)
- 📋 **Schema Registry**: `http://localhost:8082`
- 📊 **Kafka UI**: `http://localhost:8083`

### 2️⃣ Build Proiect

```bash
# Clean + Compile + Install
mvn clean install

# Build JAR-uri
mvn clean package -DskipTests
```

### 3️⃣ Rulare Aplicație

```bash
# Opția 1: Cu Maven
mvn -pl payment-gateway spring-boot:run

# Opția 2: Direct JAR
java -jar payment-gateway/target/payment-gateway-0.0.1-SNAPSHOT.jar
```

Aplicația se va porni pe `http://localhost:8080`

---

## 📖 Documentație Tehnică

### Avro Schema (payment.avsc)

```json
{
  "type": "record",
  "name": "PaymentEvent",
  "namespace": "com.zaheudev.shared.avro",
  "fields": [
    {"name": "paymentId", "type": "string"},
    {"name": "amount", "type": "double"},
    {"name": "currency", "type": "string"},
    {"name": "status", "type": "string"},
    {"name": "timestamp", "type": "long"}
  ]
}
```

### Topicuri Kafka

- **payment-events** - Events de plăți (create, update, completed)
- **payment-commands** - Comenzi de procesare (în desfășurare)
- **payment-errors** - Events de erori și retry logic

### Database Schema (PostgreSQL)

```sql
-- Se vor crea prin Spring Data JPA (Hibernate)
-- Entități: Payment, Transaction, PaymentStatus
-- Persista toate evenimentele din Kafka pentru audit trail
```

---

## 🔧 Configurare Environment

File: `payment-gateway/src/main/resources/application.properties`

```properties
# Spring
spring.application.name=payment-gateway
spring.jpa.hibernate.ddl-auto=update

# Database
spring.datasource.url=jdbc:postgresql://localhost:5433/demo
spring.datasource.username=postgres
spring.datasource.password=postgres

# Kafka
spring.kafka.bootstrap-servers=localhost:29092
spring.kafka.producer.value-serializer=io.confluent.kafka.serializers.KafkaAvroSerializer
spring.kafka.consumer.value-deserializer=io.confluent.kafka.serializers.KafkaAvroDeserializer
spring.kafka.properties.schema.registry.url=http://localhost:8082
```

---

## 📝 Probleme Rezolvate

Proiectul a depășit următoarele provocări tehnologice:

✅ Compatibilitate **Lombok 1.18.38** cu Java 21  
✅ Compatibilitate **Apache Avro 1.12.1** cu Java 21  
✅ Configurare **Maven Compiler 3.13.0** pentru Java 21  
✅ UTF-8 encoding în fișiere de configurare  
✅ Multi-module Maven setup cu dependency management  

Vezi detalii în [PROBLEME_SI_SOLUTII.md](./PROBLEME_SI_SOLUTII.md)

---

## 🧪 Testing

```bash
# Rula test-uri
mvn test

# Coverage report
mvn test jacoco:report
```

---

## 📚 Resurse & Referințe

- [Spring Boot 3.2.2 Documentation](https://spring.io/projects/spring-boot)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Apache Avro Guide](https://avro.apache.org/docs/current/)
- [Event-Driven Architecture Pattern](https://martinfowler.com/articles/201701-event-driven.html)
- [Confluent Schema Registry](https://docs.confluent.io/platform/current/schema-registry/index.html)

---

## 🤝 Contribuții

Acest proiect este în dezvoltare și nu acceptă contribuții externe în acest moment. Este un proiect academic de licență.

---

## 📄 Licență

Acest proiect este licențiat sub **MIT License** - Vezi [LICENSE](./LICENSE) pentru detalii.

---

## 👨‍💻 Autor

**Zaharia Iulian**

- 🔗 GitHub: [@zaheudev](https://github.com/zaheudev)
- 📧 Email: zaharia.iulian@example.com (dacă e cazul)

---

## 📞 Contact & Suport

Pentru întrebări sau sugestii legate de acest proiect:
- 📌 Deschide o issue pe GitHub
- 💬 Contactează autorul direct

---

## 🔔 Notă Importantă

⚠️ **Status:** Acest proiect este **în desfășurare activă** și suferă frecvente modificări. Nu este recomandat pentru producție.

Ramurile principale:
- `main` - Versiunea stabilă (updates lunare)
- `develop` - Versiunea în dezvoltare (updates frecvente)

---

**Ultima actualizare:** Februarie 2026  
**Versiune:** 0.0.1-SNAPSHOT

