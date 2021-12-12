#!/bin/bash

cd /home/martin/graphhopper

su -c ./gh-update.sh martin |& ts '[%Y-%m-%d %H:%M:%S]' >> gh-update.log 2>&1

systemctl reload nginx
