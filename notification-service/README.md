# Notification Service

Package:
com.example.notificationservice

Included files:
- NotificationChannel.java
- NotificationLog.java
- NotificationController.java
- pom.xml

Dependencies included in the POM:
- Spring Web
- Spring Data JPA
- RabbitMQ
- Eureka Client
- MySQL
- SpringDoc OpenAPI / Swagger
- Validation
- Spring Boot Test
- Spring Rabbit Test

Important:
The supplied NotificationController depends on:
- com.example.notificationservice.dto.NotificationResponse
- com.example.notificationservice.service.NotificationService

Those classes were not supplied in the request, so they are intentionally not invented here.
Add your existing DTO and Service classes to their respective packages.
