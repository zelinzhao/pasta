---
title: Download and Install
---

PASTA is implemented in pure Java. Simply clone the repo and `make` in a terminal. Ant/Maven will automatically resolve dependencies. A fresh build usually takes a few minutes:

```
$ git clone https://github.com/zelinzhao/pasta.git
$ cd pasta && make
```

Upon successful build, an all-in-one Java archive will be created in the `target/` directory, with all dependencies being packed. If you see the help message, everything is set! 

```
$ java -jar ./target/pasta-[ver]-full.jar
Usage pasta:
  pasta distiller <args>		Distill gadgets from programs.
  pasta synthesizer <args>		Synthesize transformers.
  pasta verifier <args>			Verify a transformer against test cases.
```


