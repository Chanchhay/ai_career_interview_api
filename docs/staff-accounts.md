# Creating a staff account

Staff are SUPER_ADMIN, MODERATOR and FINANCE. Creating one takes **two steps**,
in this order — a Keycloak user on its own cannot use the admin console.

## Why two steps

Keycloak holds the identity. The API holds its own `user_accounts` row, keyed by
the Keycloak user ID, plus one profile row per staff role. Nothing creates those
rows on the fly: `POST /api/v1/auth/register` is the only code path that inserts
them, and it accepts SEEKER and RECRUITER only.

The profile rows are not bookkeeping. `company_verifications.moderator_profile_id`
records who approved a company, so an approval cannot be written unless the
acting account has a moderator profile — a super admin included.

## Step 1 — Keycloak

Create the user, set a password, then assign the realm role under **Role
mapping**: `SUPER_ADMIN`, `MODERATOR`, or `FINANCE`.

Roles are read from the access token, so a role granted to a signed-in user does
nothing until they sign out and back in and a fresh token is minted.

## Step 2 — the database

Copy the user's **ID** from Keycloak (Users → the user → ID). It is a UUID and it
is the token's `sub` claim — not the username.

Put it in `v_keycloak_user_id` at the top of
[`scripts/provision-staff-account.sql`](../scripts/provision-staff-account.sql)
and run the whole block against the API's database:

```bash
psql "$DATABASE_URL" -f scripts/provision-staff-account.sql
```

It also pastes into DBeaver, pgAdmin or Railway's query console unchanged. The
script is one `DO` block, guarded and safe to re-run: it reports what it created
and does nothing on a second run.

It creates all three staff profiles — moderator, admin and finance — regardless
of the realm role. The realm role is what actually gates access; the profiles
just make the account usable, and creating all three now saves a second visit
when the finance endpoints land.

## Checking it worked

Signed in as that account:

| Call | Before | After |
| --- | --- | --- |
| `GET /api/v1/me` | 404 *Authenticated user is not registered in the application* | 200, with `profiles.moderatorProfileId` set |
| `POST /api/v1/moderator/companies/{id}/approve` | 404 *Moderator profile was not found for authenticated user* | 200 |

If `/me` still 404s, the UUID in the script did not match the token's `sub`.
The API logs the subject of every `/me` call (`jwt subject: …`) — compare that
against what you pasted.

If calls come back **403** instead, the account is missing the realm role, not
the database rows. See the rules in
[`SecurityConfig`](../src/main/java/co/istad/ai_interview_app/config/security/SecurityConfig.java),
which is where all authorization lives.

## Worth fixing eventually

Every staff account nnext.js:eeds a manual SQL step, and an account provisioned in
Keycloak but not in the database fails with a 404 that reads like a missing
company rather than a missing account. Auto-provisioning on first authenticated
request — create the `user_accounts` row and the profiles implied by the token's
realm roles — would remove this page entirely.
