#!/bin/bash
source ./set-env.sh

# Pull privateappstore JAR built locally (sibling target/)
if [[ -e ../target ]]; then
  cp -R ../target/*-SNAPSHOT.jar ./artifacts/ 2>/dev/null || true
fi

# Pull the store-template artifact built locally in a sibling repo.
# store-template is now a Jahia JavaScript module: 'mvn package' produces a .tgz
# (the engine's js: handler installs it; @jahia/cypress env.provision installs
# *-SNAPSHOT.tgz after the .jar modules + the engine).
if [[ -e ../../store-template/target ]]; then
  cp -R ../../store-template/target/*-SNAPSHOT.tgz ./artifacts/ 2>/dev/null || true
fi

version=$(node -p "require('./package.json').devDependencies['@jahia/cypress']")
echo Using @jahia/cypress@$version...
npx --yes --package @jahia/cypress@$version ci.build
