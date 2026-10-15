# Ladica Sync backend

Backend for [Ladica](https://ladica.hrva.cc/), a local-first bookmark manager maintained by [HrvaLabs](https://hrvalabs.net/).

The frontend lives in [HrvaLabs/linker-fe](https://github.com/HrvaLabs/linker-fe). The backend source is [HrvaLabs/linker-be](https://github.com/HrvaLabs/linker-be). This repository contains the Spring Boot backend, its database migrations, Docker configuration and a GitHub Actions image-publishing workflow.

## Development

Requires Java 25, Maven and PostgreSQL.

```sh
mvn clean verify
```

For local supporting services, see `docker/docker-compose.yml`. Do not commit configuration files containing credentials.

## Container publishing

On pushes to `main`, GitHub Actions builds the backend and publishes multi-platform images to GitHub Container Registry (GHCR):

```text
ghcr.io/hrvalabs/linker-be:main
ghcr.io/hrvalabs/linker-be:<commit-sha>
```

The workflow lowercases the GitHub owner when forming a GHCR image tag and includes an OCI `org.opencontainers.image.source` label pointing at `HrvaLabs/linker-be`. This makes the container reference valid and associates the image with its source repository.

New GHCR packages may initially be **private**, even when the repository is public. Verify package visibility or configure read authentication explicitly before attempting to pull an image on a server.

## Deployment

The Proxmox-oriented example is in `docker/pve/docker-compose.yml`. It references an external proxy network and uses environment variables for application secrets.

The image is deliberately configurable:

```dotenv
# Existing image (default until migration is validated)
LADICA_BACKEND_IMAGE=ghcr.io/karlohrvacic/linker-be:main

# Switch to this only after the new image has been published and tested
# LADICA_BACKEND_IMAGE=ghcr.io/hrvalabs/linker-be:main
```

Before updating production:

1. Confirm the new GitHub Actions build succeeded and published `ghcr.io/hrvalabs/linker-be:main`.
2. Confirm the GHCR package can be pulled by the deployment host; make it public or configure an appropriate read-only token.
3. Pull the new image explicitly, and keep the previous image cached for rollback.
4. Update the deployment host's Compose configuration to accept `LADICA_BACKEND_IMAGE`, then set it to the new image.
5. Recreate only the `ladica-sync-backend` service, leaving PostgreSQL and its volumes unchanged.
6. Check startup logs and exercise Ladica sign-in and cross-device synchronization.

Example commands **on the deployment host**, in the directory containing the Compose file:

```sh
docker pull ghcr.io/hrvalabs/linker-be:main
docker compose up -d --no-deps ladica-sync-backend
docker compose logs --tail=100 ladica-sync-backend
```

The `up` command uses the image set in `LADICA_BACKEND_IMAGE`; `docker pull` alone does not change the running container. Do not run `down -v`, which would delete persistent database volumes.

To roll back, set `LADICA_BACKEND_IMAGE` to `ghcr.io/karlohrvacic/linker-be:main` (or a previously known-good pinned digest) and recreate only the backend service.

## Repository ownership migration

The image published under the old personal GHCR namespace is **not automatically renamed** when this repository transfers into the HrvaLabs organization. Keep the existing image and running container until the organization-owned package is available and functional.

GitHub redirects the old repository URL, but update code links, CI integrations and deployment documentation to the new organization URL explicitly.
