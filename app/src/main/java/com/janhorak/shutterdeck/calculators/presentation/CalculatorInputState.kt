package com.janhorak.shutterdeck.calculators.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable

/** Convenience for a rotation-surviving text input state used by calculator screens. */
@Composable
fun rememberInput(initial: String): MutableState<String> =
    rememberSaveable { mutableStateOf(initial) }
