# Infinispan Remote Tutorial

## Overview

This is a tutorial which explains how to access and use Infinispan Servers 
using Remote APIs offered from within your own application.

Each tagged commit is a separate lesson teaching a single aspect of Infinispan.

## Prerequisites

### Git

- A good place to learn about setting up git is [here][git-github]
- Git [home][git-home] (download, documentation)

### JDK

- Get a [JDK][jdk-download]. You will need version 1.7 or higher

### Maven

- Get [Maven][maven-download]. You will need version 3.2 or higher

## Commits / Tutorial Outline

You can check out any point of the tutorial using

    git checkout step-?

To see the changes between any two lessons use the git diff command.

    git diff step-?..step-?

### step-0/remote-cache-manager

- Setup a basic Maven project with dependencies
- Create a simple class which connects to an Infinispan Server
- To run tutorial section:
 - Download [Infinispan Server][infinispan-server-download] and unzip locally
 - Start server running `bin/standalone.sh` or `bin/standalone.bat`
 - Run `mvn compile exec:java` to execute the application 

## Application Directory Layout

    src/                -->
      main/             -->
        java/           -->
        resources/      -->

## Contact

For more information on Infinispan please check out http://infinispan.org/

[jdk-download]: http://www.oracle.com/technetwork/articles/javase/index-jsp-138363.html
[git-home]: http://git-scm.com
[git-github]: http://help.github.com/set-up-git-redirect
[maven-download]: http://maven.apache.org/download.html
[infinispan-server-download]: http://infinispan.org/download 
