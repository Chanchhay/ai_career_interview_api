# Two-level platform lists

Industries, job categories, and skills each have their own parent categories and
subcategories. Existing endpoints and UUIDs remain in place; the hierarchy uses
an optional `parent_id` foreign key within each existing table.

## Migration

`V27__two_level_platform_lists.sql` follows V26 and runs through the existing
Spring Boot Flyway setup at backend startup. Deploy the backend before the admin
frontend that sends `parentId`. No manual database reset or replacement of an
older migration is required.

For each list, V27 creates a new General parent and assigns existing entries to
it **without changing their IDs, fields, or existing associations**. An existing
entry named General is retained as a subcategory; the new parent receives the
first available name such as General (group 1). Empty lists also receive a
General parent. Existing companies, jobs, and resumes keep their current links.

The migration tolerates `parent_id` already being created by Hibernate and does
not regroup newly added parents if the SQL is run again. Its foreign keys
restrict parent deletion and its check constraints reject self-parenting.

## API

The existing `/api/v1/admin/industries`, `/job-categories`, and `/skills` CRUD
endpoints accept `parentId` on create and update:

- `null`: a parent category.
- A UUID: a subcategory belonging to that top-level parent in the same list.

Responses add `parentId` and `parentName`; both are null for a parent. Admin list
responses contain both levels in the existing flat array, so clients group them
by `parentId`. Names remain unique across each list, matching the previous setup.

An existing entry keeps its level. Update a subcategory's `parentId` to move it
between parents. Third levels, self-parenting, and cross-list parents are rejected.
Parent rows are locked while changing their children, and a parent with children
cannot be deleted. A linked subcategory remains protected by its existing foreign
keys.

The public options endpoints return only selectable subcategories, with parent
metadata for grouping. Job and skill search filters accept either a parent ID or
a subcategory ID, while existing facet counts still describe individual
subcategories. Company/job forms must submit a subcategory ID, not a parent ID.
Only active industries beneath active parents appear in public industry options.

Recruiter-created skills and skills found while importing a job description are
subcategories. A supplied `parentId` is used for new skills; otherwise General is
used (or the oldest remaining parent if General was deleted). Existing skills are
reused in their current groups. If there are no parents, an administrator must add
one first. The existing isolated insert transaction and duplicate-name recovery
remain in place.

## Verification

- `./gradlew test`: includes hierarchy CRUD, invalid parents, safe deletion,
  automatic grouping of recruiter skills, and parent-aware job search.
- Run `psql -v ON_ERROR_STOP=1 -f src/test/resources/db/V27__two_level_platform_lists_test.sql`
  against a disposable PostgreSQL database. This verifies unchanged IDs and
  original fields, preserved links, name collisions, foreign-key protection,
  empty lists, and reruns. Its test schemas are rolled back.

The normal H2 test profile disables Flyway; the PostgreSQL check exercises V27's
actual SQL separately.
