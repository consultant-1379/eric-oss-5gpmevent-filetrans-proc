<!--Document Template information:
Prepared:***
Approved:***
Document Name:test-report
Document Number:152 83-APR 201 536/1-n
-->
# 5G PM Event File Transfer and Processing Test Report

---

## Abstract

This document lists the results for tests run against this service manually and on the CI pipeline.

The API spec can be found in the [5G PM Event File Transfer and Processing API Documentation](https://adp.ericsson.se/marketplace/5g-pm-event-file-transfer-and-processing/documentation/).
- No specific tests verify the API we expose

---

## Unit Tests

These tests are run on the CI pipeline for this microservice. There are two pipelines and both of them run these tests
- [PreCodeReview (PCR)](https://fem6s11-eiffel216.eiffel.gic.ericsson.se:8443/jenkins/job/eric-oss-5gpmevent-filetrans-proc_PreCodeReview_Hybrid/) 
- [Publish](https://fem6s11-eiffel216.eiffel.gic.ericsson.se:8443/jenkins/job/eric-oss-5gpmevent-filetrans-proc_Publish_Hybrid/)

#### Test Environment

More information [here](https://fem6s11-eiffel216.eiffel.gic.ericsson.se:8443/jenkins/)

|       Name       | Value                                                                                                                            |
|:----------------:|----------------------------------------------------------------------------------------------------------------------------------|
|     Hardware     | Jenkins Worker Node (fem6s11-eiffel216)                                                                                          |
| Operating System | SUSE Linux Enterprise Server 15 SP4. Dockerised in the image `armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob-java17mvnbuilder` |

#### Test Object(s)

|           Service            |  Version  |
|:----------------------------:|:---------:|
| eric-oss-5gpmevt-filetx-proc | 1.179.0-0 |

#### Test Execution

- Triggered at every commit (and its patchsets) - PCR
- Triggered after merging code into master - Publish

#### Test Results

|         Test Case         | Test Case Stage |  Test Result  | Comment                                                                                                                 |
|:-------------------------:|:---------------:|:-------------:|-------------------------------------------------------------------------------------------------------------------------|
| Java JUnit and Stub tests |      Test       |    205/205    | The JUnit tests verify class behaviour and Stubs test expected integration based on stubs provided by dependent service |
|       Docker Build        |      Image      |      1/1      | Tests if the docker image can be built                                                                                  |
|         Helm Lint         |      Lint       |      1/1      | Tests if the chart follows best practices based on the Helm DR checker                                                  |
|       Helm Package        |     Package     |      1/1      | Tests if the chart can be packaged into a .tgz file                                                                     |

---

## Application Staging Tests

#### Test Environment

More information [here](https://ews.rnd.gic.ericsson.se/cd.php?cluster=hart098)

|    Name     |              Value              |
|:-----------:|:-------------------------------:|
|  Hardware   |        OSS BCSS Test Env        |
|  Software   |   1.28.4-kaas.1  [ccd:2.27.0]   |
| K8S Version |             v1.28.4             |
| K8S Cluster | 1 Master Node + 15 Worker Nodes |
|  Capacity   |    16 vCPUs per Worker node     |


#### Test Object(s)

|   Service    | Version  |
|:------------:|:--------:|
| eric-oss-adc | 0.939.0  |

#### Test Execution

- Triggered after a successful Publish job run

#### Test Results

<!-- From app staging test cases -->
|                              Test Case                              | Test Result | Comment                                                                                                                            |
|:-------------------------------------------------------------------:|:-----------:|------------------------------------------------------------------------------------------------------------------------------------|
|                  **5G PM Event files Collection**                   |             |                                                                                                                                    |
|           Capture the pmevent events read total : 2376560           |     1/1     | The service successfully processes all files within configured time limit of 9 mins with active subscription to capture all events |
|          Verify failed file transfer count should be zero           |     1/1     | No file transfer failures should be observed.                                                                                      |
|           Verify pmevent successfully transfer the files            |     1/1     | Verify expected number of files were transferred                                                                                   |
|                 Verify pmevent processed all files                  |     1/1     | Verify expected number of files are processed.                                                                                     |


---

## Product Staging Tests

N/A

#### Test Environment

More information [here](https://ews.rnd.gic.ericsson.se/cd.php?cluster=hart098)

|    Name     |              Value              |
|:-----------:|:-------------------------------:|
|  Hardware   |        OSS BCSS Test Env        |
|  Software   |   1.28.4-kaas.1  [ccd:2.27.0]   |
| K8S Version |             v1.28.4             |
| K8S Cluster | 1 Master Node + 15 Worker Nodes |
|  Capacity   |    16 vCPUs per Worker node     |

#### Test Object(s)

|      Service       |  Version   |
|:------------------:|:----------:|
| eric-eiae-helmfile | "2.2297.0" |

#### Test Execution

- Triggered after a successful Application Staging run

#### Test Results

|    Test Case    | Test Result | Comment              |
|:---------------:|:-----------:|----------------------|
| Service deploys |     1/1     | No tests implemented |

---

## Performance Tests

Not done at the moment, N/A

<!-- #### Test Environment

| Name        | Value   |
| :---------: | :-----: |
| Hardware    |         |
| Software    |         |
| K8S Version |         |
| K8S Cluster |         |
| Capacity    |         |

#### Test Object(s)

| Service | Version |
| :-----: | :-----: |
| eric-oss-5gpmevt-filetx-proc |  |


#### Test Execution

- 

#### Test Results -->

---

## Deployment Tests

#### Test Environment

More information [here](https://ews.rnd.gic.ericsson.se/cd.php?cluster=hart098)

|    Name     |              Value              |
|:-----------:|:-------------------------------:|
|  Hardware   |        OSS BCSS Test Env        |
|  Software   |   1.28.4-kaas.1  [ccd:2.27.0]   |
| K8S Version |             v1.28.4             |
| K8S Cluster | 1 Master Node + 15 Worker Nodes |
|  Capacity   |    16 vCPUs per Worker node     |

#### Test Object(s)

|           Service            |  Version  |
|:----------------------------:|:---------:|
| eric-oss-5gpmevt-filetx-proc | 1.145.0+1 |
|         eric-oss-dmm         |  0.338.0  |
|         eric-oss-adc         |  0.723.0  |
|     eric-oss-common-base     |  0.255.0  |

Note: Tests not executed again for eric-oss-5gpmevt-filetx-proc-1.179.0-0 because no changes during characteristics step.

#### Test Execution

- Manual

#### Test Results

|                 Test Case                  |                   Test Result                   |                                                                                               Comment                                                                                               |
|:------------------------------------------:|:-----------------------------------------------:|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------:|
|                 Deployment                 |                                                 | The microservice will be deployed and tested using the mandatory parameters on the microservice CI pipeline. The microservice has no optional parameters or optional services to integrate towards. |
|    Scale-out (increase no of instances)    | [Deployment Appendix A](#deployment-appendix-a) |                                                                                                                                                                                                     |
|    Scale-in (decrease no of instances)     | [Deployment Appendix A](#deployment-appendix-a) |                                                                                                                                                                                                     |
| Robustness, dependent services unavailable | [Robustness Appendix A](#robustness-appendix-a) |                                                                                                                                                                                                     |

##### Deployment Appendix A

<details>
    <summary><strong>Scaling tests</strong></summary>

Each test case shares the same acceptance criteria.

For a manual test to pass, the 5g service must successfully:
- Scale its number of pods from its initial set of replicas to the expected number given.
- Be able to process a set number of files given a notification on the input Kafka topic
- Continue processing under load when scaling occurs during its processing period
- Maintain order of events throughout scale in/out test

| Initial number of pods | Scaled to | Success/Failure |                               Command                               |
|:----------------------:|:---------:|:---------------:|:-------------------------------------------------------------------:|
|           1            |     5     |     SUCCESS     | kubectl scale deployment eric-oss-5gpmevt-filetx-proc \--replicas 5 |
|           5            |     6     |     SUCCESS     | kubectl scale deployment eric-oss-5gpmevt-filetx-proc \--replicas 6 |
|           6            |     1     |     SUCCESS     | kubectl scale deployment eric-oss-5gpmevt-filetx-proc \--replicas 1 |

</details>


---

## Upgrade Tests

#### Test Environment

More information [here](https://ews.rnd.gic.ericsson.se/cd.php?cluster=hart098)

|    Name     |              Value              |
|:-----------:|:-------------------------------:|
|  Hardware   |        OSS BCSS Test Env        |
|  Software   |   1.28.4-kaas.1  [ccd:2.27.0]   |
| K8S Version |             v1.28.4             |
| K8S Cluster | 1 Master Node + 15 Worker Nodes |
|  Capacity   |    16 vCPUs per Worker node     |

#### Test Object(s)

|           Service            |  Version  |
|:----------------------------:|:---------:|
| eric-oss-5gpmevt-filetx-proc | 1.145.0+1 |
|         eric-oss-dmm         |  0.338.0  |
|         eric-oss-adc         | 0.7243.0  |
|     eric-oss-common-base     |  0.255.0  |

Note: Tests not executed again for eric-oss-5gpmevt-filetx-proc-1.179.0-0 because no changes during characteristics step.

#### Test Execution

- Manual

#### Test Results

A single deployment with 1 pod and default resources were used for the following tests.

To find the chart for a specific version, use the following formatted string

```
VERSION=1.145.0+1 

https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-released-helm-local/eric-oss-5gpmevt-filetx-proc/eric-oss-5gpmevt-filetx-proc-{VERSION}.tgz
```

|              Test Case              | Test Result | Comment                            |
|:-----------------------------------:|:-----------:|------------------------------------|
|               Upgrade               |   SUCCESS   | 1.145.0-1 -> 1.146.0-1 <br> 1m 44s |
| Multiple Upgrades to latest version |   SUCCESS   | 1.145.0-1 -> 1.146.0-1 <br> 1m 48s |
| In Service Software Upgrade (ISSU)  |   SUCCESS   | 1.145.0-1 -> 1.146.0-1 <br> 1m 45s |
|         Downgrade/Rollback          |   SUCCESS   | 1.148.0-1 -> 1.145.0-1 <br> 1m 47s |
|          Long Jump Upgrade          |   SUCCESS   | 1.146.0-1 -> 1.148.0-1 <br> 1m 46s |

---

## Robustness Tests

#### Test Environment

More information [here](https://ews.rnd.gic.ericsson.se/cd.php?cluster=hart098)

|    Name     |              Value              |
|:-----------:|:-------------------------------:|
|  Hardware   |        OSS BCSS Test Env        |
|  Software   |   1.28.4-kaas.1  [ccd:2.27.0]   |
| K8S Version |             v1.28.4             |
| K8S Cluster | 1 Master Node + 15 Worker Nodes |
|  Capacity   |    16 vCPUs per Worker node     |

#### Test Object(s)

|           Service            |  Version  |
|:----------------------------:|:---------:|
| eric-oss-5gpmevt-filetx-proc | 1.145.0+1 |
|         eric-oss-dmm         |  0.338.0  |
|         eric-oss-adc         |  0.723.0  |
|     eric-oss-common-base     |  0.255.0  |

Note: Tests not executed again for eric-oss-5gpmevt-filetx-proc-1.179.0-0 because no changes during characteristics step.

#### Test Execution

- Manual

#### Test Results

|              Test Case               |                   Test Result                   | Comment                                                                                                                                                       |
|:------------------------------------:|:-----------------------------------------------:|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
|     Robustness, Service Restart	     | [Robustness Appendix A](#robustness-appendix-a) |                                                                                                                                                               |
|  Liveness and Readiness probes test  |                     SUCCESS                     | The liveness and readiness probes are unit tested. And there is also a test in the CI that deploys the service and tests that it comes up into a ready state. |
|    SIGTERM and SIGKILL handling	     |                       N/A                       | Microservice is stateless                                                                                                                                     |
|        Move between workers	         |                       N/A                       | Microservice is stateless                                                                                                                                     |

##### Robustness Appendix A

<details>
    <summary><strong>Robustness, Service Restart</strong></summary>

To ensure Microservice can handle outages in dependencies in resilient way, testing must be performed manually with an application install on application cluster to simulate these outages.

All tests are performed on current master at time of tests (1.88.0-1).

These tests need to be automated as part of service maturity step [IDUN-8234](https://jira-oss.seli.wh.rnd.internal.ericsson.com/browse/IDUN-8234)

| Test Case ID | Test Case Description                                                                                                                             | Comments                                                                                                                                                                                                                                                                                                                                                     | Test Result | Phase                                    | Related JIRA's  |
|--------------|---------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------|------------------------------------------|-----------------|
| TC_01        | Happy path, ensure can install stub and dependent services and process files/produce messages as expected                                         |                                                                                                                                                                                                                                                                                                                                                              | SUCCESS     | Manual deployment on application cluster | IDUN-86478      |
| TC_02        | Simulate service outage with active subscription                                                                                                  | Executed after TC_01 finished processing messages                                                                                                                                                                                                                                                                                                            | SUCCESS     | Manual deployment on application cluster | IDUN-86478      |
| TC_03        | Simulate Data Catalog outage by scaling pods to 0 temporarily then scale back up                                                                  | Executed after TC_02 finished processing messages                                                                                                                                                                                                                                                                                                            | SUCCESS     | Manual deployment on application cluster | IDUN-86478      |

</details>

---

## Characteristics Tests

#### Test Environment

More information [here](https://ews.rnd.gic.ericsson.se/cd.php?cluster=hart098)

|    Name     |              Value              |
|:-----------:|:-------------------------------:|
|  Hardware   |        OSS BCSS Test Env        |
|  Software   |   1.28.4-kaas.1  [ccd:2.27.0]   |
| K8S Version |             v1.28.4             |
| K8S Cluster | 1 Master Node + 15 Worker Nodes |
|  Capacity   |    16 vCPUs per Worker node     |

#### Test Object(s)

|           Service            |  Version  |
|:----------------------------:|:---------:|
| eric-oss-5gpmevt-filetx-proc | 1.145.0+1 |
|         eric-oss-dmm         |  0.338.0  |
|         eric-oss-adc         |  0.723.0  |
|     eric-oss-common-base     |  0.255.0  |

Note: Tests not executed again for eric-oss-5gpmevt-filetx-proc-1.179.0-0 because no changes during characteristics step.

#### Test Execution

- Manual

#### Test Results

|                                  Test Case                                   |                        Test Result                         | Comment                                                                                          |
|:----------------------------------------------------------------------------:|:----------------------------------------------------------:|--------------------------------------------------------------------------------------------------|
|                        Startup time (to fully ready)                         |                           6.846s                           |   <br/>                                                                                          |
|                        Restart time (to fully ready)                         |                            31s                             |      <br/>                                                                                       |
|                        Upgrade time (to fully ready)                         |                            34s                             |                                                                                                  |
|                       Downgrade time (to fully ready)                        |                            33s                             |                                                                                                  |
|             Loss of service duration (during upgrade/downgrade)              |                             0s                             | Old pod runs till the new one is ready to take workload                                          |
|                                  Image size                                  |                           419MB                            |                                                                                                  |
|                    Microservice memory footprint required                    |            465Mi  at Idle and 1093Mi  with load            |                                                                                                  |
|                     Microservice CPU footprint required                      |         0.03  CPU at Idle and 2.224 CPU with load          | Under Resourced. Dimensioning support ticket in progress. Will be completed outside of this Epic |
|         Some kind of meaningful latency or throughput for your “API”         | [Characteristics Appendix A](#characteristics-appendix-a)  |                                                                                                  |
| Test Coverage of Characteristics requirement stated in Component Description |                                                            |                                                                                                  |

##### Characteristics Appendix A
<details>
    <summary><strong>Some kind of meaningful latency or throughput for your “API”</strong></summary>

For these tests version 1.145.0+1 is used

| **Scenario ID** | **Number of files (on enm) ** | **Events Total** | **Start time** | **End time** | **Total time** | **Files transferred total** | **Files processed total** | **Events read total** | **Events Sent total** |
|:---------------:|:-----------------------------:|:----------------:|:--------------:|:------------:|:--------------:|:---------------------------:|:-------------------------:|:---------------------:|:---------------------:|
|                 |                               |    (on PMIC)     |                |              |                |                             |                           |                       |                       |
|        1        |             5,500             |   117,546,000    |    21:24:25    |   21:33:33   |    00:09:08    |            5,500            |           5,500           |      117,546,000      |      117,546,000      |
|        2        |            20,000             |   427,440,000    |    22:00:00    |   22:07:17   |    00:07:17    |           20,000            |           20000           |      427,440,000      |      427,440,000      |
|        3        |            31,251             |   667,896,372    |    11:11:41    |   11:23:01   |    00:11:20    |           31,251            |          31,251           |      667,896,372      |      667,896,372      |
|        4        |            31,251             |   667,896,372    |    12:09:20    |   12:18:48   |    00:09:28    |           31,251            |          31,251           |      667,896,372      |      667,896,372      |


</details>

---

## Vulnerability Analysis

List of found vulnerabilities: None

---