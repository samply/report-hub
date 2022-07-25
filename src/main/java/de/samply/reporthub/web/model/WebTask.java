package de.samply.reporthub.web.model;

import java.time.OffsetDateTime;
import java.util.Optional;

public record WebTask(
    String status,
    Link activityDefinitionLink,
    OffsetDateTime lastModified,
    Optional<Link> reportLink) {

}
