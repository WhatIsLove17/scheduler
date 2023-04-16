FROM openjdk:17-oracle
MAINTAINER whatIsLove
COPY target/scheduler.jar scheduler.jar
ENTRYPOINT ["java","-jar","/scheduler.jar"]