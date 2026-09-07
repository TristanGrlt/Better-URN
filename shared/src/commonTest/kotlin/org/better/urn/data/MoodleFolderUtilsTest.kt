package org.better.urn.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MoodleFolderUtilsTest {

    @Test
    fun testNormalizePath() {
        assertEquals("/", MoodleFolderUtils.normalizePath(""))
        assertEquals("/", MoodleFolderUtils.normalizePath("/"))
        assertEquals("/TD1/", MoodleFolderUtils.normalizePath("TD1"))
        assertEquals("/TD1/", MoodleFolderUtils.normalizePath("/TD1"))
        assertEquals("/TD1/", MoodleFolderUtils.normalizePath("TD1/"))
        assertEquals("/TD1/Exercices/", MoodleFolderUtils.normalizePath("TD1/Exercices"))
    }

    @Test
    fun testGetParentPath() {
        assertEquals("/", MoodleFolderUtils.getParentPath("/"))
        assertEquals("/", MoodleFolderUtils.getParentPath("/TD1/"))
        assertEquals("/TD1/", MoodleFolderUtils.getParentPath("/TD1/Exercices/"))
        assertEquals("/TD1/Exercices/", MoodleFolderUtils.getParentPath("/TD1/Exercices/TP1/"))
    }

    @Test
    fun testBuildBreadcrumbs() {
        val rootCrumbs = MoodleFolderUtils.buildBreadcrumbs("Dossier Cours", "/")
        assertEquals(1, rootCrumbs.size)
        assertEquals("Dossier Cours", rootCrumbs[0].name)
        assertEquals("/", rootCrumbs[0].path)

        val nestedCrumbs = MoodleFolderUtils.buildBreadcrumbs("Dossier Cours", "/TD1/Exercices/")
        assertEquals(3, nestedCrumbs.size)
        assertEquals("Dossier Cours", nestedCrumbs[0].name)
        assertEquals("/", nestedCrumbs[0].path)

        assertEquals("TD1", nestedCrumbs[1].name)
        assertEquals("/TD1/", nestedCrumbs[1].path)

        assertEquals("Exercices", nestedCrumbs[2].name)
        assertEquals("/TD1/Exercices/", nestedCrumbs[2].path)
    }

    @Test
    fun testParseFolderTreeRootAndSubfolders() {
        val contents = listOf(
            ModuleContent(
                filename = "Syllabus.pdf",
                filepath = "/",
                fileurl = "https://example.com/syllabus.pdf",
                mimetype = "application/pdf"
            ),
            ModuleContent(
                filename = "Sujet_TD1.pdf",
                filepath = "/TD1/",
                fileurl = "https://example.com/td1.pdf",
                mimetype = "application/pdf"
            ),
            ModuleContent(
                filename = "Correction_TD1.pdf",
                filepath = "/TD1/",
                fileurl = "https://example.com/corr_td1.pdf",
                mimetype = "application/pdf"
            ),
            ModuleContent(
                filename = "Sujet_TD2.pdf",
                filepath = "/TD2/",
                fileurl = "https://example.com/td2.pdf",
                mimetype = "application/pdf"
            )
        )

        // Parse Root Path "/"
        val rootTree = MoodleFolderUtils.parseFolderTree(
            contents = contents,
            moduleId = 10,
            rootName = "Ressources",
            currentPath = "/",
            searchQuery = "",
            token = "token123"
        )

        assertEquals("/", rootTree.currentPath)
        assertEquals(1, rootTree.files.size)
        assertEquals("Syllabus.pdf", rootTree.files[0].viewableFile.title)

        assertEquals(2, rootTree.subfolders.size)
        val td1Subfolder = rootTree.subfolders.find { it.name == "TD1" }
        assertNotNull(td1Subfolder)
        assertEquals("/TD1/", td1Subfolder.fullPath)
        assertEquals(2, td1Subfolder.itemCount)

        val td2Subfolder = rootTree.subfolders.find { it.name == "TD2" }
        assertNotNull(td2Subfolder)
        assertEquals(1, td2Subfolder.itemCount)

        // Parse Subfolder Path "/TD1/"
        val td1Tree = MoodleFolderUtils.parseFolderTree(
            contents = contents,
            moduleId = 10,
            rootName = "Ressources",
            currentPath = "/TD1/",
            searchQuery = "",
            token = "token123"
        )

        assertEquals("/TD1/", td1Tree.currentPath)
        assertEquals(0, td1Tree.subfolders.size)
        assertEquals(2, td1Tree.files.size)
    }

    @Test
    fun testParseFolderTreeSearchFilter() {
        val contents = listOf(
            ModuleContent(
                filename = "Chapitre1_Introduction.pdf",
                filepath = "/",
                fileurl = "https://example.com/c1.pdf",
                mimetype = "application/pdf"
            ),
            ModuleContent(
                filename = "Chapitre2_Avance.pdf",
                filepath = "/",
                fileurl = "https://example.com/c2.pdf",
                mimetype = "application/pdf"
            )
        )

        val filteredTree = MoodleFolderUtils.parseFolderTree(
            contents = contents,
            moduleId = 15,
            rootName = "Cours",
            currentPath = "/",
            searchQuery = "Introduction",
            token = "token123"
        )

        assertEquals(1, filteredTree.files.size)
        assertEquals("Chapitre1_Introduction.pdf", filteredTree.files[0].viewableFile.title)
    }

    @Test
    fun testExtractAllFiles() {
        val contents = listOf(
            ModuleContent(
                filename = "doc1.pdf",
                filepath = "/",
                fileurl = "https://example.com/d1.pdf",
                mimetype = "application/pdf"
            ),
            ModuleContent(
                filename = "doc2.pdf",
                filepath = "/SousDossier/",
                fileurl = "https://example.com/d2.pdf",
                mimetype = "application/pdf"
            )
        )

        val allRootFiles = MoodleFolderUtils.extractAllFiles(contents, 20, "/", "token")
        assertEquals(2, allRootFiles.size)

        val subfolderFiles = MoodleFolderUtils.extractAllFiles(contents, 20, "/SousDossier/", "token")
        assertEquals(1, subfolderFiles.size)
        assertEquals("doc2.pdf", subfolderFiles[0].title)
    }
}
