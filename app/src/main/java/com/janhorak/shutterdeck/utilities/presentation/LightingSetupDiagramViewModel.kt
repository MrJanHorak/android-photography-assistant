package com.janhorak.shutterdeck.utilities.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.janhorak.shutterdeck.core.data.db.LightingSetupDao
import com.janhorak.shutterdeck.core.data.db.LightingSetupEntity
import com.janhorak.shutterdeck.core.data.db.LightingSetupItemEntity
import com.janhorak.shutterdeck.utilities.domain.LightingSetupDiagram
import com.janhorak.shutterdeck.utilities.domain.LightingSetupDraftItem
import com.janhorak.shutterdeck.utilities.domain.LightingSetupItemType
import com.janhorak.shutterdeck.utilities.domain.clampLightingSetupFraction
import com.janhorak.shutterdeck.utilities.domain.defaultLightingSetupDraftItems
import com.janhorak.shutterdeck.utilities.domain.nextLightingSetupLightPosition
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LightingSetupEditorState(
    val currentSetupId: Long? = null,
    val createdAtMillis: Long? = null,
    val name: String = "",
    val notes: String = "",
    val items: List<LightingSetupDraftItem> = emptyList(),
    val selectedItemId: Long? = null,
)

@HiltViewModel
class LightingSetupDiagramViewModel @Inject constructor(
    private val lightingSetupDao: LightingSetupDao,
) : ViewModel() {

    private val _editorState = MutableStateFlow(newLightingSetupEditorState())
    val editorState: StateFlow<LightingSetupEditorState> = _editorState.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    val savedSetups: StateFlow<List<LightingSetupDiagram>> = combine(
        lightingSetupDao.observeSetups(),
        lightingSetupDao.observeSetupItems(),
    ) { setups, items ->
        val itemsBySetupId = items.groupBy { item -> item.setupId }
        setups.map { setup ->
            LightingSetupDiagram(
                id = setup.id,
                name = setup.name,
                notes = setup.notes,
                createdAtMillis = setup.createdAtMillis,
                updatedAtMillis = setup.updatedAtMillis,
                items = itemsBySetupId[setup.id].orEmpty().map { item ->
                    LightingSetupDraftItem(
                        localId = item.id,
                        type = LightingSetupItemType.fromStoredValue(item.itemType),
                        label = item.label,
                        xFraction = item.xFraction,
                        yFraction = item.yFraction,
                    )
                },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateName(name: String) {
        _editorState.update { state -> state.copy(name = name) }
        clearStatus()
    }

    fun updateNotes(notes: String) {
        _editorState.update { state -> state.copy(notes = notes) }
        clearStatus()
    }

    fun selectItem(localId: Long) {
        _editorState.update { state ->
            state.copy(selectedItemId = localId)
        }
    }

    fun updateSelectedLabel(label: String) {
        _editorState.update { state ->
            val selectedId = state.selectedItemId ?: return@update state
            state.copy(
                items = state.items.map { item ->
                    if (item.localId == selectedId) {
                        item.copy(label = label)
                    } else {
                        item
                    }
                },
            )
        }
        clearStatus()
    }

    fun moveItem(
        localId: Long,
        deltaXFraction: Float,
        deltaYFraction: Float,
    ) {
        _editorState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.localId == localId) {
                        item.copy(
                            xFraction = clampLightingSetupFraction(item.xFraction + deltaXFraction),
                            yFraction = clampLightingSetupFraction(item.yFraction + deltaYFraction),
                        )
                    } else {
                        item
                    }
                },
            )
        }
    }

    fun nudgeSelectedItem(
        deltaXFraction: Float,
        deltaYFraction: Float,
    ) {
        val selectedId = _editorState.value.selectedItemId ?: return
        moveItem(
            localId = selectedId,
            deltaXFraction = deltaXFraction,
            deltaYFraction = deltaYFraction,
        )
    }

    fun centerSelectedItem() {
        _editorState.update { state ->
            val selectedId = state.selectedItemId ?: return@update state
            state.copy(
                items = state.items.map { item ->
                    if (item.localId == selectedId) {
                        item.copy(xFraction = 0.5f, yFraction = 0.5f)
                    } else {
                        item
                    }
                },
            )
        }
    }

    fun addLight() {
        _editorState.update { state ->
            val existingLightCount = state.items.count { item -> item.type == LightingSetupItemType.LIGHT }
            val nextLocalId = (state.items.maxOfOrNull { item -> item.localId } ?: 0L) + 1L
            val (xFraction, yFraction) = nextLightingSetupLightPosition(existingLightCount)
            val light = LightingSetupDraftItem(
                localId = nextLocalId,
                type = LightingSetupItemType.LIGHT,
                label = "Light ${existingLightCount + 1}",
                xFraction = xFraction,
                yFraction = yFraction,
            )
            state.copy(
                items = state.items + light,
                selectedItemId = light.localId,
            )
        }
        _status.value = "Added a light marker."
    }

    fun removeSelectedLight() {
        _editorState.update { state ->
            val selectedId = state.selectedItemId ?: return@update state
            val selectedItem = state.items.firstOrNull { item -> item.localId == selectedId }
                ?: return@update state
            if (selectedItem.type != LightingSetupItemType.LIGHT) {
                _status.value = "Only light markers can be removed."
                return@update state
            }
            val remainingItems = state.items.filterNot { item -> item.localId == selectedId }
            state.copy(
                items = remainingItems,
                selectedItemId = preferredSelectionId(remainingItems),
                currentSetupId = state.currentSetupId,
                createdAtMillis = state.createdAtMillis,
            )
        }
        if (_status.value == null) {
            _status.value = "Removed the selected light."
        }
    }

    fun createNewDraft() {
        _editorState.value = newLightingSetupEditorState()
        _status.value = "Started a fresh lighting draft."
    }

    fun loadSetup(setup: LightingSetupDiagram) {
        val draftItems = setup.items.mapIndexed { index, item ->
            item.copy(localId = index.toLong() + 1L)
        }
        _editorState.value = LightingSetupEditorState(
            currentSetupId = setup.id,
            createdAtMillis = setup.createdAtMillis,
            name = setup.name,
            notes = setup.notes,
            items = draftItems,
            selectedItemId = preferredSelectionId(draftItems),
        )
        _status.value = "Loaded \"${setup.name}\" into the editor."
    }

    fun saveCurrentSetup(): Boolean {
        val state = _editorState.value
        val trimmedName = state.name.trim()
        if (trimmedName.isEmpty()) {
            _status.value = "Name the setup before saving it."
            return false
        }
        val trimmedNotes = state.notes.trim()
        val normalizedItems = normalizeDraftItems(state.items)
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            val setupId = lightingSetupDao.saveSetup(
                setup = LightingSetupEntity(
                    id = state.currentSetupId ?: 0L,
                    name = trimmedName,
                    notes = trimmedNotes,
                    createdAtMillis = state.createdAtMillis ?: now,
                    updatedAtMillis = now,
                ),
                items = normalizedItems.mapIndexed { index, item ->
                    LightingSetupItemEntity(
                        setupId = state.currentSetupId ?: 0L,
                        itemType = item.type.name,
                        label = item.label,
                        xFraction = item.xFraction,
                        yFraction = item.yFraction,
                        sortOrder = index,
                    )
                },
            )
            _editorState.value = state.copy(
                currentSetupId = setupId,
                createdAtMillis = state.createdAtMillis ?: now,
                name = trimmedName,
                notes = trimmedNotes,
                items = normalizedItems,
                selectedItemId = state.selectedItemId ?: preferredSelectionId(normalizedItems),
            )
            _status.value = if (state.currentSetupId == null) {
                "Saved the lighting setup."
            } else {
                "Updated the lighting setup."
            }
        }
        return true
    }

    fun deleteSetup(setup: LightingSetupDiagram) {
        viewModelScope.launch {
            lightingSetupDao.deleteSetup(
                LightingSetupEntity(
                    id = setup.id,
                    name = setup.name,
                    notes = setup.notes,
                    createdAtMillis = setup.createdAtMillis,
                    updatedAtMillis = setup.updatedAtMillis,
                ),
            )
            if (_editorState.value.currentSetupId == setup.id) {
                _editorState.update { state ->
                    state.copy(currentSetupId = null, createdAtMillis = null)
                }
                _status.value = "Deleted the saved setup. The current diagram is now an unsaved draft."
            } else {
                _status.value = "Deleted \"${setup.name}\"."
            }
        }
    }

    fun setStatus(message: String?) {
        _status.value = message
    }

    fun clearStatus() {
        _status.value = null
    }
}

private fun newLightingSetupEditorState(): LightingSetupEditorState {
    val items = defaultLightingSetupDraftItems()
    return LightingSetupEditorState(
        items = items,
        selectedItemId = preferredSelectionId(items),
    )
}

private fun preferredSelectionId(items: List<LightingSetupDraftItem>): Long? =
    items.firstOrNull { item -> item.type == LightingSetupItemType.SUBJECT }?.localId
        ?: items.firstOrNull()?.localId

private fun normalizeDraftItems(items: List<LightingSetupDraftItem>): List<LightingSetupDraftItem> =
    items.map { item ->
        val normalizedLabel = item.label.trim().ifBlank {
            when (item.type) {
                LightingSetupItemType.CAMERA -> "Camera"
                LightingSetupItemType.SUBJECT -> "Subject"
                LightingSetupItemType.LIGHT -> "Light"
            }
        }
        item.copy(
            label = normalizedLabel,
            xFraction = clampLightingSetupFraction(item.xFraction),
            yFraction = clampLightingSetupFraction(item.yFraction),
        )
    }
