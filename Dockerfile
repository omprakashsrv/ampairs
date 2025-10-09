FROM eclipse-temurin:25-jre
VOLUME /tmp
COPY build/lib/*.jar auth_app.jar
ENTRYPOINT ["java","-jar","/auth_app.jar"]
