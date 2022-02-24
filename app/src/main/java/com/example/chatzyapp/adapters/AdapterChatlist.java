package com.example.chatzyapp.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatzyapp.ChatActivity;
import com.example.chatzyapp.GroupChatActivity;
import com.example.chatzyapp.GroupInfoActivity;
import com.example.chatzyapp.R;
import com.example.chatzyapp.ThereProfileActivity;
import com.example.chatzyapp.models.ModelChatlist;
import com.example.chatzyapp.models.ModelUser;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdapterChatlist extends RecyclerView.Adapter<AdapterChatlist.MyHolder> {

    Context context;
    List<ModelUser> userList; //lấy thông tin người dùng
    private HashMap<String, String> lastMessageMap;

    //get Uid
    FirebaseAuth firebaseAuth;
    String myUid;

    public AdapterChatlist(Context context, List<ModelUser> userList) {
        this.context = context;
        this.userList = userList;
        lastMessageMap = new HashMap<>();


        firebaseAuth = FirebaseAuth.getInstance();
        myUid = firebaseAuth.getUid();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //đọc xml file chuyển thuộc tính thánh 1 view
        View view = LayoutInflater.from(context).inflate(R.layout.row_chatlist,parent,false);//false chuyển thành view không làm thêm gì khác vd: hiển thị view đó
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, @SuppressLint("RecyclerView") int position) {
        //getdata
        String hisUid = userList.get(position).getUid();
        String userImage = userList.get(position).getImage();
        String userName = userList.get(position).getName();
        String lastMessage = lastMessageMap.get(hisUid);

        //set data
        holder.nameTv.setText(userName);
        if (lastMessage==null || lastMessage.equals("default")){
            holder.lastMessageTv.setVisibility(View.GONE);
        }
        else {
            holder.lastMessageTv.setVisibility(View.VISIBLE);
            holder.lastMessageTv.setText(lastMessage);
        }
        try{
            Picasso.get().load(userImage).placeholder(R.drawable.profile).into(holder.profileIv);

        }catch (Exception e){
            Picasso.get().load(R.drawable.profile).into(holder.profileIv);
        }

        holder.blockIv.setImageResource(R.drawable.ic_unblock);
        // check block or not block
        checkIsBlocked(hisUid, holder, position);


        //set online status of other users in chat list
//        if (userList.get(position).getOnlineStatus().equals("online")){
//            //online
//            holder.onlineStatusIv.setImageResource(R.drawable.circle_online);
//        }else {
//            //offline
//            holder.onlineStatusIv.setImageResource(R.drawable.circle_offline);
//        }
        //handle click of user in chatlist
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                imBlockedOrNot(hisUid);
            }
        });

        holder.blockIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (userList.get(position).isBlocked()){
                    unBlockUser(hisUid);
                }
                else{
                    blockUser(hisUid);
                }
            }
        });
    }

    private void checkIsBlocked(String hisUid, MyHolder holder, int position) {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("BlockedUsers").orderByChild("uid").equalTo(hisUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds: snapshot.getChildren()){
                            if (ds.exists()){
                                holder.blockIv.setImageResource(R.drawable.ic_block);
                                userList.get(position).setBlocked(true);
                            }
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }


    public void setLastMessageMap(String userId, String lastMessage){
        lastMessageMap.put(userId, lastMessage);
    }

    private void imBlockedOrNot(String hisUid){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(hisUid).child("BlockedUsers").orderByChild("uid").equalTo(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds: snapshot.getChildren()){
                            if (ds.exists()){
                                Toast.makeText(context, "Bạn đã bị chặn bởi người dùng này, không thể nhắn tin", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        // not blocked, start messenger

                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra("hisUid", hisUid);
                        context.startActivity(intent);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }


    private void blockUser(String hisUid) {
        // put values in hashmap to put db
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("uid", hisUid);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").child(hisUid).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //block success
                        Toast.makeText(context, "Đã chặn người này", Toast.LENGTH_SHORT).show();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //block failed
                        Toast.makeText(context, "Chặn thất bại"+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void unBlockUser(String hisUid) {

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").orderByChild("uid").equalTo(hisUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds: snapshot.getChildren()){
                            if (ds.exists()){
                                ds.getRef().removeValue()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                Toast.makeText(context, "Bỏ chặn thành công", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(context, "Bỏ Chặn thất bại"+e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    public int getItemCount() {
        return userList.size(); // kích thước danh sách
    }

    class MyHolder extends RecyclerView.ViewHolder{
        //view of row_chatlist.xml
        CircleImageView profileIv , onlineStatusIv;
        TextView nameTv, lastMessageTv, timeTv;
        ImageView blockIv;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            //init
            profileIv = itemView.findViewById(R.id.profileIv);
            onlineStatusIv = itemView.findViewById(R.id.onlineStatusIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            lastMessageTv = itemView.findViewById(R.id.lassMessageTv);
            blockIv = itemView.findViewById(R.id.blockIv);
        }
    }
}
