package com.jambus.wikihelper.data.repository

import com.jambus.wikihelper.data.model.KnowledgeDocument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KnowledgeRepository @Inject constructor() {
    
    // Mock data for now - in real implementation this would connect to Confluence API
    private val sampleDocuments = listOf(
        KnowledgeDocument(
            id = "1",
            title = "Android开发规范",
            content = "这是Android开发的基本规范...",
            department = "技术部",
            type = "开发文档",
            lastUpdated = "2024-01-15",
            rating = 5,
            icon = "android"
        ),
        KnowledgeDocument(
            id = "2",
            title = "项目管理流程",
            content = "项目管理的标准流程...",
            department = "项目管理部",
            type = "流程文档",
            lastUpdated = "2024-01-10",
            rating = 4,
            icon = "project"
        ),
        KnowledgeDocument(
            id = "3",
            title = "UI设计指南",
            content = "用户界面设计的最佳实践...",
            department = "设计部",
            type = "设计文档",
            lastUpdated = "2024-01-05",
            rating = 5,
            icon = "design"
        )
    )
    
    fun getDocuments(): Flow<List<KnowledgeDocument>> {
        return flowOf(sampleDocuments)
    }
    
    fun searchDocuments(query: String): Flow<List<KnowledgeDocument>> {
        val filteredDocuments = sampleDocuments.filter { doc ->
            doc.title.contains(query, ignoreCase = true) ||
            doc.content.contains(query, ignoreCase = true)
        }
        return flowOf(filteredDocuments)
    }
    
    suspend fun getDocumentById(id: String): KnowledgeDocument? {
        return sampleDocuments.find { it.id == id }
    }
}
