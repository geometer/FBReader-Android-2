#!/bin/sh

git checkout master
version=`cat fbreader/app/VERSION | sed 's/ //g'`
git tag $version
git checkout nook
version=`cat fbreader/app/VERSION | sed 's/ //g'`
git tag $version-nst
git checkout master
