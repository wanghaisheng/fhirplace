#!/bin/bash

git pull -f origin master
lein immutant version
lein immutant deploy
