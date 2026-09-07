package org.better.urn.data

import androidx.compose.runtime.Immutable

@Immutable
data class BreadcrumbSegment(
    val name: String,
    val path: String
)

@Immutable
data class FolderSubfolderItem(
    val name: String,
    val fullPath: String,
    val itemCount: Int
)

@Immutable
data class FolderFileItem(
    val content: ModuleContent,
    val viewableFile: ViewableFile
)

@Immutable
data class FolderTreeContent(
    val currentPath: String,
    val breadcrumbs: List<BreadcrumbSegment>,
    val subfolders: List<FolderSubfolderItem>,
    val files: List<FolderFileItem>
)

object MoodleFolderUtils {

    /**
     * Normalizes a folder path to guarantee leading and trailing slashes.
     */
    fun normalizePath(path: String): String {
        val trimmed = path.trim()
        if (trimmed.isEmpty() || trimmed == "/") return "/"
        val withLeading = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
        return if (withLeading.endsWith("/")) withLeading else "$withLeading/"
    }

    /**
     * Extracts parent path from a given normalized path. Returns "/" if at root.
     */
    fun getParentPath(path: String): String {
        val normalized = normalizePath(path)
        if (normalized == "/") return "/"
        val segments = normalized.trim('/').split('/')
        if (segments.size <= 1) return "/"
        val parentSegments = segments.dropLast(1)
        return "/" + parentSegments.joinToString("/") + "/"
    }

    /**
     * Generates breadcrumb segments for top-bar navigation inside a folder structure.
     */
    fun buildBreadcrumbs(rootName: String, currentPath: String): List<BreadcrumbSegment> {
        val normalized = normalizePath(currentPath)
        val result = mutableListOf(BreadcrumbSegment(name = rootName.ifBlank { "Dossier" }, path = "/"))

        if (normalized == "/") return result

        val segments = normalized.trim('/').split('/').filter { it.isNotBlank() }
        var accumulatedPath = "/"
        for (segment in segments) {
            accumulatedPath += "$segment/"
            result.add(BreadcrumbSegment(name = segment.cleanHtml(), path = accumulatedPath))
        }

        return result
    }

    /**
     * Converts a single [ModuleContent] into a [ViewableFile] for viewing or downloading.
     */
    fun createViewableFile(
        content: ModuleContent,
        moduleId: Int,
        index: Int,
        token: String
    ): ViewableFile? {
        val rawUrl = content.fileurl ?: return null
        if (content.filename.isNullOrBlank() || content.filename == ".") return null

        var targetUrl = rawUrl
        if (targetUrl.contains("/pluginfile.php/") && !targetUrl.contains("/webservice/pluginfile.php/")) {
            targetUrl = targetUrl.replace("/pluginfile.php/", "/webservice/pluginfile.php/")
        }
        if (targetUrl.contains("forcedownload=1")) {
            targetUrl = targetUrl.replace("forcedownload=1", "forcedownload=0")
        }

        val authenticatedUrl = if (token.isBlank() || targetUrl.contains("wstoken=") || targetUrl.contains("token=")) {
            targetUrl
        } else if (targetUrl.contains("?")) {
            "$targetUrl&token=$token"
        } else {
            "$targetUrl?token=$token"
        }

        val fileName = content.filename.cleanHtml()
        val mimeType = content.mimetype
        val detectedType = FileTypeUtils.detectType(fileName, mimeType)

        return ViewableFile(
            id = "folder_${moduleId}_${index}_${fileName.hashCode()}",
            title = fileName,
            url = authenticatedUrl,
            mimeType = mimeType,
            formattedFileSize = content.getFormattedFileSize(),
            fileType = detectedType
        )
    }

    /**
     * Parses Moodle folder content items into direct files and subfolders for [currentPath].
     */
    fun parseFolderTree(
        contents: List<ModuleContent>?,
        moduleId: Int,
        rootName: String,
        currentPath: String = "/",
        searchQuery: String = "",
        token: String
    ): FolderTreeContent {
        val normalizedCurrent = normalizePath(currentPath)
        val breadcrumbs = buildBreadcrumbs(rootName, normalizedCurrent)

        if (contents.isNullOrEmpty()) {
            return FolderTreeContent(
                currentPath = normalizedCurrent,
                breadcrumbs = breadcrumbs,
                subfolders = emptyList(),
                files = emptyList()
            )
        }

        val directFiles = mutableListOf<FolderFileItem>()
        val subfolderItemMap = mutableMapOf<String, MutableList<ModuleContent>>()

        contents.forEachIndexed { index, content ->
            val filename = content.filename?.cleanHtml() ?: return@forEachIndexed
            if (filename == ".") return@forEachIndexed

            val filePath = normalizePath(content.filepath ?: "/")

            if (filePath == normalizedCurrent) {
                val viewable = createViewableFile(content, moduleId, index, token)
                if (viewable != null) {
                    directFiles.add(FolderFileItem(content = content.sanitized(), viewableFile = viewable))
                }
            } else if (filePath.startsWith(normalizedCurrent)) {
                val relativePath = filePath.removePrefix(normalizedCurrent)
                val subfolderName = relativePath.substringBefore('/')
                if (subfolderName.isNotBlank()) {
                    subfolderItemMap.getOrPut(subfolderName) { mutableListOf() }.add(content)
                }
            }
        }

        val subfolderItems = subfolderItemMap.map { (subName, items) ->
            val fullSubPath = "$normalizedCurrent$subName/"
            FolderSubfolderItem(
                name = subName,
                fullPath = fullSubPath,
                itemCount = items.count { !it.filename.isNullOrBlank() && it.filename != "." }
            )
        }.sortedBy { it.name.lowercase() }

        val cleanQuery = searchQuery.trim().lowercase()
        val filteredFiles = if (cleanQuery.isBlank()) {
            directFiles.sortedBy { it.viewableFile.title.lowercase() }
        } else {
            directFiles.filter { it.viewableFile.title.lowercase().contains(cleanQuery) }
                .sortedBy { it.viewableFile.title.lowercase() }
        }

        val filteredSubfolders = if (cleanQuery.isBlank()) {
            subfolderItems
        } else {
            subfolderItems.filter { it.name.lowercase().contains(cleanQuery) }
        }

        return FolderTreeContent(
            currentPath = normalizedCurrent,
            breadcrumbs = breadcrumbs,
            subfolders = filteredSubfolders,
            files = filteredFiles
        )
    }

    /**
     * Extracts all viewable files belonging to a specific subfolder path (or root folder).
     */
    fun extractAllFiles(
        contents: List<ModuleContent>?,
        moduleId: Int,
        folderPath: String = "/",
        token: String
    ): List<ViewableFile> {
        if (contents.isNullOrEmpty()) return emptyList()
        val normalizedTarget = normalizePath(folderPath)

        return contents.mapIndexedNotNull { index, content ->
            val filename = content.filename?.cleanHtml() ?: return@mapIndexedNotNull null
            if (filename == ".") return@mapIndexedNotNull null

            val filePath = normalizePath(content.filepath ?: "/")
            if (filePath.startsWith(normalizedTarget)) {
                createViewableFile(content, moduleId, index, token)
            } else null
        }
    }
}
