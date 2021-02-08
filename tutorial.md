---
title: "Tutorial: Synthesizing Object Transformer for Class Changes"
---

## Prerequisites

Make sure you downloaded the source code and made a successful build following the [download and install guide](artifact/INSTALL.html). We assume you're at the `pasta` directory. The directory will look like this:

```text
$ tree .
.
├── examples
│   ├── distiller.sh
│   ├── synthesizer.sh
...
│   ├── string2file
...
│   │   ├── old
│   │   │   └── Example.java
│   │   ├── new
│   │   │   └── Example.java
...
```

## An Example Software Update

With a proof-of-concept `Example` class in `examples/string2file`:

```java
// examples/string2file/old/Example.java
import java.io.File;

public class Example {
    private String file;

    public Example(String path) {
        file = path;
    }

    public String getPath() {
        if (file == null)
            return null;
        else
            return file;
    }
}
```

The developer may feel better storing a `File` object in the `Example` class, yielding the new-version code:

```java
// examples/string2file/new/Example.java
public class Example {
    private File file;

    public Example(String path) {
        if (path != null) {
            file = new File(path);
        }
    }

    public String getPath() {
        if (file == null)
            return null;
        else
            return file.getAbsolutePath();
    }
}
```

The interface `Example.getPath()` keeps unchanged across the version. However, the internal data representation changed: the field `file` underwent a type change from `String` to `File`.

## Object Transformation

If anyone would like to dynamically update (DSU) `Example` from the old version to the new version, each live `Example` object in the heap's internal data representation must be transformed by an *object transformer*. PASTA automatically synthesizes object transformers like the following one:

```java
class DSUHelper {
    static void transform(ExampleNew object, ExampleOld stale) {
        // object: new-version object
        // stale:  old-version object
        if (stale.file != null) {
            object.file = new File(stale.file);
        }
    }
}
```

`DSUHelper` will be invoked by the DSU system over each stale object in the heap, to create a consistent heap for new-version code. PASTA separately synthesizes transformer for each changed field. Please refer to our paper for more information.

## Object Transformer Synthesis

PASTA takes two versions of a program and a target field as input, and automatically produces potentially useful field transformers for the target field.

### Preprocess the Source Code

PASTA works in two steps. First, the `distiller` analyzes the program to extract *gadgets* (code snippets with variable holes). These gadgets are used as basic components of a transformer. Given an existing code snippet (either old/new version), variables and constants inside the code snippet may be replaced by placeholders. Later, gadgets will be (re)assembled with placeholders being filled, to yield object transformers.

An example of extracted gadget:

```text
file = new File(path);   -- distill ->   [1] = new File([2]);
```

Suppose you're now at the `examples` directory. We prepared the scripts for passing correct arguments to the PASTA preprocessor:

```text
$ bash distiller.sh string2file
```

All preprocessing results go to `string2file/.pasta`. A concerned reader may particularly interested in the `.pasta/gadget/` directory, which contains extracted gadgets as JSON files, on the per-class basis (changed classes will be gadget-extracted for both old/new versions).

### Perform the Synthesis

Once the project is preprocessed, one can call the bash script to conduct the actual synthesis. Again suppose you're at the `examples` directory:

```
$ bash synthesizer.sh string2file
```

Generated transformers will be printed in terminal, ranked by their costs (lower the better). In our experiments, these outputs (field transformers) are redirected to the PASTA's built-in verifier for unit-test and filtering out test-failing transformers.

Each produced field transformer looks like the following, which can be copy-pasted into `DSUHelper`:

```java
/* -------- Transformer #5 (cost = 9.0909) -------- */
Example _var0_ = _stale_;
java.io.File _object_ = null;
java.lang.String _var2_ = _var0_.file;  /* g: String [] = [].file; */
if (!(_var0_.file == null)) {  /* g: !([].file == null) */
    _object_ = new java.io.File(_var2_);  /* g: [] = new File([]); */
}
```

Variables both start and end with an underscore (`_`) are transformer-specific.
Particularly, `_stale_` is the stale object, `_object_` is the new-version object, and others are temporary variables.

This transformer (#5) is a semantically correct field transformer for `file` in DSU of `Example`. We can rewrite it by assigning variables meaningful names and doing some simplifications:

```java
class DSUHelper {
    static void transform(ExampleNew object, ExampleOld stale) {
        String file = stale.file;
        if (stale.file != null) {
            object.file = new File(file);
        }
  }
}
```

On the other hand, the highest ranked (Transformer #1) is not correct:

```java
/* -------- Transformer #1 (cost = 2.3684) -------- */
Example _var0_ = _stale_;
java.io.File _object_ = null;
java.lang.String _var2_ = _var0_.file;  /* g: String [] = [].file; */
_object_ = new java.io.File(_var2_);  /* g: [] = new File([]); */
```

For most of the cases, this transformer works. However, if `stale.file` is `null`, this transformer will crash the system upon a `NullPointerException`. Such kind of failure is expected to be captured by a test case, with this incorrect transformer being filtered out.

### Notes

Exactly the same preprocess-synthesis procedure is done for all experimental subjects. Please refer to our reproduction docker image for more information.
