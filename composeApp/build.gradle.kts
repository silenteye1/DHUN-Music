@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.gradle.api.file.RelativePath
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.net.URI
import java.security.MessageDigest
import java.util.Properties

val isFullBuild: Boolean =
    try {
        extra["isFullBuild"] == "true"
    } catch (e: Exception) {
        false
    }

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.aboutlibraries.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.build.config)
    alias(libs.plugins.osdetector)
    alias(libs.plugins.packagedeps)
}

// composeApp uses the `android.kotlin.multiplatform.library` plugin, so with the
// default `generateResClass = auto` Compose skips generating the `Res` class
// (it treats a KMP *library* module as not owning the public resource class).
// Force it to `always` so `Res` is generated for this app module.
compose.resources {
    generateResClass = always
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xwhen-guards")
        freeCompilerArgs.add("-Xcontext-parameters")
        freeCompilerArgs.add("-Xmulti-dollar-interpolation")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    android {
        namespace = "com.maxrave.simpmusic.composeapp"
        compileSdk = 37
        minSdk = 26
        withJava()
        androidResources {
            enable = true
        }
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

//    listOf(
//        iosArm64(),
//        iosSimulatorArm64()
//    ).forEach { iosTarget ->
//        iosTarget.binaries.framework {
//            baseName = "ComposeApp"
//            isStatic = true
//        }
//    }

    jvm()

    sourceSets {
        dependencies {
            val composeBom = project.dependencies.platform(libs.compose.bom)
            val koinBom = project.dependencies.platform(libs.koin.bom)
            implementation(composeBom)
            implementation(koinBom)
            implementation(libs.commons.io)
        }
        androidMain.dependencies {
            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.android)
            implementation(libs.koin.androidx.compose)

            implementation(libs.jetbrains.ui.tooling.preview)
            implementation(libs.constraintlayout.compose)

            api(libs.work.runtime.ktx)

            // Runtime
            api(libs.startup.runtime)

            api(projects.media3)
            api(projects.media3Ui)

            // Google Cast (gated: real SDK for full builds, no-op stub for FOSS builds)
            if (isFullBuild) {
                implementation(projects.cast)
            } else {
                implementation(projects.castEmpty)
            }
        }
        commonMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.components.resources)
            implementation(libs.jetbrains.ui.tooling.preview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Compose
            implementation(libs.compose.material3.adaptive)
            implementation(libs.compose.material.ripple)

            implementation(libs.ui.tooling.preview)

            // Other module
            api(projects.common)
            api(projects.domain)
            implementation(projects.data)

            // Last.fm (gated: real scrobbler for full builds, no-op stub for FOSS builds).
            // `api` rather than `implementation` so :androidApp can hand it the credentials from
            // BuildKonfig at startup, the same way it does for Sentry.
            if (isFullBuild) {
                api(projects.lastfm)
            } else {
                api(projects.lastfmEmpty)
            }

            // Navigation Compose
            implementation(libs.navigation.compose)

            // Kotlin Serialization
            implementation(libs.kotlinx.serialization.json)

            // Coil
            api(libs.coil.compose)
            api(libs.coil.network.okhttp)
            api(libs.kmpalette.core)
            api(libs.kmpalette.network)
            implementation(libs.materialkolor)
            implementation(libs.ktor.client.cio)

            // DataStore
            implementation(libs.datastore.preferences)

            // Lottie
            implementation(libs.compottie)
            implementation(libs.compottie.dot)
            implementation(libs.compottie.network)
            implementation(libs.compottie.resources)

            // Paging 3
            implementation(libs.androidx.paging.common)
            implementation(libs.paging.compose)

            implementation(libs.aboutlibraries)
            implementation(libs.aboutlibraries.compose.m3)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Jetbrains Markdown
            api(libs.markdown)

            // Blur Haze
            implementation(libs.haze)
            implementation(libs.haze.material)

            api(libs.cmptoast)
            implementation(libs.file.picker)

            // Liquid glass
            implementation(libs.liquid.glass)
            implementation(libs.liquid.glass.shape)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            // Desktop app entry (main.kt), jpackage/Conveyor
            // packaging, and tray icon live in :desktopApp per the
            // JetBrains 2026 KMP default structure. This module keeps the
            // shared JVM UI + expect/actuals and their direct dependencies.
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.sentry.jvm)
            implementation(libs.native.tray)
            implementation(projects.mediaJvmUi)
        }
    }
}

// NOTE: compose.desktop{} application block, ProGuard config,
// linuxDebConfig{}, the custom AppImage tooling, and Conveyor packaging
// live in :desktopApp per the JetBrains 2026 KMP default structure.
//
// Native-library staging (libmpv) lives here in :composeApp — see the
// mpv-natives block below. The layout is per-arch so Conveyor bundles only
// the slice each per-machine installer actually needs.

fun downloadIfMissing(
    url: String,
    target: File,
    logPrefix: String = "mpv-multi",
) {
    if (target.exists() && target.length() > 0) {
        logger.lifecycle("[$logPrefix] Cached: ${target.name}")
        return
    }
    logger.lifecycle("[$logPrefix] Downloading $url")
    target.parentFile.mkdirs()
    // Use curl instead of Java's URL.openStream(): release downloads answer
    // with a redirect to a CDN, and Java's default HttpURLConnection redirect
    // handling is fragile — on a cross-protocol redirect or an HTML error
    // page, openStream() silently saves the error body as the target file,
    // which only surfaces as a corrupt-archive failure later. curl's `-L`
    // follows redirects robustly across protocols + mirrors, `--fail`
    // exits non-zero on HTTP errors instead of saving error bodies, and
    // `-o` writes atomically via tmp file. The downloaded artifact is
    // also size-verified (must match Content-Length).
    val curlExit =
        ProcessBuilder(
            "curl",
            "-fsSL",
            // `--retry` alone does NOT retry curl exit 56 (mid-transfer receive failure) — it only
            // retries HTTP 5xx/408/429 and connection errors. The get.videolan.org mirrors flake with
            // exit 56 mid-download, so `--retry-all-errors` is required to retry those too. Count/delay
            // bumped a bit for the occasionally-slow mirror.
            "--retry",
            "5",
            "--retry-delay",
            "5",
            "--retry-all-errors",
            "-o",
            target.absolutePath,
            url,
        ).inheritIO().start().waitFor()
    check(curlExit == 0 && target.exists() && target.length() > 0) {
        // Delete partial/empty file so the next run can retry cleanly.
        if (target.exists()) target.delete()
        "curl failed (exit $curlExit) downloading $url to $target"
    }
}

// ===========================================================================
// mpv natives (libmpv) — staged into mpv-natives/<os>-<arch>/ for packaging.
//
// mpv publishes no portable libmpv of its own and there is no Gradle plugin
// that fetches one, so each OS lifts libmpv out of a prebuilt artifact that
// ALREADY ships a relocatable dependency closure. That is what keeps this
// cheap: gathering ffmpeg / libplacebo / libass / luajit by hand and
// rewriting their install names is precisely the work these upstreams have
// already done and keep doing on every release.
//
//   Windows  shinchiro/mpv-winbuild-cmake `mpv-dev-<arch>.7z`
//            → libmpv-2.dll with ffmpeg linked in. Nothing to patch.
//   Linux    built from source in a container — see scripts/mpv-linux/.
//            → the ONE platform with no usable upstream. Every prebuilt Linux
//              mpv targets "run mpv as its own process": the AppImage ships its
//              own glibc + loader and exports the API from a PIE *executable*,
//              which glibc flatly refuses to dlopen, and whose glibc would
//              collide with the one the JVM has already mapped. Distro packages
//              trade that for a version floor set by the distro. Building
//              against Ubuntu 22.04 gives a real libmpv.so.2 that needs only
//              glibc 2.34, so it loads in-process from Ubuntu 22.04 / Debian 11
//              upwards.
//   macOS    mpv's own tagged release .zip (macos-15-arm / macos-15-intel)
//            → Contents/MacOS/mpv renamed to libmpv.dylib, its
//              @executable_path/lib/... load commands rewritten to
//              @loader_path, then re-signed ad-hoc.
//
// On macOS and Linux mpv links libmpv STATICALLY into the `mpv` executable, so
// there is no shared library to copy. Those executables are PIE and export the
// full client API (54 `mpv_*` symbols), which is why renaming them works at
// all — and why the rename matters: JNA maps Native.load("mpv") to
// libmpv.dylib / libmpv.so. Only Windows publishes a real `mpv-dev` package.
//
// macOS is the odd one out and deliberately so. The obvious shortcut — lifting
// IINA.app/Contents/Frameworks straight out of IINA's .dmg — DOES NOT WORK,
// and fails in a way worth recording so nobody retries it. In IINA 1.4.4 the
// bundled pair is version-skewed:
//
//   nm -u  libmpv.2.dylib        → _pl_log_create_349   (libplacebo API 349)
//   nm -gU libplacebo.338.dylib  → _pl_log_create_338
//
// on BOTH slices of the fat binaries, with only one libplacebo in the bundle
// and the reference not weak ("(undefined) external ... (from libplacebo.338)").
// dlopen() of that libmpv fails outright — verified with RTLD_NOW *and*
// RTLD_LAZY — and libmpv has 136 undefined _pl_* symbols riding on it. Whatever
// makes IINA itself work, that closure is not self-sufficient, and MpvLibrary
// loads with RTLD_NOW by design, so there is no flag to hide behind.
//
// mpv's own release zip avoids that entirely: libmpv and its closure come out
// of one build, so they cannot be version-skewed against each other. The
// dylibs must be re-signed ad-hoc after patching — mutating a Mach-O
// invalidates its signature and macOS refuses to load an invalidly-signed
// dylib (a hard failure on Apple Silicon). That signing step is the only
// reason these tasks need a Mac.
//
// Conveyor stages one slice per machine — see the mpv-natives inputs in
// conveyor.conf.
// ===========================================================================
val mpvCacheDir = layout.buildDirectory.dir("mpv-cache")

// Pinned upstream artifacts. Bump deliberately: MpvLibrary.kt hand-maps the
// libmpv struct layouts by raw offset, so a client-API MAJOR bump means the
// structs must be re-verified before these pins move. mpv 0.41.x is client
// API 2.5; MpvLibrary.kt was written against 2.2 and only guards the major.
// mpv's own tagged release — the source of the macOS slices.
val mpvVersion = "0.41.0"
val mpvWinBuildTag = "20260610"
val mpvWinBuildSuffix = "20260610-git-304426c"
// Linux has no upstream pin: that slice is compiled from source by
// scripts/mpv-linux/Dockerfile, which pins mpv, FFmpeg and libplacebo itself.

// Every extractor below finds the directory that actually holds the libmpv
// artifact and copies its whole sibling set, rather than hard-coding upstream
// tree shapes. Those layouts drift between releases; "the folder libmpv lives
// in" does not.
fun findDirContaining(
    root: File,
    namePredicate: (String) -> Boolean,
): File? =
    root
        .walkTopDown()
        .firstOrNull { it.isFile && namePredicate(it.name) }
        ?.parentFile

/** True when [tool] can be executed at all (i.e. it exists on PATH). */
fun toolAvailable(tool: String): Boolean =
    try {
        ProcessBuilder(tool)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
            .waitFor()
        true
    } catch (e: java.io.IOException) {
        false
    }

fun runChecked(vararg command: String) {
    val exit = ProcessBuilder(*command).inheritIO().start().waitFor()
    check(exit == 0) { "Command failed (exit $exit): ${command.joinToString(" ")}" }
}

/** Like [runChecked], but returns the command's trimmed stdout instead of forwarding it. */
fun runCapturing(vararg command: String): String {
    val process = ProcessBuilder(*command).redirectErrorStream(false).start()
    val output = process.inputStream.bufferedReader().readText()
    val exit = process.waitFor()
    check(exit == 0) { "Command failed (exit $exit): ${command.joinToString(" ")}" }
    return output.trim()
}

fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { stream ->
        val buffer = ByteArray(1 shl 16)
        while (true) {
            val read = stream.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

// ---------------------------------------------------------------------------
// macOS
// ---------------------------------------------------------------------------

fun rewriteExecutablePathRefs(
    file: File,
    prefix: String,
): Int {
    val needle = "@executable_path/lib/".toByteArray(Charsets.US_ASCII)
    val prefixBytes = prefix.toByteArray(Charsets.US_ASCII)
    check(prefixBytes.size <= needle.size) {
        "prefix '$prefix' is longer than '@executable_path/lib/' — paths can only be shortened in place"
    }
    val data = file.readBytes()
    var rewritten = 0
    var i = 0
    outer@ while (i <= data.size - needle.size) {
        for (j in needle.indices) {
            if (data[i + j] != needle[j]) {
                i++
                continue@outer
            }
        }
        var end = i + needle.size
        while (end < data.size && data[end] != 0.toByte()) end++
        val name = data.copyOfRange(i + needle.size, end)
        val replacement = prefixBytes + name
        check(replacement.size <= end - i)
        replacement.copyInto(data, i)
        for (k in i + replacement.size until end) data[k] = 0
        rewritten++
        i = end
    }
    if (rewritten > 0) file.writeBytes(data)
    return rewritten
}

fun codesignAdhoc(dir: File) {
    val machO = dir.walkTopDown().filter { it.isFile && (it.name.endsWith(".dylib") || it.extension.isEmpty()) }.toList()
    check(machO.isNotEmpty()) { "Nothing to sign in ${dir.absolutePath}" }

    check(toolAvailable("codesign")) {
        "codesign is required to re-sign the patched macOS slice — run this task on a Mac. " +
            "CI does not: it downloads the prebuilt archives instead."
    }
    logger.lifecycle("[mpv-multi] Ad-hoc signing ${machO.size} Mach-O files in ${dir.name}")
    machO.forEach { runChecked("codesign", "--force", "--sign", "-", it.absolutePath) }
}

fun extractMacMpvSlice(
    assetArch: String,
    outputDir: File,
) {
    val cache = mpvCacheDir.get().asFile
    val zip = cache.resolve("mpv-$mpvVersion-$assetArch.zip")
    downloadIfMissing(
        "https://github.com/mpv-player/mpv/releases/download/v$mpvVersion/mpv-v$mpvVersion-$assetArch.zip",
        zip,
        logPrefix = "mpv-multi",
    )

    val stage = cache.resolve("mac-$assetArch-extract")
    stage.deleteRecursively()
    stage.mkdirs()
    project.copy {
        from(zipTree(zip))
        into(stage)
    }
    val innerTar =
        stage.walkTopDown().firstOrNull { it.isFile && it.name.endsWith(".tar.gz") }
            ?: error("No inner tarball inside ${zip.name}")
    project.copy {
        from(tarTree(resources.gzip(innerTar)))
        into(stage)
    }
    val macOsDir =
        stage.walkTopDown().firstOrNull {
            it.isDirectory && it.name == "MacOS" && it.parentFile?.name == "Contents"
        } ?: error("mpv.app/Contents/MacOS not found inside ${zip.name}")
    val binary = macOsDir.resolve("mpv")
    check(binary.isFile) { "mpv binary missing from ${macOsDir.absolutePath}" }

    outputDir.deleteRecursively()
    outputDir.mkdirs()
    project.copy {
        from(macOsDir.resolve("lib"))
        into(outputDir.resolve("lib"))
    }
    val staged = outputDir.resolve("libmpv.dylib")
    binary.copyTo(staged, overwrite = true)
    staged.setWritable(true)

    var rewritten = rewriteExecutablePathRefs(staged, "@loader_path/lib/")
    outputDir.resolve("lib").listFiles()?.filter { it.isFile && it.name.endsWith(".dylib") }?.forEach { dylib ->
        dylib.setWritable(true)
        rewritten += rewriteExecutablePathRefs(dylib, "@loader_path/")
    }

    check(rewritten > 0) {
        "No @executable_path/lib/ entries found in $assetArch — mpv's macOS layout changed, " +
            "rewriteExecutablePathRefs() needs updating before this slice can be published."
    }
    logger.lifecycle("[mpv-multi] $assetArch: rewrote $rewritten install-name entries")
    codesignAdhoc(outputDir)
}

val mpvSetupMacArmCi by tasks.registering {
    group = "mpv-multi"
    description = "Cross-OS: populate mpv-natives/macos-arm64/ with libmpv + its dylib closure."
    val outputDir = rootDir.resolve("mpv-natives/macos-arm64/")
    inputs.property("mpvVersion", mpvVersion)
    outputs.dir(outputDir)
    doLast { extractMacMpvSlice("macos-15-arm", outputDir) }
}

val mpvSetupMacX64Ci by tasks.registering {
    group = "mpv-multi"
    description = "Cross-OS: populate mpv-natives/macos-x64/ with Intel libmpv + its dylib closure."
    val outputDir = rootDir.resolve("mpv-natives/macos-x64/")
    inputs.property("mpvVersion", mpvVersion)
    outputs.dir(outputDir)
    doLast { extractMacMpvSlice("macos-15-intel", outputDir) }
}

// ---------------------------------------------------------------------------
// Windows
// ---------------------------------------------------------------------------

fun extractWindowsMpvSlice(
    arch: String,
    outputDir: File,
) {
    val sevenZip =
        listOf(
            "C:\\Program Files\\7-Zip\\7z.exe",
            "C:\\Program Files (x86)\\7-Zip\\7z.exe",
            "7zz",
            "7z",
        ).firstOrNull { File(it).exists() || toolAvailable(it) }
            ?: error(
                "7-Zip is required to unpack shinchiro's .7z builds and must be 21.07 or newer. " +
                    "macOS: `brew install sevenzip` (provides 7zz). Ubuntu: install the official " +
                    "7-Zip build — distro p7zip is too old for the ARM64 archive.",
            )
    val cache = mpvCacheDir.get().asFile
    val archive = cache.resolve("mpv-dev-$arch-$mpvWinBuildSuffix.7z")
    downloadIfMissing(
        "https://github.com/shinchiro/mpv-winbuild-cmake/releases/download/" +
            "$mpvWinBuildTag/mpv-dev-$arch-$mpvWinBuildSuffix.7z",
        archive,
        logPrefix = "mpv-multi",
    )
    val extractDir = cache.resolve("mpv-dev-$arch-extract")
    extractDir.deleteRecursively()
    extractDir.mkdirs()
    runChecked(sevenZip, "x", "-y", "-o${extractDir.absolutePath}", archive.absolutePath)

    val dllDir =
        findDirContaining(extractDir) { it.startsWith("libmpv") && it.endsWith(".dll") }
            ?: error("No libmpv*.dll inside ${archive.name}")
    outputDir.deleteRecursively()
    outputDir.mkdirs()
    project.copy {
        from(dllDir)
        into(outputDir)
        include("*.dll")
        includeEmptyDirs = false
    }
}

val mpvSetupWindowsX64Ci by tasks.registering {
    group = "mpv-multi"
    description = "Cross-OS: populate mpv-natives/windows-x64/ with libmpv-2.dll."
    val outputDir = rootDir.resolve("mpv-natives/windows-x64/")
    inputs.property("mpvWinBuildSuffix", mpvWinBuildSuffix)
    outputs.dir(outputDir)
    doLast { extractWindowsMpvSlice("x86_64", outputDir) }
}

val mpvSetupWindowsArmCi by tasks.registering {
    group = "mpv-multi"
    description = "Cross-OS: populate mpv-natives/windows-arm64/ with ARM64 libmpv-2.dll."
    val outputDir = rootDir.resolve("mpv-natives/windows-arm64/")
    inputs.property("mpvWinBuildSuffix", mpvWinBuildSuffix)
    outputs.dir(outputDir)
    doLast { extractWindowsMpvSlice("aarch64", outputDir) }
}

// ---------------------------------------------------------------------------
// Linux
// ---------------------------------------------------------------------------

val mpvSetupLinuxCi by tasks.registering {
    group = "mpv-multi"
    description = "Cross-OS: build a real libmpv.so.2 in a container and stage it with its .so closure."
    val outputDir = rootDir.resolve("mpv-natives/linux-x64/")
    val dockerDir = rootDir.resolve("scripts/mpv-linux")
    inputs.dir(dockerDir)
    inputs.property("mpvVersion", mpvVersion)
    outputs.dir(outputDir)
    doLast {
        if (!toolAvailable("docker")) {
            logger.warn(
                "[mpv-multi] Skipping the Linux slice: docker is not on PATH. " +
                    "The other slices are unaffected.",
            )
            return@doLast
        }

        val tag = "simpmusic-libmpv:$mpvVersion"
        logger.lifecycle("[mpv-multi] Building $tag (libplacebo + FFmpeg + mpv from source, ~20-40 min cold)")
        runChecked("docker", "build", "-t", tag, dockerDir.absolutePath)

        val container = runCapturing("docker", "create", tag)
        check(container.isNotEmpty()) { "docker create returned no container id" }
        try {
            outputDir.deleteRecursively()
            outputDir.mkdirs()
            runChecked("docker", "cp", "$container:/out/.", outputDir.absolutePath)
        } finally {
            runChecked("docker", "rm", container)
        }

        val staged = outputDir.resolve("libmpv.so.2")
        check(staged.isFile) { "libmpv.so.2 missing from the container output" }
        val elfType =
            staged.inputStream().use { stream ->
                val header = ByteArray(18)
                check(stream.read(header) == header.size) { "libmpv.so.2 is truncated" }
                (header[0x10].toInt() and 0xFF) or ((header[0x11].toInt() and 0xFF) shl 8)
            }
        check(elfType == 3) { "libmpv.so.2 is not ET_DYN (e_type=$elfType) — it cannot be dlopen()ed" }

        val libs = outputDir.resolve("lib").listFiles()?.size ?: 0
        logger.lifecycle(
            "[mpv-multi] linux-x64: staged libmpv.so.2 + $libs shared objects " +
                "(${outputDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } / 1048576} MB)",
        )
    }
}

val mpvNativesRepo = "maxrave-dev/simpmusic-files"
val mpvNativesTag = "abc"
val mpvSlices = listOf("linux-x64", "macos-arm64", "macos-x64", "windows-x64", "windows-arm64")

val mpvBundleAll by tasks.registering {
    group = "mpv-bundle"
    description = "Mac only: build every native slice and pack them into build/mpv-dist/ for a GitHub release."
    dependsOn(
        mpvSetupLinuxCi,
        mpvSetupMacArmCi,
        mpvSetupMacX64Ci,
        mpvSetupWindowsX64Ci,
        mpvSetupWindowsArmCi,
    )
    val distDir = layout.buildDirectory.dir("mpv-dist")
    outputs.dir(distDir)
    doLast {
        val dist = distDir.get().asFile
        dist.deleteRecursively()
        dist.mkdirs()
        mpvSlices.forEach { slice ->
            val sliceDir = rootDir.resolve("mpv-natives/$slice")
            check(sliceDir.isDirectory && sliceDir.listFiles()?.isNotEmpty() == true) {
                "mpv-natives/$slice is missing or empty — cannot pack an incomplete set"
            }
            runChecked(
                "tar",
                "-czf",
                dist.resolve("mpv-natives-$slice.tar.gz").absolutePath,
                "-C",
                rootDir.resolve("mpv-natives").absolutePath,
                slice,
            )
        }
        logger.lifecycle("[mpv-bundle] Packed ${mpvSlices.size} slices into ${dist.absolutePath}")
        logger.lifecycle("[mpv-bundle] Paste these into mpvNativesChecksums:")
        mpvSlices.forEach { slice ->
            logger.lifecycle("        \"$slice\" to \"${sha256(dist.resolve("mpv-natives-$slice.tar.gz"))}\",")
        }
        logger.lifecycle("[mpv-bundle] Publish with:")
        logger.lifecycle(
            "  gh release create $mpvNativesTag ${dist.absolutePath}/*.tar.gz " +
                "--repo $mpvNativesRepo --title \"Desktop natives (mpv $mpvVersion)\" --notes \"...\"",
        )
    }
}

val mpvNativesChecksums =
    mapOf(
        "linux-x64" to "8c44b22cebdaf07af6a48a6021412fd654831e11d7a7fe4bd919730d4ccf22a5",
        "macos-arm64" to "e527daac8f6cc196324ea6f0ce54d119d04450af9795ff2856c1891806c1d5e0",
        "macos-x64" to "95170ea54e1f637fdee148a9efd97993c305a92e233b8478d3764fbecce3eb02",
        "windows-x64" to "256f17cf402c7583b8684d5a7cf585ad1b59695469219671f4887b9d8d272a99",
        "windows-arm64" to "30e04a117de0b7d6abc5f86d4231e9b4bffa3637f282ff9efe3dc66e6cc4fcba",
    )

val mpvSetupAll by tasks.registering {
    group = "mpv-multi"
    description = "Populate mpv-natives/ from the prebuilt release tarballs. Runs anywhere; this is what CI uses."
    val outputRoot = rootDir.resolve("mpv-natives")
    inputs.property("mpvNativesTag", mpvNativesTag)
    inputs.property("mpvNativesChecksums", mpvNativesChecksums)
    outputs.dir(outputRoot)
    doLast {
        val cache = mpvCacheDir.get().asFile
        mpvSlices.forEach { slice ->
            val archive = cache.resolve("mpv-natives-$slice-$mpvNativesTag.tar.gz")
            downloadIfMissing(
                "https://github.com/$mpvNativesRepo/releases/download/$mpvNativesTag/mpv-natives-$slice.tar.gz",
                archive,
                logPrefix = "mpv-multi",
            )
            val expected = mpvNativesChecksums.getValue(slice)
            val actual = sha256(archive)
            check(expected != "PENDING") {
                "No checksum pinned for $slice. Run `:composeApp:mpvBundleAll`, publish the " +
                    "archives, then paste the printed digests into mpvNativesChecksums."
            }
            check(actual == expected) {
                archive.delete()
                "Checksum mismatch for mpv-natives-$slice.tar.gz\n  expected $expected\n  actual   $actual"
            }
            val target = outputRoot.resolve(slice)
            target.deleteRecursively()
            outputRoot.mkdirs()
            runChecked("tar", "-xzf", archive.absolutePath, "-C", outputRoot.absolutePath)
            check(target.isDirectory) { "$slice missing after unpacking ${archive.name}" }

            val appleDouble = target.walkTopDown().filter { it.isFile && it.name.startsWith("._") }.toList()
            appleDouble.forEach { it.delete() }
            if (appleDouble.isNotEmpty()) {
                logger.lifecycle("[mpv-multi] $slice: stripped ${appleDouble.size} AppleDouble sidecars")
            }
        }
        logger.lifecycle("[mpv-multi] Unpacked ${mpvSlices.size} verified native slices into mpv-natives/")
    }
}

buildkonfig {
    packageName = "com.maxrave.simpmusic"
    exposeObjectWithName = "BuildKonfig"
    defaultConfigs {
        val versionName =
            libs.versions.version.name
                .get()
        val versionCode =
            libs.versions.version.code
                .get()
                .toInt()
        buildConfigField(STRING, "versionName", versionName)
        buildConfigField(INT, "versionCode", "$versionCode")

        if (isFullBuild) {
            try {
                println("Full build detected, enabling Sentry DSN")
                val properties = Properties()
                properties.load(rootProject.file("local.properties").inputStream())
                buildConfigField(
                    STRING,
                    "sentryDsn",
                    properties.getProperty("SENTRY_DSN") ?: "",
                )
                buildConfigField(
                    STRING,
                    "lastfmApiKey",
                    properties.getProperty("LASTFM_API_KEY") ?: "",
                )
                buildConfigField(
                    STRING,
                    "lastfmSecret",
                    properties.getProperty("LASTFM_SECRET") ?: "",
                )
            } catch (e: Exception) {
                println("Failed to load secrets from local.properties: ${e.message}")
                buildConfigField(STRING, "sentryDsn", "")
                buildConfigField(STRING, "lastfmApiKey", "")
                buildConfigField(STRING, "lastfmSecret", "")
            }
        } else {
            buildConfigField(STRING, "sentryDsn", "")
            buildConfigField(STRING, "lastfmApiKey", "")
            buildConfigField(STRING, "lastfmSecret", "")
        }
    }
}

aboutLibraries {
    collect.configPath = file("../config")
    export {
        outputFile = file("src/commonMain/composeResources/files/aboutlibraries.json")
        prettyPrint = true
        excludeFields = listOf("generated")
    }
    library {
        duplicationMode = com.mikepenz.aboutlibraries.plugin.DuplicateMode.MERGE
        duplicationRule = com.mikepenz.aboutlibraries.plugin.DuplicateRule.SIMPLE
    }
}

afterEvaluate {
    tasks
        .matching { it.name.startsWith("prepare") && it.name.endsWith("ArtProfile") }
        .configureEach {
            dependsOn("generateBuildKonfig")
        }
}