package com.agentdesk.app.feature.projects.data.fs

import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectSlugger @Inject constructor() {

    fun generateSlug(name: String, existingSlugs: Set<String>): String {
        var base = name.trim().lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

        if (base.isEmpty()) {
            base = "project"
        }

        if (!existingSlugs.contains(base)) {
            return base
        }

        var counter = 2
        while (existingSlugs.contains("$base-$counter")) {
            counter++
        }
        return "$base-$counter"
    }

    fun generateUniqueDir(parentDir: File, slug: String): File {
        var dir = File(parentDir, slug)
        if (!dir.exists()) return dir

        var counter = 2
        while (File(parentDir, "$slug-$counter").exists()) {
            counter++
        }
        return File(parentDir, "$slug-$counter")
    }

    fun derivePackageName(slug: String): String {
        val clean = slug.replace("-", "").lowercase(Locale.ROOT)
        return "com.example.$clean"
    }
}
