package app.tastile.android.ui.mobile

enum class EndpointsCatalog(
    val operationId: String,
    val label: String,
) {
    StartTile("start_tile", "Start tile"),
    CompleteTile("complete_tile", "Complete tile"),
    DeferTile("defer_tile", "Defer tile"),
    DeleteTile("delete_tile", "Delete tile"),
    StartBreak("start_break", "Start break"),
    ExtendTile("extend_tile", "Extend +10"),
    TriggerPrompt("trigger_prompt", "Trigger prompt"),
    CreateTile("create_tile", "Create tile"),
    ListTiles("list_tiles", "List tiles"),
    ListEvents("list_events", "List events"),
    ListIntegrations("list_integrations", "List integrations"),
    ConnectIntegration("connect_integration", "Connect integration"),
    DisconnectIntegration("disconnect_integration", "Disconnect integration"),
    UpdatePreferences("update_preferences", "Update preferences"),
    GetPreferences("get_preferences", "Get preferences"),
    SignOut("sign_out", "Sign out"),
    RefreshToken("refresh_token", "Refresh token"),
    ScheduleTile("schedule_tile", "Schedule tile"),
    UnscheduleTile("unschedule_tile", "Unschedule tile"),
    AcknowledgePrompt("acknowledge_prompt", "Acknowledge prompt"),
    SnoozePrompt("snooze_prompt", "Snooze prompt"),
    ReadRuntimeState("read_runtime_state", "Read runtime state"),
}
