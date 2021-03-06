package com.proxima.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseUser;
import com.parse.SignUpCallback;
import com.proxima.R;
import com.proxima.utils.PhotoUtils;
import com.proxima.utils.Tracker;

import java.io.ByteArrayOutputStream;


//
// Created by Andrew Clissold, Rachel Glomski, Jon Wong on 9/11/14.
// A login screen that offers login via email/password.
//
// Recent Version: 11/25/14
public class LoginActivity extends ActionBarActivity {

    private final String TAG = LoginActivity.class.getName();

    // UI references.
    private AutoCompleteTextView mUsernameView;
    private EditText mPasswordView;
    private EditText mConfirmPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private View mFocusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Set up the login form.
        mUsernameView = (AutoCompleteTextView) findViewById(R.id.profile_username);

        mPasswordView = (EditText) findViewById(R.id.password);
        mConfirmPasswordView = (EditText) findViewById(R.id.confirm_password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    /**
     * Attempts to sign in the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        final String username = mUsernameView.getText().toString();
        final String password = mPasswordView.getText().toString();
        final ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        String confirmPassword = mConfirmPasswordView.getText().toString();

        mFocusView = null;

        boolean valid = isFieldsValid(username, password, confirmPassword);

        if (!valid) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            mFocusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            ParseUser.logInInBackground(username, password, new LogInCallback() {
                public void done(ParseUser user, ParseException e) {
                    if (e == null && user != null) {
                        showProgress(false);
                        Tracker.getInstance().trackLogin(username);
                        PhotoUtils.getInstance().downloadUserPhotos();
                        installation.put("user", user);
                        installation.saveInBackground();
                        finish();
                    } else if (e.getCode() == ParseException.OBJECT_NOT_FOUND) {
                        // don't call showProgress(false) yet
                        // Either username not found or password wrong; try creating a user
                        if (TextUtils.isEmpty(mConfirmPasswordView.getText().toString())) {
                            showProgress(false);
                            mConfirmPasswordView.setError(getString(R.string.error_field_required));
                            mConfirmPasswordView.requestFocus();
                        } else {
                            attemptSignUp(username, password);
                        }
                    } else {
                        showProgress(false);
                        mPasswordView.setError(getString(R.string.error_incorrect_password));
                        mPasswordView.requestFocus();
                    }
                }
            });
        }
    }

    /**
     * Attempts to register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptSignUp(final String username, String password) {
        final ParseUser user = new ParseUser();
        final ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        user.setUsername(username);
        user.setPassword(password);
        user.signUpInBackground(new SignUpCallback() {
            @Override
            public void done(ParseException e) {
                showProgress(false);
                if (e == null) {
                    Tracker.getInstance().trackSignup(username);
                    Bitmap data = BitmapFactory.decodeResource(getResources(), R.drawable.proxima_logo);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    data.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
                    ParseFile file = new ParseFile("userIcon.jpg", byteArray);
                    user.put("icon", file);
                    user.saveInBackground();
                    PhotoUtils.getInstance().downloadUserPhotos();
                    installation.put("user", user);
                    installation.saveInBackground();
                    finish();
                } else if (e.getCode() == ParseException.USERNAME_TAKEN) {
                    mUsernameView.setError(getString(R.string.error_username_taken));
                    mUsernameView.requestFocus();
                }
            }
        });
    }

    private boolean isFieldsValid(String username, String password, String confirmPassword) {
        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && (password.length() < 6)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            mFocusView = mPasswordView;
            return false;
        }

        // Confirm the passwords match.
        if (!TextUtils.isEmpty(password) && !TextUtils.isEmpty(confirmPassword) &&
                !password.equals(confirmPassword)) {
            mConfirmPasswordView.setError(getString(R.string.error_mismatched_passwords));
            mFocusView = mConfirmPasswordView;
            return false;
        }

        // Check for empty fields.
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            mFocusView = mUsernameView;
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            mFocusView = mPasswordView;
            return false;
        }

        return true;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}

