#!/bin/sh

rm -f READY/*
mkdir -p READY

outdir="fbreader/app/build/outputs"
version=`cat fbreader/app/VERSION | sed "s/ /_/g"`

build=master
git checkout master
./gradlew clean
./gradlew zipAlignFatRelease -p fbreader/app
mv $outdir/apk/fbreader/app-fat-release.apk READY/FBReaderJ.apk
mv $outdir/mapping/fat/release/mapping.txt mappings/mapping-$version.$build.txt

build=nst
git checkout nook
./gradlew clean
./gradlew zipAlignFatRelease -p fbreader/app
mv fbreader/app/build/outputs/apk/fbreader/app-fat-release.apk READY/FBReaderJ_$build.apk
mv $outdir/mapping/fat/release/mapping.txt mappings/mapping-$version.$build.txt
