#!/bin/bash
#
# You can set up a custom text frame to record whether 
# you like songs. In this case I like songs with numbers
# 6 - 9. Numbers 5, Y or blank are mediocre.
# Numbers less that 5 are not worth listening to.
#

# Create a playlist from main library, selecting
# songs based on preference in a text frame
# the text frame is a custom text field called Peter,
# accessed as ID3V2TAGTXXXPeter. This must
# be selected as a library field in the customize
# dialog

# This sets up a table of values for selection
# It selects songs with blank, Y or 5 with a 20%
# probability, values from 6 - 9 with 100%
# probability, and other values are not selected

export PLAYLIST_SEARCH_SETUP='
  MySearch[""]=0.2
  MySearch["Y"]=0.2
  MySearch["5"]=0.2
  MySearch["6"]=1
  MySearch["7"]=1
  MySearch["8"]=1
  MySearch["9"]=1
  '

jampal playlist  'MySearch[ID3V2TAGTXXXPeter] > rand()' --sort random \
--announce --startdir 1 --playlistname peter_car 
