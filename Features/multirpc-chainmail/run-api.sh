#!/usr/bin/env sh
# run script for dockerfile
java -D java.util.concurrent.ForkJoinPool.common.parallelism=128 -jar api.jar "$@" 2>&1
