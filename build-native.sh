#!/bin/bash

cd external/alsa-lib
./gitcompile --prefix=`pwd`/../../alsa-dist
make install
cd ../..
mkdir -p alsa-dist/lib-no-symlink
cp alsa-dist/lib/libasound.so alsa-dist/lib-no-symlink
find alsa-dist/lib-no-symlink

