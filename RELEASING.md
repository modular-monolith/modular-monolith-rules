# Releasing

Releases are cut from `main` by the Release workflow in GitHub Actions. Nothing is
published from a laptop.

## Steps

1. Make sure `main` is green and `RELEASE_NOTES_v<version>.md` exists.
2. Actions, Release, Run workflow, enter the version (for example `0.4.0`) and run.
3. The workflow sets the version, builds, signs, uploads to the Central Portal and
   waits for validation. It then tags `v<version>` and creates the GitHub release.
4. In the Central Portal (Publishing, Deployments) the deployment sits as VALIDATED.
   Click Publish. Central syncs within about half an hour.
5. Replace the auto-generated GitHub release body with the contents of the release
   notes file.
6. Bump `main` to the next `-SNAPSHOT` version and update the version strings in
   `README.md` and `quickstart/README.md` if they changed.

## Secrets the workflow needs

| Secret | What it is |
|--------|------------|
| `GPG_PRIVATE_KEY` | ASCII-armoured private key, `gpg --armor --export-secret-keys <KEYID>` |
| `GPG_PASSPHRASE` | The passphrase for that key |
| `CENTRAL_USERNAME` | The user token name from central.sonatype.com (Account, Generate User Token) |
| `CENTRAL_TOKEN` | The user token password shown alongside it |

## Things that have bitten us

- **Maven 3.10 on the hosted runners breaks the Central bundle.** The runner image
  moved to Maven 3.10, and `central-publishing-maven-plugin` 0.11.0 then stages
  bookkeeping files into the bundle, which Central rejects with "Bundle has content
  that does NOT have a .pom file". The workflow pins Maven 3.9 until the plugin
  catches up. See apache/maven#13388.
- **Child modules must state their own `url` and `scm`.** Maven appends the
  artifactId to inherited URLs, so without them Central links to paths like
  `.../modular-monolith-rules/modulith-rules-core` that do not exist. The values come
  from parent properties so they stay in step.
- **Every jar artifact needs sources and javadoc jars**, including the archetype,
  which has neither of its own; it ships placeholder jars built by the jar plugin.
- **`autoPublish` is off on purpose.** A deployment that validates still needs a
  Publish click in the Portal, which gives a last look before anything goes live.
