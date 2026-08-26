FROM eclipse-temurin:17-jdk-alpine

# Instalar curl para healthcheck
RUN apk add --no-cache curl

# Definir diretório de trabalho
WORKDIR /app

# Copiar o Maven wrapper e o pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# 🔥 DAR PERMISSÃO DE EXECUÇÃO PARA O MVNW
RUN chmod +x mvnw

# Baixar dependências (cache para builds futuros)
RUN ./mvnw dependency:go-offline -B

# Copiar o código fonte
COPY src src

# Build da aplicação
RUN ./mvnw package -DskipTests

# Expor a porta
EXPOSE 8181

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:8181/actuator/health || exit 1

# Comando para rodar a aplicação
ENTRYPOINT ["java", "-jar", "target/SistemaIBA-0.0.1-SNAPSHOT.jar"]