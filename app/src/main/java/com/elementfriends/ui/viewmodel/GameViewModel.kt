package com.elementfriends.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.elementfriends.data.database.DiscoveredFormulaEntity
import com.elementfriends.data.database.GameDatabase
import com.elementfriends.data.database.ProgressEntity
import com.elementfriends.data.model.*
import com.elementfriends.data.repository.GameRepository
import com.elementfriends.data.repository.MergeResult
import com.elementfriends.ui.audio.SoundSynth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GameUiState(
    val baseElements: List<ChemicalElement> = ChemicalElement.ALL_BASE,
    val unlockedFormulas: List<DiscoveredFormulaEntity> = emptyList(),
    val progress: ProgressEntity = ProgressEntity(),
    val workspaceLeft: String? = null,
    val workspaceRight: String? = null,
    val combineAnimationActive: Boolean = false,
    val lastMergeResult: MergeResult.Success? = null,
    val errorMessage: String? = null,
    val showLevelUpDialog: Int? = null,
    val lastSyncCodeGenerated: String? = null,
    val syncSuccess: Boolean = false,
    val restoreStatus: Boolean? = null,
    val comboCount: Int = 0,
    val dailyQuiz: QuizQuestion = DailyQuizManager.getQuizForDate(DailyQuizManager.getCurrentDateString()),
    val quizSelectedElementIds: List<String> = emptyList(),
    val quizSuccess: Boolean? = null,
    val isTutorialActive: Boolean = false,
    val tutorialStep: Int = 1,
    val isAudioEnabled: Boolean = SoundSynth.isAudioEnabled,
    val badges: List<ElementBadge> = emptyList(),
    val justUnlockedBadge: ElementBadge? = null,
    val activeHintRecipe: CompoundRecipe? = null
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val database by lazy {
        Room.databaseBuilder(
            application,
            GameDatabase::class.java,
            "element_friends_database"
        ).fallbackToDestructiveMigration().build()
    }

    private val repository by lazy {
        GameRepository(database.formulaDao(), database.progressDao())
    }

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        // Collect discovered formulas from database
        viewModelScope.launch {
            repository.discoveredFormulas.collect { list ->
                _uiState.update { state ->
                    val nextFormulas = list
                    val oldBadges = state.badges
                    val newBadges = ElementBadge.getBadges(state.progress, nextFormulas)
                    
                    var unlockedAlert: ElementBadge? = null
                    if (oldBadges.isNotEmpty()) {
                        for (i in newBadges.indices) {
                            val oldB = oldBadges.getOrNull(i)
                            val newB = newBadges[i]
                            if (oldB != null && !oldB.isUnlocked && newB.isUnlocked) {
                                unlockedAlert = newB
                                viewModelScope.launch {
                                    SoundSynth.playLevelUpSound()
                                }
                                break
                            }
                        }
                    }
                    
                    state.copy(
                        unlockedFormulas = nextFormulas,
                        badges = newBadges,
                        justUnlockedBadge = unlockedAlert ?: state.justUnlockedBadge
                    )
                }
            }
        }

        // Collect progress states and detect level ups
        viewModelScope.launch {
            var isFirstCollection = true
            repository.progress.collect { prog ->
                val prevProg = _uiState.value.progress
                
                _uiState.update { state ->
                    val oldBadges = state.badges
                    val newBadges = ElementBadge.getBadges(prog, state.unlockedFormulas)
                    
                    var unlockedAlert: ElementBadge? = null
                    if (oldBadges.isNotEmpty()) {
                        for (i in newBadges.indices) {
                            val oldB = oldBadges.getOrNull(i)
                            val newB = newBadges[i]
                            if (oldB != null && !oldB.isUnlocked && newB.isUnlocked) {
                                unlockedAlert = newB
                                viewModelScope.launch {
                                    SoundSynth.playLevelUpSound()
                                }
                                break
                            }
                        }
                    }
                    
                    state.copy(
                        progress = prog,
                        badges = newBadges,
                        justUnlockedBadge = unlockedAlert ?: state.justUnlockedBadge
                    )
                }
                
                if (!isFirstCollection && prog.level > prevProg.level) {
                    _uiState.update { it.copy(showLevelUpDialog = prog.level) }
                    // Reward kid with sparkly level up chime fanfare
                    viewModelScope.launch {
                        SoundSynth.playLevelUpSound()
                    }
                }
                isFirstCollection = false
            }
        }

        // Start playing the cute baby lullaby / chime BGM on startup
        SoundSynth.startBgm()
    }

    fun selectElement(elementId: String) {
        val current = _uiState.value
        viewModelScope.launch {
            SoundSynth.playPopSound()
        }
        if (current.workspaceLeft == null) {
            _uiState.update { it.copy(workspaceLeft = elementId) }
        } else if (current.workspaceRight == null) {
            _uiState.update { it.copy(workspaceRight = elementId) }
        }
    }

    fun setLeftSlot(elementId: String) {
        viewModelScope.launch { SoundSynth.playPopSound() }
        _uiState.update { it.copy(workspaceLeft = elementId) }
    }

    fun setRightSlot(elementId: String) {
        viewModelScope.launch { SoundSynth.playPopSound() }
        _uiState.update { it.copy(workspaceRight = elementId) }
    }

    fun clearLeftSlot() {
        viewModelScope.launch { SoundSynth.playPopSound() }
        _uiState.update { it.copy(workspaceLeft = null) }
    }

    fun clearRightSlot() {
        viewModelScope.launch { SoundSynth.playPopSound() }
        _uiState.update { it.copy(workspaceRight = null) }
    }

    fun clearWorkspace() {
        viewModelScope.launch { SoundSynth.playPopSound() }
        _uiState.update { it.copy(workspaceLeft = null, workspaceRight = null) }
    }

    fun fillWorkspace(leftElementId: String, rightElementId: String) {
        viewModelScope.launch { SoundSynth.playPopSound() }
        _uiState.update { it.copy(workspaceLeft = leftElementId, workspaceRight = rightElementId) }
    }

    fun triggerCombine() {
        val state = _uiState.value
        val left = state.workspaceLeft
        val right = state.workspaceRight

        if (left == null || right == null) return

        viewModelScope.launch {
            // Trigger interactive pulsing fusion animation
            _uiState.update { it.copy(combineAnimationActive = true) }
            
            // Dramatic pause for fusion animation
            kotlinx.coroutines.delay(1400)

            val result = repository.combineElements(left, right)
            
            _uiState.update { it.copy(combineAnimationActive = false) }

            when (result) {
                is MergeResult.Success -> {
                    SoundSynth.playCombineSound(success = true)
                    val newCombo = _uiState.value.comboCount + 1
                    _uiState.update {
                        val isHintCompleted = it.activeHintRecipe?.formulaId == result.recipe.formulaId
                        it.copy(
                            lastMergeResult = result,
                            workspaceLeft = null,
                            workspaceRight = null,
                            comboCount = newCombo,
                            activeHintRecipe = if (isHintCompleted) null else it.activeHintRecipe
                        )
                    }
                    repository.updateMaxCombo(newCombo)
                }
                is MergeResult.Failure -> {
                    SoundSynth.playCombineSound(success = false)
                    _uiState.update {
                        it.copy(
                            errorMessage = result.messageKo,
                            workspaceLeft = null,
                            workspaceRight = null,
                            comboCount = 0
                        )
                    }
                }
            }
        }
    }

    fun dismissMergeDialog() {
        _uiState.update { it.copy(lastMergeResult = null) }
    }

    fun dismissErrorDialog() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissLevelUpDialog() {
        _uiState.update { it.copy(showLevelUpDialog = null) }
    }

    fun dismissBadgeDialog() {
        _uiState.update { it.copy(justUnlockedBadge = null) }
    }

    fun performCloudBackup() {
        viewModelScope.launch {
            val code = repository.generateCloudBackupCode()
            _uiState.update { it.copy(lastSyncCodeGenerated = code, syncSuccess = true) }
        }
    }

    fun dismissSyncSuccess() {
        _uiState.update { it.copy(syncSuccess = false) }
    }

    fun performCloudRestore(code: String) {
        viewModelScope.launch {
            val success = repository.restoreProgress(code)
            _uiState.update { it.copy(restoreStatus = success) }
        }
    }

    fun dismissRestoreStatus() {
        _uiState.update { it.copy(restoreStatus = null) }
    }

    fun getExportToken(onTokenReady: (String) -> Unit) {
        viewModelScope.launch {
            val token = repository.getExportPayload()
            onTokenReady(token)
        }
    }

    fun resetGame() {
        viewModelScope.launch {
            repository.resetAll()
            clearWorkspace()
            _uiState.update { it.copy(comboCount = 0, quizSelectedElementIds = emptyList(), quizSuccess = null) }
        }
    }

    fun toggleQuizElement(elementId: String) {
        val currentSelected = _uiState.value.quizSelectedElementIds
        val isCompleted = _uiState.value.progress.lastQuizSolvedDate == _uiState.value.dailyQuiz.dateString
        if (isCompleted || _uiState.value.quizSuccess == true) return

        viewModelScope.launch { SoundSynth.playPopSound() }

        if (elementId in currentSelected) {
            _uiState.update { it.copy(quizSelectedElementIds = currentSelected - elementId) }
        } else {
            if (currentSelected.size < 2) {
                _uiState.update { it.copy(quizSelectedElementIds = currentSelected + elementId) }
            }
        }
    }

    fun submitQuiz() {
        val state = _uiState.value
        val selected = state.quizSelectedElementIds
        if (selected.size != 2) return

        val target = state.dailyQuiz.targetRecipe
        val elementA = selected[0]
        val elementB = selected[1]

        val isCorrect = (elementA == target.ingredientA && elementB == target.ingredientB) || 
                        (elementA == target.ingredientB && elementB == target.ingredientA)

        if (isCorrect) {
            viewModelScope.launch {
                SoundSynth.playCombineSound(success = true)
                repository.completeDailyQuiz(state.dailyQuiz.dateString)
                _uiState.update { it.copy(quizSuccess = true) }
            }
        } else {
            viewModelScope.launch {
                SoundSynth.playCombineSound(success = false)
                _uiState.update { it.copy(quizSuccess = false) }
            }
        }
    }

    fun clearQuizSelections() {
        _uiState.update { it.copy(quizSelectedElementIds = emptyList(), quizSuccess = null) }
    }

    fun startTutorial() {
        _uiState.update { 
            it.copy(
                isTutorialActive = true, 
                tutorialStep = 1, 
                workspaceLeft = null, 
                workspaceRight = null
            ) 
        }
    }

    fun stopTutorial() {
        _uiState.update { it.copy(isTutorialActive = false) }
    }

    fun advanceTutorialStep() {
        _uiState.update { it.copy(tutorialStep = it.tutorialStep + 1) }
    }

    fun completeTutorial() {
        viewModelScope.launch {
            repository.completeTutorial()
            _uiState.update { it.copy(isTutorialActive = false, tutorialStep = 1) }
        }
    }

    fun toggleAudio() {
        SoundSynth.toggleAudio()
        _uiState.update { it.copy(isAudioEnabled = SoundSynth.isAudioEnabled) }
    }

    fun revealHint(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentDate = DailyQuizManager.getCurrentDateString()
        val currentProgress = _uiState.value.progress
        
        // Reset daily count if day changed
        val hintsUsed = if (currentProgress.lastHintDate == currentDate) {
            currentProgress.hintsUsedToday
        } else {
            0
        }
        
        if (hintsUsed >= 3) {
            onError("오늘 사용할 수 있는 힌트(3회)를 모두 사용하셨어요! 내일 다시 도전해 보아요! 🧪✨")
            return
        }
        
        val unlockedIds = _uiState.value.unlockedFormulas.map { it.formulaId }.toSet()
        val undiscoveredRecipes = CompoundRecipe.ALL_RECIPES.filter { it.formulaId !in unlockedIds }
        
        if (undiscoveredRecipes.isEmpty()) {
            onError("우와! 이미 모든 물질을 다 발견하셨어요! 더 이상 제공할 힌트가 없습니다. 대단해요! 🎉")
            return
        }
        
        // Take a recipe to give a hint for
        val hintRecipe = undiscoveredRecipes.first()
        
        viewModelScope.launch {
            val updatedProgress = currentProgress.copy(
                lastHintDate = currentDate,
                hintsUsedToday = hintsUsed + 1
            )
            repository.saveProgress(updatedProgress)
            _uiState.update {
                it.copy(
                    activeHintRecipe = hintRecipe,
                    progress = updatedProgress
                )
            }
            onSuccess()
        }
    }

    fun clearActiveHint() {
        _uiState.update { it.copy(activeHintRecipe = null) }
    }

    override fun onCleared() {
        super.onCleared()
        SoundSynth.stopBgm()
    }
}
