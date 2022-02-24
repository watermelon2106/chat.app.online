package com.example.chatzyapp.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatzyapp.GroupChatActivity;
import com.example.chatzyapp.GroupInfoActivity;
import com.example.chatzyapp.R;
import com.example.chatzyapp.models.ModelGroupChat;
import com.example.chatzyapp.models.ModelGroupChatList;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdapterGroupChat extends RecyclerView.Adapter<AdapterGroupChat.HolderGroupChat> {

    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;

    private Context context;
    private ArrayList<ModelGroupChat> modelGroupChatList;

    private FirebaseAuth firebaseAuth;



    public AdapterGroupChat(Context context, ArrayList<ModelGroupChat> modelGroupChatList) {
        this.context = context;
        this.modelGroupChatList = modelGroupChatList;
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public HolderGroupChat onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == MSG_TYPE_RIGHT){
            View view = LayoutInflater.from(context).inflate(R.layout.row_groupchat_right,parent,false);
            return new HolderGroupChat(view);
        }
        else {
            View view = LayoutInflater.from(context).inflate(R.layout.row_groupchat_left,parent,false);
            return new HolderGroupChat(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull HolderGroupChat holder, @SuppressLint("RecyclerView") int position) {
        //get data
        ModelGroupChat model = modelGroupChatList.get(position);
        String message = model.getMessage();//if text message then cotain message, if image message then contain url of the  image store in fire base store
        String senderUid = model.getSender();
        String messageType = model.getType();
        String timeStamp = model.getTimeStamp();



        //convert timestamp to dd/mm/yyyy hh:mm AM/PM
        //lấy calendar sử dụng  ngôn ngữ mặc định AM/PM.
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        // mã hóa về kieur timeStamp
        cal.setTimeInMillis(Long.parseLong(timeStamp));
        // định dạng về kiểu string
        String dateTime = DateFormat.format("hh:mm aa",cal).toString();

        //set data
        if (messageType.equals("text")){
            //text message, hide image view, show messageTv
            holder.messageIv.setVisibility(View.GONE);
            holder.messageTv.setVisibility(View.VISIBLE);
            holder.messageTv.setText(message);
        }
        else{
            //image message, hide message, show Imageview
            holder.messageIv.setVisibility(View.VISIBLE);
            holder.messageTv.setVisibility(View.GONE);
            try{
                Picasso.get().load(message).placeholder(R.drawable.ic_add_image).into(holder.messageIv);
            }
            catch (Exception e){
                holder.messageIv.setImageResource(R.drawable.ic_add_image);
            }
        }

        //click to show delete dialog

        holder.timeTv.setText(dateTime);

        setUserName(model, holder);

    }

    private void setUserName(ModelGroupChat model, HolderGroupChat holder) {
        //get sender info from uid in model
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.orderByChild("uid").equalTo(model.getSender())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds: snapshot.getChildren()){
                            String name = ""+ds.child("name").getValue();
                            holder.nameTv.setText(name);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }


    @Override
    public int getItemCount() {
        return modelGroupChatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (modelGroupChatList.get(position).getSender().equals(firebaseAuth.getUid())){
            return MSG_TYPE_RIGHT;
        }
        else {
            return MSG_TYPE_LEFT;
        }
    }

    class HolderGroupChat extends RecyclerView.ViewHolder {

        TextView nameTv, messageTv,timeTv;
        ImageView messageIv;
        LinearLayout messageLayout;
        public HolderGroupChat(@NonNull View itemView) {
            super(itemView);
            messageTv = itemView.findViewById(R.id.messageTv);
            nameTv = itemView.findViewById(R.id.nameTv);
            timeTv = itemView.findViewById(R.id.timeTv);
            messageIv = itemView.findViewById(R.id.messageIv);
            messageLayout = itemView.findViewById(R.id.messageLayout);
        }
    }
}
