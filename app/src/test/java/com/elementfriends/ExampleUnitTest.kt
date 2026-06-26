package com.elementfriends

import com.elementfriends.data.model.DailyQuizManager
import com.elementfriends.data.model.CompoundRecipe
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testDailyQuizManager() {
    val dateStr = "2026-06-22"
    val question = DailyQuizManager.getQuizForDate(dateStr)
    
    assertEquals(dateStr, question.dateString)
    assertNotNull(question.targetRecipe)
    
    // Check that we generated exactly 4 options
    assertEquals(4, question.options.size)
    
    // Check both correct ingredients are indeed in the options
    val optionIds = question.options.map { it.id }
    assertTrue(optionIds.contains(question.targetRecipe.ingredientA))
    assertTrue(optionIds.contains(question.targetRecipe.ingredientB))
    
    // Check consistency: same date should always produce same quiz
    val questionDuplicate = DailyQuizManager.getQuizForDate(dateStr)
    assertEquals(question.targetRecipe.formulaId, questionDuplicate.targetRecipe.formulaId)
    assertEquals(
      question.options.map { it.id },
      questionDuplicate.options.map { it.id }
    )
  }
}
