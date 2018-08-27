FROM openjdk:7-jdk-alpine AS build-environment

RUN apk add make

COPY . /opt/src/PicGrid/

RUN make -C /opt/src/PicGrid/ download-external-libs



FROM build-environment AS build

RUN make -C /opt/src/PicGrid PicGrid.jar PicGrid.jar.urn



FROM openjdk:7-jdk-alpine AS picgrid

COPY --from=build /opt/src/PicGrid/PicGrid.jar /opt/bin/PicGrid.jar
ENTRYPOINT ["java", "/opt/bin/PicGrid.jar"]
