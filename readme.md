
![crownedBank](https://user-images.githubusercontent.com/32541639/176990489-7ec08aab-1f3d-48ec-989e-d7d8122f50f9.png)
[![Build Status](https://jenkins.battleland.eu/buildStatus/icon?job=BattleLand%27s+Crowned+Bank)](https://jenkins.battleland.eu/job/BattleLand's%20Crowned%20Bank/)

A proper implementation of economy plugin both for networks and standalone servers. 

**Proxy**-**Server** communication is done with plugin messaging,to maintain it's reliability and compatibility with most proxies,
without having to open any new insecure network ports. 

## Users

## Developers
### How does it work?
Can communicate via multiple remotes at the same time.
It means you can have *unique currency for one server*, while having *shared currency for whole network*.
### API
Read more on wiki.
#### Maven
```xml
```
#### Gradle (Groovy DSL)
```xml
```
#### Gradle (Kotlin DSL)
```xml
```

## Capabilities
### Supported Plug-Ins
- PlaceholderAPI (spigot)
- Vault (spigot)
### Supported Platforms
- PaperMC
- BungeeCord
- Velocity

## Licensing
Project is distrubuted under [Apache License 2.0](https://choosealicense.com/licenses/apache-2.0/#).
