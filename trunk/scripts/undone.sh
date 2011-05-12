#!/bin/bash
# Undoes the effect of mp3lame renaimg wav files
# so that you can run it again.

for file in *.done
do 
  mv "$file" "${file%.done}"
done