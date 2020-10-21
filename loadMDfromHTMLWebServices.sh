#! /bin/bash
cp ./mdfromhtmlwebservices.tar.gz ./mdfromhtmlsvcs.tar.gz
gunzip mdfromhtmlsvcs.tar.gz 
docker load -i mdfromhtmlsvcs.tar 
if [ ! -d "/store/WAALogs" ] 
then
   mkdir /store/WAALogs
fi
if [ ! -d "/store/WAAData/mdfromhtmlsvcs/properties" ] 
then
   docker run --publish 9081:9081 --detach --name mdfromhtmlsvcs mdfromhtmlwebservices
else
   docker run -v /store/WAALogs:/logs -v /store/WAAData/mdfromhtml/properties/:/opt/ol/wlp/output/defaultServer/properties -v /store/WAAData/mdfromhtml/data/:/opt/ol/wlp/output/defaultServer/data --publish 9081:9081 --detach --name mdfromhtmlsvcs mdfromhtmlwebservices
fi
sleep 10
docker logs mdfromhtmlsvcs
