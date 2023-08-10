
# Summary

Certain circumstances can cause Zinc to fail to delete stale `.class` files before incremental compilation which can break the compilation.
This repository illustrates one such case, namely running compilation with one version of the JVM, then moving a class from one package to another, and finally running compilation with a different version of the JVM.
The moved class file from the previous package will still exist which can cause compilation to fail (eg. in this case from having wildcard imports which will then import both versions of the moved class).

# Instructions to reproduce

1. Clone this repository

```bash
git clone git@github.com:jackkoenig/zinc-bug-example.git
cd zinc-bug-example
```

2. Install two different major versions of the JVM

It does not matter which JVM distribution, just that you have 2 different major versions so that they have different versions of System property `java.class.version`, see commentary below.


For example, here is a quick install of GraalVM Java11 and Java17 variants for Linux x86_64.

```bash
mkdir java11
wget -q -O - https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.3.3/graalvm-ce-java11-linux-amd64-22.3.3.tar.gz | tar zx -C java11 --strip-components 1

mkdir java17
wget -q -O - https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.3.3/graalvm-ce-java17-linux-amd64-22.3.3.tar.gz | tar zx -C java17 --strip-components 1
```

Similarly for MacOS x86_64:
```bash
mkdir java11
wget -q -O - https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.3.3/graalvm-ce-java11-darwin-amd64-22.3.3.tar.gz | tar zx -C java11 --strip-components 1

mkdir java17
wget -q -O - https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-22.3.3/graalvm-ce-java17-darwin-amd64-22.3.3.tar.gz | tar zx -C java17 --strip-components 1
```

3. Run compilation using Java 11

Linux:

```bash
JAVA_HOME=$PWD/java11 PATH=$JAVA_HOME/bin:$PATH sbt compile
```

MacOS:

```bash
JAVA_HOME=$PWD/java11/Contents/Home PATH=$JAVA_HOME/bin:$PATH sbt compile
```

4. Apply the change which moves a class to another package
```bash
git apply moveB.diff
```

5. Run compilation using Java 17

Linux:

```bash
JAVA_HOME=$PWD/java17 PATH=$JAVA_HOME/bin:$PATH sbt compile
```

MacOS:

```bash
JAVA_HOME=$PWD/java17/Contents/Home PATH=$JAVA_HOME/bin:$PATH sbt compile
```

Now you will see the error
```
[error] .../src/main/scala/example/Main.scala:8:11: reference to B is ambiguous;
[error] it is imported twice in the same scope by
[error] import example.bar._
[error] and import example.foo._
[error]   println(B("world"))
[error]           ^
[error] one error found
```

You can see that this error should not happen by deleting the classes directory and rerunning.

Linux:

```bash
rm -rf target/scala-2.13/classes
JAVA_HOME=$PWD/java17 PATH=$JAVA_HOME/bin:$PATH sbt compile
```

MacOS:

```bash
rm -rf target/scala-2.13/classes
JAVA_HOME=$PWD/java17/Contents/Home PATH=$JAVA_HOME/bin:$PATH sbt compile
```

# Commentary

SBT uses `java.class.version`[1] as part of the Setup.extra[2] which causes `Analysis.empty` to be used as the analysis in incremental compilation[3].
This prevents the stale `example/foo/B.class` from being deleted, thus it is included on the classpath for the incremental compilation which fails.

- [1] https://github.com/sbt/sbt/blob/e3b7870b2d98e26e38af82f5dd5e6b2b3f77b219/main/src/main/scala/sbt/Defaults.scala#L192
- [2] https://github.com/sbt/zinc/blob/ae52a6839e7fb96f033d4ab9962c0dfffe9628ec/internal/compiler-interface/src/main/contraband-java/xsbti/compile/Setup.java#L59
- [3] https://github.com/sbt/zinc/blob/ae52a6839e7fb96f033d4ab9962c0dfffe9628ec/zinc/src/main/scala/sbt/internal/inc/IncrementalCompilerImpl.scala#L545
