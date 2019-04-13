package com.almightyalpaca.jetbrains.plugins.discord.shared.source.local

import com.almightyalpaca.jetbrains.plugins.discord.shared.source.*
import com.almightyalpaca.jetbrains.plugins.discord.shared.utils.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext

class LocalSource(location: Path) : Source, CoroutineScope {
    private val parentJob: Job = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + parentJob

    internal val path = location.resolve("icons")
    internal val pathLanguages = path.resolve("languages")
    internal val pathThemes = path.resolve("themes")
    internal val pathApplications = path.resolve("applications")

    private val languageJob: Deferred<LanguageMap> = retryAsync { readLanguages() }
    private val themeJob: Deferred<ThemeMap> = retryAsync { readThemes() }

    override fun getLanguages(): LanguageMap = languageJob.asCompletableFuture().get()
    override fun getThemes(): ThemeMap = themeJob.asCompletableFuture().get()

    override fun getLanguagesOrNull(): LanguageMap? = languageJob.getCompletedOrNull()
    override fun getThemesOrNull(): ThemeMap? = themeJob.getCompletedOrNull()

    private fun readLanguages(): LanguageMap {
        val mapper = ObjectMapper(YAMLFactory())

        val map = Files.list(pathLanguages)
                .filter { p -> p.extension.toLowerCase() == "yaml" }
                .map { p ->
                    val node: JsonNode = mapper.readTree(Files.newInputStream(p))
                    LanguageSource(p.baseName.toLowerCase(), node)
                }
                .map { p -> p.id to p }
                .toMap()

        return LocalLanguageSourceMap(this, map).toLanguageMap()
    }

    private fun readThemes(): ThemeMap {
        val mapper = ObjectMapper(YAMLFactory())

        val map = Files.list(pathThemes)
                .filter { p -> p.extension.toLowerCase() == "yaml" }
                .map { p ->
                    val node: JsonNode = mapper.readTree(Files.newInputStream(p))
                    ThemeSource(p.baseName.toLowerCase(), node)
                }
                .map { p -> p.id to p }
                .toMap()

        return LocalThemeSourceMap(this, map).toThemeMap()
    }
}
