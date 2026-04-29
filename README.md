# Pets

A lightweight companion-pet plugin for **PaperMC 1.21.x** servers. Players can
summon a friendly mob that follows them around, give it a name, and dismiss it
when they're done.

## Features

- `/pet <type>` summons a tameable companion (wolf, cat, parrot, fox, axolotl,
  frog, bee, panda, rabbit, chicken, pig, ocelot).
- `/pet remove`, `/pet name <text>`, `/pet list`, `/pet info` for management.
- One active pet per player; the pet is automatically dismissed on logout.
- Pets follow their owner, teleport when too far away, and can't be hurt by or
  hurt their owner.
- Permissions: `pets.use` (default true), `pets.admin` (default op).

## Build

Requires JDK 21 and Maven 3.6+.

```bash
mvn clean package
```

The plugin jar is produced at `target/Pets-1.0.0.jar`. Drop it into your
server's `plugins/` directory and restart.

## Compatibility

The plugin targets `api-version: '1.21'` and is built against Paper API
`1.21.4-R0.1-SNAPSHOT`. It should run on any Paper 1.21.x server.
