#!/bin/bash

echo Checks for any scripts with any cr embedded

grep -l $'\r' *.sh *.profile examples/*.sh *.conf



