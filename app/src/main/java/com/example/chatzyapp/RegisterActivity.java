package com.example.chatzyapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {
    private EditText inputEmail, inputPassword, inputConfirmPassword;
    private Button btnRegister;
    private TextView alreadyHaveAccount;
    FirebaseAuth auth;
    ProgressDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        getSupportActionBar().hide();

        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        btnRegister = findViewById(R.id.btnLogin);
        alreadyHaveAccount = findViewById(R.id.alreadyHaveAccount);
        //init
        auth = FirebaseAuth.getInstance();


        dialog = new ProgressDialog(this);
        dialog.setMessage("Đang tải thông tin...");
        dialog.setCancelable(false);


        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                register();
            }
        });

        alreadyHaveAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }// onCeate

    private void register() {
        String email, pass, confirmpass;
        email = inputEmail.getText().toString();
        pass = inputPassword.getText().toString();
        confirmpass = inputConfirmPassword.getText().toString();
        //kiểm tra email có rống hay không
        if (email.isEmpty()) {
            inputEmail.setError("Vui lòng nhập email");
            return;
        } else if (pass.isEmpty() && pass.length() < 6) {
            inputPassword.setError("Vui lòng nhập mật khẩu");
            return;
        } else if (!confirmpass.equals(pass)) {
            inputConfirmPassword.setError("Mật khẩu không đúng");
            return;
        } else {
            dialog.show();
            auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        dialog.dismiss();

                        FirebaseUser user = auth.getCurrentUser();
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
//                        hashMap.put("typingTo","noOne");
                        hashMap.put("phone","");
                        hashMap.put("image","");
                        hashMap.put("cover","");
                        //firebase database instance
                        FirebaseDatabase database = FirebaseDatabase.getInstance();
                        //path to store user data named "User"
                        DatabaseReference reference = database.getReference("Users");
                        //put data within hashMap in database
                        reference.child(uid).setValue(hashMap);


                        Toast.makeText(getApplicationContext(), "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                        Intent main = new Intent(RegisterActivity.this, DashboardActivity.class);
                        startActivity(main);
                    } else {
                        dialog.dismiss();
                        Toast.makeText(getApplicationContext(), "Tài khoản đã tồn tại hoặc không đúng định dạng!", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}