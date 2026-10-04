# koppainter
A WorldPainter plugin that allows the conversion of Köppen climate maps into Minecraft biomes, intended for use with exported World Climate Lab maps.

## Building and Testing

This project is built using Apache Maven and targets Java 21.

### Build and Package
To build the plugin and create the JAR file in `target/`:

```shell
./mvnw clean package
```

### Run Tests
To execute the unit tests:

```shell
./mvnw test
```

### Test with WorldPainter
To test the plugin directly inside WorldPainter (launches WorldPainter with the plugin on the classpath):

```shell
./mvnw test -P testWithWorldPainter
```

## Color-to-Biome Definitions

Köppainter uses a user-definable properties file to map hex colors from climate map images to Minecraft biomes.

### File Format

The definition file uses standard Java properties syntax:

```properties
<HEX_COLOR> = <BIOME>
```

- **`<HEX_COLOR>`**: 6-digit `RRGGBB` or 8-digit `AARRGGBB` hex color. Leading `#` or `0x` prefixes and casing are flexible (e.g. `FF0000`, `#00FF00`, `0x0000FF`, `ff0000`).
- **`<BIOME>`**: May be specified as:
  - Modern namespaced ID: `minecraft:desert`, `minecraft:windswept_hills`, `minecraft:pale_garden`
  - Short name: `desert`, `plains`, `cherry_grove`
  - Display name: `Desert`, `Windswept Hills`, `Pale Garden`
  - Numerical biome ID: `2` (Desert), `1` (Plains), `245` (Pale Garden)
  - Custom/modded biome: `modid:custom_biome`
- **Comments**: Lines starting with `#`, `!`, or `//` are comments. Inline comments after `#` are also supported.

### Example

```properties
# Standard Köppen climate mappings
960000 = minecraft:jungle
FFCDCD = minecraft:savanna
FFCD00 = minecraft:desert
00FF00 = minecraft:meadow
96FF00 = minecraft:forest
00FFFF = minecraft:taiga
B2B2B2 = minecraft:snowy_plains
0000FF = minecraft:ocean
```

### File Resolution Hierarchy

When loading color definitions, Köppainter checks in the following order:

1. Explicit file path passed via API or settings.
2. System property `-Dkoppainter.biomes.file=<path>`.
3. User directory: `~/.worldpainter/plugins/koppainter/biomes.properties`.
4. Working directory: `./biomes.properties`.
5. Built-in bundled defaults: `com.github.bknackkr.koppainter.biomes.properties`.

