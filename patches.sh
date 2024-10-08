#!/bin/bash

if [ "$1" == "--apply-patch" ]
   then
	if [ -e "s25client/patched" ]
	   then
	   	echo "Already Patched!"
	   	exit
	fi
	patch -s -p0 < patch/s25client.patch
	echo "patched" > s25client/patched
	echo "patched"
	exit
fi
if [ "$1" == "--create-patch" ]
   then
   	if [ ! -e "s25client_patched" ]
   	   then
   	   	echo "Place your patched version (s25client_patched) and an unpatched original version (s25client) in current folder and run again to create patch file."
   	   	exit
   	   	
   	   else
   		diff -ruN --binary -x '.*' -x 'patched'  s25client/ s25client_patched/ > s25client.patch
   		echo "Saved patch in current folder as 's25client.patch'"
   		exit
   	fi
fi

echo "Exapmle:"
echo "./patches.sh --apply-patch    To apply patch/s25client.patch to s25client"
echo "./patches.sh --create-patch   To create patch file out of s25client_patched and s25client(unchanged/original)"
