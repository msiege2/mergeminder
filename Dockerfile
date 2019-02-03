FROM java:8-jdk-alpine
MAINTAINER matt.siegel@***REMOVED***
COPY ./target/mergeminder-1.0.0-SNAPSHOT.jar /usr/app/
WORKDIR /usr/app
RUN sh -c 'touch mergeminder-1.0.0-SNAPSHOT.jar'
ENTRYPOINT ["java","-jar","mergeminder-1.0.0-SNAPSHOT.jar"]
