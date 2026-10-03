package com.example.domain.model

data class ProjectSnapshot(
    val projectPath: String,
    val modules: List<String>,
    val sourceFiles: List<String>,
    val testFiles: List<String>,
    val resourceFiles: List<String>,
    val gradleFiles: List<String>,
    val manifestFiles: List<String>,
    val dependencies: List<String>,
    val buildVariants: List<String>,
    val gitState: String,
    val detectedArchitecture: String,
    val inspectionTimestamp: Long
)
