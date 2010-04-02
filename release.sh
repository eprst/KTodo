#!/bin/sh
#because I'm lazy to mess with ant!

android_sdk=../android-sdk-linux_86
platform=$android_sdk/platforms/android-2.1
keystore=~/java.keystore
keyalias=droid
keypass=`cat keypass`

mkdir -p tmp
rm -rf tmp/*
cat AndroidManifest.xml | sed -e "s/debuggable=\\\"true/debuggable=\\\"false/" > tmp/AndroidManifest.xml
$platform/tools/aapt package -f -M tmp/AndroidManifest.xml -F tmp/resources.ars -I $platform/android.jar -S res
$android_sdk/tools/apkbuilder tmp/ktodo-unsigned.apk -u -z tmp/resources.ars -f out/production/ktodo/classes.dex
echo jarsigner -keystore $keystore -storepass \"${keypass}\" -keypass \"${keypass}\" -signedjar tmp/ktodo.apk tmp/ktodo-unsigned.apk $keyalias | $SHELL
