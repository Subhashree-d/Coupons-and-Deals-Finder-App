# API Gateway

Current routes for the two services created so far:

- /api/cashback/** -> CASHBACK-SERVICE
- /api/notifications/** -> NOTIFICATION-SERVICE

Gateway port: 8080
Eureka: http://localhost:8761/eureka/

Java: 21
Spring Boot: 4.1.1
Spring Cloud: 2025.1.3

The JWT dependencies and jwt.secret property are included for the Gateway security step.
Replace the placeholder secret with the exact secret used by Auth Service.
