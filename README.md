# Report Hub

A Service able to manage and execute FHIR Quality Reports.

## Workflow

* receive Beam Task
* parse a FHIR Message out of the Beam Tasks body
* offer that FHIR Message by means of [MessageBroker][1]

### Local

The local workflow, without consideration of external messaging, is solely managed by creating and
updating Task resources. The Report Hub uses polling (and later FHIR Subscriptions) to get noticed
of new tasks and is responsible for driving the tasks to completion, be it success or failure.

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

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is
distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing permissions and limitations under the
License.

[1]: <https://github.com/samply/report-hub/blob/main/src/main/java/de/samply/reporthub/service/fhir/messaging/MessageBroker.java>
