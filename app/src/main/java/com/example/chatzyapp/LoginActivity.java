package com.example.chatzyapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {
    private EditText inputEmail, inputPassword,inputConfirmPassword;
    private Button btnLogin;
    private TextView forgotPassword, creatNewAccount, recoverPassTv;
    FirebaseAuth auth;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        forgotPassword = findViewById(R.id.forgotPassword);
        creatNewAccount = findViewById(R.id.creatNewAccount);
        recoverPassTv = findViewById(R.id.recoverPassTv);
        getSupportActionBar().hide();

        auth = FirebaseAuth.getInstance();

        dialog = new ProgressDialog(this);
        dialog.setCancelable(false);

        creatNewAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });

        recoverPassTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRecoverPassDialog();
            }
        });
    }//

    private void showRecoverPassDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nh???p email ????? l???y l???i m???t kh???u");

        //setlayout
        LinearLayout linearLayout = new LinearLayout(this);
        //view to set
        EditText emailEt = new EditText(this);
        emailEt.setHint("Nh???p email");
        emailEt.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        emailEt.setMinEms(20);

        linearLayout.addView(emailEt);
        linearLayout.setPadding(10 , 10 ,10 ,10 );


        builder.setView(linearLayout);

        //button
        builder.setPositiveButton("X??c nh???n", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //input data
                String email = emailEt.getText().toString();
                beginRecovery(email);

            }
        });
        builder.setNegativeButton("H???y", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        //showdialog

        builder.create().show();

    }

    private void beginRecovery(String email) {
        dialog.setMessage("??ang g???i v??? email...");
        dialog.show();
        auth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                dialog.dismiss();
                if (task.isSuccessful()){
                    Toast.makeText(LoginActivity.this, "Vui l??ng v??o mail ????? x??c nh???n ?????i m???t kh???u", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(LoginActivity.this, "Failer", Toast.LENGTH_SHORT).show();
                }

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                dialog.dismiss();
                Toast.makeText(LoginActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void login(){
        String email, pass;
        email = inputEmail.getText().toString();
        pass = inputPassword.getText().toString();

        //ki???m tra tk c?? r???ng hay kh??ng
        if(email.isEmpty()){
            inputEmail.setError("Vui l??ng nh???p email");
            return;
        }
        else if(pass.isEmpty() || pass.length()<6){
            inputPassword.setError("Vui l??ng nh???p m???t kh???u");
            return;
        }
        else{
            dialog.setMessage("??ang t???i th??ng tin...");
            dialog.show();
            auth.signInWithEmailAndPassword(email,pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()){
                        dialog.dismiss();

                        FirebaseUser user = auth.getCurrentUser();
                        // login first time then show infor from gg account
                        if (task.getResult().getAdditionalUserInfo().isNewUser()){
                            // get user and, email, and uid
                            String email = user.getEmail();
                            String uid = user.getUid();
                            // when user is registered store user info in database realtime database too
                            HashMap<Object,String> hashMap = new HashMap<>();
                            //put info in hashmap
                            hashMap.put("email",email);
                            hashMap.put("uid",uid);
                            hashMap.put("name","");
                            hashMap.put("onlineStatus","online");
//                            hashMap.put("typingTo","noOne");
                            hashMap.put("phone",""); // add later
                            hashMap.put("image","");
                            hashMap.put("cover","");
                            //firebase database instance
                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            //path to store user data named "User"
                            DatabaseReference reference = database.getReference("Users");
                            //put data within hashMap in database
                            reference.child(uid).setValue(hashMap);
                        }

                        Toast.makeText(getApplicationContext(), "????ng nh???p th??nh c??ng!", Toast.LENGTH_SHORT).show();
                        Intent main = new Intent(LoginActivity.this, DashboardActivity.class);
                        startActivity(main);
                    }else{
                        dialog.dismiss();
                        Toast.makeText(getApplicationContext(), "????ng nh???p th???t b???i!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}