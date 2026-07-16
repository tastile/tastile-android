package app.tastile.android.ui.mobile.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tastile.android.R
import app.tastile.android.ui.designsystem.AppTheme
import app.tastile.android.ui.mobile.Overlay
import app.tastile.android.ui.mobile.OverlayViewModel
import app.tastile.android.ui.mobile.designsystem.AppPrimaryButton
import app.tastile.android.ui.mobile.designsystem.AppSecondaryButton
import app.tastile.android.ui.mobile.designsystem.MobileSpacing
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    PanelSheet(
        title = stringResource(R.string.preferences_account_subscription_heading),
        sheetState = sheetState,
        onDismiss = { overlay.dismiss() },
    ) {
        SubscriptionBody()
    }
}

@Composable
private fun SubscriptionBody() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = MobileSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MobileSpacing.sm),
    ) {
        Text(
            text = stringResource(R.string.preferences_account_subscription_guide),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.onSurfaceVariant,
        )
        PlanCard()
    }
}

@Composable
private fun PlanCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MobileSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(MobileSpacing.xs),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.account_subscription_current_plan),
                style = AppTheme.typography.titleSmall,
                color = AppTheme.colors.onSurface,
            )
            Text(
                text = stringResource(R.string.account_subscription_free_description),
                style = AppTheme.typography.bodySmall,
                color = AppTheme.colors.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.account_subscription_free_badge),
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.onSurfaceVariant,
            )
        }
        AppSecondaryButton(
            text = stringResource(R.string.account_subscription_manage),
            onClick = { /* TODO: open Stripe Customer Portal */ },
            modifier = Modifier.fillMaxWidth(),
        )
        AppPrimaryButton(
            text = stringResource(R.string.account_subscription_upgrade),
            onClick = { /* TODO: open /pricing */ },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}