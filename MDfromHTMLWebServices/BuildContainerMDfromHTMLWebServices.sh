#! /bin/bash
echo ""
echo ""
echo "This assumes you are in the MDfromHTML/MDfromHTML directory. If so press enter. Otherwise press Ctrl+C"
read
# build MDfromHTLMWebServices
echo "Press enter to build MDfromHTMLWebServices"
read
cp MDfromHTMLWebServices/Dockerfile_MDfromHTMLWebServices Dockerfile
docker build --tag tmp:1.0.8 .
docker run --publish 9081:9081 --detach --name mdfromhtml tmp:1.0.8 >mdfromhtml.container
docker logs "$(cat mdfromhtml.container)"
docker container ls |grep mdfromhtml
docker container ls |grep mdfromhtml | awk '{ print $1 }' > mdfromhtml.containerid
docker commit "$(cat mdfromhtml.container)"  mdfromhtmlwebservices
echo "Saving mdfromhtmlwebservices.tar"
docker save mdfromhtmlwebservices > mdfromhtmlwebservices.tar
docker container stop  "$(cat mdfromhtml.containerid)"
echo "removing container $(cat mdfromhtml.containerid)"
docker container rm -f "$(cat mdfromhtml.containerid)"
docker image ls |grep "mdfromhtmlwebservices"
docker image ls |grep "mdfromhtmlwebservices" | awk '{ print $3 }' > mdfromhtml.imageid
echo "Removing mdfromhtml.imageid $(cat mdfromhtml.imageid)"
docker image rm -f "$(cat mdfromhtml.imageid)"
docker image ls |grep "tmp"
docker image ls |grep "tmp" | awk '{ print $3 }' > tmpmdfromhtml.imageid
echo "Removing tmp using $(cat tmpmdfromhtml.imageid)"
docker image rm -f "$(cat tmpmdfromhtml.imageid)"
# echo "Press enter to continue cleanup"
# read
rm -f mdfromhtml.container
rm -f mdfromhtml.containerid
rm -f mdfromhtml.imageid
rm -f tmpmdfromhtml.imageid
echo "gzipping mdfromhtmlwebservices.tar"
gzip mdfromhtmlwebservices.tar
