package com.example.chatzyapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.example.chatzyapp.fragments.ChatListFragment;
import com.example.chatzyapp.fragments.GroupChatsFragment;
import com.example.chatzyapp.fragments.ProfileFragment;
import com.example.chatzyapp.fragments.UsersFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class DashboardActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    ActionBar actionBar;
    String uid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        //
        actionBar = getSupportActionBar();
        actionBar.setTitle("Main");

        firebaseAuth = FirebaseAuth.getInstance();

        //botton Navigation
        BottomNavigationView navigationView = findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(selectedListener);

        //profile fragment transaction(default is start app)
        actionBar.setTitle("Nhắn tin");
        ChatListFragment fragmentChatList = new ChatListFragment();
        FragmentTransaction ft3 = getSupportFragmentManager().beginTransaction();
        ft3.replace(R.id.content,fragmentChatList,"");
        ft3.commit();
    }//

    private BottomNavigationView.OnNavigationItemSelectedListener selectedListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    //handle item clicks
                    switch (item.getItemId()){
                        case R.id.nav_chat:
                            //profile fragment transaction
                            actionBar.setTitle("Nhắn tin");
                            ChatListFragment fragmentChatList = new ChatListFragment();
                            FragmentTransaction ft3 = getSupportFragmentManager().beginTransaction();
                            ft3.replace(R.id.content,fragmentChatList,"");
                            ft3.commit();
                            return true;
                        case R.id.nav_users:
                            //profile fragment transaction
                            actionBar.setTitle("Tìm kiếm bạn bè");
                            UsersFragment fragmentUsers = new UsersFragment();
                            FragmentTransaction ft4 = getSupportFragmentManager().beginTransaction();
                            ft4.replace(R.id.content,fragmentUsers,"");
                            ft4.commit();
                            return true;
                        case R.id.nav_groupchats:
                            //profile fragment transaction
                            actionBar.setTitle("Nhắn tin nhóm");
                            GroupChatsFragment fragmentGroupChat = new GroupChatsFragment();
                            FragmentTransaction ft5 = getSupportFragmentManager().beginTransaction();
                            ft5.replace(R.id.content,fragmentGroupChat,"");
                            ft5.commit();
                            return true;
                        case R.id.nav_profile:
                            //profile fragment transaction
                            actionBar.setTitle("Cài đặt");
                            ProfileFragment fragmentProfile = new ProfileFragment();
                            FragmentTransaction ft2 = getSupportFragmentManager().beginTransaction();
                            ft2.replace(R.id.content,fragmentProfile,"");
                            ft2.commit();
                            return true;
                    }
                    return false;
                }
            };

    private void checkUserStatus(){
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null){
            // user is signed in stay here
        }else {
            // user not signed go to login now
            startActivity(new Intent(DashboardActivity.this,MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onStart() {
        //check on start of app
        checkUserStatus();
        super.onStart();
    }


}