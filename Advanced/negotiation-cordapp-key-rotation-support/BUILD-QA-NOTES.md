# Build / QA Notes — negotiation-cordapp-key-rotation-support (ENT-14906)

How to build and test this sample locally against Corda Enterprise **4.15-SNAPSHOT**, which is
not published publicly and only lives in R3's internal Artifactory.

## Background — why the build needs setup

The sample needs `4.15-SNAPSHOT` of **two** groups:

- `net.corda:corda-core` (OS)
- `com.r3.corda:corda`, `corda-rpc`, `corda-jackson`, `corda-node-driver`, `corda-node-api`, `corda-shell` (Enterprise)

These only exist in R3's internal Artifactory (`r3-corda-dev`). Two issues in the committed
`repositories.gradle` block a direct Artifactory resolve:

1. Its Enterprise repo is wired only for `com.r3.*`, so `net.corda:corda-core` is routed to
   mavenLocal only.
2. It lists `https://repo.gradle.org/gradle/libs-releases`, which returns **HTTP 409** for these
   paths — a fatal error that aborts resolution before any Enterprise repo is consulted.

Rather than edit the committed file, a **local Gradle init script** handles both, scoped to the
`samples-java` tree only.

## One-time machine setup (already in place)

- **`~/.gradle/init.d/corda-ent-qa.gradle`** — for projects under `samples-java`: adds
  `https://software.r3.com/artifactory/r3-corda-dev` serving **both** `net.corda` and
  `com.r3.corda`, and removes `repo.gradle.org` so its 409 can't abort resolution. Reads creds
  from env (`CORDA_ENTERPRISE_REPOSITORY_*` / `CORDA_ARTIFACTORY_*`) or gradle.properties.
- **`~/.corda-qa/hosts`** — maps `software.r3.com` to the live AWS ELB origin, bypassing
  Cloudflare. The JVM uses this file via `-Djdk.net.hosts.file=...` already set in
  `~/.gradle/gradle.properties` (no root / `/etc/hosts` needed).

## Step 1 — Get an Artifactory identity token

Raw SSO passwords are rejected with 401, so generate a token:

1. Open <https://software.r3.com> and sign in (SSO).
2. Profile (top-right) → **Edit Profile** / **Generate an Identity Token**.
3. Copy the token (shown once).

## Step 2 — Set credentials (pick one)

**Option A — `~/.gradle/gradle.properties`** (persists across shells):

```
cordaEnterpriseRepositoryUsername=your.username@r3.com
cordaEnterpriseRepositoryPassword=<paste-identity-token>
```

**Option B — terminal env vars** (nothing written to disk):

```bash
export CORDA_ENTERPRISE_REPOSITORY_USERNAME='your.username@r3.com'
export CORDA_ENTERPRISE_REPOSITORY_PASSWORD='<paste-identity-token>'
```

## Step 3 — Assert the DNS bypass is live

It can revert; re-assert before building (no sudo needed):

```bash
printf '34.242.109.200 software.r3.com\n' > ~/.corda-qa/hosts && cat ~/.corda-qa/hosts
```

## Step 4 — Resolve + compile

```bash
cd /Users/saurabh.katke/Corda-4/others/samples-java/Advanced/negotiation-cordapp-key-rotation-support
./gradlew clean assemble --refresh-dependencies --console=plain
```

Expect `BUILD SUCCESSFUL`. The first run downloads the Corda artifacts (a few minutes).

## Step 5 — Run the key-rotation tests

```bash
./gradlew :workflows:test --tests "net.corda.samples.negotiation.flows.KeyRotationFlowTests" --console=plain
```

HTML report: `workflows/build/reports/tests/test/index.html`

## Troubleshooting

| Symptom | Cause → Fix |
|---|---|
| `401 Unauthorized` from `software.r3.com` | Bad/missing token → regenerate identity token; check for typos / trailing spaces in Step 2 |
| `UnknownHostException` / connect timeout to `software.r3.com` | `~/.corda-qa/hosts` stale → re-run Step 3. If still failing, the ELB IP rotated → `dig +short a32e42249d58447e1864cb368b1582d9-1839528598.eu-west-1.elb.amazonaws.com` and put a returned IP in the hosts file |
| `409` from `repo.gradle.org` returns | init script not applied → confirm `~/.gradle/init.d/corda-ent-qa.gradle` exists and you're building under `samples-java` |
| `Timeout waiting to lock ... fileContent.lock` | stuck Gradle daemon → `./gradlew --stop; pkill -f 'org.gradle.launcher.daemon.bootstrap.GradleDaemon'` |

## Notes

- `4.15-SNAPSHOT` is a moving target; `--refresh-dependencies` forces Gradle to re-check for the
  latest snapshot and bypass any cached negative lookups.
- The key-rotation tests require Corda **Enterprise** flows (platform version 170); they will not
  run against Corda OS nodes.
- Issues 1 and 2 above are real gaps in the sample's committed `repositories.gradle` for anyone
  using the Artifactory path — worth raising with the sample author (ENT-14906).
