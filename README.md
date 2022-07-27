# Report Hub

A Service able to manage and execute FHIR Quality Reports.

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
