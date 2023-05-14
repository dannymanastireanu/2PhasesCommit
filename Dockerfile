FROM openjdk:17-oracle
WORKDIR /app
COPY build/libs/2pc-fat.jar /app/my-java-app.jar
EXPOSE 9999

ENTRYPOINT ["java", "-jar", "/app/my-java-app.jar"]
# docker build -t 2pc .

# docker run -p 9999:9999 2pc coordinator
# docker run -p 9999:9999 2pc node