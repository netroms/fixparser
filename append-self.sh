#!/bin/bash                                                                     

# Appends input to output 200000 number of times

IN="${1}"
OUT="${2}"

for i in {1..200000}; do
    echo "${IN}"
done | xargs cat > "${OUT}"
