package com.example.chatzyapp.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatzyapp.R;
import com.example.chatzyapp.models.ModelChat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class AdapterChat extends RecyclerView.Adapter<AdapterChat.MyHolder>{

    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;
    Context context;
    List<ModelChat> chatList;
    String imageUrl;

    FirebaseUser user;

    public AdapterChat(Context context, List<ModelChat> chatList, String imageUrl) {
        this.context = context;
        this.chatList = chatList;
        this.imageUrl = imageUrl;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate layout: row_chat_left.xml for receiver, row_chat_right for sender
        //gọi ra 2 layout ánh xạ view và data
        if (viewType == MSG_TYPE_RIGHT){
            //đổ data vào view
            View view = LayoutInflater.from(context).inflate(R.layout.row_chat_right, parent, false);
            return new MyHolder(view);
        }else {
            View view = LayoutInflater.from(context).inflate(R.layout.row_chat_left, parent, false);
            return new MyHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, @SuppressLint("RecyclerView") final int position) {
        //get data
        String message = chatList.get(position).getMessage();
        String timeStamp = chatList.get(position).getTimeStamp();
        String type = chatList.get(position).getType();


        //convert timestamp to dd/mm/yyyy hh:mm AM/PM
        //lấy calendar sử dụng  ngôn ngữ mặc định AM/PM.
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        // mã hóa về kieur timeStamp
        cal.setTimeInMillis(Long.parseLong(timeStamp));
        // định dạng về kiểu string
        String dateTime = DateFormat.format("hh:mm aa",cal).toString();

        if (type.equals("text")){
            //text message
            holder.messageTv.setVisibility(View.VISIBLE);
            holder.messageIv.setVisibility(View.GONE);
            holder.messageTv.setText(message);
        }else {
            //image message
            holder.messageTv.setVisibility(View.GONE);
            holder.messageIv.setVisibility(View.VISIBLE);

            Picasso.get().load(message).placeholder(R.drawable.ic_add_image).into(holder.messageIv);
        }

        //setData
        holder.messageTv.setText(message);
        holder.timeTv.setText(dateTime);

        try {
            Picasso.get().load(imageUrl).into(holder.profileIv);

        }catch (Exception e){

        }

        //click to show delete dialog
        holder.messageLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // show delete dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Xóa tin nhắn");
                builder.setMessage("Bạn có chắc muốn xóa tin nhắn này?");
                //delete button
                builder.setPositiveButton("Xóa", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteMessage(position);
                    }
                });
                //cancel delete  button
                builder.setNegativeButton("Không", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //dissmiss dialog
                        dialogInterface.dismiss();
                    }
                });
                builder.create().show();
            }
        });

        //set Seen/delivered status of message
        if (position == chatList.size()-1){
            if (chatList.get(position).isSeen()){
                holder.isSeenTv.setText("Đã xem");
            }else {
                holder.isSeenTv.setText("Đã gửi");
            }
        }else{
            holder.isSeenTv.setVisibility(View.GONE);
        }

    }

    private void deleteMessage(int x) {

        String myUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        /*-lấy timestamp khi ấn vào tin nhắn
         - so sánh timestamp của tin nhắn đc chọn với tất cả các tin nhắn trong node Chats
        */

        String msgTimeStamp = chatList.get(x).getTimeStamp();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        Query query = dbRef.orderByChild("timeStamp").equalTo(msgTimeStamp);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds: snapshot.getChildren()){
                    if (ds.child("sender").getValue().equals(myUID)){
                        // remove the message from chat
                        ds.getRef().removeValue();
                        //
                        Toast.makeText(context, "Tin nhắn đã được xóa...", Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(context, "Chỉ được xóa tin nhắn của bạn", Toast.LENGTH_SHORT).show();
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
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        //get currently sign in user
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (chatList.get(position).getSender().equals(user.getUid())){
            return MSG_TYPE_RIGHT;
        }else {
            return MSG_TYPE_LEFT;
        }
    }

    //view holder class
    class  MyHolder extends RecyclerView.ViewHolder{

        //view
        CircleImageView profileIv;
        ImageView messageIv;
        TextView messageTv, timeTv, isSeenTv;
        ConstraintLayout messageLayout; // for click listener to show delete

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            //
            profileIv = itemView.findViewById(R.id.profileIv);
            messageTv = itemView.findViewById(R.id.messageTv);
            timeTv = itemView.findViewById(R.id.timeTv);
            isSeenTv = itemView.findViewById(R.id.isSeenTv);
            messageLayout = itemView.findViewById(R.id.messageLayout);
            messageIv = itemView.findViewById(R.id.messageIv);

        }
    }
}
