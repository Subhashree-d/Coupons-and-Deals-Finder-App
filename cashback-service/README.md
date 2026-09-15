# Cashback Service

Included exactly from the supplied code:
- CashbackController
- CashbackTransaction
- TransactionType
- Wallet
- pom.xml

POM includes:
- Spring Web
- Spring Data JPA
- Validation
- RabbitMQ
- Eureka Client
- MySQL
- Swagger/OpenAPI
- Spring Boot Test
- Spring Rabbit Test

The supplied controller references these classes, which were not included in the request:
- com.example.cashbackservice.dto.CashbackTransactionResponse
- com.example.cashbackservice.dto.RedeemCashbackRequest
- com.example.cashbackservice.dto.WalletResponse
- com.example.cashbackservice.service.CashbackService

They are intentionally not invented here. Add your existing DTO and Service classes to their respective packages.
