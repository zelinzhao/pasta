---
title: Docker Image
---

## Get the Image

We enclose the PASTA tool and all experimental subjects in the research paper into a [docker image (OSF)](https://osf.io/f3etu/). The container (`pasta-icse21.tar.gz`) is ~900 MB and includes all pre-compiled experimental subjects. Researchers can use this package to reproduce our experimental results.

## Load the Image

Use the `docker load` in the command line. Loading the container image may take a few minutes.

```
$ docker load --input pasta-icse21.tar.gz
114ca5b7280f: Loading ...
...
Loaded image: pasta-icse21:latest
```

Upon a successful load, you may see the PASTA image in the docker's image list:

```
$ docker image ls
REPOSITORY     TAG       IMAGE ID       CREATED          SIZE
pasta-icse21   latest    b06f063db252   xxx              2.44GB
```

## Start a Container

After loading the image, you can start a container to run the experiment, logged in as root. All experimental subjects are located in the `/home/dsu/` directory.

```
$ docker run -it pasta-icse21:latest bash
root@fbd4c1397dae#:/ ls /home/dsu
icse21  pasta-tool
```

The `icse21` directory contains subjects and scripts to run the experiment, and `pasta-tool` contains a copy of the PASTA tool and necessary libraries. You can run this PASTA exactly like the tutorial:

```
root@fbd4c1397dae:/home/dsu# java -jar pasta-tool/pasta/pasta-1.0-full.jar
Usage pasta:
  pasta distiller <args>                Distill gadgets from programs.
  pasta synthesizer <args>              Synthesize transformers.
  pasta verifier <args>                 Verify a transformer against test cases.
```

## Run the Experiments

Get into `icse21` directory. The `ftpserver` directory contains 4 changed classes and `tomcat80` directory contains 22 changed classes. Details can be found in our research paper.

For all experiments, we assume you're at the `/home/dsu/icse21` directory.

### Run Experiment for One Subject

`runOne.sh` accepts an example as input. You can also specify the timeout value (in minutes) for the synthesizer to run. Preprocessing time is not accounted for the timeout. Preprocessing large projects may take a few minutes.

To try the motivating example in the research paper (`SocketProcessor`), you may set a 2-minute timeout:
```
# bash runOne.sh tomcat80/f4451c 2
Run example f4451c from tomcat80
Run dpg
Run distiller
Distill gadgets for tomcat80/f4451c
Distilling   2%  
...
Run synthesizer in verifier mode
```
After 2 minutes, two log files are generated inside `tomcat80/f4451c`:
```
# ls tomcat80/f4451c
summary.log  verifier.log ... ...
```
The `verifier.log` contains the generated transformers and their testing results, like:
```
# cat tomcat80/f4451c/verifier.log
/* -------- Transformer #1 (cost = 2.3684) -------- */
 ... transformer code ...

------------------
Test transformer:
[SuccessRate]   0.6667
[CompareTimes]  3       [IdenticalTimes]        2       [NotIdenticalTimes]     1
[NotIdenticalStates]    TransformerTest.TestSocketNull+socketNull
[IdenticalStates]       TransformerTest.TestSocketNotNull2+socketNotNull
[IdenticalStates]       TransformerTest.TestSocketNotNull1+socketNotNull
```

This transformer did not pass all test cases (success rate 0.6667). `[CompareTimes]` means how many tests are executed, `[IdenticalTimes]` and `[NotIdenticalTimes]` mean how many tests this transformer passes or fails.

Finally, the `summary.log` contains a summary of all generated transformers:

```
# cat tomcat80/f4451c/summary.log
For tomcat80/f4451c:
Number of all tested transformers: 21
Number of all test-passing transformers: 2
Id of the first test-passing transformer: 4
```

In our 2-minute run, PASTA generated and tested 21 candidate transformers. 2 transformers pass all tests. The first test-passing transformer is the Transformer #4. The algorithm should be deterministic.

### Run Batched Experiments for All Subjects

`runAll.sh` only accepts a timeout value (in minutes, default is 30, for each subject) as its parameter. Synthesizer will execute for that amount of time for each subject. PASTA also generates the `verifier.log` and `summary.log` in each subject's root directory.

Below is an independent full run of PASTA (docker image) with a 15-minute timeout on an Intel i5 machine. For the third column (`n/m`): `n` means the n-th test passing transformer is the semantics-correct one; `m` means how many transformers have been tested when finding the semantics-correct one. All major results in the paper are reproduced (all cases succeeded with a correct transformer also succeeded). There may be slight performance variance due to hardware/software configurations and our minor changes to the implementation.

|ID| Subject        | Result            |
|:-| :------------- | ----------------- |
|1 | tomcat80/6a940d|    1/36           |
|2 | tomcat80/ec8dff|    &#x2717;       |
|3 | tomcat80/f84800|    &#x2717;       |
|4 | tomcat80/a752f3|    &#x2717;       |
|5 | tomcat80/c0d4f7-WsHandshakeResponse| 1/316|
|6 | tomcat80/c0d4f7-PojoMethodMapping  | 1/15 |
|7 | tomcat80/dbb784|    1/5            |
|8 | tomcat80/a8d16b|    1/12           |
|9 | tomcat80/358f94|    1/9            |
|10| tomcat80/ad012e|    1/3            |
|11| tomcat80/c0d4f7-WsOutputStream     | &#x2717;|
|12| tomcat80/c0d4f7-WsWriter|  &#x2717;|
|13| tomcat80/db1a6e|  &#x2717;         |
|14| tomcat80/69196d|    1/1            |
|15| tomcat80/d8ad3c|    1/7            |
|16| tomcat80/2e7c68|    1/214          |
|17| tomcat80/5952de|     &#x2717;      |
|18| tomcat80/6b64bb-noPluggabilityListeners-loop| 1/58  |
|19| tomcat80/6b64bb-noPluggabilityServletContext|&#x2717;|
|20| tomcat80/766c9e|     &#x2717;      |
|21| tomcat80/f4451c|    1/4            |
|22| tomcat80/4355ed|    1/41           |
|23|ftpserver/faa153|    1/14           |
|24|ftpserver/32ed0b|    1/7            |
|25|ftpserver/1b2ea6|     &#x2717;      |
|26|ftpserver/afffc8|    1/40           |
|**SUMMARY**| Tomcat (22), FtpSever (4) | **16**/26 Success |
