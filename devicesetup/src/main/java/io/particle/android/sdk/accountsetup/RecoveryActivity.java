package io.particle.android.sdk.accountsetup;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import butterknife.ButterKnife;
import butterknife.OnClick;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.R2;
import io.particle.android.sdk.ui.BaseActivity;
import io.particle.android.sdk.utils.ui.ParticleUi;
import io.particle.android.sdk.utils.ui.Ui;

public class RecoveryActivity extends BaseActivity {

    @OnClick(R2.id.action_verify)
    public void onVerify() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recovery);
        ButterKnife.bind(this);

        ParticleUi.enableBrandLogoInverseVisibilityAgainstSoftKeyboard(this);

        Toolbar toolbar = Ui.findView(this, R.id.toolbar);
        toolbar.setNavigationIcon(Ui.getTintedDrawable(this, R.drawable.ic_back, R.color.element_tint_color));
        toolbar.setNavigationOnClickListener(view -> finish());
    }
}
