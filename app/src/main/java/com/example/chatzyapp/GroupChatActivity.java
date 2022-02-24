package com.example.chatzyapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
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
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatzyapp.adapters.AdapterGroupChat;
import com.example.chatzyapp.models.ModelGroupChat;
import com.example.chatzyapp.models.ModelUser;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class GroupChatActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    private String groupId, myGroupRole="";

    private Toolbar toolbar;
    private ImageView groupIconIv;
    private TextView groupTitleTv;
    private ImageButton attachBtn,senBtn, backhome;
    private EditText messageEt;
    private RecyclerView chatRv;

    private ArrayList<ModelGroupChat> groupChatList;
    private AdapterGroupChat adapterGroupChat;

    //permissions request constants
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    //image pick constants
    private static final int IMAGE_PICK_CAMERA_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;
    //permission to be request
    String cameraPermissions[];
    String storagePermissions[];
    //uri of pick image
    Uri image_uri = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        toolbar = findViewById(R.id.toolbar);
        groupIconIv = findViewById(R.id.groupIconIv);
        groupTitleTv = findViewById(R.id.groupTitleTv);
        attachBtn = findViewById(R.id.attachBtn);
        messageEt = findViewById(R.id.messageEt);
        senBtn = findViewById(R.id.senBtn);
        chatRv = findViewById(R.id.chatRv);
        backhome = findViewById(R.id.backhome);

        setSupportActionBar(toolbar);

//        getWindow().setBackgroundDrawableResource(R.drawable.bg_tele);

        //get id of the group
        Intent intent = getIntent();
        groupId = intent.getStringExtra("groupId");

        //init required permission
        //khởi tạo mảng đc phân quyền
        cameraPermissions = new  String[]{
                Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        storagePermissions = new  String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        firebaseAuth = FirebaseAuth.getInstance();
        loadGroupInfo();
        loadGroupMessages();
        loadMyGroupRole();
        senBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //input data
                String message = messageEt.getText().toString().trim();
                //validate
                if (TextUtils.isEmpty(message)){
                    //empty
                    Toast.makeText(GroupChatActivity.this, "Tin nhắn không được để trống", Toast.LENGTH_SHORT).show();
                }
                else {
                    //send message
                    sendMessage(message);
                }
            }
        });
        attachBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //pick image from camera/gallery
                showImagePickDialog();
            }
        });

        backhome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }


    private void sendImageMessage(){
        //progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang gửi ảnh...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        String fileNamePath = "ChatImages/"+""+System.currentTimeMillis();
        // tạo node Chats chứa tất cả ảnh gửi trên chat
        StorageReference storageReference = FirebaseStorage.getInstance().getReference(fileNamePath);
        storageReference.putFile(image_uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //images uploaded
                        progressDialog.dismiss();
                        //get url of uploaded image
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        Uri downloadUri = uriTask.getResult();

                        if (uriTask.isSuccessful()){
                            //image url received, save in db

                            //timestamp
                            String timeStamp = ""+System.currentTimeMillis();
                            //setup required data
                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("sender", "" +firebaseAuth.getUid());
                            hashMap.put("message",""+downloadUri);
                            hashMap.put("timeStamp",""+timeStamp);
                            hashMap.put("type", "" + "image");//text/image/file

                            //add indb

                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
                            ref.child(groupId).child("Messages").child(timeStamp)
                                    .setValue(hashMap)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            //message sent
                                            //clear messageEt
                                            messageEt.setText("");
                                            progressDialog.dismiss();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            progressDialog.dismiss();
                                            //message send fail
                                            Toast.makeText(GroupChatActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //faile upload
                        Toast.makeText(GroupChatActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                });
    }

    private void showImagePickDialog() {
        // hiển thị hộp thoại chứa máy ảnh tùy chọn và thư viện để chọn hình ảnh
        String options[] = {"Camera","Thư Viện"};
        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(GroupChatActivity.this);
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

    private void pickFromGallery() {
        //pick from gallery
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {

        //Intent of pick image from device from device camera
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION,"Temp Description");
        //put image uri
        image_uri = GroupChatActivity.this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        //intent to start camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent,IMAGE_PICK_CAMERA_CODE);
    }

    private boolean checkStoragePermission(){
        // check storage permission is enabled
        // return true if enabled
        //return false if not enabled
        boolean result = ContextCompat.checkSelfPermission(GroupChatActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ==(PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private  void requestStoragePermission(){
        //request runtime storage permission(cho phép)
        ActivityCompat.requestPermissions(this,storagePermissions,STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        // check storage permission is enabled
        // return true if enabled
        //return false if not enabled
        boolean result = ContextCompat.checkSelfPermission(GroupChatActivity.this,Manifest.permission.CAMERA)
                ==(PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(GroupChatActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ==(PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private  void requestCameraPermission(){
        //request runtime storage permission(cho phép)
        ActivityCompat.requestPermissions(this,cameraPermissions,CAMERA_REQUEST_CODE);
    }

    private void loadMyGroupRole() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("Participants")
                .orderByChild("uid").equalTo(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds: snapshot.getChildren()){
                            myGroupRole = ""+ds.child("role").getValue();
                            //refresh menu items
                            invalidateOptionsMenu();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadGroupMessages() {
        //init list
        groupChatList = new ArrayList<>();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("Messages").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupChatList.clear();
                for (DataSnapshot ds: snapshot.getChildren()){
                    ModelGroupChat model = ds.getValue(ModelGroupChat.class);
                    groupChatList.add(model);
                }
                //adapter
                adapterGroupChat = new AdapterGroupChat(GroupChatActivity.this,groupChatList);
                //send to recyclerview
                chatRv.setAdapter(adapterGroupChat);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendMessage(String message) {
        //timestamp
        String timeStamp = ""+System.currentTimeMillis();

        //setup message data
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", ""+firebaseAuth.getUid());
        hashMap.put("message", ""+message);
        hashMap.put("timeStamp", ""+timeStamp);
        hashMap.put("type", ""+ "text"); // text/image/file

        //add indb

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(groupId).child("Messages").child(timeStamp)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //message sent
                        //clear messageEt
                        messageEt.setText("");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //message send fail
                        Toast.makeText(GroupChatActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });



    }

    private void loadGroupInfo(){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        // tham chiếu sx thứ tự nhóm theo Id
        ref.orderByChild("groupId").equalTo(groupId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds: snapshot.getChildren()){
                            String groupTitle = ""+ds.child("groupTitle").getValue();
                            String groupDescription = ""+ds.child("groupDescription").getValue();
                            String groupIcon = ""+ds.child("groupIcon").getValue();
                            String timeStamp = ""+ds.child("timeStamp").getValue();
                            String createdBy = ""+ds.child("createdBy").getValue();

                            groupTitleTv.setText(groupTitle);
                            try {
                                Picasso.get().load(groupIcon).placeholder(R.drawable.ic_group_chat).into(groupIconIv);
                            }
                            catch (Exception e){
                                groupIconIv.setImageResource(R.drawable.ic_group_chat);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);

        menu.findItem(R.id.action_settings).setVisible(false);
        menu.findItem(R.id.action_creat_group).setVisible(false);
        menu.findItem(R.id.action_logout).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_add_participant).setVisible(false);

        if (myGroupRole.equals("creator") || myGroupRole.equals("admin")){
            // im admin/creator, show add person option
            menu.findItem(R.id.action_add_participant).setVisible(true);
        }
        else {
            menu.findItem(R.id.action_add_participant).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_participant){
            Intent intent = new Intent(this, GroupParticipantAddActivity.class);
            intent.putExtra("groupId",groupId);
            startActivity(intent);
        }
        else if (id == R.id.action_groupinfo){
            Intent intent = new Intent(this, GroupInfoActivity.class);
            intent.putExtra("groupId",groupId);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK){
            if (requestCode == IMAGE_PICK_GALLERY_CODE){
                image_uri = data.getData();
                sendImageMessage();
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE){
                sendImageMessage();
            }
        }
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
                        Toast.makeText(GroupChatActivity.this, "Vui lòng cho phép sử dụng camera và thư viện ảnh", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE: {
                // khi chọn ảnh kiểm tra camera và thư viện ảnh có cho phép hay không
                if (grantResults.length > 0) {

                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted) {
                        // permission enabled
                        pickFromGallery();
                    } else {
                        // permission denied
                        Toast.makeText(GroupChatActivity.this, "Vui lòng cho phép sử dụng thư viện ảnh", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }

    }
}