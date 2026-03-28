package app.tastile.android.ui.dashboard

import app.tastile.android.data.model.Tile
import app.tastile.android.data.model.TileLifecycle

fun Tile.isStarted(): Boolean = lifecycle == TileLifecycle.STARTED.value

fun Tile.isDone(): Boolean = lifecycle == TileLifecycle.DONE.value
