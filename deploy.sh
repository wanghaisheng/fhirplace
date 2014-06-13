#!/bin/bash

git pull -f origin master
lein immutant version | grep -i immutant
if [ $? -ne 0 ]
then
  lein immutant install
fi
lein immutant deploy
