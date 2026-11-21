package com.agentdesk.app.feature.build.data.check

import com.agentdesk.app.core.common.AppResult
import com.agentdesk.app.feature.build.data.engine.BuildContext
import com.agentdesk.app.feature.build.data.engine.BuildEnvironment
import com.agentdesk.app.feature.build.data.engine.steps.AlignStep
import com.agentdesk.app.feature.build.data.engine.steps.DexStep
import com.agentdesk.app.feature.build.data.engine.steps.JavaCompileStep
import com.agentdesk.app.feature.build.data.engine.steps.PackageStep
import com.agentdesk.app.feature.build.data.engine.steps.ResourceCompileStep
import com.agentdesk.app.feature.build.data.engine.steps.ResourceLinkStep
import com.agentdesk.app.feature.build.data.engine.steps.SignStep
import com.agentdesk.app.feature.build.data.parse.ManifestReader
import com.agentdesk.app.feature.build.domain.KeystoreManager
import com.agentdesk.app.feature.build.domain.model.BuildVariant
import com.agentdesk.app.feature.build.domain.model.StepStatus
import com.agentdesk.app.feature.projects.data.template.TemplateRepository
import com.agentdesk.app.feature.projects.domain.model.TemplateId
import com.agentdesk.app.feature.setup.domain.PostInstallCheck
import com.agentdesk.app.feature.setup.domain.model.CheckResult
import com.agentdesk.app.feature.terminal.domain.PathProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HelloWorldSmokeTest @Inject constructor(
    private val pathProvider: PathProvider,
    private val templateRepository: TemplateRepository,
    private val manifestReader: ManifestReader,
    private val keystoreManager: KeystoreManager,
    private val buildEnvironment: BuildEnvironment,
    private val resourceCompileStep: ResourceCompileStep,
    private val resourceLinkStep: ResourceLinkStep,
    private val javaCompileStep: JavaCompileStep,
    private val dexStep: DexStep,
    private val packageStep: PackageStep,
    private val alignStep: AlignStep,
    private val signStep: SignStep
) : PostInstallCheck {

    override val id: String = "hello-world-smoke-test"
    override val title: String = "Build System Smoke Test (Hello World)"

    override suspend fun run(): CheckResult {
        val tempDir = File(pathProvider.tmpDir, "smoke_test_hello_world")
        tempDir.deleteRecursively()
        tempDir.mkdirs()

        try {
            // 1. Populate project from hello-java template
            val popRes = templateRepository.populateProject(
                targetDir = tempDir,
                template = TemplateId.HELLO_JAVA,
                packageName = "com.agentdesk.smoketest",
                appName = "SmokeTest"
            )
            if (popRes is AppResult.Failure) {
                return CheckResult.Fail("Template population failed: ${popRes.error.message}")
            }

            // 2. Read manifest
            val manifestFile = File(tempDir, "AndroidManifest.xml")
            val manifestRes = manifestReader.read(manifestFile)
            if (manifestRes is AppResult.Failure) {
                return CheckResult.Fail("Manifest parsing failed: ${manifestRes.error.message}")
            }
            val manifestInfo = (manifestRes as AppResult.Success).value

            // 3. Get debug keystore
            val signRes = keystoreManager.getOrCreateDebugKeystore()
            if (signRes is AppResult.Failure) {
                return CheckResult.Fail("Debug keystore creation failed: ${signRes.error.message}")
            }
            val signingConfig = (signRes as AppResult.Success).value

            val buildDir = File(tempDir, "build").apply { mkdirs() }
            val guestTempPath = pathProvider.hostToGuest(tempDir) ?: "/root/tmp/smoke_test_hello_world"

            val context = BuildContext(
                projectId = "smoke_test",
                projectSlug = "smoke_test_hello_world",
                projectDir = tempDir,
                guestProjectDir = guestTempPath,
                buildDir = buildDir,
                manifestInfo = manifestInfo,
                variant = BuildVariant.DEBUG,
                signingConfig = signingConfig,
                androidJarGuestPath = buildEnvironment.resolveAndroidJar(),
                buildToolsGuestDir = buildEnvironment.resolveBuildTools(),
                javaHomeGuestPath = buildEnvironment.resolveJavaHome()
            )

            val logSink: (String) -> Unit = {}

            // Step 1: Resource compile
            val s1 = resourceCompileStep.execute(context, logSink)
            if (s1.status == StepStatus.FAILED) return CheckResult.Fail("ResourceCompileStep failed", s1.rawLog)

            // Step 2: Resource link
            val s2 = resourceLinkStep.execute(context, logSink)
            if (s2.status == StepStatus.FAILED) return CheckResult.Fail("ResourceLinkStep failed", s2.rawLog)

            // Step 3: Java compile
            val s3 = javaCompileStep.execute(context, logSink)
            if (s3.status == StepStatus.FAILED) return CheckResult.Fail("JavaCompileStep failed", s3.rawLog)

            // Step 4: Dex
            val s4 = dexStep.execute(context, logSink)
            if (s4.status == StepStatus.FAILED) return CheckResult.Fail("DexStep failed", s4.rawLog)

            // Step 5: Package
            val s5 = packageStep.execute(context, logSink)
            if (s5.status == StepStatus.FAILED) return CheckResult.Fail("PackageStep failed", s5.rawLog)

            // Step 6: Align
            val s6 = alignStep.execute(context, logSink)
            if (s6.status == StepStatus.FAILED) return CheckResult.Fail("AlignStep failed", s6.rawLog)

            // Step 7: Sign & Verify
            val s7 = signStep.execute(context, logSink)
            if (s7.status == StepStatus.FAILED) return CheckResult.Fail("SignStep failed", s7.rawLog)

            val apk = File(buildDir, "app.apk")
            if (!apk.exists() || apk.length() == 0L) {
                return CheckResult.Fail("build/app.apk was not produced or is 0 bytes")
            }

            return CheckResult.Pass
        } catch (e: Exception) {
            return CheckResult.Fail("Smoke test encountered unexpected error: ${e.message}")
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
