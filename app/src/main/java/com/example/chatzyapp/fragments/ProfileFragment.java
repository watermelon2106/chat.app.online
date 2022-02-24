package com.example.chatzyapp.fragments;

import static android.app.Activity.RESULT_OK;
import static com.google.firebase.storage.FirebaseStorage.getInstance;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chatzyapp.MainActivity;
import com.example.chatzyapp.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;


public class ProfileFragment extends Fragment {

    //firebase
    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    //storage
    StorageReference storageReference;
    //đường dẫn chứa hình ảnh của hồ sơ người dùng đc tải lên
    String storagePath = "Users_Profile_Cover_Imgs";
    // views from xml
    ImageView coverIv;
    CircleImageView avatarTv;
    TextView  emailTv, phoneTv,nameTv;
    FloatingActionButton fab;

    //progress dialog
    ProgressDialog dialog;

    //hằng số riêng để phân quyền sử dụng
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;

    // mảng quyền được yêu cầu
    String cameraPermissions[];
    String storagePermissions[];

    //uri of pick image
    Uri image_uri;

    //để kiểm tra hồ sơ hoặc ảnh bìa
    String profileOrCoverPhoto;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // init firebase
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("Users");
        storageReference = FirebaseStorage.getInstance().getReference();

        //khởi tạo mảng đc phân quyền
        //cho phép đọc bộ nhớ ngoài của camera
        cameraPermissions = new  String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new  String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //init views
        avatarTv = view.findViewById(R.id.avatarTv);
        coverIv = view.findViewById(R.id.coverIv);
        nameTv = view.findViewById(R.id.nameTv);
        emailTv = view.findViewById(R.id.emailTv);
        phoneTv = view.findViewById(R.id.phoneTv);
        fab = view.findViewById(R.id.fab);

        //init dialog
        dialog = new ProgressDialog(getActivity());


        // Hiển thị thông tin cá nhân
        //lấy dữ liệu ng dùng đăng nhập, truy xuất thông tin người dùng bằng email
        //orderByChild: truy vấn CSDL từng node 1,so sanh với tài khoản email đang đăng nhập là khóa chính
        // tiềm kiếm tất cả các node, khớp với khóa chính sẽ hiển thị thông tin chi tiết của khóa đó
        Query query = databaseReference.orderByChild("email").equalTo(user.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // kiểm tra cho đến khi nhận đc dữ liệu
                for (DataSnapshot ds: snapshot.getChildren()){
                    //get data
                    String name = ""+ds.child("name").getValue();
                    String email = ""+ds.child("email").getValue();
                    String phone = ""+ds.child("phone").getValue();
                    String image = ""+ds.child("image").getValue();
                    String cover = ""+ds.child("cover").getValue();

                    //set data
                    nameTv.setText(name);
                    emailTv.setText(email);
                    phoneTv.setText(phone);
                    try {
                        // if received then set
                        Picasso.get().load(image).into(avatarTv);
                    }catch (Exception e){
                        // image default
                        Picasso.get().load(R.drawable.profile).into(avatarTv);
                    }
                    try {
                        Picasso.get().load(cover).into(coverIv);
                    }catch (Exception e){
                        //
                    }

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //fab button click
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEditProfileDialog();
            }
        });

        return view;
    }
    private boolean checkStoragePermission(){
        //Kiểm tra quyền đc cấp hay chưa
        boolean result = ContextCompat.checkSelfPermission(getActivity(),Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ==(PackageManager.PERMISSION_GRANTED);
        return result;
    }
    private  void requestStoragePermission(){
        //request runtime storage permission(cho phép)
        requestPermissions(storagePermissions,STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        // check storage permission is enabled
        // return true if enabled
        //return false if not enabled
        boolean result = ContextCompat.checkSelfPermission(getActivity(),Manifest.permission.CAMERA)
                ==(PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(getActivity(),Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ==(PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }
    private  void requestCameraPermission(){
        //request runtime storage permission(cho phép)
        requestPermissions(cameraPermissions,CAMERA_REQUEST_CODE);
    }

    //Item chỉnh sửa thông tin
    private void showEditProfileDialog() {
        //option to show dialog
        String options[] = {"Sửa ảnh đại diện","Sửa ảnh nền","Sửa tên","Sửa số điện thoại"};
        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //set title
        builder.setTitle("Lựa chọn: ");
        //set items to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                //handle dialog item clicks
                if (which == 0){
                    //Edit profile click
                    dialog.setMessage("Cập nhật ảnh đại diện");
                    profileOrCoverPhoto = "image";
                    showImagePickDialog();
                }else if (which == 1){
                    //Edit cover clicked
                    dialog.setMessage("Cập nhật ảnh nền");
                    profileOrCoverPhoto = "cover";
                    showImagePickDialog();
                }else if (which == 2){
                    //Edit name clicked
                    dialog.setMessage("Cập nhật tên");
                    // phương thức với khóa chính là "name" tham chiếu để cập nhật dữ liệu trong database
                    showNamePhoneUpdateDialog("name");
                }else if (which == 3){
                    //Edit Phone clicked
                    dialog.setMessage("Cập nhật số điện thoại");
                    showNamePhoneUpdateDialog("phone");
                }
            }
        });
        //create and show dialog
        builder.create().show();

    }

    private void showNamePhoneUpdateDialog(String key) {
        /* tham chiếu "key" có chứa giá trị
         *  "name" là  khóa trong cơ sở dữ liệu người dùng được sử dụng để cập nhật tên người dùng
         *  "phone" là  khóa trong cơ sở dữ liệu người dùng được sử dụng để cập nhật SDT người dùng*/
        //custom dialog
        AlertDialog.Builder builder = new  AlertDialog.Builder(getActivity());
        builder.setTitle("Cập nhật ");
        //set layout of dialog
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(10,10,10,10);
        //add edit text
        EditText editText = new EditText(getActivity());
        editText.setHint("Nhập thông tin"); // edit name or phone
        linearLayout.addView(editText);

        builder.setView(linearLayout);

        //add button in dialog to update
        builder.setPositiveButton("Cập nhật", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                //input text from edit text
                String value = editText.getText().toString().trim();
                //validate if user has entered something or not
                if(!TextUtils.isEmpty(value)){
                    dialog.show();
                    HashMap<String, Object> results = new HashMap<>();
                    results.put(key,value);

                    databaseReference.child(user.getUid()).updateChildren(results)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    //updated
                                    dialog.dismiss();
                                    Toast.makeText(getActivity(), "Đã cập nhật", Toast.LENGTH_SHORT).show();
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //failer
                            dialog.dismiss();
                            Toast.makeText(getActivity(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }else {
                    Toast.makeText(getActivity(), "Vui lòng nhập thông tin", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //add button in dialog to cancel
        builder.setNegativeButton("Hủy", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                dialog.dismiss();
            }
        });
        //creat and show dialog
        builder.create().show();
    }

    private void showImagePickDialog() {
        // hiển thị hộp thoại chứa máy ảnh tùy chọn và thư viện để chọn hình ảnh
        String options[] = {"Camera","Thư viện ảnh"};
        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //set title
        builder.setTitle("Chọn ảnh từ:");
        //set items to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
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

    // yêu cầu s/d thư viện ảnh/ gallery
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //khi người dùng nhấn cho phép hoặc từ chối từ hộp thoại yêu cầu hiển thị
        // xứ lí cho phép và từ chối

        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                // khi chọn ảnh kiểm tra camera  có cho phép hay không
                if (grantResults.length > 0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted){
                        // permission enabled
                        pickFromCamera();
                    }else{
                        // permission denied
                        Toast.makeText(getActivity(), "Vui lòng cho phép sử dụng camera và thư viện ảnh", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE:{
                // khi chọn ảnh kiểm tra camera và thư viện ảnh có cho phép hay không
                if (grantResults.length > 0){
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted){
                        // permission enabled
                        pickFromGallery();
                    }else{
                        // permission denied
                        Toast.makeText(getActivity(), "Vui lòng cho phép sử dụng thư viện ảnh", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }

    }


    //phương thức lấy dữ liệu ảnh từ hệ thống
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //phương thức gọi sau khi chọn ảnh từ camera hoặc thư viện ảnh
        if (resultCode == RESULT_OK){
            if(requestCode == IMAGE_PICK_GALLERY_CODE){
                //get uri of image
                image_uri = data.getData();
                uploadProfileCoverPhoto(image_uri);

            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE){

                uploadProfileCoverPhoto(image_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // upload ảnh lên CSDL
    private void uploadProfileCoverPhoto(Uri uri) {
        //show progress
        dialog.show();
        //đường dẫn và tên của hình ảnh sẽ được lưu trữ trong bộ nhớ firebase
        //eg: Users_Profile_Cover_Imgs/image_123123fda.jpg
        String filePathAndName = storagePath+ ""+ profileOrCoverPhoto +"_"+ user.getUid();

        StorageReference storageReference2nd = storageReference.child(filePathAndName);
        storageReference2nd.putFile(uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // ảnh đc upload lên storage, lấy uri và store trong user database
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        Uri downloadUri = uriTask.getResult();
                        //check
                        if (uriTask.isSuccessful()){
                            // image upload
                            // add/update url in users database
                            HashMap<String, Object> results = new HashMap<>();
                            /* - Tham số đầu tiên là profileOrCoverPhoto có giá trị "image" hoặc "cover"
                            Đó là các khóa trong cơ sở dữ liệu người dùng, nơi url của hình ảnh sẽ được lưu vào một trong số chúng
                            - Tham số thứ hai chứa url của hình ảnh được lưu trữ trong bộ nhớ firebase, url này sẽ được lưu trữ dưới dạng giá trị đối với khóa "image" hoặc "cover" */
                            results.put(profileOrCoverPhoto,downloadUri.toString());

                            databaseReference.child(user.getUid()).updateChildren(results)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            //url in database of user is added success
                                            //dissmiss progress bar
                                            dialog.dismiss();
                                            Toast.makeText(getActivity(), "Ảnh đã được tải lên", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            //error
                                            dialog.dismiss();
                                            Toast.makeText(getActivity(), "Lỗi không thể tải ảnh", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                        }else {
                            //error
                            dialog.dismiss();
                            Toast.makeText(getActivity(), "Lỗi dữ liệu", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        dialog.dismiss();
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void pickFromCamera(){
        //lấy ảnh từ hệ thống camera
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION,"Temp Description");
        //put image uri
        //EXTERNAL_CONTENT_URI : nội dung của ảnh
        // chứa dữ liệu  ảnh
        //getContentResolver: tham chiểu nội dung ảnh
        //uri mã hóa dữ liệu ảnh
        image_uri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);

        //Khởi động camera có sẵn trên điện thoại
        //ACTION_IMAGE_CAPTURE: Nó trả về hình ảnh đã được chụp từ camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        // nhận kết quả trả về từ camera
        startActivityForResult(cameraIntent,IMAGE_PICK_CAMERA_CODE);
    }

    private void pickFromGallery() {
        //pick from gallery
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE);
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main,menu);
        //hide searchview, as we dont need here
        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_settings).setVisible(false);
        //hide menu
        menu.findItem(R.id.action_creat_group).setVisible(false);
        menu.findItem(R.id.action_add_participant).setVisible(false);
        menu.findItem(R.id.action_groupinfo).setVisible(false);
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
        return super.onOptionsItemSelected(item);
    }


}