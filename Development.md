# Development Guide

## Project Structure

```
src/main/kotlin/de/cadeda/mcp/
├── Main.kt                    # Application entry point
├── server/
│   └── tools/                # Individual tool implementations
├── adb/                      # ADB interaction layer
└── model/
    └── uihierarchy/          # UI hierarchy types and error classes
```

## Building from Source

```bash
# Clean and build
./gradlew clean build

# Run tests
./gradlew test

# Create distribution JAR
./gradlew jar
```

## Version Management

The project version is centrally managed in `build.gradle.kts`. The build system automatically:
- Generates `AppVersion.kt` with the current version
- Creates a version-less JAR file (`android-mcp.jar`) for deployment stability
- Embeds version information in the JAR manifest for debugging
- Updates version references throughout the codebase

To update the version, modify only the `version` property in `build.gradle.kts`.

**JAR Naming**: The JAR uses a version-less name (`android-mcp.jar`) following best practices for standalone applications. This ensures configuration files and deployment scripts remain stable across version updates. The actual version is available via the JAR manifest and runtime version reporting.

## Development Commands

```bash
# Build and run the Kotlin server
./gradlew run

# Build only
./gradlew build

# Run tests
./gradlew test

# Run the JAR directly
java -jar build/libs/android-mcp.jar

# Check version
java -jar build/libs/android-mcp.jar --version

# Show help
java -jar build/libs/android-mcp.jar --help
```

## Testing

### Integration Testing

Run comprehensive integration tests:

```bash
# Test all MCP tools with a connected Android device
./test-integration.sh

# Test specific functionality
./test-kotlin-server.sh
./test-view-hierarchy.sh
```

### Unit Testing

```bash
# Run all unit tests
./gradlew test

# Run tests with coverage
./gradlew test jacocoTestReport
```

## Code Quality

The project uses detekt for static code analysis:

```bash
# Run code quality checks
./gradlew detekt

# Auto-fix style issues
./gradlew detektFormat
```

## Release Process

1. Update version in `build.gradle.kts`
2. Commit changes and create a git tag:
   ```bash
   git tag v1.2.0
   git push origin v1.2.0
   ```
3. GitHub Actions automatically:
   - Builds the JAR file
   - Runs tests on Java 17 and 21
   - Creates a GitHub release
   - Uploads `android-mcp.jar` with checksums
   - Generates changelog from commit messages

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes in the Kotlin source files
4. Test with `./gradlew test`
5. Build with `./gradlew build`
6. Submit a pull request

### Development Guidelines

- Follow Kotlin coding conventions
- Write unit tests for new functionality
- Update documentation for API changes
- Run integration tests before submitting PRs
- Ensure detekt checks pass

## Architecture Notes

### Clean Architecture

The project follows SOLID principles with separated concerns:
- **MCP Server**: Handles protocol communication
- **ADB Manager**: Facade coordinating specialized components
- **Tool Classes**: Individual tool implementations following consistent patterns
- **Model Classes**: Type-safe data structures with serialization support

### Performance Optimizations

- **Coroutines**: Non-blocking ADB operations
- **Caching**: Device list and ADB path caching with TTL
- **Connection Pooling**: Persistent device connections
- **Parallel Processing**: Concurrent execution of independent commands

### Error Handling

Uses sealed classes for type-safe error handling with specific exception types:
- `LayoutInspectorError.AdbNotFound`
- `LayoutInspectorError.DeviceNotFound`
- `LayoutInspectorError.UiDumpFailed`
- `LayoutInspectorError.ConnectionClosed`
- And more specific error types for better debugging

## Troubleshooting Development Issues

### Build Issues

1. **Java Version**: Ensure Java 17+ is installed
2. **Gradle Wrapper**: Use `./gradlew` instead of system gradle
3. **Clean Build**: Run `./gradlew clean build` for fresh compilation

### ADB Issues

- The server automatically searches for ADB in common locations
- If ADB is not found, install Android SDK platform-tools
- Verify USB debugging is enabled on your Android device
- Check device authorization status with `adb devices`

### Debug Mode

Add logging to see detailed ADB command execution:

```kotlin
// In AdbManager.kt, uncomment debug statements
System.err.println("Executing: ${command.joinToString(" ")}")
```