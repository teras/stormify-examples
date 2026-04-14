// Init script that adds the Central Portal "validated but not yet published"
// deployments endpoint as an authenticated Maven repository, so example
// projects can consume stormify artifacts before the release is finalised.
//
// Usage:
//   export CENTRAL_USER=...
//   export CENTRAL_PASS=...
//   export CENTRAL_DEPLOYMENT_ID=<uuid>
//   gradle --init-script path/to/central-validated.init.gradle.kts build
//
// The repository is injected into both settings-level dependency resolution
// (for projects using dependencyResolutionManagement) and into every project's
// repositories list (for projects that declare repositories the old way).
//
// NOTE: the generic /deployments/download/ endpoint is effectively throttled
// in our experience, so we use the deployment-id-scoped endpoint which is
// fast and reliable.

val centralUser: String? = System.getenv("CENTRAL_USER")
val centralPass: String? = System.getenv("CENTRAL_PASS")
val deploymentId: String? = System.getenv("CENTRAL_DEPLOYMENT_ID")

if (centralUser.isNullOrBlank() || centralPass.isNullOrBlank()) {
    throw GradleException(
        "CENTRAL_USER and CENTRAL_PASS environment variables must be set " +
        "to access the Central Portal validated deployments endpoint."
    )
}
if (deploymentId.isNullOrBlank()) {
    throw GradleException(
        "CENTRAL_DEPLOYMENT_ID environment variable must be set to the UUID " +
        "of the validated deployment to consume."
    )
}

val bearer = java.util.Base64.getEncoder()
    .encodeToString("$centralUser:$centralPass".toByteArray(Charsets.UTF_8))

val repoUrl = "https://central.sonatype.com/api/v1/publisher/deployment/$deploymentId/download/"

fun RepositoryHandler.addCentralValidated() {
    maven {
        name = "CentralPortalValidated"
        setUrl(repoUrl)
        credentials(HttpHeaderCredentials::class.java) {
            name = "Authorization"
            value = "Bearer $bearer"
        }
        authentication {
            create("header", HttpHeaderAuthentication::class.java)
        }
        content {
            includeGroup("onl.ycode")
        }
    }
}

// Track whether settings-level dependency resolution has claimed control.
// If it has (and in FAIL_ON_PROJECT_REPOS mode), we must NOT also inject the
// repository at the project level or Gradle will fail the build.
var settingsOwnsRepos = false

beforeSettings {
    dependencyResolutionManagement {
        repositories {
            addCentralValidated()
        }
    }
}

gradle.settingsEvaluated {
    val mode = dependencyResolutionManagement.repositoriesMode.orNull
    settingsOwnsRepos =
        mode == org.gradle.api.initialization.resolve.RepositoriesMode.FAIL_ON_PROJECT_REPOS
}

allprojects {
    if (!settingsOwnsRepos) {
        repositories {
            addCentralValidated()
        }
    }
}
