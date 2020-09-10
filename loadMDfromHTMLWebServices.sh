#! /bin/bash
cp ./mdfromhtmlwebservices.tar.gz ./mdfromhtmlsvcs.tar.gz
gunzip mdfromhtmlsvcs.tar.gz 
docker load -i mdfromhtmlsvcs.tar 
docker run --publish 9080:9080 --detach --name mdfromhtmlsvcs mdfromhtmlwebservices
sleep 10
docker logs mdfromhtmlsvcs
