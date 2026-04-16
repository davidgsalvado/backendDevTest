# Similar Products Service

This project implements a Spring Boot application that provides a REST API for fetching similar products based on a given product ID. The application follows the hexagonal architecture pattern, ensuring a clear separation of concerns and maintainability.

## Features

- **REST API**: Exposes an endpoint to retrieve similar products.
- **Hexagonal Architecture**: Implements inbound and outbound ports for better separation of business logic and external dependencies.
- **Docker Support**: Easily deployable using Docker.

## Project Structure

The project is organized as follows:

- **src/main/java/com/ams/similarproducts**: Contains the main application code.
  - **SimilarProductsApplication.java**: Entry point of the Spring Boot application.
  - **config**: Configuration classes for the application.
  - **domain**: Contains the domain model and exceptions.
  - **application**: Business logic and service classes.
  - **adapters**: Inbound and outbound adapters for handling requests and external calls.
  - **ports**: DTOs for inbound and outbound communication.
  - **util**: Utility classes for common functionalities.

- **src/main/resources**: Contains configuration files and OpenAPI specifications.
  - **application.yml**: Configuration settings for the application.
  - **openapi**: OpenAPI specifications for the API.

- **src/test/java/com/ams/similarproducts**: Contains unit tests for the application.

- **Dockerfile**: Instructions for building the Docker image.

- **docker-compose.yml**: Configuration for running the application in a Docker environment.

## Getting Started

To run the application, ensure you have Docker installed and follow these steps:

1. Enable file sharing for the `shared` folder in your Docker settings.
2. Start the necessary infrastructure using Docker Compose:
   ```
   docker-compose up -d simulado influxdb grafana
   ```
3. Check that the mocks are working with a sample request:
   ```
   http://localhost:3001/product/1/similarids
   ```
4. Run the tests:
   ```
   docker-compose run --rm k6 run scripts/test.js
   ```
5. View performance test results at:
   ```
   http://localhost:3000/d/Le2Ku9NMk/k6-performance-test
   ```