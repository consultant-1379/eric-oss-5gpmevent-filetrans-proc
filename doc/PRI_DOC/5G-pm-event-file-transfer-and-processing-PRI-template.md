**5G PM event File Transfer & Processing 1.0.0 PRI**

**APR 201 536/1, R1A** \
**5G PM event File Transfer & Processing X.Y.Z+A**

Contents

[1 Reason for Revision](#reason-for-revision)

* [1.1 Reason for Major Version step](#reason-for-major-version-step)

[2 Evidence of Conformity with the Acceptance Criteria](#evidence-of-conformity-with-the-acceptance-criteria)

[3 Technical Solution](#technical-solution)

* [3.1 Implemented Requirements](#implemented-requirements)

* [3.2 Implemented additional features](#implemented-additional-features)

* [3.3 Implemented API Changes](#implemented-api-changes)

* [3.4 SW Library](#sw-library)

* [3.5 Reusable Images](#reusable-images)

* [3.6 Impact on Users: Abrupt NBC](#impact-on-users-abrupt-nbc)

* [3.7 Impact on Users: NUC/NRC](#impact-on-users-nucnrc)

* [3.8 Impact on Users: NBC (Deprecation Ended)](#impact-on-users-nbc-deprecation-ended)

* [3.9 Impact on Users: Started/Ongoing Deprecations](#impact-on-users-startedongoing-deprecations)

* [3.10 Corrected Trouble Reports](#corrected-trouble-reports)

* [3.11 Restrictions and Limitations](#restrictions-and-limitations)

[4 Product Deliverables](#product-deliverables)

* [4.1 Software Products](#software-products)

* [4.2 New and Updated 2PP/3PP](#new-and-updated-2pp3pp)

* [4.3 Helm Chart Link](#helm-chart-link)

* [4.4 Related Documents](#related-documents)

[5 Product Documentation](#product-documentation)

* [5.1 Developer Product Information](#developer-product-information)

* [5.2 Customer Product Information](#customer-product-information)

[6 Deployment Information](#deployment-information)

* [6.1 Deployment Instructions](#deployment-instructions)

* [6.2 Upgrade Information](#upgrade-information)

[7 Verification Status](#verification-status)

* [7.1 Stakeholder Verification](#stakeholder-verification)

[8 Support](#support)

[9 References](#references)

Revision History

| **Revision** | **Date**   | **Reason for Revision**                      |
|--------------|------------|----------------------------------------------|
| PA1          | 2022-07-26 | First Draft                                  |
| PB1          | 2023-05-29 | Update for Service Mesh                      |
| PC1          | 2023-07-29 | Update for DC-C Registration                 |
| PD1          | 2023-09-27 | Update for Processing based on Subscriptions |

# Reason for Revision

5G PM event File Transfer & Processing service- 1.121.0-1 introduces processing events based on subscription, which save resource and enable EIAP to be scaled down when services are no longer being consumed.

<!---
(In case of Emergency Package (EP), report “This is an emergency
correction release meant for limited use. Details on the restrictions
and limitations for this release are reported in [section 3.11](#Restrictions-and-Limitations).”)
-->
## Reason for Major Version step

This is not a major version step
<!---
(This section shall be created only in case of releases that introduce a
Major Version step to clarify the reason for it.)

Following changes in this release introduced a backwards incompatibility
that required a major version step:

(Provide a list of NBC/NUC/NRC/Abrupt NBC JIRA items that caused the
major version step. Free text items for causes that are not tracked in
JIRA items.)

- [IDUN-00](https://jira.link): Service API NBC XXXX

- [IDUN-00](https://jira.link): Impacts on upgrade (NUC)

- [IDUN-00](https://jira.link): Impacts on Rollback (NRC)

- \<Free text for other reasons not tracked as JIRA items\>
-->

# Evidence of Conformity with the Acceptance Criteria


The release criteria have been fulfilled.

The release decision has been taken by the approval of this document.

The release decision has been taken by the approver of this document.

Acceptance criteria relating to the performance of the ervice was moved to a future step.
<!---
This release has been done in accordance with the \<Link to the PRA
Checklist in full released and checked revision\>.

Free text\
(If there are no deviations state “The release criteria have been
fulfilled”)\
(If there are deviations state:\
“The release criteria have been fulfilled except for:

- Reason A

- Reason B

“)
-->
# Technical Solution

## Implemented Requirements

| Requirement ID (MR/JIRA ID) | Heading/DESCRIPTION                                                          |
|-----------------------------|------------------------------------------------------------------------------|
| IDUN-19479                  | "5G PM Event File Transfer and processing" Processing based on subscriptions |


In this step, 5G PM Events File Transfer & Processing implements processing based on subscriptions.

<!---
All implemented requirements in this chapter have Feature Maturity
Stable. Requirements with Feature Maturity lower than Stable are listed
in chapter [3.11.5](#Features-not-ready-for-commercial-use).

(If no requirement implemented write: No requirement implemented in this
release)
-->

## Implemented additional features

The service queries the corresponding active subscriptions from the Data Catalog and listens to the subscription input topic produced by the Data Catalog for subscription updates. The service only produces events that match the subscription predicates (node name(s) and/or event id(s)).


<!---
| Jira-id   | jira Heading/DESCRIPTION |
|-----------|--------------------------|
| IDUN-00   | Description              |

All implemented additional features in this chapter have Feature
Maturity Stable. Additional features with Feature Maturity lower than
Stable are listed in chapter [3.11.5](#Features-not-ready-for-commercial-use).

(If no feature implemented write: No additional features implemented in
this release)
-->
## Implemented API Changes

### 5G PM Event File Transfer and Processing Service API Specification

Stable API Documentation:

| Document ID             | Title        | Rev | Support/Other |
|-------------------------|--------------|-----|---------------|
| n/155 19-CAF 201 536/1  | API Document | 3   | EriDoc        |

<!---
(If no CAF product exists for the service write "No API exists for this
product")

### \<API Name – This subchapter is added for each CAF product\>

Free text\
(Describe the change, including any changes in API Maturity. By default,
if there are no changes, please state “No API change implemented in this
release”)

(The “API Documentation” table shall always be included if a CAF product
is provided and contain the highest version with API Maturity Stable.)
(If any API versions are included in the release with an API Maturity
lower than Stable they shall be listed in the “Non-stable API
documentation” table.)

(The API Maturity changes table shall be omitted if there are no
included API version with API Maturity lower than Stable.)

Non-stable API Documentation:

| Document ID           | Title       | Rev     | Maturity |
|-----------------------|-------------|---------|----------|
| n/155 19-CAF xxx xx/x | Description | \<rev\> | Beta     |
| n/155 19-CAF xxx xx/x | Description | \<rev\> | Alpha    |

-->
## SW Library

No SW library product for this service

## Reusable Images

No reusable images products for this service
<!---
(This section is for Reusable images provided by this service and NOT
for the ones it includes as 2PPs (that shall instead be reported in
[chapter 4.2](#New-and-Updated-2PP_3PP)). If there is no 2PP reusable image provided by the service,
please state “No reusable images products for this service”)

### \<Reusable Image Name Changes– This subchapter is added for each reusable image CXU product\>
Free text

(Describe the change and refer to the Application Developers Guide for
details. By default, if there are no changes, please state “No change in
reusable image implemented in this release”)

(If there are compatibility aspects to consider for the reusable images
describe them here in free text or table format if possible. Possible
examples of compatibility aspects are: oldest reusable image version
compatible with the service, oldest compatible version of a third
service interacting with reusable image, etc.).
-->
## Impact on Users: Abrupt NBC

No Abrupt NBC introduced in this release

<!---

(If no abrupt NBC is introduced by the service, please state “No Abrupt
NBC introduced in this release”)

The tables below describe the abrupt non-backward compatible changes
(Abrupt NBC) introduced in this service version. These functions have
been modified and require their users to adapt in this version of the
service.

| [IDUN-00](https://eteamproject.internal.ericsson.com/browse/IDUN-7008) | \<Summary\>                            |
|------------------------------------------------------------------------------|----------------------------------------|
| Impact on other IDUN Service Components                                       | Description (multiple lines if needed) |
| Impact on interface users                                                    | Description (multiple lines if needed) |
| Impact on end-customers                                                      | Description (multiple lines if needed) |

Free text

(The table and the free text are repeated for each Abrupt NBC)
-->
## Impact on Users: NUC/NRC

No NUC/NRC introduced in this release

<!---

(If no abrupt NUC/NRC is introduced by the service, please state “No
NUC/NRC introduced in this release”)

The tables below describe the Non-upgradeable (NUC) and Non-rollbackable
(NRC) changes introduced in this service version.

| [IDUN-00](https://eteamproject.internal.ericsson.com/browse/IDUN-7008) | \<Summary\>                            |
|------------------------------------------------------------------------------|----------------------------------------|
| Impact on end-customers                                                      | Description (multiple lines if needed) |

Free text

(The table and the free text are repeated for each NUC/NRC)
-->
## Impact on Users: NBC (Deprecation Ended)

No NBC introduced in this release

<!---
(If no NBC is introduced by the service, please state “No NBC introduced
in this release”)

The tables below describe the non-backward compatible changes (NBC)
introduced in this service version. These functions have been deprecated
and are now removed in this version and can no longer be used.

| [IDUN-00](https://eteamproject.internal.ericsson.com/browse/IDUN-7008) | \<Summary\>                            |
|------------------------------------------------------------------------------|----------------------------------------|
| Impact on other ADC Service Components                                       | Description (multiple lines if needed) |
| Impact on interface users                                                    | Description (multiple lines if needed) |
| Impact on end-customers                                                      | Description (multiple lines if needed) |

Free text

(The table and the free text are repeated for each NBC)
-->
## Impact on Users: Started/Ongoing Deprecations

No NBC introduced in started/ongoing deprecations

<!---
The tables below describe the started deprecations for this service. As
a user of a deprecated function, please ensure to stop using the
deprecated function as early as possible. For more details, see the JIRA
ticket(s).

| [IDUN-00](https://eteamproject.internal.ericsson.com/browse/IDUN-7008) | \<Summary\>                            |
|------------------------------------------------------------------------------|----------------------------------------|
| Start Date                                                                   | 2020-01-01                             |
| End Date                                                                     | 2020-05-02                             |
| Impact on other ADC Service Components                                       | Description (multiple lines if needed) |
| Impact on interface users                                                    | Description (multiple lines if needed) |
| Impact on end-customers                                                      | Description (multiple lines if needed) |

Free text

(The table and the free text are repeated for each Deprecation)
-->

## Corrected Trouble Reports

No trouble reports fixed in this release

| **TR ID** | **TR HEADING** |
|-----------|----------------|
| IDUN-00 | Description    |

<!---
The table below lists the Trouble Reports that is reported in the
[JIRA](https://jira-oss.seli.wh.rnd.internal.ericsson.com/secure/RapidBoard.jspa?rapidView=7820&projectKey=IDUN&view=planning&selectedIssue=IDUN-5166&issueLimit=100) and
is corrected in the 5G PM event File Transfer & Processing service:

(If no fix implemented write: No trouble reports fixed in this release)
-->
### Corrected Vulnerability Trouble Reports

The table below lists the Vulnerability Trouble Reports that is reported
in the
[JIRA](https://jira-oss.seli.wh.rnd.internal.ericsson.com/secure/RapidBoard.jspa?rapidView=7820&projectKey=IDUN&view=planning&selectedIssue=IDUN-5166&issueLimit=100) and
is corrected in the 5G PM event File Transfer & Processing service:


| Vulnerability ID(s) | Vulnerability Description          | TR ID        |
|---------------------|------------------------------------|--------------|
| CVE-2023-2976       | guava                              | IDUN-82704   |
| CVE-2023-34453      | snappy-java                        | IDUN-82705   |
| CVE-2023-34454      | snappy-java                        | IDUN-82706   |
| CVE-2023-34455      | snappy-java                        | IDUN-82707   |
| CVE-2023-3635       | Okio                               | IDUN-86358   |
| CVE-2023-36054      |                                    | IDUN-88960   |
| CVE-2023-22036      |                                    | IDUN-89012   |
| CVE-2022-4304       |                                    | IDUN-89013  |
| CVE-2023-22006      |                                    | IDUN-89014  |
| CVE-2023-22041      |                                    | IDUN-89015  |
| CVE-2023-22045      |                                    | IDUN-89016  |
| CVE-2023-25193      |                                    | IDUN-89017  |
| CVE-2023-22044      |                                    | IDUN-89018  |
| CVE-2023-22049      |                                    | IDUN-89019  |

-->
## Restrictions and Limitations

<!---
(Report here general restrictions or limitations to the usage of this
release: for instance, if the release is limited to specific
Customer/Applications for which the release provides an emergency
correction (EP scenario). Leave empty and jump to next subsection if
nothing to report here.)
-->
### Exemptions

Exemptions were submitted for the following CVEs:

| Vulnerability ID(s) | Description         |
|---------------------|---------------------|
| CVE-2022-42252      | tomcat-embeded-core |
| CVE-2022-43551      | libcurl4            |
| CVE-2022-45143      | tomcat-embeded-core |
| CVE-2022-47629      | libksba8            |

<!---
(Description of the exemption shall be done to a proper detail level.
For example, for non-root DR exemption, description should include what
capabilities and/or privileges are included.)

(If no exemption is present, please state “No exemption is present in
this release.”)
-->
### Backward incompatibilities
<!---
(If no NBC is introduced by this service release, please state “No NBC
introduced in this release”, otherwise report the text below)\
For the list of non-backward compatible changes (NBC) introduced in this
release refer to [chapters 3.6](#Impact-on-Users_Abrupt-NBC) and [3.8](#Impact-on-Users_NBC-Deprecation-Ended).
-->
### Unsupported Upgrade/Rollback paths

No NUC/NRC introduced in this release
<!---
(If the service is introducing or has introduced in previous releases a
NUC/NRC that limit the possible upgrade/rollback paths for the service,
report, in a table format, the oldest service version for which upgrade
and rollback are supported. See example below)

| **Oldest version for which upgrade to this release is supported** | **Oldest version for which rollback from this release is supported** |
|-------------------------------------------------------------------|----------------------------------------------------------------------|
| 1.0.0                                                             | **1.5.0**                                                            |

(If no NUC/NRC is introduced by this service release, please state “No
NUC/NRC introduced in this release”, otherwise report the text below)\
For the list of Non-Upgradeable (NUC) and Non-Rollbackable (NRC) changes
introduced in this release refer to [chapter 3.7](#Impact-on-Users_NUC_NRC).
-->
### Features not ready for commercial use

 No Feature with Feature Maturity Alpha/Beta included in this release
<!---
The table below lists the included features that are not yet ready for
commercial use and thus have Feature Maturity Alpha or Beta. These
features should not be used in any commercial or otherwise sensitive
deployment unless a proper risk analysis has been performed as their
function is not yet final. Alpha features shall be disabled by default,
Beta features may be enabled by default. The default state of each
feature can be found in the Helm chart for this release.

| Requirement ID (MR/JIRA ID) | DESCRIPTION | feature maturity |
|-----------------------------|-------------|------------------|
| IDUN-00                   | Description | Alpha            |
| IDUN-00                   | Description | Beta             |
-->
# Product Deliverables

## Software Products

The following table shows the software products of this release.

| Product Type | Name                                                  | Product ID   | New Version |
|--------------|-------------------------------------------------------|--------------|-------------|
| HELM chart   | 5G PM Event File Transfer and Processing HELM         | CXU 101 304  | 1.121.0-1   |
| Docker image | 5G PM Event File Transfer and Processing              | CXU 101 1380 | 1.121.0-1   |
| Code Source  | 5G PM Event File Transfer and Processing              | CAV 101 0285 | 1.121.0-1   |

## New and Updated 2PP/3PP

<!---
(This section shall report all new and updated 2PPs and 3PPs integrated
by the service. This includes SW libraries and reusable images embedded
as 2PP in the service.)

(NOTE: Either PRIM or Mimer product ID can be used in the list depending
on the PLM System used by the Service.)
-->
The following 2PP/3PP’s are new or updated:

| Name              | Product ID     | Old version | New version |
|-------------------|----------------|-------------|-------------|
| CBOS              | APR 901 0622   | 5.17.0-9    | 6.0.0-18    |


## Helm Chart Link

The following table shows the repository manager links for this release:

| Release                                                 | Helm package link                                                                                                                            |
|---------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| 5G PM Event File Transfer and Processing HELM 1.121.0-2 | https://arm.seli.gic.ericsson.se/artifactory/proj-eric-oss-drop-helm/eric-oss-5gpmevt-filetx-proc/eric-oss-5gpmevt-filetx-proc-1.121.0-2.tgz |

## Related Documents

> The following list contains all the document deliverables. The
> documents are stored in Eridoc. All the documents can be accessible
> from Marketplace, see chapter “Product Documentation” for details.
> Revisions to be stated in letters, e.g A, B, C, etc.


| Document ID            | Title                                         | Rev |
|------------------------|-----------------------------------------------|-----|
| 19817-APR 201 536/1    | Application Developers Guide                  | 1   |
| 1553-APR  201 536/1    | User Guide                                    | 1   |
| 15519-CAF 201 536/1    | API Documentation                             | 2   |
| 15241-APR 201 536/1    | Test Specification                            | 1   |
| 00664-APR 201 536/1    | Risk Assessment and Privacy Impact Assessment | 3   |
| 1597-APR 201 536/1     | Vulnerability Analysis                        | 2   |
| 152 83-APR 201 536/1   | Test Report                                   | 1   |


# Product Documentation

## Developer Product Information

The Developer Product Information (DPI) documentation can be accessed
using the following link:\
<https://adp.ericsson.se/marketplace/5g-pm-event-file-transfer-and-processing/documentation/development/dpi/>

## Customer Product Information

The service provides reusable Customer Product Information (CPI) content
that can be reused in ADC application CPI libraries. This content is
published in an ADC specific ELEX library to show how the information
could be reused. It helps applications to produce their own CPI
libraries based on the reusable content. The ELEX library can be
accessed using the following link:
<https://calstore.internal.ericsson.com/elex?LI=EN/LZN7950041*&FN=2_1543-AOM901560Uen.*.html>.

| Document ID         | Title                          |
|---------------------|--------------------------------|
| 2/1543-CSX 101 135  | ADC System Administrator Guide |
| 2/15901-CSX 101 135 | ADC Troubleshooting Guide      |

<!---
(If no CPI provided at all for this service write: This service does not
provide any CPI content.)

The service provides reusable Customer Product Information (CPI) content
that can be reused in ADC application CPI libraries. This content is
published in an ADC specific ELEX library to show how the information
could be reused. It helps applications to produce their own CPI
libraries based on the reusable content. The ELEX library can be
accessed using the following link:

<http://calstore.internal.ericsson.com/elex?LI=EN/LZN7950007*>.

The ADC example documents in this ELEX library are only used to
illustrate how CPI topics from DITA CMS DRM "ADC Content for Reuse"
could be reused in ADC application documents. The ADC example documents
must in general not be published in CPI libraries of ADC applications.
How ADC CPI is to be reused by ADC applications is described in the ELEX
library, see HOW TO REUSE OVERVIEW
(<http://calstore.internal.ericsson.com/elex?LI=EN/LZN7950007*&FB=0_0_0&FN=1_1551-LZA9018693Uen.*.html>).
-->

# Deployment Information

The 5G PM event File Transfer & Processing Service can be deployed in a Kubernetes PaaS
environment.

## Deployment Instructions

The target group for the deployment instructions is only application
developers and application integrators. Deployment instruction can be
found in [5G PM Event File Transfer & Processing User
Guide](https://adp.ericsson.se/marketplace/5g-pm-event-file-transfer-and-processing/documentation/development/dpi/service-user-guide).

## Upgrade Information

This is a new product.

# Verification Status

The verification status is described in the [5G PM Event File Transfer & Processing Test Report](https://adp.ericsson.se/marketplace/5g-pm-event-file-transfer-and-processing/documentation/development/release-documents/test-report).

## Stakeholder Verification

It is verified in ADC APP staging. See [CI/CD
Dashboard](https://cicd-ng.web.adp.gic.ericsson.se/view/6/dashboard/22) for further
understanding.

Note: that result showing in this CI/CD Dashboard pipeline is always
result of latest build and is not this specific PRA release.

# Support

For support use the [JIRA](https://jira-oss.seli.wh.rnd.internal.ericsson.com/secure/RapidBoard.jspa?rapidView=7820&projectKey=IDUN&view=planning&selectedIssue=IDUN-5166&issueLimit=100)
project, please also see the 5G PM event File Transfer & Processing Service troubleshooting
guidelines in Marketplace where you will find more detailed support
information.

# References

1. [JIRA](https://jira-oss.seli.wh.rnd.internal.ericsson.com/secure/RapidBoard.jspa?rapidView=7820&projectKey=IDUN&view=planning&selectedIssue=IDUN-5166&issueLimit=100)

2. [Marketplace](https://adp.ericsson.se/marketplace/5g-pm-event-file-transfer-and-processing)

3. [CPI](http://calstore.internal.ericsson.com/elex?LI=EN/LZN7950007*)

