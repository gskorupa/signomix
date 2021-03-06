#
# Copyright (C) Grzegorz Skorupa 2018.
# Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
#
# This is Dockerfile template for Cricket based services
# 
# The file is used by Ant build to cereate Dockerfile.
# 1.2.4 parameter will be replaced automatically with the actual project version configured in build.xml
#

FROM azul/zulu-openjdk-alpine:13

WORKDIR /usr/signomix
RUN mkdir /usr/signomix/dbdata
RUN mkdir /usr/signomix/dbdata/db
RUN mkdir /usr/signomix/dbdata/logs
RUN mkdir /usr/signomix/dbdata/files
RUN mkdir /usr/signomix/dbdata/assets
RUN mkdir /usr/signomix/dbdata/backup
#VOLUME /usr/signomix/dbdata

COPY target/signomix-ce.jar /usr/signomix/
COPY src/main/resources/settings.json /usr/signomix/config/
COPY src/main/resources/device-script-template.js /usr/signomix/config/
COPY src/main/resources/payload-decoder-envelope.js /usr/signomix/config/
COPY src/main/www /usr/signomix/www

#CMD ["java", "-Xms100m",  "-Xmx1g", "--illegal-access=deny", "-cp", "signomix.jar:jboss-client.jar:javax.activation-1.2.0.jar:jaxb-api-2.4.0.jar:jaxb-core-2.3.0.1.jar:jaxb-impl-2.4.0.jar", "org.cricketmsf.Runner", "-r", "-c", "config/settings.json"]
CMD ["java", "-Xms100m",  "-Xmx1g", "-jar", "./signomix-ce.jar", "-r", "-c", "config/settings.json"]
