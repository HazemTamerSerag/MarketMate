package com.example.marketmate.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.marketmate.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelector(
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.select_your_preferred_language),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    textAlign = TextAlign.Center
                )

                // Arabic Button
                Button(
                    onClick = { onLanguageSelected("ar") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                    ),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Text(
                        text = stringResource(R.string.Arabic),
                        color = Color.Black,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // English Button
                Button(
                    onClick = { onLanguageSelected("en") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0A4C7C)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.english),
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
