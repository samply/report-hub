# Messages

## Evaluate Measure

```json
{
  "resourceType": "Bundle",
  "id": "9e1ad660-9fa4-465a-9fca-b21c24c45347",
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
            "value": "https://dktk.dkfz.de/fhir/Measure/exliquid-dashboard"
          }
        ]
      }
    }
  ]
}
```
