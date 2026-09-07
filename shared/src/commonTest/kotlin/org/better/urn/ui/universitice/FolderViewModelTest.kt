package org.better.urn.ui.universitice

import kotlinx.coroutines.Dispatchers
import org.better.urn.data.CourseModule
import org.better.urn.data.ModuleContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FolderViewModelTest {

    private fun createSampleFolderModule(): CourseModule {
        return CourseModule(
            id = 50,
            name = "Dossier de Travaux Dirigés",
            modname = "folder",
            description = "Contient tous les exercices du semestre.",
            contents = listOf(
                ModuleContent(
                    filename = "Consignes.pdf",
                    filepath = "/",
                    fileurl = "https://example.com/consignes.pdf",
                    mimetype = "application/pdf"
                ),
                ModuleContent(
                    filename = "Sujet_1.pdf",
                    filepath = "/TD1/",
                    fileurl = "https://example.com/sujet1.pdf",
                    mimetype = "application/pdf"
                )
            )
        )
    }

    @Test
    fun testOpenAndCloseFolder() {
        val viewModel = UniversiticeViewModel(Dispatchers.Unconfined)
        val folder = createSampleFolderModule()

        assertNull(viewModel.uiState.value.selectedFolderModule)

        viewModel.openFolder(folder)

        val state = viewModel.uiState.value
        assertNotNull(state.selectedFolderModule)
        assertEquals(50, state.selectedFolderModule?.id)
        assertEquals("/", state.currentFolderPath)
        assertNotNull(state.folderTreeContent)
        assertEquals(1, state.folderTreeContent?.files?.size)
        assertEquals(1, state.folderTreeContent?.subfolders?.size)

        viewModel.closeFolder()
        assertNull(viewModel.uiState.value.selectedFolderModule)
    }

    @Test
    fun testNavigateToSubfolderAndFolderUp() {
        val viewModel = UniversiticeViewModel(Dispatchers.Unconfined)
        val folder = createSampleFolderModule()

        viewModel.openFolder(folder)
        viewModel.navigateToSubfolder("/TD1/")

        assertEquals("/TD1/", viewModel.uiState.value.currentFolderPath)
        val subfolderTree = viewModel.uiState.value.folderTreeContent
        assertNotNull(subfolderTree)
        assertEquals(1, subfolderTree.files.size)
        assertEquals("Sujet_1.pdf", subfolderTree.files[0].viewableFile.title)

        viewModel.navigateFolderUp()
        assertEquals("/", viewModel.uiState.value.currentFolderPath)

        viewModel.navigateFolderUp()
        assertNull(viewModel.uiState.value.selectedFolderModule)
    }

    @Test
    fun testFolderSearchQueryChange() {
        val viewModel = UniversiticeViewModel(Dispatchers.Unconfined)
        val folder = createSampleFolderModule()

        viewModel.openFolder(folder)
        viewModel.onFolderSearchQueryChange("Consignes")

        assertEquals("Consignes", viewModel.uiState.value.folderSearchQuery)
        val tree = viewModel.uiState.value.folderTreeContent
        assertNotNull(tree)
        assertEquals(1, tree.files.size)
        assertEquals("Consignes.pdf", tree.files[0].viewableFile.title)
    }

    @Test
    fun testOpenModuleFileWithFolderModname() {
        val viewModel = UniversiticeViewModel(Dispatchers.Unconfined)
        val folder = createSampleFolderModule()

        val opened = viewModel.openModuleFile(folder)
        assertTrue(opened)
        assertNotNull(viewModel.uiState.value.selectedFolderModule)
        assertEquals(50, viewModel.uiState.value.selectedFolderModule?.id)
    }

    @Test
    fun testCloseCourseClearsFolderState() {
        val viewModel = UniversiticeViewModel(Dispatchers.Unconfined)
        val folder = createSampleFolderModule()

        viewModel.openFolder(folder)
        assertNotNull(viewModel.uiState.value.selectedFolderModule)

        viewModel.closeCourse()
        assertNull(viewModel.uiState.value.selectedFolderModule)
        assertEquals("/", viewModel.uiState.value.currentFolderPath)
        assertEquals("", viewModel.uiState.value.folderSearchQuery)
    }
}
