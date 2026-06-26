package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

data class QuizQuestion(
    val dateString: String,
    val targetRecipe: CompoundRecipe,
    val options: List<ChemicalElement>
)

object DailyQuizManager {
    fun getCurrentDateString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }

    fun getQuizForDate(dateString: String): QuizQuestion {
        val hash = Math.abs(dateString.hashCode())
        val index = hash % CompoundRecipe.ALL_RECIPES.size
        val targetRecipe = CompoundRecipe.ALL_RECIPES[index]

        val correctIds = listOf(targetRecipe.ingredientA, targetRecipe.ingredientB)
        // Distractors from other base elements
        val otherElements = ChemicalElement.ALL_BASE.filter { it.id !in correctIds }
        
        // Deterministic random generator for choices based on date hash
        val random = Random(hash.toLong())
        val distractors = otherElements.shuffled(random).take(2)
        
        val chosenOptions = (ChemicalElement.ALL_BASE.filter { it.id in correctIds } + distractors)
            .shuffled(random)

        return QuizQuestion(
            dateString = dateString,
            targetRecipe = targetRecipe,
            options = chosenOptions
        )
    }
}
