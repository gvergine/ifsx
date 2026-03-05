# IFSX Web Deployment

Requirements: Tomcat 10+, Java 17+, QNX SDP on server

    export IFSX_DUMPIFS=/opt/qnx800/host/linux/x86_64/usr/bin/dumpifs
    ./gradlew :ifsx-web:war
    cp ifsx-web/build/libs/ifsx-web-0.1.0.war $CATALINA_HOME/webapps/

POST /api/inspect with multipart field "image" to inspect an IFS.
