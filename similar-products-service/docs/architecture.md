# Architecture Overview

## Introduction
The Similar Products Service is designed to provide customers with recommendations for products similar to the one they are currently viewing. This service is built using Spring Boot and follows the hexagonal architecture pattern, which promotes separation of concerns and enhances maintainability.

## Hexagonal Architecture
Hexagonal architecture, also known as the Ports and Adapters pattern, allows the application to be decoupled from external systems. This architecture consists of three main components:

1. **Domain Layer**: Contains the core business logic and domain entities.
2. **Application Layer**: Defines the use cases and orchestrates the flow of data between the domain and external systems.
3. **Adapters Layer**: Implements the interfaces defined in the application layer to interact with external systems (e.g., REST APIs, databases).

### Domain Layer
- **Entities**: The core entity in this service is the `Product`, which encapsulates the properties of a product such as `id`, `name`, `description`, and `price`.
- **Exceptions**: The `DomainException` class is used to handle any domain-related errors that may occur during processing.

### Application Layer
- **Inbound Ports**: The `GetSimilarProductsPort` interface defines the contract for retrieving similar products.
- **Outbound Ports**: 
  - `FetchSimilarIdsPort` is responsible for fetching similar product IDs from an external service.
  - `FetchProductPort` is responsible for fetching product details from an external service.
- **Service**: The `GetSimilarProductsService` class implements the logic for retrieving similar products by coordinating between the inbound and outbound ports.

### Adapters Layer
- **Inbound Adapters**: 
  - The `SimilarProductsController` handles incoming REST requests and maps them to the appropriate service calls.
  - DTOs such as `ProductResponse` and `ErrorResponse` are used to structure the data sent to clients.
- **Outbound Adapters**: 
  - `SimilarIdsRestClient` and `ProductRestClient` implement the logic for making REST calls to fetch similar product IDs and product details, respectively.

## Configuration
The application is configured using `application.yml`, which specifies settings such as the server port and any necessary external service endpoints.

## Testing
Unit tests are provided for both the controller and service layers to ensure the correctness of the application logic. The tests are located in the `src/test/java` directory and follow the naming conventions of their respective classes.

## Conclusion
This architecture allows for a clean separation of concerns, making the application easier to maintain and extend. By adhering to the hexagonal architecture pattern, the Similar Products Service is well-equipped to adapt to future changes in requirements or technology.