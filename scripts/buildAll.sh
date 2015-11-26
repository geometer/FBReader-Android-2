#!/bin/sh

rm -f READY/*
mkdir -p READY

outdir="fbreader/app/build/outputs"
version=`cat VERSION | sed "s/ /_/g"`

build=master
git checkout master
./gradlew clean
./gradlew zipAlignFatRelease -p fbreader/app
mv $outdir/apk/fbreader/app-fat-release.apk READY/FBReaderJ.apk
mv $outdir/mapping/fat/release/mapping.txt mappings/mapping-$version.$build.txt

build=ice-cream-sandwich
git checkout yota2
./gradlew clean
for arch in Arm Armv7a X86 Mips; do
  ./gradlew zipAlign${arch}Release -p fbreader/app
  lower=`echo $arch | tr '[:upper:]' '[:lower:]'`
  mv $outdir/apk/fbreader/app-$lower-release.apk READY/FBReaderJ_$build-$lower.apk
	mv $outdir/mapping/$lower/release/mapping.txt mappings/mapping-$version.$build-$lower.txt
done
./gradlew zipAlignFatRelease -p fbreader/app
mv $outdir/apk/fbreader/app-fat-release.apk READY/FBReaderJ_$build.apk
mv $outdir/mapping/fat/release/mapping.txt mappings/mapping-$version.$build.txt

for plugin in bookshelf comicbook djvu; do
	./gradlew zipAlignRelease -p plugin/$plugin
	mv plugin/$plugin/build/outputs/apk/plugin/$plugin-release.apk READY/$plugin.apk
done
for plugin in pdf; do
	for arch in Fat Arm Armv7a X86 Mips; do
		./gradlew zipAlign${arch}Release -p fbreader/app
		lower=`echo $arch | tr '[:upper:]' '[:lower:]'`
		mv plugin/$plugin/build/outputs/apk/plugin/$plugin-$lower-release.apk READY/$plugin-$lower.apk
		mv plugin/$plugin/build/outputs/mapping/$lower/release/mapping.txt mappings/mapping-$plugin-$version.$build-$lower.txt
	done
done

build=nst
git checkout nook
./gradlew clean
./gradlew zipAlignFatRelease -p fbreader/app
mv fbreader/app/build/outputs/apk/fbreader/app-fat-release.apk READY/FBReaderJ_$build.apk
mv $outdir/mapping/fat/release/mapping.txt mappings/mapping-$version.$build.txt

build=kindlehd
git checkout kindle
./gradlew clean
./gradlew zipAlignFatRelease -p fbreader/app
mv fbreader/app/build/outputs/apk/fbreader/app-fat-release.apk READY/FBReaderJ_$build.apk
mv $outdir/mapping/fat/release/mapping.txt mappings/mapping-$version.$build.txt
