# Messages

## Evaluate Measure

### Beam Task

```json
{
  "id": "9e1ad660-9fa4-465a-9fca-b21c24c45347",
  "from": "app.proxy.broker",
  "to": [
    "app.proxy.broker"
  ],
  "metadata": "<currently not used>",
  "body": "<base64 JSON encoded Beam Task Body>",
  "failure_strategy": {
    "retry": {
      "backoff_millisecs": 1000,
      "max_tries": 5
    }
  }
}
```

### Beam Task Body

```json
{
  "resourceType": "Bundle",
  "type": "message",
  "entry": [
    {
      "resource": {
        "resourceType": "MessageHeader",
        "eventCoding": {
          "system": "https://dktk.dkfz.de/fhir/CodeSystem/message-event",
          "code": "evaluate-measure"
        },
        "focus": [
          {
            "reference": "urn:uuid:0740757b-6415-4d98-94b9-3343d8c5a957"
          }
        ]
      }
    },
    {
      "fullUrl": "urn:uuid:0740757b-6415-4d98-94b9-3343d8c5a957",
      "resource": {
        "resourceType": "Parameters",
        "parameter": [
          {
            "name": "measure",
            "valueCanonical": "https://dktk.dkfz.de/fhir/Measure/exliquid-dashboard"
          }
        ]
      }
    }
  ]
}
```

### FHIR Message

The FHIR Message is build from the Beam Task. The Beam Task Id is put into the FHIR MessageHeader as
FHIR Message Id.

```json
{
  "resourceType": "Bundle",
  "type": "message",
  "entry": [
    {
      "resource": {
        "resourceType": "MessageHeader",
        "id": "9e1ad660-9fa4-465a-9fca-b21c24c45347",
        "eventCoding": {
          "system": "https://dktk.dkfz.de/fhir/CodeSystem/message-event",
          "code": "evaluate-measure"
        },
        "focus": [
          {
            "reference": "urn:uuid:0740757b-6415-4d98-94b9-3343d8c5a957"
          }
        ]
      }
    },
    {
      "fullUrl": "urn:uuid:0740757b-6415-4d98-94b9-3343d8c5a957",
      "resource": {
        "resourceType": "Parameters",
        "parameter": [
          {
            "name": "measure",
            "valueCanonical": "https://dktk.dkfz.de/fhir/Measure/exliquid-dashboard"
          }
        ]
      }
    }
  ]
}
```

## Evaluate Measure Response

### FHIR Message

```json
{
  "resourceType": "Bundle",
  "type": "message",
  "entry": [
    {
      "resource": {
        "resourceType": "MessageHeader",
        "id": "8c41540d-f832-4bdf-b2a3-f0613714f5e8",
        "eventCoding": {
          "system": "https://dktk.dkfz.de/fhir/CodeSystem/message-event",
          "code": "evaluate-measure-response"
        },
        "response": {
          "identifier": "9e1ad660-9fa4-465a-9fca-b21c24c45347",
          "code": "ok"
        },
        "focus": [
          {
            "reference": "urn:uuid:224a1318-860b-449c-b20f-03f76fc243fb"
          }
        ]
      }
    },
    {
      "fullUrl": "urn:uuid:224a1318-860b-449c-b20f-03f76fc243fb",
      "resource": {
        "resourceType": "MeasureReport",
        "status": "complete",
        "type": "summary",
        "measure": "https://dktk.dkfz.de/fhir/Measure/exliquid-dashboard",
        "date": "1970-01-01T00:00:00Z",
        "period": {
          "start": "1970-01-01T00:00:00Z",
          "end": "1970-01-01T00:00:00Z"
        }
      }
    }
  ]
}
```

### Beam Result

The Beam Result is build from the FHIR Message. The `Bundle.enty[0].resource.response.identifier` is
the Beam Task Id. The `Bundle.enty[0].resource.response.code` will be mapped to the Beam Result
Status. The MeasureReport is the Beam Result Body.

```json
{
  "from": "app.proxy.broker",
  "to": [
    "app.proxy.broker"
  ],
  "task": "9e1ad660-9fa4-465a-9fca-b21c24c45347",
  "status": "succeeded",
  "metadata": "<currently not used>",
  "body": "<base64 JSON encoded Beam Result Body>"
}
```

### Beam Result Body

The Beam Result is just

```json
{
  "resourceType": "MeasureReport",
  "status": "complete",
  "type": "summary",
  "measure": "https://dktk.dkfz.de/fhir/Measure/exliquid-dashboard",
  "date": "1970-01-01T00:00:00Z",
  "period": {
    "start": "1970-01-01T00:00:00Z",
    "end": "1970-01-01T00:00:00Z"
  }
}
```
