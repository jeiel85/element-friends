package com.example.data.model

import com.example.data.database.DiscoveredFormulaEntity
import com.example.data.database.ProgressEntity

data class ElementBadge(
    val id: String,
    val title: String,
    val description: String,
    val requirement: String,
    val emoji: String,
    val isUnlocked: Boolean,
    val currentProgress: Int,
    val maxProgress: Int
) {
    companion object {
        fun getBadges(progress: ProgressEntity, unlockedFormulas: List<DiscoveredFormulaEntity>): List<ElementBadge> {
            val totalFormulasCount = unlockedFormulas.size
            val hasWater = unlockedFormulas.any { it.formulaId == "H2O" }
            val hasTutorial = progress.hasCompletedTutorial
            val maxCombo = progress.maxCombo
            val quizSolves = progress.totalQuizzesSolved
            val streak = progress.quizStreak

            return listOf(
                ElementBadge(
                    id = "baby_alchemist",
                    title = "아기 연금술사",
                    description = "첫 번째 화학 결합을 완성해 아기 연금술사가 되었습니다!",
                    requirement = "수소(H)와 산소(O)로 '물'을 합성하거나, 첫 결합 완성",
                    emoji = "💧",
                    isUnlocked = totalFormulasCount >= 1 || hasWater,
                    currentProgress = if (totalFormulasCount >= 1 || hasWater) 1 else 0,
                    maxProgress = 1
                ),
                ElementBadge(
                    id = "junior_scientist",
                    title = "꼬마 학자",
                    description = "실험실 가이드를 마스터하고 당당히 첫걸음을 내디뎠습니다!",
                    requirement = "꼬마 연구원 가이드 정독 및 미션 달성",
                    emoji = "🎓",
                    isUnlocked = hasTutorial,
                    currentProgress = if (hasTutorial) 1 else 0,
                    maxProgress = 1
                ),
                ElementBadge(
                    id = "combo_sprout",
                    title = "콤보 꿈나무",
                    description = "연속 3회 동안 결합을 성공시켜 신스 실력을 뽐내고 있습니다!",
                    requirement = "연속 결합 3콤보 이상 달성",
                    emoji = "🌱",
                    isUnlocked = maxCombo >= 3,
                    currentProgress = Math.min(maxCombo, 3),
                    maxProgress = 3
                ),
                ElementBadge(
                    id = "combo_master",
                    title = "콤보 마스터",
                    description = "놀라운 집중력으로 연속 5회 결합에 완벽히 골인하였습니다!",
                    requirement = "연속 결합 5콤보 이상 달성",
                    emoji = "⚡",
                    isUnlocked = maxCombo >= 5,
                    currentProgress = Math.min(maxCombo, 5),
                    maxProgress = 5
                ),
                ElementBadge(
                    id = "lab_explorer",
                    title = "실험실 대탐험가",
                    description = "도감 속에 신기한 화학 결합 레시피 3개 이상을 등록했습니다!",
                    requirement = "화학식 누적 3종 이상 발견",
                    emoji = "🐨",
                    isUnlocked = totalFormulasCount >= 3,
                    currentProgress = Math.min(totalFormulasCount, 3),
                    maxProgress = 3
                ),
                ElementBadge(
                    id = "element_doctor",
                    title = "웅장한 원소 박사",
                    description = "우주와 지구가 품은 화학식 7종의 레시피를 완벽히 마스터한 대현자!",
                    requirement = "파스텔 도감 화학식 ALL 발견 (7종 전부)",
                    emoji = "🏆",
                    isUnlocked = totalFormulasCount >= 7,
                    currentProgress = Math.min(totalFormulasCount, 7),
                    maxProgress = 7
                ),
                ElementBadge(
                    id = "quiz_king",
                    title = "퀴즈 도전자",
                    description = "매일 출제되는 지혜로운 일일 퀴즈를 당당히 풀었습니다!",
                    requirement = "일일 퀴즈 1회 이상 정답 완료",
                    emoji = "🎯",
                    isUnlocked = quizSolves >= 1,
                    currentProgress = Math.min(quizSolves, 1),
                    maxProgress = 1
                ),
                ElementBadge(
                    id = "steady_scientist",
                    title = "꾸준한 과학자",
                    description = "그치지 않는 정성으로 연속 퀴즈 스트릭 및 누적 도전을 이어가는 학자!",
                    requirement = "누적 퀴즈 3회 문제 해결 또는 스트릭 2회 달성",
                    emoji = "📅",
                    isUnlocked = quizSolves >= 3 || streak >= 2,
                    currentProgress = if (quizSolves >= 3 || streak >= 2) 3 else Math.max(quizSolves, streak),
                    maxProgress = 3
                )
            )
        }
    }
}
