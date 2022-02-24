package com.example.chatzyapp.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import com.example.chatzyapp.GroupCreateActivity;
import com.example.chatzyapp.MainActivity;
import com.example.chatzyapp.R;
import com.example.chatzyapp.adapters.AdapterUsers;
import com.example.chatzyapp.models.ModelUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class UsersFragment extends Fragment {


    RecyclerView recyclerView;
    AdapterUsers adapterUsers;
    List<ModelUser> userList;

    FirebaseAuth firebaseAuth;


    public UsersFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_users, container, false);

        //init
        firebaseAuth = FirebaseAuth.getInstance();

        //init
        recyclerView = view.findViewById(R.id.users_recyclerView);
        //set properties
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        //init user list
        userList = new ArrayList<>();
        getAllUsers();
        
        return view;
    }

    private void getAllUsers() {
        //get current user

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        //get path of database name "Users" containing(lưu trữ) users info
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        //get all data from path
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot ds: snapshot.getChildren()){
                    ModelUser modelUser = ds.getValue(ModelUser.class);

                    //get all users except currently signed in user
                    if (!modelUser.getUid().equals(user.getUid())){
                        userList.add(modelUser);
                    }
                    //adapter
                    adapterUsers = new AdapterUsers(getActivity(),userList);
                    //set adapter to recycler view
                    recyclerView.setAdapter(adapterUsers);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // event SEARCH USER
    private void searchUsers(String query) {
        //get current user

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        //get path of database name "Users" containing(lưu trữ) users info
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        //get all data from path
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot ds: snapshot.getChildren()){
                    ModelUser modelUser = ds.getValue(ModelUser.class);

                    /*điểu kiện search
                    * 1, người dùng k phải là người dùng hiện tại
                    * 2, tên người dùng hoặc email có chứa văn bản được nhập trong SearchView */

                    //get all search users except currently signed in user
                    if (!modelUser.getUid().equals(user.getUid())){
                        if (modelUser.getName().toLowerCase().contains(query.toLowerCase()) ||
                                modelUser.getEmail().toLowerCase().contains(query.toLowerCase())){
                            userList.add(modelUser);
                        }
                    }
                    //adapter
                    adapterUsers = new AdapterUsers(getActivity(),userList);
                    //refresh adapter
                    adapterUsers.notifyDataSetChanged();
                    //set adapter to recycler view
                    recyclerView.setAdapter(adapterUsers);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void checkUserStatus(){
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null){
            // user is signed in stay here
        }else {
            // user not signed go to login now
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().finish();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true); // to show menu option in  fragment
        super.onCreate(savedInstanceState);
    }

    //inflate option menu
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //inflate menu
        inflater.inflate(R.menu.menu_main,menu);

        menu.findItem(R.id.action_logout).setVisible(false);
        menu.findItem(R.id.action_settings).setVisible(false);
        menu.findItem(R.id.action_add_participant).setVisible(false);
        menu.findItem(R.id.action_groupinfo).setVisible(false);

        //searchView
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        //search listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //được gọi khi người dùng nhấn nút tìm kiếm trên bàn phím
                //kiểm tra rỗng
                if (!TextUtils.isEmpty(s.trim())){
                    //search text contains text, search it
                    searchUsers(s);
                }else {
                    //search text empty, get all users
                    getAllUsers();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //khi nào người dùng nhấn bất kỳ sẽ tìm kiếm theo ký tự đầu tiên
                //
                if (!TextUtils.isEmpty(s.trim())){
                    //search text contains text, search it
                    searchUsers(s);
                }else {
                    //search text empty, get all users
                    getAllUsers();
                }
                return false;
            }
        });


        super.onCreateOptionsMenu(menu,inflater);
    }
    //handle click
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //get item id
        int id = item.getItemId();
        if (id == R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        }
        //go to groupcreatactivity
        else if (id==R.id.action_creat_group){
            startActivity(new Intent(getActivity(), GroupCreateActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

}