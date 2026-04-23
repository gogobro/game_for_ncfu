#!/usr/bin/env bash
set -e
cd "$(dirname "$0")/dist"
java -jar horror-rooms.jar
