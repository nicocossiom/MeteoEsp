package com.mssdepas.meteoesp.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mssdepas.meteoesp.data.model.Municipio
import com.mssdepas.meteoesp.data.model.Provincia

@Composable
fun ProvinciaRow(p: Provincia) = ListItem(
    headlineContent = { Text(p.nombre) }
)

@Composable
fun MunicipioRow(m: Municipio, onClick: () -> Unit) = ListItem(
    headlineContent = { Text(m.nombre) },
    modifier = Modifier.clickable { onClick() }
)