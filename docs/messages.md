# Messages

## Evaluate Measure

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
