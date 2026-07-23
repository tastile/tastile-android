package app.tastile.android.ui.mobile.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// m2-allow: experimental-annotation
import androidx.compose.material3.ExperimentalMaterial3Api
// m2-allow: theme-bridge
import androidx.compose.material3.MaterialTheme
// m2-allow: primitive
import androidx.compose.material3.Text
// m2-allow: m3-component
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.core.designsystem.component.NiaButton
import app.tastile.android.core.designsystem.component.NiaOutlinedButton
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.sheets.PanelSheet

/**
 * Subscription panel — mirrors the `SubscriptionSection` from
 * `tastile-web/src/components/account/SubscriptionSection.tsx`. The
 * C7 plan treats the full Free/Pro comparison cards as "passthrough
 * re-export" until the web audit lands, so this sheet surfaces a plan
 * card + Manage Billing / Upgrade to Pro CTA. Full Free/Pro feature
 * matrix lives behind the `account.subscription.*` i18n keys added in
 * C7 and will be wired in once `/api/billing/subscription` is exposed
 * via the v1 daemon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionSheet(
    overlay: OverlayViewModel,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    val current by overlay.current.collectAsStateWithLifecycle()
    if (current !is Overlay.Subscription) return
    val sheetState = rememberModalBottomSheetState()

    PanelSheet(
        sheetState = sheetState,
        onDismiss = { overlay.dismiss() },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.preferences_account_subscription_heading),
                style = MaterialTheme.typography.titleLarge,
            )
            SubscriptionBody()
        }
    }
}

@Composable
private fun SubscriptionBody() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.preferences_account_subscription_guide),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PlanCard()
    }
}

@Composable
private fun PlanCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.account_subscription_current_plan),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.account_subscription_free_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.account_subscription_free_badge),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        NiaOutlinedButton(
            onClick = { /* TODO: open Stripe Customer Portal */ },
            text = { Text(stringResource(R.string.account_subscription_manage)) },
            modifier = Modifier.fillMaxWidth(),
        )
        NiaButton(
            onClick = { /* TODO: open /pricing */ },
            text = { Text(stringResource(R.string.account_subscription_upgrade)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}