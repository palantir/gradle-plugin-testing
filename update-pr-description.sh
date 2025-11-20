#!/bin/bash
cat > /pr-description.txt << 'EOFPR'
# Migration: AbstractTestingPluginSpec to AbstractTestingPluginTest

A formatted diff between the old and new test can be viewed [here](https://htmlpreview.github.io/?https://raw.githubusercontent.com/palantir/gradle-baseline/develop/test-migration-notes/AbstractTestingPluginTest.html)

EOFPR

cat /repo/test-migration-errors.md >> /pr-description.txt
