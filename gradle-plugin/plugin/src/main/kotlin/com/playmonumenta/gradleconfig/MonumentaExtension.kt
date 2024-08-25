package com.playmonumenta.gradleconfig

import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

interface MonumentaExtension {
    fun versionAdapter(name: String)
    fun snapshotRepo(url: String)
    fun releasesRepo(url: String)
    fun name(name: String)

    fun paper(
        main: String, order: BukkitPluginDescription.PluginLoadOrder, apiVersion: String,
        authors: List<String> = listOf("Team Monumenta"),
        depends: List<String> = listOf(),
        softDepends: List<String> = listOf()
    )

    fun waterfall(
        main: String,
        apiVersion: String,
        authors: List<String> = listOf("Team Monumenta"),
        depends: List<String> = listOf(),
        softDepends: List<String> = listOf()
    )

    fun publishingCredentials(name: String, token: String)
}
