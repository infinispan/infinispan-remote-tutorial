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

### step-1/remote-cache-reading-writing

- Automate downloading and unzipping of Infinispan Server via Maven
- Automate starting and stopping Infinispan Server using Arquillian
- Create a test where the Infinispan Server details are injected
- Connect to the Infinispan Server and obtain the default cache
- Store an entry into the remote cache
- Retrieve an entry from the remote cache
- Print the entry and assert that it contains the expected value
- Execute `mvn verify -Dit.test=RemoteIT` to verify remote cache 
reads and writes as expected
 - You can run this section and all other integration tests by calling `mvn verify`
 
### step-2/remote-expiration

- Storing entries which expire
- Execute `mvn verify -Dit.test=RemoteIT` to verify expiration works 
(`mvn verify` to run all integration tests)

### step-3/remote-concurrency

- Demonstrates how to keep a counter consistent with concurrent modifications
using the remote concurrent APIs
- Execute `mvn verify -Dit.test=RemoteConcurrencyIT` to verify that the 
concurrent APIs work as expected (`mvn verify` to run all integration tests)

### step-4/remote-listeners

- Receive events in the client when entries in the cache change
- Execute `mvn verify -Dit.test=RemoteListenerIT` to see remote events in action 
(`mvn verify` to run all integration tests)

### step-5/remote-filter-listeners

- Receive events in the client only for filtered keys
- Core dependency added to implement cache event filter SPI
- Execute `mvn verify -Dit.test=RemoteListenerFilterIT` to see filtered remote events in action 
(`mvn verify` to run all integration tests)

## Application Directory Layout

    src/                -->
      main/             -->
        java/           -->
        resources/      -->
      test/             --> test code root directory
        java/           --> java test classes root directory
        resources/      --> test resources, e.g. arquillian configuration file

## Contact

For more information on Infinispan please check out http://infinispan.org/

[jdk-download]: http://www.oracle.com/technetwork/articles/javase/index-jsp-138363.html
[git-home]: http://git-scm.com
[git-github]: http://help.github.com/set-up-git-redirect
[maven-download]: http://maven.apache.org/download.html
[infinispan-server-download]: http://infinispan.org/download 
