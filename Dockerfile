# ===============================
# 🏗️ BUILD STAGE
# ===============================
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app
ENV MAVEN_CONFIG=""

# Copier wrapper
COPY mvnw ./
COPY .mvn .mvn

# Donner les droits
RUN chmod +x mvnw
RUN sed -i 's/\r$//' mvnw
# Copier pom
COPY pom.xml .

# Télécharger dépendances
RUN ./mvnw -B dependency:go-offline

# Copier code
COPY src ./src

# Build
RUN ./mvnw clean package -DskipTests


# ===============================
#  RUNTIME
# ===============================
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update && apt-get install -y \
    curl \
    jq \
    postgresql-client \
    net-tools \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]