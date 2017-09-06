#!/usr/bin/env bash

REPOSITORY_ID="hummingbird-snapshot"
DEPLOYMENT_REPO="$REPOSITORY_ID::default::https://repo.vaadin.com/nexus/content/repositories/flow-snapshots/"

if [ "$TRAVIS_PULL_REQUEST" == "false" ]
then
    mvn -B -e -V -DskipTests -DskipITs -Djetty.skip -Dgatling.skip -repositoryId=$REPOSITORY_ID -DaltDeploymentRepository=$DEPLOYMENT_REPO deploy --settings settings.xml

    # Copy demo-flow-components to the server
    scp -o "StrictHostKeyChecking no" -P 5177 ./flow-components-parent/demo-flow-components/target/demo-flow-components*.war dev@virtuallypreinstalled.com:tomcat/webapps/
fi
