#!/bin/bash
set -e

# TODO rather get it by listing active processes
active=`cat gh.active`

echo "Active: $active"

echo "Downloading"

rm -f tmp/europe-latest.osm.pbf extract.pbf
wget -nv https://download.geofabrik.de/europe-latest.osm.pbf

echo "Extracting"

osmium extract --set-bounds -p limit.geojson tmp/europe-latest.osm.pbf -o extract.pbf
rm tmp/europe-latest.osm.pbf

if [[ "$active" == "a" ]]; then
	next="b"
else
	next="a"
fi

echo "Importing: $next"

java -Xmx32g -jar graphhopper-web-5.0-SNAPSHOT.jar import config-freemap.${next}.yml

echo "Starting: $next"

java -Xmx16g -jar graphhopper-web-5.0-SNAPSHOT.jar server config-freemap.${next}.yml &

echo $next > gh.active

rm -f extract.pbf

# wait one minute for GH to become active
echo Waiting

sleep 60

kill $(ps aux | grep "java.*graphhopper.*config-freemap\\.${active}\\.yml" | awk '{print $2}')

rm -rf /fm/sdata/martin/graph-cache.${active}
mkdir /fm/sdata/martin/graph-cache.${active}

rm -f ./graphhopper.freemap.sk
ln -s ./graphhopper.freemap.sk.b ./graphhopper.freemap.sk

echo Done
