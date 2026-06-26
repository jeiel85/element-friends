package com.elementfriends.data.model

import androidx.compose.ui.graphics.Color
import com.elementfriends.ui.theme.*

enum class ElementCategory(val nameKo: String, val emoji: String) {
    ALL("전체", "✨"),
    METAL("금속", "⛓️"),
    GAS("기체", "💨"),
    NON_METAL_LIQUID("비금속/액체", "💧")
}

data class ChemicalElement(
    val id: String,         // Symbol: e.g., "H", "O", "C", "Na", "Cl", "He", "N", "Fe"
    val symbol: String,     // Display Symbol
    val nameKo: String,     // Korean Name
    val nameEn: String,     // English Name
    val color: Color,       // Pastel theme color
    val description: String, // Child-friendly description
    val expression: String, // Cute face emoji or cute text face
    val atomicNumber: Int
) {
    companion object {
        val H = ChemicalElement(
            id = "H",
            symbol = "H",
            nameKo = "수소",
            nameEn = "Hydrogen",
            color = ColorH,
            description = "우주에서 가장 가볍고 슝슝 날아가는 호기심 많은 기체 친구예요! 물속에 꼭꼭 숨어있답니다.",
            expression = "U◡U ♥",
            atomicNumber = 1
        )
        val O = ChemicalElement(
            id = "O",
            symbol = "O",
            nameKo = "산소",
            nameEn = "Oxygen",
            color = ColorO,
            description = "우리가 하아~ 하고 숨을 쉴 때 꼭 마셔야 하는 소중한 산소 친구예요! 친구들과 잘 사귀어요.",
            expression = "●ˇ∀ˇ●",
            atomicNumber = 8
        )
        val C = ChemicalElement(
            id = "C",
            symbol = "C",
            nameKo = "탄소",
            nameEn = "Carbon",
            color = ColorC,
            description = "연필심 속에도 있고, 단단하고 값비싼 다이아몬드도 탄소예요! 생명의 튼튼한 뼈대랍니다.",
            expression = "(=ㅅ=)💤",
            atomicNumber = 6
        )
        val Na = ChemicalElement(
            id = "Na",
            symbol = "Na",
            nameKo = "나트륨",
            nameEn = "Sodium",
            color = ColorNa,
            description = "노란빛을 내는 활기찬 금속이에요! 물을 만나는 걸 좋아해서 뽀글뽀글 신나게 반응해요.",
            expression = "★_★!",
            atomicNumber = 11
        )
        val Cl = ChemicalElement(
            id = "Cl",
            symbol = "Cl",
            nameKo = "염소",
            nameEn = "Chlorine",
            color = ColorCl,
            description = "톡 쏘는 수영장 냄새가 매력적인 기체예요! 세균을 혼내주고 나트륨과 만나 소금이 돼요.",
            expression = "q(≧▽≦q)",
            atomicNumber = 17
        )
        val He = ChemicalElement(
            id = "He",
            symbol = "He",
            nameKo = "헬륨",
            nameEn = "Helium",
            color = ColorHe,
            description = "정말 가벼워서 둥실둥실 풍선을 날려주고, 마시면 목소리가 오리처럼 변하는 말썽쟁이예요!",
            expression = "🎈(^▽^)",
            atomicNumber = 2
        )
        val N = ChemicalElement(
            id = "N",
            symbol = "N",
            nameKo = "질소",
            nameEn = "Nitrogen",
            color = ColorN,
            description = "공기 중에 가장 얌전히 많이 들어있는 든든한 친구예요. 과자 봉지를 폭신하게 채워줘요.",
            expression = "(✿◡_◡)",
            atomicNumber = 7
        )
        val Fe = ChemicalElement(
            id = "Fe",
            symbol = "Fe",
            nameKo = "철",
            nameEn = "Iron",
            color = ColorFe,
            description = "아주 튼튼해서 기찻길이나 무거운 탑을 만드는 기둥이 돼요! 자석과 꼭 안아주기를 좋아해요.",
            expression = "🤖[O_O]",
            atomicNumber = 26
        )

        val ALL_BASE = listOf(H, O, C, Na, Cl, He, N, Fe)
        val BASE_MAP = ALL_BASE.associateBy { it.id }
    }
}

data class CompoundRecipe(
    val formulaId: String,      // Chemical symbol, e.g., "H2O"
    val ingredientA: String,    // ID of ingredient A
    val ingredientB: String,    // ID of ingredient B
    val nameKo: String,
    val nameEn: String,
    val description: String,
    val characterEmoji: String,
    val color: Color = ColorCompound
) {
    companion object {
        val ALL_RECIPES = listOf(
            CompoundRecipe(
                formulaId = "H2O",
                ingredientA = "H",
                ingredientB = "O",
                nameKo = "물 (Water)",
                nameEn = "Water",
                description = "생명에게 가장 소중하고 깨끗한 물이에요! 수소와 산소가 합체해서 투명한 방울이 되었답니다.",
                characterEmoji = "💧",
                color = ColorH
            ),
            CompoundRecipe(
                formulaId = "CO2",
                ingredientA = "C",
                ingredientB = "O",
                nameKo = "이산화탄소 (Carbon Dioxide)",
                nameEn = "Carbon Dioxide",
                description = "식물들의 맛있는 밥이자, 우리가 숨을 내쉴 때 보이지 않게 나가는 뽀글뽀글 거품 가스예요.",
                characterEmoji = "🫧",
                color = ColorC
            ),
            CompoundRecipe(
                formulaId = "NaCl",
                ingredientA = "Na",
                ingredientB = "Cl",
                nameKo = "소금 (Salt)",
                nameEn = "Sodium Chloride",
                description = "음식을 아주 맛있게 만들어 주는 하얗고 보배로운 소금이에요! 바다에 아주 많답니다.",
                characterEmoji = "🧂",
                color = ColorNa
            ),
            CompoundRecipe(
                formulaId = "HCl",
                ingredientA = "H",
                ingredientB = "Cl",
                nameKo = "염산 (Hydrochloric Acid)",
                nameEn = "Hydrochloric Acid",
                description = "우리 위장 속에서 피자나 밥을 골고루 소화시키는 용감한 액체예요! 조심스레 다뤄야 해요.",
                characterEmoji = "🧪",
                color = ColorCl
            ),
            CompoundRecipe(
                formulaId = "NH3",
                ingredientA = "H",
                ingredientB = "N",
                nameKo = "암모니아 (Ammonia)",
                nameEn = "Ammonia",
                description = "약간 찌릿찌릿 삐~ 하는 냄새를 풍기지만, 농부 아저씨의 밭을 기름지게 자라게 돕는 영양소랍니다.",
                characterEmoji = "💨",
                color = ColorN
            ),
            CompoundRecipe(
                formulaId = "Fe2O3",
                ingredientA = "Fe",
                ingredientB = "O",
                nameKo = "녹 (Rust)",
                nameEn = "Iron Oxide",
                description = "단단한 철이 산소 공기랑 친구가 되면서 오렌지색으로 변신하는 귀여운 결합체예요.",
                characterEmoji = "🧱",
                color = ColorFe
            ),
            CompoundRecipe(
                formulaId = "CH4",
                ingredientA = "C",
                ingredientB = "H",
                nameKo = "메탄가스 (Methane)",
                nameEn = "Methane",
                description = "젖소들이 뿡~ 하고 뀌는 방귀나 주방의 가스렌지에 들어 있어 깨끗한 열에너지를 만들어 줘요.",
                characterEmoji = "🐮",
                color = ColorCompound
            )
        )

        // Support standard combinations (e.g. H + O, Na + Cl)
        fun find(a: String, b: String): CompoundRecipe? {
            return ALL_RECIPES.firstOrNull {
                (it.ingredientA == a && it.ingredientB == b) || (it.ingredientA == b && it.ingredientB == a)
            }
        }
    }
}

val ChemicalElement.category: ElementCategory
    get() = when (id) {
        "Na", "Fe" -> ElementCategory.METAL
        "C" -> ElementCategory.NON_METAL_LIQUID
        else -> ElementCategory.GAS // H, O, Cl, He, N
    }

val CompoundRecipe.category: ElementCategory
    get() = when (formulaId) {
        "NaCl", "Fe2O3" -> ElementCategory.METAL
        "H2O", "HCl" -> ElementCategory.NON_METAL_LIQUID
        else -> ElementCategory.GAS // CO2, NH3, CH4
    }
