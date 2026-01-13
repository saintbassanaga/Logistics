package tech.bytesmind.logistics.agency.domain;

/**
 * Represents different types of locations within an organization or operational structure.
 * This enum defines the distinct categories of locations used for business operations,
 * logistics, and customer interaction points.
 * <p>
 * Possible values:
 * - HEADQUARTERS: The main office or central location of the organization.
 * - BRANCH: A secondary or regional office of the organization.
 * - WAREHOUSE: A storage facility used for inventory and goods.
 * - PICKUP_POINT: A designated location for customers to pick up orders.
 * - SORTING_CENTER: A facility where items are sorted for distribution or delivery.
 */
public enum LocationType {
    HEADQUARTERS,
    BRANCH,
    WAREHOUSE,
    PICKUP_POINT,
    SORTING_CENTER
}