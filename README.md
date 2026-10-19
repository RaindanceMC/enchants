# RaindanceMC/enchants

- **`enchants-module`** — Rain's Flexible Modules — Enchants: Rain's own enchantments — Veinminer, Night Vision, Flight and Double Jump — and the advancements for them.
- **`enchants-standalone`** — Rain's Enchants: Rain's Enchants, as a plugin of its own: the enchants module and the standard wrapper, shaded together.

Part of [RaindanceMC](https://github.com/RaindanceMC); built on [RainsCore](https://github.com/RaindanceMC/RainsCore) and the shared parent in [framework](https://github.com/RaindanceMC/framework). Split out of `rains-flexible-modules` on 16.10.2026, history kept.

## Build

```
mvn install
```

Needs Java 25 and read access to `packages.tstieh.de` (server ids `packages-releases` / `packages-snapshots` in `~/.m2/settings.xml`).

## Plugins

- `enchants-standalone/target/RainsEnchants-0.33.0.jar` — also published as `de.raindancer.modules:enchants-standalone:0.33.0`

## Versions

A module's version is a property in this repository's root `pom.xml`, a plugin's is `plugin.version` in its own pom. CI publishes every version that is not in the registry yet on the next push to master.
