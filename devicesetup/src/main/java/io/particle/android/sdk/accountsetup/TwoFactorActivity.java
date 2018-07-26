package io.particle.android.sdk.accountsetup;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import butterknife.ButterKnife;
import butterknife.OnClick;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.R2;
import io.particle.android.sdk.ui.BaseActivity;
import io.particle.android.sdk.utils.ui.Ui;

public class TwoFactorActivity extends BaseActivity {

    @OnClick(R2.id.recover_auth)
    public void onRecover() {
        startActivity(new Intent(this, RecoveryActivity.class));
    }

    @OnClick(R2.id.action_verify)
    public void onVerify() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two_factor);
        ButterKnife.bind(this);

        Ui.setTextFromHtml(this, R.id.recover_auth, R.string.recover_link_text);

        Toolbar toolbar = Ui.findView(this, R.id.toolbar);
        toolbar.setNavigationIcon(Ui.getTintedDrawable(this, R.drawable.ic_back, R.color.element_tint_color));
        toolbar.setNavigationOnClickListener(view -> finish());
    }
}
