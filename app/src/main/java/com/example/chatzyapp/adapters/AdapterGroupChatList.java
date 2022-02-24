package com.example.chatzyapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatzyapp.GroupChatActivity;
import com.example.chatzyapp.R;
import com.example.chatzyapp.models.ModelGroupChatList;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import javax.xml.datatype.DatatypeConfigurationException;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdapterGroupChatList extends RecyclerView.Adapter<AdapterGroupChatList.HolderGroupChatList> {

    private Context context;
    private ArrayList<ModelGroupChatList> groupChatLists;

    public AdapterGroupChatList(Context context, ArrayList<ModelGroupChatList> groupChatLists) {
        this.context = context;
        this.groupChatLists = groupChatLists;
    }

    @NonNull
    @Override
    public HolderGroupChatList onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate layout
        View view = LayoutInflater.from(context).inflate(R.layout.row_groupchats_list,parent,false);
        return new HolderGroupChatList(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderGroupChatList holder, int position) {

        //get data
        ModelGroupChatList model = groupChatLists.get(position);
        String groupId = model.getGroupId();
        String groupIcon = model.getGroupIcon();
        String groupTitle = model.getGroupTitle();

        holder.nameTv.setText("");
        holder.timeTv.setText("");
        holder.messageTv.setText("");

        //load last messenger and message-time
        loadLastMessage(model,holder);

        //set data
        holder.groupTitleTv.setText(groupTitle);
        try{

            Picasso.get().load(groupIcon).placeholder(R.drawable.ic_profile).into(holder.groupIconIv);

        }catch (Exception e){
            holder.groupIconIv.setImageResource(R.drawable.profile);
        }

        //handle group click
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //open group chat
                Intent intent = new Intent(context, GroupChatActivity.class);
                intent.putExtra("groupId", groupId);
                context.startActivity(intent);
            }
        });


    }

    private void loadLastMessage(ModelGroupChatList model, HolderGroupChatList holder) {
        //get last message from group
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child((model.getGroupId())).child("Messages").limitToLast(1)//get last item(message) from that child
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds:snapshot.getChildren()){

                            //get data
                            String message = ""+ds.child("message").getValue();
                            String sender = ""+ds.child("sender").getValue();
                            String messagetype = ""+ds.child("type").getValue();
                            String timeStamp = ""+ds.child("timeStamp").getValue();


                            //convert timestamp to dd/mm/yyyy hh:mm AM/PM
                            //lấy calendar sử dụng  ngôn ngữ mặc định AM/PM.
                            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                            // mã hóa về kieur timeStamp
                            cal.setTimeInMillis(Long.parseLong(timeStamp));
                            // định dạng về kiểu string
                            String dateTime = DateFormat.format("hh:mm aa",cal).toString();

                            if (messagetype.equals("image")){
                                holder.messageTv.setText("Đã gửi 1 ảnh");
                            }
                            else {
                                holder.messageTv.setText(message);
                            }
//
                            holder.timeTv.setText(dateTime);

                            //get info of sender of last message
                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                            ref.orderByChild("uid").equalTo(sender)
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
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    public int getItemCount() {
        return groupChatLists.size();
    }

    //view holder class
    class HolderGroupChatList extends RecyclerView.ViewHolder{

        //UI view

        CircleImageView groupIconIv;
        TextView groupTitleTv, nameTv, messageTv, timeTv;

        public HolderGroupChatList(@NonNull View itemView) {
            super(itemView);

            groupIconIv = itemView.findViewById(R.id.groupIconIv);
            groupTitleTv = itemView.findViewById(R.id.groupTitleTv);
            nameTv = itemView.findViewById(R.id.nameTv);
            messageTv = itemView.findViewById(R.id.messageTv);
            timeTv = itemView.findViewById(R.id.timeTv);

        }
    }
}
