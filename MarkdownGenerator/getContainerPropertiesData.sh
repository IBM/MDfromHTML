#! /bin/bash
if [ ! -d "/store/WAAData" ] 
then
    echo "making the /store/WAAData directory"
    mkdir /store/WAAData 
fi

if [ ! -d "/store/WAAData/mdfromhtmlsvcs" ] 
then
    echo "making the /store/WAAData/mdfromhtmlsvcs directory"
    mkdir /store/WAAData/mdfromhtmlsvcs
fi

docker cp mdfromhtmlsvcs:/opt/ol/wlp/output/defaultServer/properties/ /store/WAAData/mdfromhtmlsvcs/.
docker cp mdfromhtmlsvcs:/opt/ol/wlp/output/defaultServer/data/ /store/WAAData/mdfromhtmlsvcs/.