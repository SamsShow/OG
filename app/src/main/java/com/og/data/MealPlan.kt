package com.og.data

data class FoodItem(
    val name: String,
    val qty: String,
    val proteinG: Double,
    val kcal: Double,
)

enum class MealKind {
    /** Counts toward the daily plan and diet-completion score. */
    MAIN,

    /** Counts toward completion but carries no protein (creatine). */
    SUPPLEMENT,

    /** Only surfaces when the day is short on protein. Never counts against completion. */
    ADAPTIVE,
}

data class Meal(
    val id: String,
    val slot: String,
    val time: String,
    val kind: MealKind,
    val items: List<FoodItem>,
    val note: String? = null,
    /** Cheaper or easier substitutions, since the plan has to survive a tight week. */
    val swaps: List<String> = emptyList(),
) {
    val proteinG: Double get() = items.sumOf { it.proteinG }
    val kcal: Double get() = items.sumOf { it.kcal }
}

object MealPlan {

    val morning = Meal(
        id = "m_morning",
        slot = "Morning",
        time = "07:30 – 09:00",
        kind = MealKind.MAIN,
        items = listOf(
            FoodItem("Oats (dry)", "80 g", 10.6, 304.0),
            FoodItem("Milk", "250 ml", 8.0, 153.0),
            FoodItem("Banana", "1 medium", 1.3, 105.0),
            FoodItem("Peanut butter", "15 g", 3.8, 88.0),
        ),
        note = "Cook the oats in the milk rather than water — same cost, all the protein counts.",
        swaps = listOf(
            "Drop the peanut butter to cut cost — you lose 4 g protein and 88 kcal",
            "No milk? Water + half a whey scoop is cheaper and adds 4 g protein",
        ),
    )

    val afternoon = Meal(
        id = "m_afternoon",
        slot = "Afternoon",
        time = "13:00 – 14:30",
        kind = MealKind.MAIN,
        items = listOf(
            FoodItem("Whole eggs", "3", 18.9, 216.0),
            FoodItem("Whole-wheat bread", "2 slices", 6.0, 160.0),
        ),
        note = "Whole eggs, not just whites — the yolk is where most of the micronutrients are.",
        swaps = listOf(
            "Add 2 egg whites for +7 g protein at almost no cost",
            "Bread → 2 chapatis if that is what is already in the house",
        ),
    )

    val evening = Meal(
        id = "m_evening",
        slot = "Evening",
        time = "17:30 – 19:00",
        kind = MealKind.MAIN,
        items = listOf(
            FoodItem("Whey", "1 scoop (30 g)", 24.0, 120.0),
            FoodItem("Curd / plain yoghurt", "150 g", 5.3, 90.0),
        ),
        note = "Take this after training on gym days.",
        swaps = listOf(
            "Whey → 50 g soya chunks (26 g protein, the cheapest protein per gram there is)",
            "Curd → 100 g paneer for +18 g protein when the budget allows",
        ),
    )

    val night = Meal(
        id = "m_night",
        slot = "Night",
        time = "20:30 – 22:00",
        kind = MealKind.MAIN,
        items = listOf(
            FoodItem("Chicken breast (raw weight)", "250 g", 56.3, 413.0),
            FoodItem("Rice (cooked)", "100 g", 2.7, 130.0),
            FoodItem("Cooking oil", "5 g", 0.0, 44.0),
        ),
        note = "250 g raw cooks down to roughly 180 g. Weigh it raw so the protein number stays honest.",
        swaps = listOf(
            "Chicken → 200 g soya chunks on a tight week (still over 50 g protein)",
            "Rice → 2 chapatis, or drop it entirely on rest days to cut ~130 kcal",
        ),
    )

    val creatine = Meal(
        id = "m_creatine",
        slot = "Creatine",
        time = "Any time",
        kind = MealKind.SUPPLEMENT,
        items = listOf(FoodItem("Creatine monohydrate", "5 g", 0.0, 0.0)),
        note = "Timing does not matter. Taking it every day does.",
    )

    val wheyTopUp = Meal(
        id = "m_whey2",
        slot = "Top-up",
        time = "When short",
        kind = MealKind.ADAPTIVE,
        items = listOf(FoodItem("Whey", "1 scoop (30 g)", 24.0, 120.0)),
        note = "Your optional second scoop. Only worth taking on days the food came up short.",
    )

    /** Meals that make up the daily plan, in the order they are eaten. */
    val scheduled = listOf(morning, afternoon, evening, night, creatine)

    val all = scheduled + wheyTopUp

    private val byId = all.associateBy { it.id }

    operator fun get(id: String): Meal? = byId[id]

    /** Protein the plan delivers when every scheduled meal is eaten in full: 136.9 g. */
    val plannedProteinG: Double = scheduled.sumOf { it.proteinG }

    /** Calories for the full scheduled plan: ~1823 kcal. */
    val plannedKcal: Double = scheduled.sumOf { it.kcal }

    /** Meals that count toward diet completion — the adaptive scoop is excluded by design. */
    val completionCount: Int = scheduled.size
}
