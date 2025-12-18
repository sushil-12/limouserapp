package com.example.limouserapp.ui.booking.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.limouserapp.R
import com.example.limouserapp.data.model.booking.*
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoGreen
import com.example.limouserapp.ui.theme.LimoWhite

// --- Constants & Colors ---
private val HeaderBackground = Color(0xFFF9F9F9) // Subtle Gray for Header
private val DividerColor = Color(0xFFEEEEEE)
private val SearchBackground = Color(0xFFF0F0F0)

@Composable
fun FilterDialog(
    filterData: FilterData?,
    currentSelection: FilterSelectionState,
    onDismiss: () -> Unit,
    onApply: (FilterSelectionState) -> Unit,
    onClear: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var localSelection by remember { mutableStateOf(currentSelection) }

    // State management for expanding categories
    var expandedCategories by remember { mutableStateOf<Set<FilterCategory>>(emptySet()) }
    var expandedSubCategories by remember { mutableStateOf<Set<FilterCategory>>(emptySet()) }

    // Pagination state (page counts per category)
    var paginationState by remember { mutableStateOf<Map<FilterCategory, Int>>(emptyMap()) }
    val itemsPerPage = 12

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(16.dp)),
                color = LimoWhite,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // 1. Header (Updated with Gray Background)
                    DialogHeader(onDismiss = onDismiss)

                    // 2. Content
                    Box(modifier = Modifier.weight(1f)) {
                        if (isLoading) {
                            LoadingState()
                        } else if (filterData == null) {
                            EmptyState(errorMessage ?: "No data available")
                        } else {
                            FilterContentList(
                                filterData = filterData,
                                selection = localSelection,
                                expandedCategories = expandedCategories,
                                expandedSubCategories = expandedSubCategories,
                                paginationState = paginationState,
                                itemsPerPage = itemsPerPage,
                                onSelectionChange = { localSelection = it },
                                onToggleCategory = { category ->
                                    expandedCategories = if (category in expandedCategories) expandedCategories - category else expandedCategories + category
                                },
                                onToggleSubCategory = { subCategory ->
                                    expandedSubCategories = if (subCategory in expandedSubCategories) expandedSubCategories - subCategory else expandedSubCategories + subCategory
                                },
                                onLoadMore = { category ->
                                    paginationState = paginationState + (category to (paginationState[category] ?: 0) + 1)
                                }
                            )
                        }
                    }

                    // 3. Footer (Smaller Buttons)
                    DialogFooter(
                        onClear = {
                            localSelection = FilterSelectionState()
                            onClear()
                        },
                        onApply = { onApply(localSelection) }
                    )
                }
            }
        }
    }
}

// --- 1. Header Component ---

@Composable
private fun DialogHeader(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderBackground) // Slight gray background
    ) {
        // Drag Handle Visual
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.LightGray.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
            )
        }

        // Title and Close
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f)) // Spacer to push title center (approx)

            Text(
                text = "Filters",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LimoBlack
                ),
                modifier = Modifier.padding(start = 24.dp) // Compensate for close icon to center visually
            )

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = LimoBlack,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }
        HorizontalDivider(color = DividerColor, thickness = 1.dp)
    }
}

// --- 2. List Content Logic ---

@Composable
private fun FilterContentList(
    filterData: FilterData,
    selection: FilterSelectionState,
    expandedCategories: Set<FilterCategory>,
    expandedSubCategories: Set<FilterCategory>,
    paginationState: Map<FilterCategory, Int>,
    itemsPerPage: Int,
    onSelectionChange: (FilterSelectionState) -> Unit,
    onToggleCategory: (FilterCategory) -> Unit,
    onToggleSubCategory: (FilterCategory) -> Unit,
    onLoadMore: (FilterCategory) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        itemsIndexed(FilterCategory.mainCategories) { index, category ->
            val isExpanded = category in expandedCategories

            // Render logic based on category type
            when (category) {
                FilterCategory.DRIVER_PREFERENCES -> {
                    NestedAccordionSection(
                        mainCategory = category,
                        subCategories = FilterCategory.driverPreferencesSubCategories,
                        filterData = filterData,
                        selection = selection,
                        isMainExpanded = isExpanded,
                        expandedSubCategories = expandedSubCategories,
                        paginationState = paginationState,
                        onToggleMain = { onToggleCategory(category) },
                        onToggleSub = onToggleSubCategory,
                        onLoadMore = onLoadMore,
                        onSelectionChange = onSelectionChange
                    )
                }
                FilterCategory.MAKE_MODEL -> {
                    NestedAccordionSection(
                        mainCategory = category,
                        subCategories = FilterCategory.makeModelSubCategories,
                        filterData = filterData,
                        selection = selection,
                        isMainExpanded = isExpanded,
                        expandedSubCategories = expandedSubCategories,
                        paginationState = paginationState,
                        onToggleMain = { onToggleCategory(category) },
                        onToggleSub = onToggleSubCategory,
                        onLoadMore = onLoadMore,
                        onSelectionChange = onSelectionChange
                    )
                }
                else -> {
                    // Standard Single Level Accordion
                    StandardAccordionSection(
                        category = category,
                        filterData = filterData,
                        selection = selection,
                        isExpanded = isExpanded,
                        currentPage = paginationState[category] ?: 0,
                        itemsPerPage = itemsPerPage,
                        onToggle = { onToggleCategory(category) },
                        onLoadMore = { onLoadMore(category) },
                        onSelectionChange = onSelectionChange
                    )
                }
            }

            if (index < FilterCategory.mainCategories.size - 1) {
                HorizontalDivider(color = DividerColor, thickness = 1.dp)
            }
        }
    }
}

// --- 3. Accordion Components ---

@Composable
private fun StandardAccordionSection(
    category: FilterCategory,
    filterData: FilterData,
    selection: FilterSelectionState,
    isExpanded: Boolean,
    currentPage: Int,
    itemsPerPage: Int,
    onToggle: () -> Unit,
    onLoadMore: () -> Unit,
    onSelectionChange: (FilterSelectionState) -> Unit
) {
    var searchText by remember { mutableStateOf("") }

    Column {
        AccordionHeader(title = category.displayName, isExpanded = isExpanded, onClick = onToggle)

        if (isExpanded) {
            Column(modifier = Modifier.background(LimoWhite)) {
                // Search
                CompactSearchBar(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                // Items
                val allItems = getItemsForCategory(category, filterData)
                val filteredItems = if (searchText.isEmpty()) {
                    val limit = minOf((currentPage + 1) * itemsPerPage, allItems.size)
                    allItems.take(limit)
                } else {
                    allItems.filter { it.name.contains(searchText, ignoreCase = true) }
                }

                filteredItems.forEach { item ->
                    FilterItemRow(
                        item = item,
                        category = category,
                        selection = selection,
                        onSelectionChange = onSelectionChange
                    )
                }

                if (searchText.isEmpty() && (currentPage + 1) * itemsPerPage < allItems.size) {
                    ShowMoreButton(onClick = onLoadMore)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun NestedAccordionSection(
    mainCategory: FilterCategory,
    subCategories: List<FilterCategory>,
    filterData: FilterData,
    selection: FilterSelectionState,
    isMainExpanded: Boolean,
    expandedSubCategories: Set<FilterCategory>,
    paginationState: Map<FilterCategory, Int>,
    onToggleMain: () -> Unit,
    onToggleSub: (FilterCategory) -> Unit,
    onLoadMore: (FilterCategory) -> Unit,
    onSelectionChange: (FilterSelectionState) -> Unit
) {
    Column {
        AccordionHeader(title = mainCategory.displayName, isExpanded = isMainExpanded, onClick = onToggleMain)

        if (isMainExpanded) {
            Column {
                subCategories.forEachIndexed { index, subCat ->
                    val isSubExpanded = subCat in expandedSubCategories
                    var searchText by remember { mutableStateOf("") }
                    val currentPage = paginationState[subCat] ?: 0

                    // Sub-Accordion Header with Indentation
                    AccordionHeader(
                        title = subCat.displayName,
                        isExpanded = isSubExpanded,
                        onClick = { onToggleSub(subCat) },
                        modifier = Modifier.padding(start = 16.dp), // Indent sub-categories
                        isSubHeader = true
                    )

                    if (isSubExpanded) {
                        Column {
                            CompactSearchBar(
                                value = searchText,
                                onValueChange = { searchText = it },
                                modifier = Modifier.padding(start = 36.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)
                            )

                            val allItems = getItemsForCategory(subCat, filterData)
                            val filteredItems = if (searchText.isEmpty()) {
                                val limit = minOf((currentPage + 1) * 12, allItems.size)
                                allItems.take(limit)
                            } else {
                                allItems.filter { it.name.contains(searchText, ignoreCase = true) }
                            }

                            filteredItems.forEach { item ->
                                FilterItemRow(
                                    item = item,
                                    category = subCat,
                                    selection = selection,
                                    onSelectionChange = onSelectionChange,
                                    modifier = Modifier.padding(start = 16.dp) // Indent items further
                                )
                            }

                            if (searchText.isEmpty() && (currentPage + 1) * 12 < allItems.size) {
                                ShowMoreButton(
                                    onClick = { onLoadMore(subCat) },
                                    modifier = Modifier.padding(start = 36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (index < subCategories.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 20.dp),
                            color = DividerColor
                        )
                    }
                }
            }
        }
    }
}

// --- 4. Reusable UI Components ---

@Composable
private fun AccordionHeader(
    title: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSubHeader: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(modifier)
            .padding(horizontal = 20.dp, vertical = if (isSubHeader) 14.dp else 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = TextStyle(
                fontSize = if (isSubHeader) 15.sp else 16.sp,
                fontWeight = if (isSubHeader) FontWeight.SemiBold else FontWeight.Bold,
                color = LimoBlack
            )
        )
        Icon(
            painter = painterResource(id = R.drawable.cheveron_down),
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = Color.Gray,
            modifier = Modifier
                .size(12.dp)
                .rotate(if (isExpanded) 180f else 0f)
        )
    }
}

@Composable
private fun CompactSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 14.sp, color = LimoBlack),
        cursorBrush = SolidColor(LimoGreen),
        decorationBox = { innerTextField ->
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(36.dp) // Fixed small height
                    .background(SearchBackground, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text("Search...", style = TextStyle(fontSize = 14.sp, color = Color.Gray))
                    }
                    innerTextField()
                }
            }
        }
    )
}

@Composable
private fun FilterItemRow(
    item: FilterItem,
    category: FilterCategory,
    selection: FilterSelectionState,
    onSelectionChange: (FilterSelectionState) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = isItemSelected(category, selection, item)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val updated = updateSelection(category, selection, item, isSelected)
                onSelectionChange(updated)
            }
            .then(modifier)
            .padding(horizontal = 20.dp, vertical = 10.dp), // Compact vertical padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = if (isSelected) LimoGreen else Color.LightGray,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.name,
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = LimoBlack
            )
        )
    }
}

@Composable
private fun ShowMoreButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Show More (+12)",
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LimoGreen)
        )
    }
}

// --- 5. Footer Component ---

@Composable
private fun DialogFooter(
    onClear: () -> Unit,
    onApply: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(LimoWhite)) {
        HorizontalDivider(color = DividerColor, thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onClear,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp), // Smaller height
                colors = ButtonDefaults.buttonColors(containerColor = LimoBlack),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp) // Reset padding to center text in smaller button
            ) {
                Text("Clear", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
            }

            Button(
                onClick = onApply,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LimoGreen),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Apply Filter", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

// --- 6. Loading & Empty States ---

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = LimoGreen)
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = Color.Gray, style = TextStyle(fontSize = 14.sp))
    }
}

// --- 7. Logic Helpers (Logic preserved/cleaned) ---

private fun isItemSelected(cat: FilterCategory, state: FilterSelectionState, item: FilterItem): Boolean {
    return when (cat) {
        FilterCategory.VEHICLE_TYPE -> state.selectedVehicleTypes.contains(item.id)
        FilterCategory.AMENITIES -> state.selectedAmenities.contains(item.id)
        FilterCategory.EXTRA_AMENITIES -> state.selectedSpecialAmenities.contains(item.id)
        FilterCategory.INTERIORS -> state.selectedInteriors.contains(item.id)
        FilterCategory.MAKE -> state.selectedMakes.contains(item.id)
        FilterCategory.MODEL -> state.selectedModels.contains(item.id)
        FilterCategory.YEARS -> state.selectedYears.contains(item.id)
        FilterCategory.COLORS -> state.selectedColors.contains(item.id)
        FilterCategory.DRIVER_DRESSES -> state.selectedDriverDresses.contains(item.id)
        FilterCategory.DRIVER_LANGUAGES -> state.selectedDriverLanguages.contains(item.id)
        FilterCategory.DRIVER_GENDER -> state.selectedDriverGenders.contains(item.slug)
        FilterCategory.VEHICLE_SERVICE_AREA -> state.selectedVehicleServiceAreas.contains(item.slug)
        FilterCategory.OPERATOR_PREFERENCES -> state.selectedAffiliatePreferences.contains(item.slug)
        else -> false
    }
}

private fun updateSelection(
    category: FilterCategory,
    state: FilterSelectionState,
    item: FilterItem,
    isSelected: Boolean
): FilterSelectionState {
    // Helper to add/remove
    fun <T> Set<T>.toggle(element: T): Set<T> = if (isSelected) this - element else this + element

    return when (category) {
        FilterCategory.VEHICLE_TYPE -> state.copy(selectedVehicleTypes = state.selectedVehicleTypes.toggle(item.id))
        FilterCategory.AMENITIES -> state.copy(selectedAmenities = state.selectedAmenities.toggle(item.id))
        FilterCategory.EXTRA_AMENITIES -> state.copy(selectedSpecialAmenities = state.selectedSpecialAmenities.toggle(item.id))
        FilterCategory.INTERIORS -> state.copy(selectedInteriors = state.selectedInteriors.toggle(item.id))
        FilterCategory.MAKE -> state.copy(selectedMakes = state.selectedMakes.toggle(item.id))
        FilterCategory.MODEL -> state.copy(selectedModels = state.selectedModels.toggle(item.id))
        FilterCategory.YEARS -> state.copy(selectedYears = state.selectedYears.toggle(item.id))
        FilterCategory.COLORS -> state.copy(selectedColors = state.selectedColors.toggle(item.id))
        FilterCategory.DRIVER_DRESSES -> state.copy(selectedDriverDresses = state.selectedDriverDresses.toggle(item.id))
        FilterCategory.DRIVER_LANGUAGES -> state.copy(selectedDriverLanguages = state.selectedDriverLanguages.toggle(item.id))
        FilterCategory.DRIVER_GENDER -> state.copy(selectedDriverGenders = state.selectedDriverGenders.toggle(item.slug))
        FilterCategory.VEHICLE_SERVICE_AREA -> state.copy(selectedVehicleServiceAreas = state.selectedVehicleServiceAreas.toggle(item.slug))
        FilterCategory.OPERATOR_PREFERENCES -> state.copy(selectedAffiliatePreferences = state.selectedAffiliatePreferences.toggle(item.slug))
        else -> state
    }
}

// Keep your existing getItemsForCategory and FilterItem interface logic exactly as is.
// Including interface here to ensure compilation.
private interface FilterItem {
    val id: Int
    val name: String
    val slug: String
}

private fun getItemsForCategory(category: FilterCategory, filterData: FilterData): List<FilterItem> {
    return when (category) {
        FilterCategory.VEHICLE_TYPE -> filterData.vehicleType.sortedBy { it.sortOrder ?: 0 }.map { object : FilterItem { override val id = it.id; override val name = it.name; override val slug = "" } }
        FilterCategory.AMENITIES -> filterData.amenities.map { object : FilterItem { override val id = it.id; override val name = it.name; override val slug = "" } }
        FilterCategory.EXTRA_AMENITIES -> filterData.specialAmenities.map { object : FilterItem { override val id = it.id; override val name = it.name; override val slug = "" } }
        FilterCategory.INTERIORS -> filterData.interiors.map { object : FilterItem { override val id = it.id; override val name = it.name; override val slug = "" } }
        FilterCategory.MAKE -> filterData.make.map { object : FilterItem { override val id = it.id; override val name = it.name; override val slug = "" } }
        FilterCategory.MODEL -> filterData.model.map { object : FilterItem { override val id = it.id; override val name = it.name; override val slug = "" } }
        FilterCategory.YEARS -> filterData.years.sortedByDescending { it.name }.map { object : FilterItem { override val id = it.id; override val name = it.name; override val slug = "" } }
        FilterCategory.COLORS -> filterData.colors.map { object : FilterItem { override val id = it.id; override val name = it.name; override val slug = "" } }
        FilterCategory.DRIVER_DRESSES -> filterData.driverDresses.map { object : FilterItem { override val id = it.id; override val name = it.name; override val slug = "" } }
        FilterCategory.DRIVER_LANGUAGES -> filterData.driverLanguages.map { object : FilterItem { override val id = it.id; override val name = it.name; override val slug = "" } }
        FilterCategory.DRIVER_GENDER -> filterData.driverGender.map { object : FilterItem { override val id = 0; override val name = it.name; override val slug = it.slug } }
        FilterCategory.VEHICLE_SERVICE_AREA -> filterData.vehicleServiceArea.map { object : FilterItem { override val id = 0; override val name = it.name; override val slug = it.slug } }
        FilterCategory.OPERATOR_PREFERENCES -> filterData.affiliatePreferences.map { object : FilterItem { override val id = 0; override val name = it.name; override val slug = it.slug } }
        else -> emptyList()
    }
}

// Keep Enum logic
enum class FilterCategory(val displayName: String) {
    VEHICLE_TYPE("Vehicle Type Preferences"),
    DRIVER_PREFERENCES("Driver Preferences"),
    EXTRA_AMENITIES("Extra $ Amenities"),
    YEARS("Years"),
    COLORS("Colors"),
    INTERIORS("Interiors"),
    AMENITIES("Amenities"),
    MAKE_MODEL("Make Model"),
    VEHICLE_SERVICE_AREA("Vehicle Service Area Type"),
    OPERATOR_PREFERENCES("Operator Preferences"),
    // Subcategories
    DRIVER_DRESSES("Dresses"),
    DRIVER_BACKGROUND("Background"),
    DRIVER_LANGUAGES("Languages"),
    DRIVER_GENDER("Gender"),
    MAKE("Make"),
    MODEL("Model");

    companion object {
        val mainCategories = listOf(VEHICLE_TYPE, DRIVER_PREFERENCES, EXTRA_AMENITIES, YEARS, COLORS, INTERIORS, AMENITIES, MAKE_MODEL, VEHICLE_SERVICE_AREA, OPERATOR_PREFERENCES)
        val driverPreferencesSubCategories = listOf(DRIVER_DRESSES, DRIVER_LANGUAGES, DRIVER_GENDER)
        val makeModelSubCategories = listOf(MAKE, MODEL)
    }
}