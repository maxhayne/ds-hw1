#!/bin/bash

VAR1="session_"

for i in {1..50}
do
   tmux new -s ${VAR1}${i} & 
done
