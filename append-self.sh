#!/bin/bash                                                                     

# Appends input to output N number of times

# Aprox. 314MB
N=200000

IN="${1}"
OUT="${2}"

for i in {1..$N}; do
    echo "${IN}"
done | xargs cat > "${OUT}"
