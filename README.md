# Report Hub

A Service able to manage and execute FHIR Quality Reports.

## Workflow

* receive Beam task
* parse a FHIR Task out of the Beam tasks body which is a FHIR Bundle
* the following information will be added to the FHIR Task resource:
  * the status will be set to received
  * the requester will be the Beam application of the "from" field
  *

### Local

The local workflow, without consideration of external messaging, is solely managed by creating and
updating Task resources. The Report Hub uses polling (and later FHIR Subscriptions) to get noticed
of new tasks and is responsible for drivung the tasks to completion, be it success or failure.

## Fragen an Beam

* eigentlich will ich nur Nachrichten verschicken und brauche die Task/Result Abstraktion nicht
* encryptete Nachrichten über einen zentralen Broker schicken ist eine gute Idee, warum nehmen wir
  nicht einfach Signal?
* warum muss ich gegenüber dem Proxy long-pollen, warum kann der mich nicht anpingen?
* was passiert, wenn ich 1000 Nachrichten an eine App schicke, die nicht online ist? muss ich dann
  1000 Verbindungen offenhalten um die Results zu pollen? also eine Verbindung pro Nachricht? dass
  muss min. auf Queue-Ebene gehen.
* haben Nachrichten eine Reihenfolge? Was passiert, wenn die Abarbeitung der Nachrichten
  reihenfolgenabhängig ist?
* es sollte möglich sein, dass ein Task/Message mit einer vom Client vorgegebenen ID Idempotent nur
  einmal verschickt wird

## Dev Setup

Start the services Report Hub depends on by running:

```sh
docker compose up
```

Start Report Hub using Maven:

```sh
mvn spring-boot:run -Dspring-boot.run.arguments="--logging.level.de=DEBUG"
```

## License

Copyright 2022 - 2022 The Samply Community

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
