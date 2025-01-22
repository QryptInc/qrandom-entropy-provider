# Use the keyfactor/ejbca-ce image as the base
FROM keyfactor/ejbca-ce:8.3.2

# Define a variable for the base modules path
ARG PROJECT_VERSION=1.0-SNAPSHOT
ARG MODULES_PATH=/opt/keyfactor/appserver/modules/system/layers/base

# Create necessary directories
RUN mkdir -p ${MODULES_PATH}/com/qrypt/qrandom/core/main \
    ${MODULES_PATH}/com/google/code/gson/main

# Copy module.xml and random-provider jar to the randomprovider module directory
COPY qrandom-core-${PROJECT_VERSION}.jar ${MODULES_PATH}/com/qrypt/qrandom/core/main/
COPY module-qrandom-core.xml ${MODULES_PATH}/com/qrypt/qrandom/core/main/module.xml

#COPY bcprov stuff
COPY qrandom-bcprov-1.0-SNAPSHOT.jar ${MODULES_PATH}/org/bouncycastle/bcprov/main/
COPY module-bcprov.xml ${MODULES_PATH}/org/bouncycastle/bcprov/main/module.xml

# Copy missing google gson module dependency
COPY gson-2.8.9.jar ${MODULES_PATH}/com/google/code/gson/main
COPY module-gson.xml ${MODULES_PATH}/com/google/code/gson/main/module.xml

# Define com.qrypt.randomprovider as global-module (don't remember why though??)
RUN sed -i '/<subsystem xmlns="urn:jboss:domain:ee:6\.0">/a\    <global-modules><module name="com.qrypt.qrandom.core" slot="main"/></global-modules>' /opt/keyfactor/appserver/standalone/configuration/standalone.xml

# Append lines to standalone.conf at build time
COPY standalone.conf /opt/keyfactor/appserver/bin/


# Copy random provider initializer and secure random demo to deployments dir
COPY qrandom-ejbca-loader.war /opt/keyfactor/appserver/standalone/deployments
