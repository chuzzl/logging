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