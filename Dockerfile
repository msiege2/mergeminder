FROM adoptopenjdk/openjdk11:alpine-jre
MAINTAINER git@siegelonline.com

COPY ./target/mergeminder-1.2.0-SNAPSHOT.jar /usr/app/
WORKDIR /usr/app

RUN sh -c 'touch mergeminder-1.2.0-SNAPSHOT.jar'
ENTRYPOINT ["java","-jar","mergeminder-1.2.0-SNAPSHOT.jar"]
