FROM java:openjdk-7u79-jre

MAINTAINER james_naslund@freddiemac.com

RUN mkdir -p /opt/CausalOrder

COPY target/CausalOrder-1.0.3-SNAPSHOT.jar /opt/CausalOrder/CausalOrder.jar
COPY target/CausalOrder-1.0.3-SNAPSHOT-tests.jar /opt/CausalOrder/CausalOrder-tests.jar

ENV CLASSPATH /opt/CausalOrder/CausalOrder.jar:/opt/CausalOrder/CausalOrder-tests.jar

ENTRYPOINT ["java", "org.brann.clock.test.Testbed"]

