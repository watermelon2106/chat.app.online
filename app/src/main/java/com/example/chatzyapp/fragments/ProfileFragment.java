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
    //???????ng d???n ch???a h??nh ???nh c???a h??? s?? ng?????i d??ng ??c t???i l??n
    String storagePath = "Users_Profile_Cover_Imgs";
    // views from xml
    ImageView coverIv;
    CircleImageView avatarTv;
    TextView  emailTv, phoneTv,nameTv;
    FloatingActionButton fab;

    //progress dialog
    ProgressDialog dialog;

    //h???ng s??? ri??ng ????? ph??n quy???n s??? d???ng
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;

    // m???ng quy???n ???????c y??u c???u
    String cameraPermissions[];
    String storagePermissions[];

    //uri of pick image
    Uri image_uri;

    //????? ki???m tra h??? s?? ho???c ???nh b??a
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

        //kh???i t???o m???ng ??c ph??n quy???n
        //cho ph??p ?????c b??? nh??? ngo??i c???a camera
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


        // Hi???n th??? th??ng tin c?? nh??n
        //l???y d??? li???u ng d??ng ????ng nh???p, truy xu???t th??ng tin ng?????i d??ng b???ng email
        //orderByChild: truy v???n CSDL t???ng node 1,so sanh v???i t??i kho???n email ??ang ????ng nh???p l?? kh??a ch??nh
        // ti???m ki???m t???t c??? c??c node, kh???p v???i kh??a ch??nh s??? hi???n th??? th??ng tin chi ti???t c???a kh??a ????
        Query query = databaseReference.orderByChild("email").equalTo(user.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // ki???m tra cho ?????n khi nh???n ??c d??? li???u
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
        //Ki???m tra quy???n ??c c???p hay ch??a
        boolean result = ContextCompat.checkSelfPermission(getActivity(),Manifest.permission.WRITE_EXTERNAL_STORAGE)
                ==(PackageManager.PERMISSION_GRANTED);
        return result;
    }
    private  void requestStoragePermission(){
        //request runtime storage permission(cho ph??p)
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
        //request runtime storage permission(cho ph??p)
        requestPermissions(cameraPermissions,CAMERA_REQUEST_CODE);
    }

    //Item ch???nh s???a th??ng tin
    private void showEditProfileDialog() {
        //option to show dialog
        String options[] = {"S???a ???nh ?????i di???n","S???a ???nh n???n","S???a t??n","S???a s??? ??i???n tho???i"};
        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //set title
        builder.setTitle("L???a ch???n: ");
        //set items to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                //handle dialog item clicks
                if (which == 0){
                    //Edit profile click
                    dialog.setMessage("C???p nh???t ???nh ?????i di???n");
                    profileOrCoverPhoto = "image";
                    showImagePickDialog();
                }else if (which == 1){
                    //Edit cover clicked
                    dialog.setMessage("C???p nh???t ???nh n???n");
                    profileOrCoverPhoto = "cover";
                    showImagePickDialog();
                }else if (which == 2){
                    //Edit name clicked
                    dialog.setMessage("C???p nh???t t??n");
                    // ph????ng th???c v???i kh??a ch??nh l?? "name" tham chi???u ????? c???p nh???t d??? li???u trong database
                    showNamePhoneUpdateDialog("name");
                }else if (which == 3){
                    //Edit Phone clicked
                    dialog.setMessage("C???p nh???t s??? ??i???n tho???i");
                    showNamePhoneUpdateDialog("phone");
                }
            }
        });
        //create and show dialog
        builder.create().show();

    }

    private void showNamePhoneUpdateDialog(String key) {
        /* tham chi???u "key" c?? ch???a gi?? tr???
         *  "name" l??  kh??a trong c?? s??? d??? li???u ng?????i d??ng ???????c s??? d???ng ????? c???p nh???t t??n ng?????i d??ng
         *  "phone" l??  kh??a trong c?? s??? d??? li???u ng?????i d??ng ???????c s??? d???ng ????? c???p nh???t SDT ng?????i d??ng*/
        //custom dialog
        AlertDialog.Builder builder = new  AlertDialog.Builder(getActivity());
        builder.setTitle("C???p nh???t ");
        //set layout of dialog
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(10,10,10,10);
        //add edit text
        EditText editText = new EditText(getActivity());
        editText.setHint("Nh???p th??ng tin"); // edit name or phone
        linearLayout.addView(editText);

        builder.setView(linearLayout);

        //add button in dialog to update
        builder.setPositiveButton("C???p nh???t", new DialogInterface.OnClickListener() {
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
                                    Toast.makeText(getActivity(), "???? c???p nh???t", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(getActivity(), "Vui l??ng nh???p th??ng tin", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //add button in dialog to cancel
        builder.setNegativeButton("H???y", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                dialog.dismiss();
            }
        });
        //creat and show dialog
        builder.create().show();
    }

    private void showImagePickDialog() {
        // hi???n th??? h???p tho???i ch???a m??y ???nh t??y ch???n v?? th?? vi???n ????? ch???n h??nh ???nh
        String options[] = {"Camera","Th?? vi???n ???nh"};
        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //set title
        builder.setTitle("Ch???n ???nh t???:");
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

    // y??u c???u s/d th?? vi???n ???nh/ gallery
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //khi ng?????i d??ng nh???n cho ph??p ho???c t??? ch???i t??? h???p tho???i y??u c???u hi???n th???
        // x??? l?? cho ph??p v?? t??? ch???i

        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                // khi ch???n ???nh ki???m tra camera  c?? cho ph??p hay kh??ng
                if (grantResults.length > 0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted){
                        // permission enabled
                        pickFromCamera();
                    }else{
                        // permission denied
                        Toast.makeText(getActivity(), "Vui l??ng cho ph??p s??? d???ng camera v?? th?? vi???n ???nh", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
            case STORAGE_REQUEST_CODE:{
                // khi ch???n ???nh ki???m tra camera v?? th?? vi???n ???nh c?? cho ph??p hay kh??ng
                if (grantResults.length > 0){
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted){
                        // permission enabled
                        pickFromGallery();
                    }else{
                        // permission denied
                        Toast.makeText(getActivity(), "Vui l??ng cho ph??p s??? d???ng th?? vi???n ???nh", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }

    }


    //ph????ng th???c l???y d??? li???u ???nh t??? h??? th???ng
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //ph????ng th???c g???i sau khi ch???n ???nh t??? camera ho???c th?? vi???n ???nh
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

    // upload ???nh l??n CSDL
    private void uploadProfileCoverPhoto(Uri uri) {
        //show progress
        dialog.show();
        //???????ng d???n v?? t??n c???a h??nh ???nh s??? ???????c l??u tr??? trong b??? nh??? firebase
        //eg: Users_Profile_Cover_Imgs/image_123123fda.jpg
        String filePathAndName = storagePath+ ""+ profileOrCoverPhoto +"_"+ user.getUid();

        StorageReference storageReference2nd = storageReference.child(filePathAndName);
        storageReference2nd.putFile(uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // ???nh ??c upload l??n storage, l???y uri v?? store trong user database
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        Uri downloadUri = uriTask.getResult();
                        //check
                        if (uriTask.isSuccessful()){
                            // image upload
                            // add/update url in users database
                            HashMap<String, Object> results = new HashMap<>();
                            /* - Tham s??? ?????u ti??n l?? profileOrCoverPhoto c?? gi?? tr??? "image" ho???c "cover"
                            ???? l?? c??c kh??a trong c?? s??? d??? li???u ng?????i d??ng, n??i url c???a h??nh ???nh s??? ???????c l??u v??o m???t trong s??? ch??ng
                            - Tham s??? th??? hai ch???a url c???a h??nh ???nh ???????c l??u tr??? trong b??? nh??? firebase, url n??y s??? ???????c l??u tr??? d?????i d???ng gi?? tr??? ?????i v???i kh??a "image" ho???c "cover" */
                            results.put(profileOrCoverPhoto,downloadUri.toString());

                            databaseReference.child(user.getUid()).updateChildren(results)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            //url in database of user is added success
                                            //dissmiss progress bar
                                            dialog.dismiss();
                                            Toast.makeText(getActivity(), "???nh ???? ???????c t???i l??n", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            //error
                                            dialog.dismiss();
                                            Toast.makeText(getActivity(), "L???i kh??ng th??? t???i ???nh", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                        }else {
                            //error
                            dialog.dismiss();
                            Toast.makeText(getActivity(), "L???i d??? li???u", Toast.LENGTH_SHORT).show();
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
        //l???y ???nh t??? h??? th???ng camera
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION,"Temp Description");
        //put image uri
        //EXTERNAL_CONTENT_URI : n???i dung c???a ???nh
        // ch???a d??? li???u  ???nh
        //getContentResolver: tham chi???u n???i dung ???nh
        //uri m?? h??a d??? li???u ???nh
        image_uri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);

        //Kh???i ?????ng camera c?? s???n tr??n ??i???n tho???i
        //ACTION_IMAGE_CAPTURE: N?? tr??? v??? h??nh ???nh ???? ???????c ch???p t??? camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        // nh???n k???t qu??? tr??? v??? t??? camera
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