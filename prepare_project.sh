#!/bin/sh

# run this once to checkout and prepare required dependencies

pushd .
cd ..
git clone https://github.com/eprst/android-ColorPickerPreference.git
android update project -p .
popd
