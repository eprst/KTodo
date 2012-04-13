#!/bin/sh

# run this once to checkout and prepare required dependencies

cd ..
git checkout https://github.com/attenzione/android-ColorPickerPreference.git
cp ktodo/android-ColorPickerPreference.iml android-ColorPickerPreference
cp ktodo/android-ColorPickerPreference_project.properties android-ColorPickerPreference/project.properties
