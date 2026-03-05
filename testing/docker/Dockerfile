# Test ifsx CLI .deb installation on a clean system.
#
# 
# ./gradlew release
# 
# copy the deb here
#
# docker build -t ifsx-test .
#
# docker run -it --rm -v ~/qnx800:/opt/qnx800:ro -v ~/qnx-build/image.ifs:/tmp/image.ifs ifsx-test  /bin/bash


FROM ubuntu:24.04

COPY ifsx_*.deb /tmp/ifsx.deb

RUN apt-get update && \
    dpkg -i /tmp/ifsx.deb || apt-get install -f -y && \
    rm /tmp/ifsx.deb
