# Loggers for SpringBoot

There is a bunch of loggers in here for logging:

- Incoming requests/response for spring-mvc, spring-webflux
- Clients request/response for resttemplate, webclient
- JDBC logger

All loggers (except jdbc) are autoconfigure.
JDBC logging currently enable by changing url 

`jdbc:drivername://1.1.1.1:3333/db`
to
`jdbc:p6spy:drivername://1.1.1.1:3333/db`

## Build requirements

Spring Boot 4.1.2, Spring Framework 7, and Java 17 or newer.
Run `./mvnw verify` with `JAVA_HOME` pointing to a supported JDK.
Servlet integrations use `jakarta.servlet`; tracing uses Micrometer Tracing.
