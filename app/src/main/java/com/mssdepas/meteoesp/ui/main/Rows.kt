package com.mssdepas.meteoesp.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mssdepas.meteoesp.data.model.Municipio
import com.mssdepas.meteoesp.data.model.Provincia

@Composable
fun ProvinciaRow(p: Provincia, onClick: () -> Unit) = ListItem(
    headlineContent = { Text(p.nombre) },
    modifier = Modifier.clickable { onClick() }
)

@Composable
fun MunicipioRow(m: Municipio, onRowClick: () -> Unit, onFavClick: () -> Unit, isFavorite: Boolean) = ListItem(
    headlineContent = { Text(m.nombre) },
    modifier = Modifier.clickable { onRowClick() },
    trailingContent = {
        IconButton(onClick = onFavClick) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isFavorite) "Quitar de favoritos" else "Añadir a favoritos"
            )
        }
    }
)