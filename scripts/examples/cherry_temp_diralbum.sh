#!/bin/bash

# This creates a playlist directory from a library called
# temp-playlist, which I created by dragging songs from the 
# main library into a temporary library.
# By running this the songs are copied into staging directories
# and organized into directories ready fror burning to CDs
# for playing in the car.

jampal playlist '1' --library temp-playlist --dirchange DIRALBUM --playlistname cherry_cds
