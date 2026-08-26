FROM openjdk:17-jdk-slim

# Instalar curl para healthcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Definir diretório de trabalho
WORKDIR /app

# Copiar o Maven wrapper e o pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

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