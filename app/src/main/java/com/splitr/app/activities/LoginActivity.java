package com.splitr.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.splitr.app.R;
import com.splitr.app.api.RetrofitClient;
import com.splitr.app.models.AuthRequest;
import com.splitr.app.models.AuthResponse;
import com.splitr.app.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private SessionManager session;

    // Login views
    private EditText etLoginUser, etLoginPass;
    private Button   btnLogin;

    // Register views
    private EditText etRegUser, etRegPass;
    private Button   btnRegister;

    // Tab toggle
    private TextView tabSignIn, tabRegister;
    private View     paneLogin, paneRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        session = new SessionManager(this);
        if (session.isLoggedIn()) {
            startDashboard();
            return;
        }

        bindViews();
        setupTabs();
        setupListeners();
    }

    private void bindViews() {
        tabSignIn   = findViewById(R.id.tabSignIn);
        tabRegister = findViewById(R.id.tabRegister);
        paneLogin   = findViewById(R.id.paneLogin);
        paneRegister= findViewById(R.id.paneRegister);

        etLoginUser = findViewById(R.id.etLoginUser);
        etLoginPass = findViewById(R.id.etLoginPass);
        btnLogin    = findViewById(R.id.btnLogin);

        etRegUser   = findViewById(R.id.etRegUser);
        etRegPass   = findViewById(R.id.etRegPass);
        btnRegister = findViewById(R.id.btnRegister);
    }

    private void setupTabs() {
        tabSignIn.setOnClickListener(v -> showPane(true));
        tabRegister.setOnClickListener(v -> showPane(false));
        showPane(true);
    }

    private void showPane(boolean loginVisible) {
        paneLogin.setVisibility(loginVisible ? View.VISIBLE : View.GONE);
        paneRegister.setVisibility(loginVisible ? View.GONE : View.VISIBLE);
        tabSignIn.setSelected(loginVisible);
        tabRegister.setSelected(!loginVisible);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> doLogin());
        btnRegister.setOnClickListener(v -> doRegister());
    }

    // ─── LOGIN ───────────────────────────────────────────────────────────────

    private void doLogin() {
        String user = etLoginUser.getText().toString().trim();
        String pass = etLoginPass.getText().toString();
        if (user.isEmpty() || pass.isEmpty()) { toast("Fill all fields"); return; }

        btnLogin.setEnabled(false);
        btnLogin.setText("Signing in…");

        RetrofitClient.getService().login(new AuthRequest(user, pass))
                .enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> resp) {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Sign In");
                        if (resp.isSuccessful() && resp.body() != null && resp.body().userId != 0) {
                            AuthResponse body = resp.body();
                            session.saveSession(body.userId, body.username);
                            toast("Welcome back, " + body.username + "!");
                            startDashboard();
                        } else {
                            toast("Invalid credentials");
                        }
                    }
                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Sign In");
                        toast("Cannot reach server: " + t.getMessage());
                    }
                });
    }

    // ─── REGISTER ────────────────────────────────────────────────────────────

    private void doRegister() {
        String user = etRegUser.getText().toString().trim();
        String pass = etRegPass.getText().toString();
        if (user.isEmpty() || pass.isEmpty()) { toast("Fill all fields"); return; }

        btnRegister.setEnabled(false);
        btnRegister.setText("Creating…");

        RetrofitClient.getService().register(new AuthRequest(user, pass))
                .enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call, Response<AuthResponse> resp) {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Create Account");
                        if (resp.isSuccessful()) {
                            toast("Account created! Sign in now.");
                            etLoginUser.setText(user);
                            showPane(true);
                        } else {
                            toast("Registration failed. Username may be taken.");
                        }
                    }
                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Create Account");
                        toast("Cannot reach server");
                    }
                });
    }

    private void startDashboard() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
