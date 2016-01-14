#!/bin/sh

git checkout master
version=`cat fbreader/app/VERSION | sed 's/ //g'`
git tag $version
#git checkout ice-cream-sandwich
git checkout yota2
version=`cat fbreader/app/VERSION | sed 's/ //g'`
git tag $version-ics
git checkout nook
version=`cat fbreader/app/VERSION | sed 's/ //g'`
git tag $version-nst
git checkout kindle
version=`cat fbreader/app/VERSION | sed 's/ //g'`
git tag $version-kindlefire
git checkout master
