<!--Document Template information:
Prepared:Shaun Kinsella
Approved:***
Document Name:api-documentation
Document Number:n/155 19-CAF 201 536/1
-->
# 5G PM Event File Transfer and Processing Service API Specification

## Overview

This is the API definition for the 5G PM Event File Transfer and Processing Service, detailing how to produce input for the service to process 5G PM Event files and how to consume events processed by it.
The service consumes kafka data collection controller and file notification service topics via a KafkaListener interface, and can produce 5G PM Events onto two Output Topics on the same Kafka Cluster. 
The Ericsson output topic is **not currently supported**.

### Data Collection Controller Topic

This topic notifies services of changes in subscriptions.
Upon receiving a notification containing the service name and service instance name the microservice will fetch subscriptions from data catalog.

This topic is created by the Data Catalog.

### Kafka Input Topic message

The Input Topic message consists of the below schema in JSON format.

```json
{
"nodeName": "CellTrace_DU0_1_6",
"dataType": "PM_CELLTRACE",
"nodeType": "RadioNode",
"fileLocation": "/sftp/ericsson/pmic1/CELLTRACE/CellTrace_DU0_1_6/A20220322.0715-0730_SubNetwork=ONRM_ROOT_MO_R,SubNetwork=RAN,SubNetwork=QC,SubNetwork=SD,MeContext=gNB3347,ManagedElement=gNB3347_celltracefile_CellTrace_DU0_1_6.gpb.gz"
}
```

#### Parameters

| Name         | In      | Type   | Required | Description                                                 |
|--------------|---------|--------|----------|-------------------------------------------------------------|
| nodeName     | payload | String | true     | The name of the node from which the PmEvent file originated |
| dataType     | payload | String | true     | The type of file, i.e. PM_CELLTRACE                         |
| nodeType     | payload | String | true     | The type of the node                                        |
| fileLocation | payload | String | true     | The physical location of the file on the ENM                |
Additional fields may be included in the input topic message but will be ignored by the service's listener.

The naming convention of the Input Topic name follows:

```text
"file-notification-service--5g-event--[ENM-IDENTIFIER]"
```
This topic is created by the File Notification Service.

### Kafka Output Topics message
The output topics are created by the 5G PM Event File Transfer and Processing Service. <br>
The functionality relating to the Ericsson output topic is **not currently supported**. <br>
The 5G PM Event File Transfer and Processing Service produces Kafka messages to **two** output topics following the below schema. <br>
These messages will be produced onto the same Kafka Cluster as the Input Topic messages. <br>
The Events written to this topic are transactional, i.e. all events must be successfully written to the partition for the transaction to be marked as successful. <br>
For Services that wish to consume these events and ensure that each message is only processed using "exactly once semantics", it is necessary that the consumer is configured with:

```text
"isolation_level_config = "read_committed"
```
This will ensure that the consumer will not read messages that are part of a rolled back transaction.

#### Kafka output topics message parameters

| Name     | In      | Type                                 | Required | Description                                                          |
|----------|---------|--------------------------------------|----------|----------------------------------------------------------------------|
| event_id | header  | String                               | true     | The Event ID of the inner PmEvent specifying the schema              |
| nodeName | key     | String                               | true     | The name of the node from which the PmEvent file originated          |
| pmEvent  | payload | PmEventOuterClass.PmEvent (Protobuf) | true     | Wrapper class common to all PmEvents containing the pm event payload |

The naming convention of the **standardized** Output Topic name follows:

```text
"5g-pm-event-file-transfer-and-processing--standardized"
```
The naming convention of the **Ericsson** Output Topic name follows:

```text
"5g-pm-event-file-transfer-and-processing--ericsson"
```
> **_Note:_** The creation of and the producing to the Ericsson topic requires the helm parameter eventRegulation.produceNonStandard=true.

### Data Catalog Data Service Instance De-Registration

**Description**: De-registers the current data service instance from Data Catalog in the current namespace

**Path**: DELETE /data-service-instance

**Request URL**:
```
Designed for internal use only
curl -X DELETE "http://${FIVEGPMEVENT_SERVICE_NAME}:${FIVEGPMEVENT_SERVICE_PORT}/data-service-instance" 
```
**Query parameters**

None

**Responses**

**Media type**: application/json

| Response Code              | Response Status           | Description                                                                                                                                              | Response Data Structure    |
|----------------------------|---------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|
| **200**, **204** , **404** | OK, No Content, Not Found | Successfully de-registered the data service instance from Data Catalog                                                                                   | Void                       |
| **Other response**         | Bad Request               | Failure to de-register the data service instance from Data Catalog or <br/> Runtime error during data service instance de-registration from Data Catalog | Void                       |
