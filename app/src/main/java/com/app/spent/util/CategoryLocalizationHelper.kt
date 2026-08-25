package com.app.spent.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.app.spent.R
import com.app.spent.data.local.entity.CategoryEntity

object CategoryLocalizationHelper {

    fun getLocalizedCategoryName(category: CategoryEntity, context: Context): String {
        return getLocalizedNameById(category.id, category.name, context)
    }

    fun getLocalizedNameById(categoryId: String, defaultName: String, context: Context): String {
        val stringResId = getCategoryStringResId(categoryId)
        return if (stringResId != null) context.getString(stringResId) else defaultName
    }

    @Composable
    fun getLocalizedCategoryName(category: CategoryEntity): String {
        val stringResId = getCategoryStringResId(category.id)
        return if (stringResId != null) stringResource(stringResId) else category.name
    }

    @Composable
    fun getLocalizedNameById(categoryId: String, defaultName: String): String {
        val stringResId = getCategoryStringResId(categoryId)
        return if (stringResId != null) stringResource(stringResId) else defaultName
    }

    private fun getCategoryStringResId(categoryId: String): Int? {
        return when (categoryId.lowercase().trim()) {
            "cat_general" -> R.string.cat_general_name
            "cat_salary" -> R.string.cat_salary_name
            "cat_groceries" -> R.string.cat_groceries_name
            "cat_food" -> R.string.cat_food_name
            "cat_housing" -> R.string.cat_housing_name
            "cat_transport" -> R.string.cat_transport_name
            "cat_utilities" -> R.string.cat_utilities_name
            "cat_entertainment" -> R.string.cat_entertainment_name
            "cat_shopping" -> R.string.cat_shopping_name
            "cat_health" -> R.string.cat_health_name
            "cat_savings" -> R.string.cat_savings_name
            "cat_education" -> R.string.cat_education_name
            "cat_travel" -> R.string.cat_travel_name
            "cat_fitness" -> R.string.cat_fitness_name
            "cat_pets" -> R.string.cat_pets_name
            else -> null
        }
    }
}
