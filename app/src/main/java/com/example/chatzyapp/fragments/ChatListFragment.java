package com.example.chatzyapp.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
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
import com.example.chatzyapp.adapters.AdapterChatlist;
import com.example.chatzyapp.models.ModelChat;
import com.example.chatzyapp.models.ModelChatlist;
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

public class ChatListFragment extends Fragment {

    //String myUid;

    RecyclerView recyclerView;
    List<ModelChatlist> chatlistList;
    List<ModelUser> userList;
    //
    FirebaseAuth firebaseAuth;
    DatabaseReference reference;
    FirebaseUser currentUser;
    //
    AdapterChatlist adapterChatlist;

    public ChatListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);

        //init
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        recyclerView = view.findViewById(R.id.recyclerView);

        chatlistList = new ArrayList<>();

        reference = FirebaseDatabase.getInstance().getReference("Chatlist").child(currentUser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatlistList.clear();
                // snapshot.getChildren: Cấp quyền truy cập trực tiếp  cho tất cả các phần tử con
                for (DataSnapshot ds: snapshot.getChildren()){
                    ModelChatlist chatlist = ds.getValue(ModelChatlist.class);
                    chatlistList.add(chatlist);
                }
                loadChats();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        return view;
    }// on create

    private void loadChats() {
        userList = new ArrayList<>();
        reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot ds: snapshot.getChildren()){
                    ModelUser user = ds.getValue(ModelUser.class);
                    for (ModelChatlist chatlist: chatlistList){
                        if (user.getUid()!=null && user.getUid().equals(chatlist.getId())){
                            userList.add(user);
                            break;
                        }
                    }
                    //adapter
                    adapterChatlist = new AdapterChatlist(getContext(),userList);
                    //set adapter
                    recyclerView.setAdapter(adapterChatlist);
                    //set last message
                    for (int i=0; i<userList.size(); i++){
                        lastMessage(userList.get(i).getUid());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void lastMessage(String userId) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String theLastMessage = "default";
                for (DataSnapshot ds: snapshot.getChildren()){
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if (chat==null){
                        continue;
                    }
                    String sender = chat.getSender();
                    String receiver = chat.getReceiver();
                    if (sender == null || receiver == null){
                        continue;
                    }
                    if (chat.getReceiver().equals(currentUser.getUid()) &&
                            chat.getSender().equals(userId) ||
                    chat.getReceiver().equals(userId) &&
                    chat.getSender().equals(currentUser.getUid())){
                            //instead of displaying url in message show"sent photo"
                            if (chat.getType().equals("image")){
                                theLastMessage = "Sent a photo...";
                            }else {
                                // show text
                                theLastMessage = chat.getMessage();
                            }

                    }
                }
                adapterChatlist.setLastMessageMap(userId, theLastMessage);
                adapterChatlist.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //-------------------------search---------------------------
    // event SEARCH USER
    private void searchUsers(String query) {
        userList = new ArrayList<>();
        //get current user
        //get path of database name "Users" containing(lưu trữ) users info
        reference = FirebaseDatabase.getInstance().getReference("Users");
        //get all data from path
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot ds: snapshot.getChildren()){

                    /*điểu kiện search
                     * 1, người dùng k phải là người dùng hiện tại
                     * 2, tên người dùng hoặc email có chứa văn bản được nhập trong SearchView */

                    ModelUser user = ds.getValue(ModelUser.class);
                    //get all users except currently signed in user

                        if (!user.getUid().equals(currentUser.getUid())){
                            if (user.getName().toLowerCase().contains(query.toLowerCase()) ||
                                    user.getEmail().toLowerCase().contains(query.toLowerCase())){
                                userList.add(user);
                            }
                        }


                    //adapter
                    adapterChatlist = new AdapterChatlist(getContext(),userList);
                    //refresh adapter
                    adapterChatlist.notifyDataSetChanged();
                    //set adapter to recycler view
                    recyclerView.setAdapter(adapterChatlist);

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
//            myUid = user.getUid();
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
                //called when user press search button form keyboard
                //if search query is not empty then search
                if (!TextUtils.isEmpty(s.trim())){
                    //search text contains text, search it
                    searchUsers(s);
                }else {
                    //search text empty, get all users
                    loadChats();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //called whenever user press any single letter
                //if search query is not empty then search
                if (!TextUtils.isEmpty(s.trim())){
                    //search text contains text, search it
                    searchUsers(s);
                }else {
                    //search text empty, get all users
                    loadChats();
                }
                return false;
            }
        });


        super.onCreateOptionsMenu(menu,inflater);
    }

        @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //get item id
        int id = item.getItemId();
        if (id == R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        }
        // go to group creat activivity
        else if (id==R.id.action_creat_group){
            startActivity(new Intent(getActivity(), GroupCreateActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}