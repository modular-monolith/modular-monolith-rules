# Publishing to Maven Central

## Release sequence (the short version)

1. Merge the release branch to `main` and wait for CI to go green.
2. Trigger the **Release** workflow manually: GitHub > Actions > Release >
   Run workflow, with `version` set to `0.2.0`. The workflow is
   `workflow_dispatch` only; pushing a tag does not start it. The workflow
   itself creates and pushes the `v0.2.0` tag and the GitHub release after a
   successful deploy, so do not tag by hand.
3. Log in to <https://central.sonatype.com>, open Deployments, and press
   **Publish** on the validated deployment. The build uses
   `autoPublish: false`, so this manual approval is the final gate before
   artifacts go public. Publishing is permanent; artifacts cannot be removed
   from Maven Central afterwards.
4. Run the post-publish verification below once the artifacts resolve
   (allow up to a couple of hours for propagation to search).

## Publishing route

The project publishes through the **Central Publishing Portal**
(central.sonatype.com), not the legacy OSSRH path. Two places encode this and
must stay in sync:

- `release.yml` configures `server-id: central` in the `setup-java` step
- the root POM's release profile runs `central-publishing-maven-plugin`
  with `publishingServerId: central`

The release version is applied at build time by `mvn versions:set`, so the
sources on `main` always carry the development version (`0.2.0-SNAPSHOT`).
Only `modulith-rules-core` and `modulith-rules-spring` are published; the
example module sets `skipPublishing` and stays in the repo only.

## Prerequisites (one-time setup, outside the repo)

- [ ] A Sonatype Central account at central.sonatype.com with the
      `io.modulith` namespace verified.
- [ ] A user token generated in the Central Portal account page. The token
      name and token value are the credentials Maven uses, not your portal
      login.
- [ ] A GPG key pair for signing, with the public key published to a
      keyserver (`gpg --keyserver keyserver.ubuntu.com --send-keys <KEYID>`).
      Central verifies signatures against public keyservers.
- [ ] The following GitHub Actions secrets, under these exact names, which
      `release.yml` expects:

| Secret name | Content |
|---|---|
| `OSSRH_USERNAME` | Central Portal token name (the name is historical; the value is a Portal token, not an OSSRH login) |
| `OSSRH_TOKEN` | Central Portal token value |
| `GPG_PRIVATE_KEY` | ASCII-armored private signing key (`gpg --armor --export-secret-keys <KEYID>`) |
| `GPG_PASSPHRASE` | Passphrase for that key |

## Post-publish verification

This is a manual check, run after pressing Publish in the portal.

1. Create a throwaway Maven project:

   ```bash
   mvn archetype:generate -DgroupId=com.acme.check -DartifactId=central-check \
       -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
   ```

2. Add the published modules and ArchUnit to its `pom.xml`:

   ```xml
   <dependency>
       <groupId>io.modulith</groupId>
       <artifactId>modulith-rules-core</artifactId>
       <version>0.2.0</version>
       <scope>test</scope>
   </dependency>
   <dependency>
       <groupId>io.modulith</groupId>
       <artifactId>modulith-rules-spring</artifactId>
       <version>0.2.0</version>
       <scope>test</scope>
   </dependency>
   <dependency>
       <groupId>com.tngtech.archunit</groupId>
       <artifactId>archunit-junit5</artifactId>
       <version>1.4.2</version>
       <scope>test</scope>
   </dependency>
   ```

3. Add a trivial test and confirm it downloads, compiles, and runs:

   ```java
   @Test
   void resolvesFromCentral() {
       JavaClasses classes = new ClassFileImporter().importPackages("com.acme.check");
       ModulithRules.forPackage("com.acme.check", "app").cycleRules()
               .noModuleCycles().check(classes);
   }
   ```

4. `mvn -U test` must pass with the dependencies coming from Maven Central,
   not the local repository (`rm -rf ~/.m2/repository/io/modulith` first to
   be certain).

## Security posture

Already in place:

- Workflow tokens are least privilege: CI is read-only, the release job has
  `contents: write` only for the tag and release.
- Credentials only enter the build through GitHub Actions secrets mapped to
  environment variables; nothing is echoed or written to disk.
- Artifacts are GPG-signed by the release profile, and the Portal validates
  signatures, checksums, sources, and javadoc before anything can publish.
- `autoPublish` is off, keeping a human approval between CI and the public
  repository.
- Builds are reproducible via `project.build.outputTimestamp`, so a rebuild
  of a tag can be compared byte for byte against the published jars.
- Dependabot watches GitHub Actions and Maven dependencies weekly.

Recommended, outside the repo:

- Enable two-factor authentication on the Sonatype and GitHub accounts, and
  branch protection plus required CI on `main`.
- Restrict who can run the Release workflow (repository settings, Actions
  permissions) since anyone who can dispatch it can publish with the stored
  secrets.
- Consider pinning third-party actions (`softprops/action-gh-release`) to a
  full commit SHA rather than a tag. Look the SHA up from the action's
  repository when doing this; a tag can be moved, a SHA cannot.
- Rotate the Portal token and GPG passphrase if a fork or contributor ever
  gains write access to secrets, and set a GPG key expiry so a leaked key
  ages out.
