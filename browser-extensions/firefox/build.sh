#!/bin/bash

set -e

# This script builds the Firefox extension.

if [ -f firefox.zip ]; then
  rm firefox.zip
fi

cd src

zip -r ../firefox.zip -- *
