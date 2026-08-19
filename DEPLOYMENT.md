# Jenkins CI/CD — AI Career backend

Same shape as `ite-sb-api`: Jenkins builds the JAR, bakes a thin runtime image,
pushes it to Artifact Registry, then SSHes into the app server and restarts one
compose service. Two repos, two pipelines, one compose stack on the server.

| Repo | Image | Compose service | Tag key in `.env` | Deploy branch |
| --- | --- | --- | --- | --- |
| `ai_interview_backend_api` | `ai-career-api` | `api` | `API_IMAGE_TAG` | `chanchhay-dev-two` |
| `gateway-bff` | `ai-career-gateway` | `gateway` | `GATEWAY_IMAGE_TAG` | `master` |

Only the gateway is published through Traefik. The API sits on the internal
compose network and is reached as `http://api:8080`.

## Files added

Backend repo:
- `Jenkinsfile` — pipeline
- `Dockerfile.ci` — thin JRE image, expects `app.jar` from the Jenkins build
- `compose.yml` — the whole server stack (db + api + gateway)
- `.env.example` — template for the server-side `.env`
- `Dockerfile` (unchanged) — still the multi-stage image Railway builds

Gateway repo:
- `Jenkinsfile`, `Dockerfile.ci`, `.dockerignore`

## One-time server setup

```bash
# Traefik's shared network, if it does not exist yet
docker network create proxy

sudo mkdir -p /opt/apps/ai-career
sudo chown "$USER":"$USER" /opt/apps/ai-career
cd /opt/apps/ai-career

# copy compose.yml from this repo, and .env.example -> .env, then fill .env in
chmod 600 .env

# let the server pull from Artifact Registry
gcloud auth configure-docker asia-southeast1-docker.pkg.dev

docker compose up -d db
docker compose up -d api gateway
```

`compose.yml` is not auto-synced — when you change it here, copy it to the
server again.

## One-time Jenkins setup

1. Credentials → add the SSH private key that the app server accepts, with id
   `ai-career-server-ssh` (or change `SSH_CRED` in both Jenkinsfiles).
2. Jenkins agent needs: JDK 25, Docker, and `gcloud auth configure-docker` for
   the registry host so `docker push` works.
3. Create two Multibranch Pipeline jobs, one per repo, pointing at the
   `Jenkinsfile` in each.
4. Fill in the `CHANGE_ME_*` values at the top of both Jenkinsfiles
   (`PROJECT_ID`, `DEPLOY_HOST`) and the `CHANGE_ME_DOMAIN` Traefik rule in
   `compose.yml`.

## What a deploy does

Rewrites its own `*_IMAGE_TAG` line in `/opt/apps/ai-career/.env` to the commit
SHA, then `docker compose pull <service>` and
`docker compose up -d --no-deps <service>`. `--no-deps` is what keeps an API
deploy from bouncing the database or the gateway.

Rollback: set the tag key in `.env` to an older SHA and re-run those two
commands by hand.
