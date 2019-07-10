# scala_toolchain

`scala_toolchain` allows you to define global configuration to all Scala targets.

Currently, the only option that can be set is `scalacopts` but the plan is to expand it to other options as well.

**Some scala_toolchain must be registered!**

### Several options to configure `scala_toolchain`:

#### A) Use the default `scala_toolchain`:

In your workspace file add the following lines:

```python
# WORKSPACE
# register default scala toolchain
load("@io_bazel_rules_scala//scala:toolchains.bzl", "scala_register_toolchains")
scala_register_toolchains()
```

#### B) Defining your own `scala_toolchain` requires 2 steps:

1. Add your own definition of `scala_toolchain` to a `BUILD` file:
    ```python
    # //toolchains/BUILD
    load("@io_bazel_rules_scala//scala:scala_toolchain.bzl", "scala_toolchain")

    scala_toolchain(
        name = "my_toolchain_impl",
        scalacopts = ["-Ywarn-unused"],
        unused_dependency_checker_mode = "off",
        visibility = ["//visibility:public"]
    )

    toolchain(
        name = "my_scala_toolchain",
        toolchain_type = "@io_bazel_rules_scala//scala:toolchain_type",
        toolchain = "my_toolchain_impl",
        visibility = ["//visibility:public"]
    )
    ```

2. Register your custom toolchain from `WORKSPACE`:
    ```python
    # WORKSPACE
    register_toolchains("//toolchains:my_scala_toolchain")
    ```
