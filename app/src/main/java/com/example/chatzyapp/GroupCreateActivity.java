package com.example.chatzyapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.chatzyapp.fragments.ChatListFragment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupCreateActivity extends AppCompatActivity {

    //phân quyền hằng số
    //permission constants
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;

    //image pick constants
    private static final int IMAGE_PICK_CAMERA_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;
    //permisson arrays
    private String[] cameraPermissions;
    private String[] storagePermissions;
    //pick image uri
    private Uri image_uri = null;

    //actionBar
    private ActionBar actionBar;

    // firebase auth
    private FirebaseAuth firebaseAuth;
    //UI views
    private CircleImageView groupIconIv;
    private EditText groupTitleEt, groupDescriptionEt;
    private FloatingActionButton createGroupBtn;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_create);

        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setTitle("Tạo nhóm chat");

        //init UI views
        groupIconIv = findViewById(R.id.groupIconIv);
        groupTitleEt = findViewById(R.id.groupTitleEt);
        groupDescriptionEt = findViewById(R.id.groupDescriptionEt);
        createGroupBtn = findViewById(R.id.createGroupBtn);

        //khởi tạo mảng đc phân quyền
        cameraPermissions = new  String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new  String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        firebaseAuth = FirebaseAuth.getInstance();
        checkUser();

        //pick image
        groupIconIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showImagePickDialog();
            }
        });
        //handle click event
        createGroupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startCreatingGroup();
            }
        });
    }

    private void startCreatingGroup() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Tạo nhóm");

        //đầu vào là tên nhóm, mô tả nhóm
        String groupTitle = groupTitleEt.getText().toString().trim();
        String groupDescription = groupDescriptionEt.getText().toString().trim();

        // kiểm tra
        if (TextUtils.isEmpty(groupTitle)){
            Toast.makeText(this, "Vui lòng nhập tên nhóm...", Toast.LENGTH_SHORT).show();
            return;// không tiếp tục
        }

        progressDialog.show();

        //timeStamp: for groupicon image, groupId, timeCreated etc
        String g_timestamp = ""+System.currentTimeMillis();
        if (image_uri == null){
            //creating group without icon image
            createGroup(
                    ""+g_timestamp,
                    ""+groupTitle,
                    ""+groupDescription,
                    ""
            );
        }else{
            //creating group without icon image
            //upload image
            //image name and path
            String fileNameAndPath = "Group_Imgs/" + "image" + g_timestamp;

            StorageReference storageReference = FirebaseStorage.getInstance().getReference(fileNameAndPath);
            storageReference.putFile(image_uri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //image uploaded, get url
                            Task<Uri> p_uriTask = taskSnapshot.getStorage().getDownloadUrl();
                            while (!p_uriTask.isSuccessful());
                            Uri p_dowloadUri = p_uriTask.getResult();
                            if (p_uriTask.isSuccessful()){
                                createGroup(
                                        ""+g_timestamp,
                                        ""+groupTitle,
                                        ""+groupDescription,
                                        ""+p_dowloadUri
                                );
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //failed uploading image
                            progressDialog.dismiss();
                            Toast.makeText(GroupCreateActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void createGroup(String g_timestamp, String groupTitle, String groupDescription, String groupIcon) {
        //setup info of group
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("groupId",""+g_timestamp);
        hashMap.put("groupTitle",""+groupTitle);
        hashMap.put("groupDescription",""+groupDescription);
        hashMap.put("groupIcon",""+groupIcon);
        hashMap.put("timestamp",""+g_timestamp);
        hashMap.put("createdBy",""+ firebaseAuth.getUid());

        //create group
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Groups");
        ref.child(g_timestamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //created successfully

                        //setup member infor ( add current user in group's participants list)
                        HashMap<String, String> hashMap1 = new HashMap<>();
                        hashMap1.put("uid", firebaseAuth.getUid());
                        hashMap1.put("role","creator");
                        hashMap1.put("timestamp", g_timestamp);

                        DatabaseReference ref1 = FirebaseDatabase.getInstance().getReference("Groups");
                        ref1.child(g_timestamp).child("Participants").child(firebaseAuth.getUid())
                                .setValue(hashMap1)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        //participant added
                                        progressDialog.dismiss();
                                        Toast.makeText(GroupCreateActivity.this, "Tạo nhóm thành công", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(GroupCreateActivity.this, DashboardActivity.class));
                                        finish();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        //failer adding participant
                                        progressDialog.dismiss();
                                        Toast.makeText(GroupCreateActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed
                        progressDialog.dismiss();
                        Toast.makeText(GroupCreateActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }) ;
    }

    private void showImagePickDialog() {
        //options to pick image from
        String[] option = {"Camera", "Thư viện ảnh"};
        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //set title & set Item
        builder.setTitle("Chọn ảnh từ: ")
                .setItems(option, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //handle click
                        if (i==0){
                            //camera click
                            if (!checkCameraPermissions()){
                                requestCameraPermissions();
                            }else{
                                pickFromCamera();
                            }
                        }else {
                            //gallery click
                            if (!checkStoragePermissions()){
                                requestStoragePermissions();
                            }else{
                                pickFromGallery();
                            }
                        }
                    }
                }).show();
    }

    private void pickFromGallery(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }
    private void pickFromCamera(){
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.TITLE,"Ảnh nhóm");
        cv.put(MediaStore.Images.Media.DESCRIPTION,"Mô tả ảnh nhóm");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cv);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);
    }

    //METHOD CHECK CAMERA AND GALLERY
    private  boolean checkStoragePermissions(){
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
    }
    private void requestStoragePermissions(){
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }
    private boolean checkCameraPermissions(){
        boolean result = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }
    private void requestCameraPermissions(){
        ActivityCompat.requestPermissions(this, cameraPermissions,CAMERA_REQUEST_CODE);
    }

    // check user
    private void checkUser() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null){
            actionBar.setSubtitle(user.getEmail());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    // phương thức xử lý yêu cầu quyền hệ thống cho phép khi chọn camera hoặc thư viện ảnh
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //handle permission result
        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                if (grantResults.length > 0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && storageAccepted){
                        //permissions allowed
                        pickFromCamera();
                    }else {
                        // both or one is denied
                        Toast.makeText(this, "Bắt buộc cho phép sử dụng máy ảnh", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE:{
                if (grantResults.length > 0){
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (storageAccepted){
                        //permissions allowed
                        pickFromGallery();
                    }else {
                        Toast.makeText(this, "Bắt buộc cho phép sử dụng camera", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    // lấy dữ liệu từ điện thoại
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //handle image pick result
        if (resultCode == RESULT_OK){
            if (requestCode == IMAGE_PICK_GALLERY_CODE){
                //was picked from gallery
                image_uri = data.getData();
                //set to imageView layout
                groupIconIv.setImageURI(image_uri);
            }
            else if(requestCode == IMAGE_PICK_CAMERA_CODE){
                //was picked from camera
                //set to imageView layout
                groupIconIv.setImageURI(image_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}