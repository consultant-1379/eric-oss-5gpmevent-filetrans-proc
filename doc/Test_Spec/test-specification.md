<!--Document Template information:
Prepared:***
Approved:***
Document Name:test-specification
Document Number:n/152 41-APR 201 536/1
-->

**5G PM Event File Transfer and Processing Test Specification**

**Abstract**

This document describes the test cases used to verify the 5G PM Event File Transfer and Processing:

- Source code
- Dockerfile
- Helm chart
- Integration with File Notification Service, Data Catalog, Connected Systems, Message Bus KF, Schema Registry, Service Mesh (Proxy) Sidecar and Ericsson Network Manager (ENM) stub

# 1. Revision Information

| **Revision** | **Date**   | **Description** | **Prepared By** |
|--------------|------------|-----------------|-----------------|
| A            | 2022-05-24 | First Revision  | Team Quaranteam |
| B            | 2023-05-19 | Second Revision | Team Quaranteam |
| C            | 2023-07-31 | Third Revision  | Team Quaranteam | 
| D            | 2023-09-27 | Fourth Revision | Team Quaranteam | 



# 2. Introduction

This document contains the Test Specification for the 5G PM Event File Transfer and Processing service.

# 3. Functional Test

## 3.1 Junit

Verify the functionality of individual classes within the 5G PM Event File Transfer and Processing service.

Verify the collective functionality of the integrated classes.

| **Use Case**                                                                                                                                                                                                     |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 5G PM Event File Transfer and Processing consumes off input topic notifications transfers and splits events to output topic with exactly once-semantics for node names and event ID's with active subscriptions. |

## 3.2 Contract Test

Verify the integration of the 5G PM Event File Transfer and Processing service with the following dependent services:

- Data Catalog (installed as part of Data Management and Movement (DMM))
- Connected Systems

# 4. Compliance Test

## 4.1 Lint

Perform lint scan of the 5G PM Event File Transfer and Processing service and verify it complies with enforced design rules.

## 4.2 SonarQube

Perform SonarQube scan and gating of the 5G PM Event File Transfer and Processing service and verify it complies with the coding guidelines and best practices.

# 5. System Test

## 5.1 Application Staging

- Verify the 5G PM Event File Transfer and Processing service receives 100 notifications with active subscriptions and successfully downloads and 
  processes all event 
  files with TLS enabled and running in Service Mesh.

## 5.2 Product Staging

- Verify the 5G PM Event File Transfer and Processing service receives 30K notifications with active subscriptions and successfully downloads and
  processes all event
  files with TLS enabled and running in Service Mesh.

# 6. Deployment Test

## 6.1 Initial Install

Automated initial install test:

- Verify the 5G PM Event File Transfer and Processing service is successfully installed on the microservice CI with mandatory parameters.
- Verify the 5G PM Event File Transfer and Processing service is successfully installed on Application Staging with mandatory parameters.

Manual initial install test:

- Verify the 5G PM Event File Transfer and Processing service is successfully installed with optional parameters

## 6.2 Upgrade

Automated upgrade test:

- Verify the 5G PM Event File Transfer and Processing service is successfully upgraded in Application Staging.

For manual upgrade test the following paths are verified:

- Single upgrade to latest version
- Multiple upgrades to latest version
- Long Jump Upgrade
- In Service Software Upgrade (perform upgrade with traffic to verify service availability and ensure the ISSU expectations are met)

## 6.3 Rollback

Manual rollback test:

- Verify rollback of the 5G PM Event File Transfer and Processing service to previous version

# 7. Robustness Test

- Verify the 5G PM Event File Transfer and Processing service comes back up seamlessly when all instances of the service restart simultaneously.
- Verify the 5G PM Event File Transfer and Processing service handles scenario where all instances of the service restart simultaneously and dependent services are not immediately available.
- Verify the 5G PM Event File Transfer and Processing service resumes processing data following a restart and that no event files are missed.

# 8. Performance Test

No automated performance test for this release.

# 9. Scalability Test

Manual scalability testing performed:

- Verify scale in and scale out of the 5G PM Event File Transfer and Processing service
- Verify scale in and scale out of the File Notification Service
- Verify scale in and scale out of the Data Catalog
- Verify scale in and scale out of the Connected Systems
- Verify scale in and scale out of Message Bus KF

# 10. Characteristics

Measured the following characteristics on a KaaS environment

- Deployment time
- Restart time
- Upgrade time
- Loss of service time during upgrade / rollback
- Docker image size
- Microservice memory footprint required to achieve SLO
- Microservice CPU footprint required to achieve SLO