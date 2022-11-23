# Træfik (Report Hub Behind a Reverse Proxy)

Inside the `docs/traefik` directory, you will find a Docker Compose file with a project putting the
Report Hub and it's FHIR servers behind a Træfik reverse proxy. You can start the project with:

```sh
docker-compose up
```

and access the Report Hub via http://localhost/report-hub.
