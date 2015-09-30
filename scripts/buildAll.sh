#!/bin/sh

rm -f READY/*
mkdir -p READY

outdir="fbreader/app/build/outputs"
version=`cat VERSION | sed "s/ /_/g"`

build=master
git checkout master
./gradlew clean
./gradlew zipAlignFatRelease
mv $outdir/apk/fbreader/app-fat-release.apk READY/FBReaderJ.apk
mv $outdir/mapping/fat/release/mapping.txt mappings/mapping-$version.$build.txt

build=ice-cream-sandwich
git checkout yota2
./gradlew clean
for arch in Arm Armv7a X86 Mips; do
  ./gradlew zipAlign${arch}Release
  lower=`echo $arch | tr '[:upper:]' '[:lower:]'`
  mv $outdir/apk/fbreader/app-$lower-release.apk READY/FBReaderJ_$build-$lower.apk
	mv $outdir/mapping/$lower/release/mapping.txt mappings/mapping-$version.$build-$lower.txt
done
./gradlew zipAlignFatRelease
mv $outdir/apk/fbreader/app-fat-release.apk READY/FBReaderJ_$build.apk
mv $outdir/mapping/fat/release/mapping.txt mappings/mapping-$version.$build.txt

build=nst
git checkout nook
./gradlew clean
./gradlew zipAlignFatRelease
mv fbreader/app/build/outputs/apk/fbreader/app-fat-release.apk READY/FBReaderJ_$build.apk
mv $outdir/mapping/fat/release/mapping.txt mappings/mapping-$version.$build.txt

build=kindlehd
git checkout kindle
./gradlew clean
./gradlew zipAlignFatRelease
mv fbreader/app/build/outputs/apk/fbreader/app-fat-release.apk READY/FBReaderJ_$build.apk
mv $outdir/mapping/fat/release/mapping.txt mappings/mapping-$version.$build.txt
