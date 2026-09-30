package com.example.domain.tools

interface AiTool {
    val id: String
    val name: String
    val description: String
    val inputSchema: Map<String, String> // Map of argument names to types/descriptions
    suspend fun execute(arguments: Map<String, Any>): String
}

object ToolRegistry {
    private val tools = mutableMapOf<String, AiTool>()

    fun registerTool(tool: AiTool) {
        tools[tool.id] = tool
    }

    fun getTools(): List<AiTool> = tools.values.toList()

    fun getTool(id: String): AiTool? = tools[id]

    fun clear() {
        tools.clear()
    }
}
