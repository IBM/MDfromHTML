#! /bin/bash
if [ ! -d "/store/WAAExec" ] 
then
    echo "making the /store/WAAExec directory"
    mkdir /store/WAAExec 
fi
cd /store/WAAExec
docker cp mdfromhtmlsvcs:/opt/ol/wlp/usr/servers/defaultServer/apps/expanded/MDfromHTMLWebServices-1.0.8.war/WEB-INF/lib/ ./Markdown
cd Markdown
ln -s ../../WAAData/mdfromhtmlsvcs/properties properties
ln -s ../../WAAData/mdfromhtmlsvcs/data data
ls -l
