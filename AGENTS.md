# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Project Overview

Apache Drools is an open-source business rule management system (BRMS) with:
- **Forward-chaining and backward-chaining inference-based rules engine** for fast and reliable evaluation of business rules
- **DMN (Decision Model and Notation) engine** for decision modeling
- **Complex Event Processing (CEP)** capabilities for real-time event analysis
- **PMML (Predictive Model Markup Language)** support via kie-pmml-trusty modules

This is a multi-module Maven project under the Apache KIE (Knowledge Is Everything) umbrella, targeting Java and the JVM platform.

**Key Technologies:**
- Java (JVM-based)
- Maven (build system)
- Quarkus (for cloud-native extensions)
- ANTLR (for grammar parsing)
- Apache License 2.0

## Project Structure

The repository is organized into multiple Maven modules:

**Core Engine Modules:**
- `drools-core` - Core rule engine implementation
- `drools-base` - Base classes and interfaces
- `drools-compiler` - Rule compilation and parsing
- `drools-kiesession` - KIE session management
- `drools-engine` / `drools-engine-classic` - Engine implementations

**Language & Parsing:**
- `drools-drl` - DRL (Drools Rule Language) parser and AST
- `drools-mvel` - MVEL expression language support
- `drools-drlonyaml-parent` - YAML-based rule definitions

**Decision & Prediction:**
- `kie-dmn` - DMN engine implementation
- `kie-pmml-trusty` - PMML model execution
- `efesto` - Unified execution framework

**Extensions & Integration:**
- `drools-quarkus-extension` - Quarkus integration
- `drools-persistence` - Persistence layer
- `drools-reliability` - Reliability features (H2, Infinispan)
- `drools-ruleunits` - Rule units API

**Tooling & Analysis:**
- `drools-impact-analysis` - Rule impact analysis
- `drools-retediagram` - Rete network visualization
- `drools-verifier` - Rule verification
- `kie-maven-plugin` - Maven plugin for KIE projects

## Building and Running

### Prerequisites
- Java JDK (version specified in build-parent/pom.xml)
- Maven 3.x
- UTF-8 file encoding (`MAVEN_OPTS=-Dfile.encoding=UTF-8`)

### Build Commands

**Full build:**
```bash
make build
# or
mvn clean install
```

**Quick build (skip tests):**
```bash
make build-quickly
# or
mvn clean install -Dquickly
```

**Run tests:**
```bash
make test
# or
mvn clean verify
```

**Quick tests only (unit tests):**
```bash
make quick-test
# or
mvn clean verify -DquickTests
```

**Build with full profile (includes distribution):**
```bash
mvn clean install -Pfull
```

### Locale-Specific Testing

Some tests require `en_US` locale. Use the `test-en` profile on machines with different locales:
```bash
make test -Ptest-en
# or
mvn test -DTestEn
```

### Cross-Repository Builds

Drools is part of a larger ecosystem. Use build-chain for cross-repository builds:

```bash
# Build upstream dependencies
make build-upstream

# Build from a specific PR
make build-pr pr_link=https://github.com/apache/incubator-kie-drools/pull/XXX
```

**Build-chain tool** handles dependencies across related repositories (kogito-runtimes, kogito-apps, kogito-examples).

## Development Conventions

### Communication Style
- Always include a clickable link to every file mentioned in chat, formatted as
  `[`filename OR language.declaration()`](relative/file/path.ext:line)`.
  Line number is required for specific symbol references, optional for plain file links.

### Code Style
- Follow existing code patterns in the module you're working on
- Use PlantUML (`.puml` files) for architectural and design documentation
- IDE plugins available for IntelliJ, Eclipse, and Visual Studio

### Testing
- Write tests for new features and bug fixes
- Tests should pass on `en_US` locale
- Use JUnit 5 (migration from JUnit 4 is ongoing via OpenRewrite)
- Mock external dependencies appropriately

### Module Dependencies
- Respect module boundaries - avoid circular dependencies
- Core modules should not depend on higher-level modules
- Use `kie-api` for public APIs
- Use `kie-internal` for internal cross-module APIs

### Pull Requests
- Target branch: `main` (for Drools 8 / Kogito)
- Link related GitHub issues
- Reference related PRs if applicable
- CI checks must pass (GitHub Actions + Jenkins)
- Use `run_fdb` label for full downstream builds

### Troubleshooting Build Issues

**UnmappableCharacterException:**
```bash
export MAVEN_OPTS=-Dfile.encoding=UTF-8
mvn clean install
```

**Grammar regeneration (if needed):**
```bash
mvn clean install -Pgrammars -Dgenerategrammars=true
```

### Environment Updates

Update Quarkus version:
```bash
make update-quarkus quarkus_version=X.Y.Z
```

Prepare for specific environment:
```bash
make prepare-env environment=<env-name>
```

## Key Concepts

### Rule Engine Architecture
- **Rete Algorithm**: Pattern-matching algorithm for efficient rule evaluation
- **Alpha Network**: Filters individual facts
- **Beta Network**: Joins multiple facts
- **Agenda**: Manages rule activation and execution order

### KIE (Knowledge Is Everything)
- **KieBase**: Repository of compiled rules
- **KieSession**: Runtime instance for rule execution
- **KieContainer**: Manages KieBases and KieSessions

### Rule Units
Modern approach to rule organization with:
- Type-safe data sources
- Declarative rule queries
- REST endpoint generation (with Quarkus)

## Related Resources

- **Website**: https://kie.apache.org/docs/components/drools/
- **Documentation**: https://kie.apache.org/docs/documentation/
- **Issues**: https://github.com/apache/incubator-kie-issues/issues
- **CI**: https://ci-builds.apache.org/job/KIE/
- **Mailing Lists**: dev@kie.apache.org, users@kie.apache.org

## License

Apache License 2.0 - See LICENSE and NOTICE files for details.
