package com.studentassoc.financialtracker.View;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.studentassoc.financialtracker.Security.SecurityManager;

public class BaseActivity extends AppCompatActivity {

    private SecurityManager securityManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        securityManager = SecurityManager.getInstance(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (securityManager.shouldLock()) {
            redirectToLockScreen();
        } else {
            securityManager.updateActivity();
        }
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        securityManager.updateActivity();
    }

   private void redirectToLockScreen() {
       Intent intent = new Intent(this, LockScreenActivity.class);

       intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
       startActivity(intent);
       finish();
   }
}
