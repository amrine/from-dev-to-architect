package io.teampulse.organization.api.lifecycle;

/**
 * Public business snapshot without persistence or internal domain types.
 *
 * @param organizationReference functional organization reference
 * @param name normalized organization name
 * @param timezone timezone identifier
 * @param status textual business status
 * @param administratorReference assigned administrator, or null if unassigned
 * @param managerReference assigned manager, or null if unassigned
 */
public record OrganizationLifecycleResult(
    String organizationReference,
    String name,
    String timezone,
    String status,
    String administratorReference,
    String managerReference
) {

    public static final String STATUS_CREATING = "CREATING";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_SUSPENDED = "SUSPENDED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";
}
