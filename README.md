# Credit Service

Microservicio encargado de la administración de créditos bancarios.

## Tecnologías

- Java 17
- Spring Boot
- Spring WebFlux
- MongoDB
- Eureka Client
- Docker
- Maven
- OpenAPI
- JUnit 5
- Mockito
- JaCoCo

## Funcionalidades

- CRUD de créditos.
- Pago de créditos.
- Consumo de tarjetas de crédito.
- Validación de deuda vencida.
- Consulta de saldos.
- Reglas para créditos personales, empresariales y tarjetas.

## Ejecución local

```bash
mvn clean package
mvn spring-boot:run
```

## Puerto

```
8083
```

## Documentación OpenAPI

```
src/main/resources/openapi/credit-openapi.yml
```

## Docker

```bash
docker build -t credit-service .
```

## Pruebas

```bash
mvn test
```

## Cobertura

```
target/site/jacoco/index.html
```

## Infraestructura

La infraestructura Docker se encuentra en el repositorio **bank-infra**.