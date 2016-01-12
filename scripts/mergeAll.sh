#!/bin/sh

git checkout ice-cream-sandwich
git merge master

git checkout yota2
git merge ice-cream-sandwich

git checkout beta
git merge yota2

git checkout kindle
git merge ice-cream-sandwich

git checkout nook
git merge master

git checkout master
