package com.example.chatzyapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Adapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatzyapp.adapters.AdapterChat;
import com.example.chatzyapp.adapters.AdapterUsers;
import com.example.chatzyapp.models.ModelUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ThereProfileActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;

    List<ModelUser> userList;
    AdapterUsers adapterUsers;
    String uid;

    // views from xml
    ImageView coverIv;
    CircleImageView avatarTv;
    TextView emailTv, phoneTv,nameTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_there_profile);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Thông tin tài khoản");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        //init views
        avatarTv = findViewById(R.id.avatarTv);
        coverIv = findViewById(R.id.coverIv);
        nameTv = findViewById(R.id.nameTv);
        emailTv = findViewById(R.id.emailTv);
        phoneTv = findViewById(R.id.phoneTv);

        firebaseAuth = FirebaseAuth.getInstance();


        Intent intent = getIntent();
        uid = intent.getStringExtra("uid");


        Query query = FirebaseDatabase.getInstance().getReference("Users").orderByChild("uid").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // kiểm tra cho đến khi nhận đc dữ liệu
                for (DataSnapshot ds: snapshot.getChildren()){
                    //get data
                    String name = ""+ds.child("name").getValue();
                    String email = ""+ds.child("email").getValue();
                    String phone = ""+ds.child("phone").getValue();
                    String image = ""+ds.child("image").getValue();
                    String cover = ""+ds.child("cover").getValue();

                    //set data
                    nameTv.setText(name);
                    emailTv.setText(email);
                    phoneTv.setText(phone);
                    try {
                        // if received then set
                        Picasso.get().load(image).into(avatarTv);
                    }catch (Exception e){
                        // image default
                        Picasso.get().load(R.drawable.profile).into(avatarTv);
                    }
                    try {
                        Picasso.get().load(cover).into(coverIv);
                    }catch (Exception e){
                        //
                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });



        userList = new ArrayList<>();

        loadhisProfile();

        checkUserStatus();


    }

    private void loadhisProfile() {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");

        Query query = ref.orderByChild("uid").equalTo(uid);
        //get all data
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot ds: snapshot.getChildren()){
                    ModelUser myusers = ds.getValue(ModelUser.class);

                    //add list
                    userList.add(myusers);

                    //adapter
                    adapterUsers = new AdapterUsers(ThereProfileActivity.this, userList);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ThereProfileActivity.this, "", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkUserStatus(){
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null){
            // user is signed in stay here
        }else {
            // user not signed go to login now
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return super.onCreateOptionsMenu(menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//
//        //get item id
//        int id = item.getItemId();
//        if (id == R.id.action_logout){
//            firebaseAuth.signOut();
//            checkUserStatus();
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
}