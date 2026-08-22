package app.tastile.android.ui.mobile

import androidx.annotation.StringRes
import app.tastile.android.R

enum class EndpointsCatalog(
    val operationId: String,
    @StringRes val labelRes: Int,
) {
    StartTile("start_tile", R.string.endpoint_start_tile),
    CompleteTile("complete_tile", R.string.endpoint_complete_tile),
    DeferTile("defer_tile", R.string.endpoint_defer_tile),
    DeleteTile("delete_tile", R.string.endpoint_delete_tile),
    StartBreak("start_break", R.string.endpoint_start_break),
    ExtendTile("extend_tile", R.string.endpoint_extend_tile),
    TriggerPrompt("trigger_prompt", R.string.endpoint_trigger_prompt),
    CreateTile("create_tile", R.string.endpoint_create_tile),
    ListTiles("list_tiles", R.string.endpoint_list_tiles),
    ListEvents("list_events", R.string.endpoint_list_events),
    ListIntegrations("list_integrations", R.string.endpoint_list_integrations),
    ConnectIntegration("connect_integration", R.string.endpoint_connect_integration),
    DisconnectIntegration("disconnect_integration", R.string.endpoint_disconnect_integration),
    UpdatePreferences("update_preferences", R.string.endpoint_update_preferences),
    GetPreferences("get_preferences", R.string.endpoint_get_preferences),
    SignOut("sign_out", R.string.endpoint_sign_out),
    RefreshToken("refresh_token", R.string.endpoint_refresh_token),
    ScheduleTile("schedule_tile", R.string.endpoint_schedule_tile),
    UnscheduleTile("unschedule_tile", R.string.endpoint_unschedule_tile),
    AcknowledgePrompt("acknowledge_prompt", R.string.endpoint_acknowledge_prompt),
    SnoozePrompt("snooze_prompt", R.string.endpoint_snooze_prompt),
    ReadRuntimeState("read_runtime_state", R.string.endpoint_read_runtime_state),
}
