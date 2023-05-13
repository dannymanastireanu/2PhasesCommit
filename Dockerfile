FROM openjdk:17-oracle
WORKDIR /app
COPY build/libs/2pc-fat.jar /app/my-java-app.jar
EXPOSE 9999

ENTRYPOINT ["java", "-jar", "/app/my-java-app.jar"]
# docker build --build-arg TYPE="coordinator" -t coordinator .
# docker build --build-arg TYPE="node" -t node .

# docker run -p 9999:9999 coordinator