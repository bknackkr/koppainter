# koppainter
A WorldPainter plugin that allows the conversion of climate maps into Minecraft biomes, intended for use with exported World Climate Lab maps.

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
To test the plugin directly inside WorldPainter (launches WorldPainter with the plugin on the classpath). These scripts automatically locate your WorldPainter installation, install the required commercial JIDE GUI libraries (`jide-common` and `jide-dock`) into your local Maven cache (`~/.m2/repository`), and launch WorldPainter:

On Windows (PowerShell / CMD):
```powershell
./test-with-worldpainter.ps1
# or in Command Prompt:
test-with-worldpainter.cmd
```

On Linux / macOS:
```shell
./test-with-worldpainter.sh
```

Or manually with Maven (requires JIDE libraries to be pre-installed in your local repository):
```shell
./mvnw test -P testWithWorldPainter
```
## Usage

This plugin is intended to be used with Köppen climate maps exported from [World Climate Lab](https://store.steampowered.com/app/4875150/World_Climate_Lab/), but can be used with any image.

The color definitions are entirely user-definable and are stored in a properties file, so you can draw custom maps with arbitrary colors and it will still work.

![Color Map Example](img/ColorMapExample.png)



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

## AI transparency statement
I believe it is important for any user of this program to be aware that a non-trivial amount of code in this repository was written by generative AI. It is also important to clarify that code, and only code, was generated this way. All statements, opinions, and images are my own and I am opposed to the use of generative AI for the purposes of image generation and creative/informative writing. Yes, I know I'm a hypocrite. Thank you for reading.

## Acknowledgements

This project is a plugin for [WorldPainter](https://www.worldpainter.net/), an interactive map generator for Minecraft developed by Pepijn Schmitz ([pepsoft.org](https://www.pepsoft.org/)).

WorldPainter is free and open-source software licensed under the [GNU General Public License, Version 3](https://www.gnu.org/licenses/gpl-3.0.html) (GPLv3). Its source code and license details can be found on [GitHub](https://github.com/Captain-Chaos/WorldPainter).
