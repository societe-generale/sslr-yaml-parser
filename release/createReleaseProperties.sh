#!/bin/bash

set -ve

echo "scm.url=scm:git:git@github.com:societe-generale/sslr-yaml-parser.git" >> release.properties
echo "scm.tag=$TRAVIS_TAG" >> release.properties

echo "******release.properties content*******"
cat release.properties

echo "******directory content*******"
pwd
ls -al
