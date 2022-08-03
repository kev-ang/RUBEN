# RuleEngineBenchmark

This evaluation framework is based on the OpenRuleBench [1] scripts. The aim is
to evaluate rule based reasoning engines with various tests covering well-known
tasks of reasoning engines.

## Test data

The test data for the reasoning engines was created by using the data generators
provided by OpenRuleBench. For engines not covered in the latest run of
OpenRuleBench, we transformed the data and rules manually.

The test data set used for the evaluation in the paper "SemReasoner - A
high-performance Knowledge Graph Store and rule-based Reasoner" can be found
[here](https://drive.google.com/file/d/17qSa3PrHFnV6YmdHcGPqxlkqRCXiRtwz/view?usp=sharing)
.

## Prerequesites

For running the evaluation using all the currently included reasoning engines a
docker installation is required. If you do not have docker installed, go
to https://docs.docker.com/get-docker/

Evaluating [Drools](https://www.drools.org/) resulted in `OutOfMemoryErrors` for
all test cases. Therefore, we excluded Drools from the configuration. If you
want to enable the evaluation for drools, insert the following configuration for
the engines:

```
{
   "name": "Drools",
   "classpath": "at.sti2.engines.Drools"
}
```

One of the evaluated reasoning engines is [Stardog](https://stardog.com) which
requires a license key. A free license key is
available [here](https://www.stardog.com/get-started/). Make sure to update the
path of the volume in the `src/main/docker-compose.yml` accordingly.

Make sure to have docker running before the evaluation is started. Loading the
relevant docker containers and starting them, as well as shutting them down is
handled by the framework itself. You can easily add new docker containers by
modifying the `docker-compose.yml` in `src/main/resources`. For starting a
docker container within the preparation phase of a reasoning engines,
use `at.sti2.at.sti2.utils.BenchmarkUtils.startContainerForEngine(String name)`
.

## Running the Evaluation

Make sure to download the test data beforehand and copy the path of the root
folder into the configuration (can be found
in `src/main/resources/Benchmark_Configuration.json`). For example if the test
data is stored in `/User/test/test_data/` then the configuration must contain:

```
{
    ...
    "testDataPath": "/User/test/test_data",
    ...
}
```

**Important**: Note that there is no `/` (slash) at the end of the path. The
correct path building is done in the framework.

Read the **Prerequesites** before running the evaluation. The evaluation can be
executed in two ways: from within the IDE or via a
runnable JAR.

### IDE

1. Clone the repository or download the code
2. Load it into your IDE
3. Configure the framework properly by modifying
   the `src/main/resources/Benchmark_Configuration.json`
    1. You can remove engines or tests by removing their JSON Object.
4. Go to the run configurations
    1. Set the memory limit you want to use by providing the VM
       argument `-Xmx32g` (if you want to use 32 gigabytes)
    2. Set the path to the configuration file as program argument (e.g.,
       ./src/main/resources/Benchmark_Configuration.json)
5. Run the main-method within `at.sti2.Ruben`
6. When the evaluation is done, the console will show "DONE!"
7. On the root level of the project a new file `Results.json` will contain all
   the evaluation results

### Runnable JAR

1. Clone the repository or download the code
2. Switch into the root folder of the project
3. Run `mvn clean compile assembly:single` to compile the runnable JAR
4. The resulting jar is located in `./target/` with the
   name `OpenRuleBenchmark-1.0-SNAPSHOT-jar-with-dependencies.jar`.
5. Configure the framework properly by modifying
   the `src/main/resources/Benchmark_Configuration.json`
    1. You can remove engines or tests by removing their JSON Object.
6. Move the jar close to the test data folder and move the config and
   docker-compose file to the same
   folder
7. Execute the jar
   using `java -Xmx32g -jar OpenRuleBenchmark-1.0-SNAPSHOT-jar-with-dependencies.jar ./Benchmark_Configuration.json`
8. When the evaluation is done, the console will show "Benchmark finished in X
   minutes!"
9. In the same folder as the JAR a new file `Results.json` will contain all
   the evaluation results

## Known Issues

...

## Future Work

...

## Contribution

If you want to contribute to this evaluation framework feel free to open a pull
request or contact us (kevin.angele[at]sti2.at).

## References

[1]  Liang, S., Fodor, P., Wan, H., Kifer, M.: Openrulebench: An analysis of the
per-
formance of rule engines. In: Proceedings of the 18th international conference
on
World wide web. pp. 601–610 (2009)