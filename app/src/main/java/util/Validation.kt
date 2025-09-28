package com.example.weight_trackerapp.util

// Original validator: assumes weight is already in kilograms
fun validateWeightKg(value: Double?): String? {
    value ?: return "Enter a number"
    if (value.isNaN() || value.isInfinite()) return "Enter a valid number"
    if (value < 30.0) return "Too low to be realistic"
    if (value > 350.0) return "Too high to be realistic"
    return null
}

/**
 * Unit-aware validator.
 * If user is in pounds mode, we check against a different sensible range.
 */
fun validateWeight(value: Double?, unit: UnitSystem): String? {
    value ?: return "Enter a number"
    if (value.isNaN() || value.isInfinite()) return "Enter a valid number"

    return when (unit) {
        UnitSystem.KG -> {
            if (value < 30.0) "Too low to be realistic"
            else if (value > 350.0) "Too high to be realistic"
            else null
        }
        UnitSystem.LB -> {
            if (value < 66.0) "Too low to be realistic"
            else if (value > 770.0) "Too high to be realistic"
            else null
        }
    }
}