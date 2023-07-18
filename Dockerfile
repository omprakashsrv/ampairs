FROM openjdk:17-jre-oracle
VOLUME /tmp
COPY build/lib/*.jar auth_app.jar
ENTRYPOINT ["java","-jar","/auth_app.jar"]