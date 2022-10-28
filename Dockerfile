FROM maven:eclipse-temurin AS build

WORKDIR /app

COPY . ./

RUN mvn -DskipTests=true package

FROM eclipse-temurin:17-focal

RUN apt-get update && apt-get upgrade -y && \
    apt-get purge curl libbinutils libctf0 libctf-nobfd0 libncurses6 -y && \
    apt-get autoremove -y && apt-get clean

COPY --from=build /app/target/report-hub.jar /app/

WORKDIR /app
USER 1001

CMD ["java", "-jar", "report-hub.jar"]
