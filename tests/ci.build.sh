#!/bin/bash
source ./set-env.sh

# Pull privateappstore JAR built locally (sibling target/)
if [[ -e ../target ]]; then
  cp -R ../target/*-SNAPSHOT.jar ./artifacts/ 2>/dev/null || true
fi

# Pull store-template JAR if it has been built locally in a sibling repo
if [[ -e ../../store-template/target ]]; then
  cp -R ../../store-template/target/*-SNAPSHOT.jar ./artifacts/ 2>/dev/null || true
fi

version=$(node -p "require('./package.json').devDependencies['@jahia/cypress']")
echo Using @jahia/cypress@$version...
npx --yes --package @jahia/cypress@$version ci.build
