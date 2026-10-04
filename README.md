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
