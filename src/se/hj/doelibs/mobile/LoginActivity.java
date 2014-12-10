package se.hj.doelibs.mobile;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import se.hj.doelibs.mobile.asynctask.LoginAsyncTask;
import se.hj.doelibs.mobile.asynctask.TaskCallback;
import se.hj.doelibs.mobile.utils.ConnectionUtils;
import se.hj.doelibs.mobile.utils.ProgressDialogUtils;

/**
 * @author Christoph
 */
public class LoginActivity extends BaseActivity {
    private EditText usernameField;
    private EditText passwordField;
    private Button btnLogin;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View contentView = inflater.inflate(R.layout.activity_login, null, false);
        drawerLayout.addView(contentView, 0);

        usernameField = (EditText)findViewById(R.id.login_username);
        passwordField = (EditText)findViewById(R.id.login_password);
        btnLogin = (Button)findViewById(R.id.login_button);

        TextView loginUsr = (TextView)findViewById(R.id.login_username_header);
        TextView loginPsw = (TextView)findViewById(R.id.login_password_header);

        Typeface novaThin = Typeface.createFromAsset(contentView.getResources().getAssets(), "fonts/Proxima Nova Thin.otf");
        Typeface novaLight = Typeface.createFromAsset(contentView.getResources().getAssets(), "fonts/Proxima Nova Alt Condensed Light.otf");

        usernameField.setTypeface(novaLight);
        passwordField.setTypeface(novaLight);
        btnLogin.setTypeface(novaLight);
        loginPsw.setTypeface(novaThin);
        loginUsr.setTypeface(novaThin);

        if(!ConnectionUtils.isConnected(this)) {
            btnLogin.setActivated(false);
            btnLogin.setEnabled(false);

            new AlertDialog.Builder(this)
                    .setMessage(R.string.login_error_internet_connection)
                    .show();

        }
    }

    public void onLogin(View view) {
        //at a login we do a simple request to a page which requires a logged in user and check the statuscodes

        String username = usernameField.getText().toString();
        String password = passwordField.getText().toString();

        Log.i("Login", "Try to login as user: " + username);

        new LoginAsyncTask(this, username, password, new TaskCallback<Boolean>() {
            private ProgressDialog dialog;

            @Override
            public void onTaskCompleted(Boolean loginSuccessfull) {
                ProgressDialogUtils.dismissQuitely(dialog);

                if(loginSuccessfull) {
                    Intent myLoanActivity = new Intent(LoginActivity.this, MyLoansActivity.class);
                    startActivity(myLoanActivity);
                    finish();
                } else {
                    passwordField.setText("");
                    Toast.makeText(LoginActivity.this, getResources().getText(R.string.login_error_credentials), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void beforeTaskRun() {
                dialog = new ProgressDialog(LoginActivity.this);
                dialog.setMessage(getResources().getText(R.string.login_checking_credentials));
                dialog.setCancelable(false);
                dialog.show();
            }
        }).execute();
    }
}