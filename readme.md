
![crownedBank](https://user-images.githubusercontent.com/32541639/176990489-7ec08aab-1f3d-48ec-989e-d7d8122f50f9.png)
[![Build Status](https://jenkins.battleland.eu/buildStatus/icon?job=BattleLand%27s+Crowned+Bank)](https://jenkins.battleland.eu/job/BattleLand's%20Crowned%20Bank/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)


A proper implementation of economy plugin both for networks and standalone servers. 

**Proxy**-**Server** communication is done with plugin messaging,to maintain it's reliability and compatibility with most proxies,
without having to open any new insecure network ports. 

## Users
Download latest build over at [Jenkins](https://jenkins.battleland.eu/job/BattleLand's%20Crowned%20Bank/lastBuild/).

## Developers
### How does it work?
Can communicate via multiple remotes at the same time.
It means you can have *unique currency for one server*, while having *shared currency for whole network*.
### API
Read more on wiki.
#### Maven
```xml
<repository>
  <id>battleland</id>
  <url>https://nexus.battleland.eu/repository/central/</url>
</repository>
```
```xml
<dependency>
  <groupId>eu.battleland.crownedbank</groupId>
  <artifactId>crowned-bank-[platform]</artifactId>
  <version>[version]</version>
</dependency>
```
#### Gradle (Groovy DSL)
```groovy
maven { url = "https://nexus.battleland.eu/repository/central/" }
```
```groovy
compileOnly 'eu.battleland.crownedbank:crowned-bank-[platform]:[version]'
```
#### Gradle (Kotlin DSL)
```kotlin
maven (url = "https://nexus.battleland.eu/repository/central/" )
```
```kotlin
compileOnly("eu.battleland.crownedbank:crowned-bank-[platform]:[version]")
```

## Capabilities
### Supported Plug-Ins
- PlaceholderAPI (spigot)
- Vault (spigot)
### Supported Platforms
- PaperMC (platform `paper`)
- BungeeCord (platform `bungee`)
- Velocity (platform `velocity`)

## Licensing
Project is distrubuted under [Apache License 2.0](https://choosealicense.com/licenses/apache-2.0/#).
