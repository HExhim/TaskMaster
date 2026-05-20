package com.twa.taskmaster.ui.main_activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.preference.PreferenceManager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.twa.taskmaster.R;

import java.lang.ref.WeakReference;
import java.util.regex.Pattern;

public class LoginActivity extends BaseActivity {

    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "LoginActivity";
    private static final long VERIFICATION_COOLDOWN = 60000; // 1 minute
    private static final long RESET_COOLDOWN = 60000; // 1 minute
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    // Firebase instances
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    // Views
    private TextInputEditText etEmail, etPassword,etRPassword;
    private TextInputLayout retypePassword;
    private MaterialButton btnLogin;
    private TextView tvToggleAuthMode, tvForgotPassword, tvAuthTitle;
    private TextView tvRuleLength, tvRuleLower, tvRuleUpper, tvRuleDigit, tvRuleSpecial,tvGoBack;
    private LinearLayout formContainer, passwordChecklist;
    private MaterialCardView emailVerificationPopup;
    private TextView tvEmailToVerify, tvCooldown;
    private TextView tvGuestMode;
    private Button btnVerifyNow, btnCheckAgain;
    private Button btnGoogleSignIn;

    // State variables
    private boolean isLoginMode = true;
    private long lastVerificationRequestTime = 0;
    private long lastResetRequestTime = 0;
    private final CooldownHandler cooldownHandler = new CooldownHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_fragment);

        initializeViews();
        setupFirebase();
        setupGoogleSignIn();
        setupListeners();
        setupPasswordMatchValidation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cooldownHandler.clear();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etRPassword = findViewById(R.id.etRPassword);
        retypePassword = findViewById(R.id.passwordInputLayout1);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        tvToggleAuthMode = findViewById(R.id.tvToggleAuthMode);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvAuthTitle = findViewById(R.id.tvAuthTitle);
        formContainer = findViewById(R.id.formContainer);
        tvGoBack = findViewById(R.id.tvGoBack);
        tvGuestMode = findViewById(R.id.tvGuestMode);

        // Password validation views
        tvRuleLength = findViewById(R.id.tvRuleLength);
        tvRuleLower = findViewById(R.id.tvRuleLower);
        tvRuleUpper = findViewById(R.id.tvRuleUpper);
        tvRuleDigit = findViewById(R.id.tvRuleDigit);
        tvRuleSpecial = findViewById(R.id.tvRuleSpecial);
        passwordChecklist = findViewById(R.id.passwordChecklist);

        // Email verification views
        emailVerificationPopup = findViewById(R.id.emailVerificationPopup);
        tvEmailToVerify = findViewById(R.id.tvEmailToVerify);
        btnVerifyNow = findViewById(R.id.btnVerifyNow);
        btnCheckAgain = findViewById(R.id.btnCheckAgain);
        tvCooldown = findViewById(R.id.tvCooldown);
    }

    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupListeners() {
        // Password focus and validation
        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !isLoginMode) {
                passwordChecklist.setVisibility(View.VISIBLE);
            } else {
                passwordChecklist.setVisibility(View.GONE);
            }
        });

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordValidationUI(s.toString());
            }
        });


        // Auth mode toggle
        tvToggleAuthMode.setOnClickListener(v -> toggleAuthMode());

        // Forgot password
        tvForgotPassword.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (validateEmail(email)) {
                sendPasswordResetEmail(email);
            }
        });

        // Login/Signup button
        btnLogin.setOnClickListener(v -> {
            Log.d(TAG, "setupListeners: Login button Clicked");
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (password.isEmpty()) {
                etPassword.setError("Please enter a password");
                return;
            }

            if (isLoginMode) {
                loginUser(email, password);
            } else {
                signUpUser(email, password);

            }
        });

        // Google Sign-In
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        // Email verification buttons
        btnVerifyNow.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                sendVerificationEmail(user);
            }
        });

        btnCheckAgain.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            verifyEmail(user);
        });
        tvGoBack.setOnClickListener(v -> finish());
        
        // Guest Mode
        tvGuestMode.setOnClickListener(v -> {
            // Mark as guest
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putBoolean("is_guest_mode", true).apply();
            
            signInAnonymously();
        });
    }
    
    private void signInAnonymously() {
        showProgress(true);
        mAuth.signInAnonymously()
            .addOnCompleteListener(this, task -> {
                showProgress(false);
                if (task.isSuccessful()) {
                    // Sign in success
                    Log.d(TAG, "signInAnonymously:success");
                    proceedToMainApp();
                } else {
                    // If sign in fails, we proceed anyway for offline support
                    Log.w(TAG, "signInAnonymously:failure", task.getException());
                    Toast.makeText(this, "Offline Mode Enabled",
                            Toast.LENGTH_SHORT).show();
                    proceedToMainApp();
                }
            });
    }

    private void setupPasswordMatchValidation() {
        TextWatcher passwordMatchWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isLoginMode) { // Only validate in signup mode
                    String password = etPassword.getText().toString();
                    String rePassword = etRPassword.getText().toString();

                    boolean isMatching = password.equals(rePassword);
                    btnLogin.setEnabled(isMatching);

                    retypePassword.setError(isMatching || rePassword.isEmpty() ? null : "Passwords do not match");
                } else {
                    btnLogin.setEnabled(true); // Always enable in login mode
                    retypePassword.setError(null);
                }
            }
        };

        etPassword.addTextChangedListener(passwordMatchWatcher);
        etRPassword.addTextChangedListener(passwordMatchWatcher);
    }

    private void toggleAuthMode() {
        isLoginMode = !isLoginMode;

        if (isLoginMode) {
            tvAuthTitle.setText(R.string.login_title);
            btnLogin.setText(R.string.login);
            tvToggleAuthMode.setText("No Account, Create New");
            tvForgotPassword.setVisibility(View.VISIBLE);
            passwordChecklist.setVisibility(View.GONE);
            retypePassword.setVisibility(View.GONE);
            btnLogin.setEnabled(true); // Ensure button isn't disabled by old validation state
        } else {
            tvAuthTitle.setText(R.string.sign_up_title);
            btnLogin.setText(R.string.sign_up);
            tvToggleAuthMode.setText(R.string.have_account_login);
            tvForgotPassword.setVisibility(View.GONE);
            retypePassword.setVisibility(View.VISIBLE);
        }

        // Reset fields
        etEmail.setText("");
        etPassword.setText("");
        etRPassword.setText("");
        retypePassword.setError(null);
        etEmail.requestFocus();
    }

    private boolean validateEmail(String email) {
        if (email.isEmpty()) {
            etEmail.setError("Please enter your email");
            return false;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            return false;
        }

        return true;
    }

    private boolean validatePassword(String password) {
        boolean isValid = password.length() >= 8 &&
                password.matches(".*[a-z].*") &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*\\d.*") &&
                password.matches(".*[!@#$%^&*()\\-+=].*");

        if (!isValid) {
            etPassword.setError("Password doesn't meet requirements");
            passwordChecklist.setVisibility(View.VISIBLE);
        }

        return isValid;
    }

    private void updatePasswordValidationUI(String password) {
        setRule(tvRuleLength, password.length() >= 8, "Minimum 8 characters");
        setRule(tvRuleLower, password.matches(".*[a-z].*"), "At least one lowercase letter");
        setRule(tvRuleUpper, password.matches(".*[A-Z].*"), "At least one uppercase letter");
        setRule(tvRuleDigit, password.matches(".*\\d.*"), "At least one digit");
        setRule(tvRuleSpecial, password.matches(".*[!@#$%^&*()\\-+=].*"), "At least one special character");
    }

    private void setRule(TextView view, boolean valid, String ruleText) {
        view.setText(valid ? "✅ " + ruleText : "❌ " + ruleText);
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                // proceed to main screen
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            } else {
                                showEmailVerificationPopup(user);
                            }
                        }
                    } else handleLoginError(task.getException());

                });
    }


    private void signUpUser(String email, String password) {
        showProgress(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showProgress(false);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            sendVerificationEmail(user);
                        }
                    } else {
                        handleSignUpError(task.getException());
                    }
                });
    }

    private void handleLoginError(Exception e) {
        String errorMsg = "Login failed. Please try again.";
        boolean handled = false;

        if (e instanceof FirebaseAuthInvalidCredentialsException || e instanceof FirebaseAuthException) {
            String errorCode = (e instanceof FirebaseAuthInvalidCredentialsException)
                    ? ((FirebaseAuthInvalidCredentialsException) e).getErrorCode()
                    : ((FirebaseAuthException) e).getErrorCode();

            handled = handleAuthErrorByCode(errorCode);
        } else if (e instanceof FirebaseAuthUserCollisionException) {
            etEmail.setError("This email is already registered.");
            errorMsg = "This email is already linked with another method. Try logging in.";
            handled = true;
        } else if (e instanceof FirebaseNetworkException) {
            errorMsg = "Network error. Please check your internet connection.";
            handled = true;
        } else if (e instanceof FirebaseAuthRecentLoginRequiredException) {
            errorMsg = "Please re-authenticate to continue.";
            handled = true;
        }

        // Fallback only if nothing was handled by errorCode
        if (!handled && e.getMessage() != null && e.getMessage().contains("INVALID_LOGIN_CREDENTIALS")) {
            etEmail.setError("");
            etPassword.setError("");
            Toast.makeText(this,"Login Failed. Check Email or Password.",Toast.LENGTH_LONG).show();
            handled = true;
        }

        if (!handled && errorMsg != null) {
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        }

        Log.e(TAG, "Authentication Error", e);
    }

    private boolean handleAuthErrorByCode(String errorCode) {
        switch (errorCode) {
            case "ERROR_INVALID_EMAIL":
                etEmail.setError("Enter a valid email address.");
                return true;
            case "ERROR_USER_NOT_FOUND":
                etEmail.setError("No account found with this email.");
                return true;
//        case "ERROR_WRONG_PASSWORD":
//            etPassword.setError("Incorrect password.");
//            return true;
            default:
                return false;
        }
    }


    private void handleSignUpError(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException) {
            etEmail.setError("This email is already registered");
        } else {
            Toast.makeText(this, "Sign up failed: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
        Log.e(TAG, "Sign up error", e);
    }


    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            try {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                }
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        showProgress(true);

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    showProgress(false);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        verifyEmail(user);
                    } else {
                        Log.w(TAG, "Google authentication failed", task.getException());
                        Toast.makeText(this, "Google authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void sendPasswordResetEmail(String email) {
        long now = System.currentTimeMillis();
        if (now - lastResetRequestTime < RESET_COOLDOWN) {
            long seconds = (RESET_COOLDOWN - (now - lastResetRequestTime)) / 1000;
            Toast.makeText(this, "Please wait " + seconds + " seconds before requesting again", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showProgress(false);

                    if (task.isSuccessful()) {
                        lastResetRequestTime = now;
                        Toast.makeText(this, "Reset email sent to " + email, Toast.LENGTH_SHORT).show();
                    } else {
                        handlePasswordResetError(task.getException());
                    }
                });
    }
    private void handlePasswordResetError(Exception e) {
        if (e instanceof FirebaseAuthInvalidUserException) {
            etEmail.setError("No account found with this email");
        } else {
            Toast.makeText(this, "Failed to send reset email: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
        Log.e(TAG, "Password reset error", e);
    }
    private void sendVerificationEmail(FirebaseUser user) {
        long now = System.currentTimeMillis();
        if (now - lastVerificationRequestTime < VERIFICATION_COOLDOWN) {
            long secondsRemaining = (VERIFICATION_COOLDOWN - (now - lastVerificationRequestTime)) / 1000;
            cooldownHandler.startCooldown(secondsRemaining);
            return;
        }

        showProgress(true);

        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    showProgress(false);

                    if (task.isSuccessful()) {
                        lastVerificationRequestTime = now;
                        btnVerifyNow.setEnabled(false);
                        cooldownHandler.startCooldown(VERIFICATION_COOLDOWN / 1000);
                        showEmailVerificationPopup(user);
                        Toast.makeText(this, "Verification email sent", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to send verification email", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Send verification email failed", task.getException());
                    }
                });
    }
    private void verifyEmail(FirebaseUser user) {
        if (user == null) return;

        showProgress(true);

        user.reload().addOnCompleteListener(task -> {
            showProgress(false);

            if (task.isSuccessful()) {
                if (user.isEmailVerified()) {
                    proceedToMainApp();
                } else {
                    showEmailVerificationPopup(user);
                }
            } else {
                Toast.makeText(this, "Failed to verify email status", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void showEmailVerificationPopup(FirebaseUser user) {
        formContainer.setVisibility(View.GONE);
        emailVerificationPopup.setVisibility(View.VISIBLE);
        tvEmailToVerify.setText(user.getEmail());
    }
    private void proceedToMainApp() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
    private void showProgress(boolean show) {
        // Implement your progress dialog/indicator here
        btnLogin.setEnabled(!show);
        btnGoogleSignIn.setEnabled(!show);
    }
    private static class CooldownHandler {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final WeakReference<LoginActivity> activityRef;
        private Runnable cooldownRunnable;
        private long currentSecondsRemaining; // Track seconds as a member variable

        CooldownHandler(LoginActivity activity) {
            this.activityRef = new WeakReference<>(activity);
        }

        void startCooldown(long secondsRemaining) {
            clear();
            currentSecondsRemaining = secondsRemaining; // Initialize the counter

            cooldownRunnable = new Runnable() {
                @Override
                public void run() {
                    LoginActivity activity = activityRef.get();
                    if (activity == null) return;

                    if (currentSecondsRemaining > 0) {
                        activity.btnVerifyNow.setText("Resend in " + currentSecondsRemaining + "s");
                        handler.postDelayed(this, 1000);
                        currentSecondsRemaining--; // Now we're modifying a member variable
                    } else {
                        activity.btnVerifyNow.setText("Resend Email");
                        activity.btnVerifyNow.setEnabled(true);
                    }
                }
            };

            handler.post(cooldownRunnable);
        }

        void clear() {
            if (cooldownRunnable != null) {
                handler.removeCallbacks(cooldownRunnable);
                cooldownRunnable = null;
            }
        }
    }
}