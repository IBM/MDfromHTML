#! /bin/bash
docker container ls
docker container stop mdfromhtmlsvcs
docker container rm -f mdfromhtmlsvcs
docker image rm -f mdfromhtmlwebservices
rm -f mdfromhtmlsvcs.tar
rm -f mdfromhtmlwebservices.tar
rm -f mdfromhtmlwebservices.tar.gz
