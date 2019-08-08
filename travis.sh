#!/bin/bash

set -euo pipefail

function installTravisTools {
  mkdir -p ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v56 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}
installTravisTools

. ~/.local/bin/installMaven35

if [ "${TRAVIS_BRANCH}" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
  echo '======== Build and analyze master'
  # Analyze with SNAPSHOT version as long as SQ does not correctly handle
  # purge of release data
  CURRENT_VERSION=`maven_expression "project.version"`

  . set_maven_build_version $TRAVIS_BUILD_NUMBER

  mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent verify sonar:sonar \
      -Pcoverage \
      -Dmaven.test.redirectTestOutputToFile=false \
      -Dsonar.host.url=$SONAR_HOST_URL \
      -Dsonar.login=$SONAR_TOKEN \
      -Dsonar.projectVersion=$CURRENT_VERSION \
      -Dsonar.analysis.buildNumber=$TRAVIS_BUILD_NUMBER \
      -Dsonar.analysis.pipeline=$TRAVIS_BUILD_NUMBER \
      -Dsonar.analysis.sha1=$TRAVIS_COMMIT \
      -Dsonar.analysis.repository=$TRAVIS_REPO_SLUG \
      -Dsonar.projectKey=societe-generale_sslr-yaml-parser \
      -B -v -e $*

elif [ "$TRAVIS_PULL_REQUEST" != "false" ] && [ -n "${GITHUB_TOKEN:-}" ]; then
  echo '======= Build and analyze pull request'

  # Do not deploy a SNAPSHOT version but the release version related to this build and PR
  . set_maven_build_version $TRAVIS_BUILD_NUMBER

  mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent verify sonar:sonar \
      -Pcoverage \
      -Dmaven.test.redirectTestOutputToFile=false \
      -Dsonar.host.url=$SONAR_HOST_URL \
      -Dsonar.login=$SONAR_TOKEN \
      -Dsonar.analysis.buildNumber=$TRAVIS_BUILD_NUMBER \
      -Dsonar.analysis.pipeline=$TRAVIS_BUILD_NUMBER \
      -Dsonar.analysis.sha1=$TRAVIS_COMMIT \
      -Dsonar.analysis.repository=$TRAVIS_REPO_SLUG \
      -Dsonar.analysis.prNumber=$TRAVIS_PULL_REQUEST \
      -Dsonar.pullrequest.branch=$TRAVIS_PULL_REQUEST_BRANCH \
      -Dsonar.pullrequest.base=$TRAVIS_BRANCH \
      -Dsonar.pullrequest.key=$TRAVIS_PULL_REQUEST \
      -Dsonar.pullrequest.provider=github \
      -Dsonar.pullrequest.github.repository=$TRAVIS_REPO_SLUG \
      -Dsonar.projectKey=societe-generale_sslr-yaml-parser \
      -B -v -e $*
else
  echo '======= Build, no analysis, no deploy'

  # No need for Maven phase "install" as the generated JAR files do not need to be installed
  # in Maven local repository. Phase "verify" is enough.

  mvn verify \
      -Dmaven.test.redirectTestOutputToFile=false \
      -B -e -V $*
fi
