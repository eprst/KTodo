#!/bin/sh
#because I'm lazy to mess with ant!

android_sdk=../android-sdk-linux_86
platform=$android_sdk/platforms/android-2.1
keystore=~/java.keystore
keyalias=droid
keypass=`cat keypass`
workdir=tmp

mkdir -p $workdir
rm -rf $workdir/*
cat AndroidManifest.xml | sed -e "s/debuggable=\\\"true/debuggable=\\\"false/" > $workdir/AndroidManifest.xml
$platform/tools/aapt package -f -M $workdir/AndroidManifest.xml -F $workdir/resources.ars -I $platform/android.jar -S res
$android_sdk/tools/apkbuilder $workdir/ktodo-unsigned.apk -u -z $workdir/resources.ars -f out/production/ktodo/classes.dex
echo jarsigner -keystore $keystore -storepass \"${keypass}\" -keypass \"${keypass}\" -signedjar $workdir/ktodo-unaligned.apk $workdir/ktodo-unsigned.apk $keyalias | $SHELL
$android_sdk/tools/zipalign 4 $workdir/ktodo-unaligned.apk $workdir/ktodo.apk
