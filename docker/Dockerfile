FROM openjdk:7-jdk-alpine AS build-environment

RUN apk add make

# For the build environment, copy only util and Makefile
# so that we can re-use the container in which
# dependencies have been downloaded:
COPY util /opt/src/PicGrid/util/
COPY Makefile /opt/src/PicGrid/Makefile

# Ignore complaints of src/*/jave etc not found.
# That's for other targets that we're not building.
RUN make -C /opt/src/PicGrid/ download-external-libs


FROM build-environment AS build

# This should probably be a mounted volume,
# but I don't know if there's any way to do that entirely within the Dockerfile,
# so WHATEVER JUST COPY IT IN:
COPY . /opt/src/PicGrid/

RUN make -C /opt/src/PicGrid PicGrid.jar PicGrid.jar.urn



FROM openjdk:7-jdk-alpine AS picgrid

RUN apk --update add imagemagick
COPY --from=build /opt/src/PicGrid/PicGrid.jar /opt/bin/PicGrid.jar
ENTRYPOINT ["java", "-jar", "/opt/bin/PicGrid.jar"]
