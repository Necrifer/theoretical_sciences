# libs/

Put these three jars here before running `gradlew build`. They are compiled against only (never bundled), because
the mod's EMI, AE2 and Jade integrations use their APIs:

| Mod | Version used | Get it from |
|---|---|---|
| EMI | `emi-1.1.24+1.21.1+neoforge.jar` | CurseForge / Modrinth |
| Applied Energistics 2 | `appliedenergistics2-19.2.18.jar` | CurseForge / Modrinth |
| Jade | `Jade-1.21.1-NeoForge-15.10.6.jar` | CurseForge / Modrinth |

Newer versions of the same Minecraft version should work too. JEI, KubeJS and Rhino come from Maven
automatically (see `build.gradle`).
