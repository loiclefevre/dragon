# database init, supports Oracle Autonomous Database too
database=oracleautonomousdatabase
spring.datasource.url=jdbc:oracle:thin:@<dbNameLower>_low?TNS_ADMIN=<config.extractedWallet>
spring.datasource.username=<config.dbUserName>
spring.datasource.password=<config.dbUserPassword>
# SQL is written to be idempotent so this is safe
spring.datasource.initialization-mode=always
spring.datasource.schema=classpath*:db/${database}/schema.sql
spring.datasource.data=classpath*:db/${database}/data.sql

# Web
spring.thymeleaf.mode=HTML

# JPA
spring.jpa.hibernate.ddl-auto=none
spring.jpa.open-in-view=false

# Internationalization
spring.messages.basename=messages/messages

# Actuator
management.endpoints.web.exposure.include=*

# Logging
logging.level.org.springframework=INFO
# logging.level.org.springframework.web=DEBUG
# logging.level.org.springframework.context.annotation=TRACE

# Maximum time static resources should be cached
spring.resources.cache.cachecontrol.max-age=12h
