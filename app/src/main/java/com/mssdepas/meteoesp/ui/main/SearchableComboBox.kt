package com.mssdepas.meteoesp.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SearchableComboBox(
    label: String,
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    onQueryChanged: (String) -> Unit,
    itemText: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClear: (() -> Unit)? = null,
    colors: TextFieldColors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
    searchTextFieldColors: TextFieldColors = colors,
    dropdownMenuModifier: Modifier = Modifier,
    dropdownMenuItemColors: MenuItemColors = MenuDefaults.itemColors()
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Show selected item text or placeholder
    val displayText = selectedItem?.let { itemText(it) } ?: ""

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded && enabled,
            onExpandedChange = {
                if (enabled) {
                    expanded = !expanded
                    if (expanded) {
                        searchQuery = ""
                        onQueryChanged("")
                    }
                }
            }
        ) {
            OutlinedTextField(
                value = displayText,
                onValueChange = { }, // Read-only field
                readOnly = true,
                label = { Text(label) },
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = {
                    Row {
                        if (selectedItem != null && onClear != null) {
                            IconButton(
                                onClick = {
                                    onClear()
                                    expanded = false
                                }
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                },
                colors = colors
            )

            if (expanded) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .then(dropdownMenuModifier)
                ) {
                    // Search field at the top of the dropdown
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { query ->
                            searchQuery = query
                            onQueryChanged(query)
                        },
                        label = { Text("Buscar...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true,
                        colors = searchTextFieldColors
                    )

                    // Divider
                    HorizontalDivider()

                    // Show all filtered items without artificial limit
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(itemText(item)) },
                            onClick = {
                                onItemSelected(item)
                                expanded = false
                                searchQuery = ""
                                onQueryChanged("")
                            },
                            colors = dropdownMenuItemColors
                        )
                    }

                    // Show message if no items found
                    if (items.isEmpty() && searchQuery.isNotEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "No se encontraron resultados",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = { },
                            enabled = false,
                            colors = dropdownMenuItemColors
                        )
                    }
                }
            }
        }
    }
}