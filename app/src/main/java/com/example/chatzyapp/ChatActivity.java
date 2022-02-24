package com.example.chatzyapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatzyapp.adapters.AdapterChat;
import com.example.chatzyapp.adapters.AdapterUsers;
import com.example.chatzyapp.models.ModelChat;
import com.example.chatzyapp.models.ModelUser;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    //views from xml
    Toolbar toolbar;
    RecyclerView recyclerView;
    CircleImageView profileIv;
    TextView nameTv, userStatusTv;
    EditText messageEt;
    ImageButton sendBtn, attachBtn, backhome;
    ImageView blockIv;

    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference userDbRef;
    //check use has seen message or not
    ValueEventListener seenListener;
    DatabaseReference userRefForSeen;

    List<ModelChat> chatList;
    AdapterChat adapterChat;

    String hisUid;
    String myUid;
    String hisImage;

    boolean isBlocked = false;

    //permissions constants
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    //image pick constants
    private static final int IMAGE_PICK_CAMERA_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;
    //permission array
    String cameraPermissions[];
    String storagePermissions[];
    //image picked will be samed in this uri
    Uri image_uri = null;
    //
    private boolean notify = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //set fix image is scroll
//        getWindow().setBackgroundDrawableResource(R.drawable.bg_tele);

        //init views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");

        recyclerView = findViewById(R.id.chat_recyclerView);
        profileIv = findViewById(R.id.profileIv);
        nameTv = findViewById(R.id.nameTv);
        userStatusTv = findViewById(R.id.userStatusTv);
        messageEt = findViewById(R.id.messageEt);
        sendBtn = findViewById(R.id.senBtn);
        attachBtn = findViewById(R.id.attachBtn);
        backhome = findViewById(R.id.backhome);
        blockIv = findViewById(R.id.blockIv);

        backhome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        //init permissions arrays
        //khởi tạo mảng đc phân quyền
        cameraPermissions = new  String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new  String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //layout (LinearLayout) for RecyclerView
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        //recycler properties
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        /*Khi nhấp vào người dùng từ danh sách người dùng, sẽ chuyển UID của người dùng đó sang chatActivity bằng cách sử dụng "Intent"
          => lấy uid đó ở  để lấy ảnh, sdt, tên và bắt đầu trò chuyện với người dùng đó*/
        Intent intent = getIntent();
        hisUid = intent.getStringExtra("hisUid");
        //init firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        userDbRef = firebaseDatabase.getReference("Users");
        //tìm kiếm người dùng để lấy thông tin của người dùng đó
        Query userQuery = userDbRef.orderByChild("uid").equalTo(hisUid);
        //lấy ảnh và tên của người dùng lên toolBar
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // kiểm tra cho đến khi nhận được thông tin
                for (DataSnapshot ds: snapshot.getChildren()){
                    //get data
                    String name =""+ds.child("name").getValue();
                    hisImage =""+ds.child("image").getValue();
//                    String typingStatus =""+ds.child("typingTo").getValue();

                    //check typing status
//                    if (typingStatus.equals(myUid)){
//                        userStatusTv.setText("Đang nhắn tin...");
//                    }else {
//
//                    }

                    //get value of onlineStatus
                    String onlineStatus = ""+ds.child("onlineStatus").getValue();
                    if (onlineStatus.equals("online")){
                        userStatusTv.setText("Đang hoạt động");
                    }else {
//                        userStatusTv.setText("offline");
                        //convert timestamp to dd/mm/yyyy hh:mm AM/PM
                        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                        cal.setTimeInMillis(Long.parseLong(onlineStatus));
                        String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa",cal).toString();
                        userStatusTv.setText("Đã xem lần cuối: "+dateTime);
                    }

                    //set data
                    nameTv.setText(name);

                    try {
                        // nhận ảnh và đẩy ảnh lên tool bar
                        Picasso.get().load(hisImage).placeholder(R.drawable.profile).into(profileIv);
                    }catch (Exception e){
                        // set image  default
                        Picasso.get().load(R.drawable.profile).into(profileIv);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //click button send message
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //lấy text từ editText
                String message = messageEt.getText().toString().trim();
                //kiểm tra rỗng
                if (TextUtils.isEmpty(message)){
                    //text rỗng
                    Toast.makeText(ChatActivity.this, "Vui lòng nhập ký tự bất kỳ...", Toast.LENGTH_SHORT).show();
                }else{
                    //text không rỗng
                    sendMessage(message);
                }
            }
        });

//        messageEt.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                if (charSequence.toString().trim().length() == 0){
//                    checkTypingStatus("noOne");
//                }else {
//                    checkOnlineStatus(hisUid);// UID receiver
//                }
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//
//            }
//        });
        //click button to import image
        attachBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //show image pick dialog
                showImagePickDialog();
            }
        });

        blockIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isBlocked){
                    unBlockUser();
                }
                else{
                    blockUser();
                }
            }
        });


        readMessage();
        checkIsBlocked();
        seenMessage();


    }//oncreate


    private void checkIsBlocked() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("BlockedUsers").orderByChild("uid").equalTo(hisUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds: snapshot.getChildren()){
                            if (ds.exists()){
                                blockIv.setImageResource(R.drawable.ic_block);
                                isBlocked = true;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }

    private void blockUser() {
        // put values in hashmap to put db
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("uid", hisUid);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").child(hisUid).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //block success
                        Toast.makeText(ChatActivity.this, "Đã chặn người này", Toast.LENGTH_SHORT).show();
                        blockIv.setImageResource(R.drawable.ic_block);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //block failed
                        Toast.makeText(ChatActivity.this, "Chặn thất bại"+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void unBlockUser() {

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
                                                Toast.makeText(ChatActivity.this, "Bỏ chặn thành công", Toast.LENGTH_SHORT).show();
                                                blockIv.setImageResource(R.drawable.ic_unblock);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(ChatActivity.this, "Bỏ Chặn thất bại"+e.getMessage(), Toast.LENGTH_SHORT).show();
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


    //method status mesage
    private void seenMessage() {
        userRefForSeen = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = userRefForSeen.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds: snapshot.getChildren()){
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)){

                        HashMap<String, Object> hasSeenHashMap = new HashMap<>();
                        hasSeenHashMap.put("isSeen", true);
                        ds.getRef().updateChildren(hasSeenHashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // method readMessage
    private void readMessage() {
        chatList = new ArrayList<>();
        // make node Chats
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                for (DataSnapshot ds: snapshot.getChildren()){
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid) ||
                            chat.getReceiver().equals(hisUid) && chat.getSender().equals(myUid)){
                        chatList.add(chat);
                    }
                    //adapter
                    adapterChat = new AdapterChat(ChatActivity.this, chatList, hisImage);
                    //reset adapter
                    adapterChat.notifyDataSetChanged();
                    //set adapter to recyclerview
                    recyclerView.setAdapter(adapterChat);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // Method SendMessage
    private void sendMessage(String message) {
        /*- Nút "Chats" sẽ được tạo để chứa tất cả các cuộc trò chuyện
            - Bất cứ khi nào người dùng gửi tin nhắn, nó sẽ tạo con mới trong nút "Chats" và con đó sẽ chứa các giá trị của khóa sau
            -sender: UID của người gửi
            -receiver: UID  người nhận
            -message: tin nhắn */
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        //System.currentTimeMillis(): đo khoảng thời gian làm một việc gì đó (chat)
        //Timestamp: lớp mô tả ngày tháng năm và thời gian
        String timestamp = String.valueOf(System.currentTimeMillis());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender",myUid);
        hashMap.put("receiver",hisUid);
        hashMap.put("message",message);
        hashMap.put("timeStamp",timestamp);
        hashMap.put("isSeen",false);
        hashMap.put("type","text");
        // tham chiếu vào database để tạo node "Chats"
        databaseReference.child("Chats").push().setValue(hashMap);

        //reset editText after chat
        messageEt.setText("");

        //create CHATLIST node/child in firebase database
        DatabaseReference chatRef1 = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(myUid)
                .child(hisUid);
        chatRef1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()){
                    chatRef1.child("id").setValue(hisUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        DatabaseReference chatRef2 = FirebaseDatabase.getInstance().getReference("Chatlist")
                .child(hisUid)
                .child(myUid);
        chatRef2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()){
                    chatRef2.child("id").setValue(myUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendImageMessage(Uri image_uri) throws IOException {
        notify = true;

        //progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang gửi ảnh...");
        progressDialog.show();

        String timeStamp = ""+System.currentTimeMillis();

        String fileNameAndPath = "ChatImages/"+"post_"+timeStamp;
        // tạo node Chats chứa tất cả ảnh gửi trên chat
        //get bitmap from image uri
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), image_uri);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] data = baos.toByteArray(); // convert IMAGE to bytes
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(fileNameAndPath);
        ref.putBytes(data)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //images uploaded
                        progressDialog.dismiss();
                        //get url of uploaded image
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        String downloadUri = uriTask.getResult().toString();

                        if (uriTask.isSuccessful()){
                            //add image uri and other info to database
                            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

                            //setup required data
                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("sender", myUid);
                            hashMap.put("receiver",hisUid);
                            hashMap.put("message",downloadUri);
                            hashMap.put("timeStamp",timeStamp);
                            hashMap.put("type", "image");
                            hashMap.put("isSeen",false);

                            //put this data to firebase
                            databaseReference.child("Chats").push().setValue(hashMap);

                            //send notification
                            DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users");
                            database.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    ModelUser user = snapshot.getValue(ModelUser.class);
                                    if (notify){
                                    }
                                    notify = false;
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });

                            //create CHATLIST node/child in firebase database
                            DatabaseReference chatRef1 = FirebaseDatabase.getInstance().getReference("Chatlist")
                                    .child(myUid)
                                    .child(hisUid);
                            chatRef1.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (!snapshot.exists()){
                                        chatRef1.child("id").setValue(hisUid);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });
                            DatabaseReference chatRef2 = FirebaseDatabase.getInstance().getReference("Chatlist")
                                    .child(hisUid)
                                    .child(myUid);
                            chatRef2.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (!snapshot.exists()){
                                        chatRef2.child("id").setValue(myUid);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });



                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //faile upload
                        progressDialog.dismiss();
                    }
                });


    }

    private void checkUserStatus(){
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null){
            // user is signed in stay here
            myUid = user.getUid(); //ng UID dùng hiện tại đang đăng nhập

        }else {
            // user not signed go to login now
            startActivity(new Intent(this,MainActivity.class));
            finish();
        }
    }

    private void checkOnlineStatus(String status){
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("onlineStatus",status);
        //update values of onlineStatus of current user
        dbRef.updateChildren(hashMap);
    }

//    private void checkTypingStatus(String typing){
//        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
//        HashMap<String, Object> hashMap = new HashMap<>();
//        hashMap.put("typingTo",typing);
//        //update values of onlineStatus of current user
//        dbRef.updateChildren(hashMap);
//    }


    //--------------handle check up image-----------------------------------------------------------

    private boolean checkStoragePermission(){
        // check storage permission is enabled
        // return true if enabled
        //return false if not enabled
        boolean result = ContextCompat.checkSelfPermission(ChatActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ==(PackageManager.PERMISSION_GRANTED);
        return result;
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private  void requestStoragePermission(){
        //request runtime storage permission(cho phép)
        requestPermissions(storagePermissions,STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        // check storage permission is enabled
        // return true if enabled
        //return false if not enabled
        boolean result = ContextCompat.checkSelfPermission(ChatActivity.this,Manifest.permission.CAMERA)
                ==(PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(ChatActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ==(PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private  void requestCameraPermission(){
        //request runtime storage permission(cho phép)
        requestPermissions(cameraPermissions,CAMERA_REQUEST_CODE);
    }

    private void showImagePickDialog() {
        // hiển thị hộp thoại chứa máy ảnh tùy chọn và thư viện để chọn hình ảnh
        String options[] = {"Camera","Thư Viện"};
        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
        //set title
        builder.setTitle("Chọn ảnh từ: ");
        //set items to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                //handle dialog item clicks
                if (which == 0){
                    //camera click
                    if (!checkCameraPermission()){
                        requestCameraPermission();
                    }else {
                        pickFromCamera();
                    }
                }else if (which == 1){
                    //Gallery clicked
                    if (!checkStoragePermission()){
                        requestStoragePermission();
                    }else {
                        pickFromGallery();
                    }
                }
            }
        });
        //create and show dialog
        builder.create().show();
    }

    private void pickFromCamera(){
        //Intent of pick image from device from device camera
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION,"Temp Description");
        //put image uri
        image_uri = ChatActivity.this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        //intent to start camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent,IMAGE_PICK_CAMERA_CODE);
    }
    private void pickFromGallery() {
        //pick from gallery
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE);
    }


    //handle permission results (IMAGE)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //khi người dùng nhấn cho phép hoặc từ chối từ hộp thoại yêu cầu hiển thị
        // xứ lí cho phép và từ chối

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                // khi chọn ảnh kiểm tra camera  có cho phép hay không
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted) {
                        // permission enabled
                        pickFromCamera();
                    } else {
                        // permission denied
                        Toast.makeText(ChatActivity.this, "Vui lòng cho phép sử dụng camera và thư viện ảnh", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE: {
                // khi chọn ảnh kiểm tra camera và thư viện ảnh có cho phép hay không
                if (grantResults.length > 0) {
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted) {
                        // permission enabled
                        pickFromGallery();
                    } else {
                        // permission denied
                        Toast.makeText(ChatActivity.this, "Vui lòng cho phép sử dụng thư viện ảnh", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }

    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //phương thức gọi sau khi chọn ảnh từ camera hoặc thư viện ảnh
        if (resultCode == RESULT_OK){
            if(requestCode == IMAGE_PICK_GALLERY_CODE){
                // image is picked from camera, get uri of image
                image_uri = data.getData();
                //use this image uri to up load to firebase storage
                try {
                    sendImageMessage(image_uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE){
                // image is picked from camera, get uri of image
                try {
                    sendImageMessage(image_uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //----------------------------------------------------------------------------------------------

    @Override
    protected void onStart() {
        checkUserStatus();
        //set online
        checkOnlineStatus("online");
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();

        //get timestamp
        String timestamp = String.valueOf(System.currentTimeMillis());

        //get offline with last seen time stamp
        checkOnlineStatus(timestamp);
//        checkTypingStatus("noOne");

        //reset status seen or develired
        userRefForSeen.removeEventListener(seenListener);
    }

    @Override
    protected void onResume() {
        //set online
        checkOnlineStatus("online");
        super.onResume();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        //hide searchview, as we dont need here
        menu.findItem(R.id.action_search).setVisible(false);

        menu.findItem(R.id.action_creat_group).setVisible(false);

        menu.findItem(R.id.action_add_participant).setVisible(false);

        menu.findItem(R.id.action_groupinfo).setVisible(false);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //get item id
        int id = item.getItemId();
        if (id == R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }
}