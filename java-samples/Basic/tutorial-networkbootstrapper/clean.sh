#!/bin/sh

echo "Remove the generated node folders."
rm -rf ./PA
rm -rf ./PB
rm -rf ./Notary

echo "Remove the bootstrapping log files."
rm checkpoints*
rm diagnostic*
rm node-admin*
