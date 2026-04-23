# ── Stage 1: Build React App ──────────────────────────────────────────────────
FROM node:20-alpine AS frontend-build
WORKDIR /app
COPY frontend/package.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# ── Stage 2: Build Java WAR ───────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-19 AS backend-build
WORKDIR /app
COPY pom.xml ./
COPY src/ ./src/
# Copy built React app into the webapp directory so it gets packaged into the WAR
COPY --from=frontend-build /app/build/ ./src/main/webapp/react/
RUN mvn package -DskipTests

# ── Stage 3: Run on Tomcat ────────────────────────────────────────────────────
FROM tomcat:10.1-jre19-temurin
# Remove default ROOT app
RUN rm -rf /usr/local/tomcat/webapps/ROOT

# Copy WAR
COPY --from=backend-build /app/target/is-project-1.0-SNAPSHOT.war \
     /usr/local/tomcat/webapps/is-project.war

EXPOSE 8080
CMD ["catalina.sh", "run"]
