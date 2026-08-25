/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */

package app.tastile.android.core.designsystem.component

/**
 * Action set exposed by a dashboard card. Maps to the three branches of the
 * legacy `CardPrimaryActions` `when (status)` block.
 */
sealed interface TastileCardActions {
    data object Ready : TastileCardActions
    data object Started : TastileCardActions
    data object DoneOrArchived : TastileCardActions
}