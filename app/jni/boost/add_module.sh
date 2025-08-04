#!/bin/bash
echo "Input module name"
read in

git submodule add https://github.com/boostorg/$in $in
cd $in
git checkout boost-1.85.0
exit
