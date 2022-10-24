# Setup

## Dependencies

### Clojure

```shell
brew install clojure
```

### asdf

```shell
brew install asdf
```

See [asdf](https://github.com/asdf-vm/asdf) for the setup instruction.

asdf will consult [this](https://github.com/firepandalabs/firepandalabs/blob/main/coinguard/.tool-versions) .tools-version file for the right tools.

Please have the [nodejs plugin](https://github.com/asdf-vm/asdf-nodejs) configured (and potentially other plugins), and then run `asdf install` inside the `firepandalabs/coinguard/` directory.

> asdf install

This will install Bazel, nodejs, and others.