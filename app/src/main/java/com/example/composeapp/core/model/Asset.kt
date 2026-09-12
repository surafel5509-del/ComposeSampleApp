package com.example.composeapp.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Asset(
    val assetId: String,
    val kind: AssetKind,
    val originalUri: String? = null,
    val localPath: String? = null,
    val proxyPath: String? = null,
    val checksum: String? = null,
    val version: Int = 1,
)

@Serializable
enum class AssetKind {
    VIDEO,
    IMAGE,
    AUDIO,
    FONT,
    TEMPLATE_RESOURCE,
    EFFECT_RESOURCE,
}
