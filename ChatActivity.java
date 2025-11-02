package com.yourpackagename.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.*;
import com.yourpackagename.R;
import com.yourpackagename.databinding.ActivityChatBinding;
import com.yourpackagename.models.ModelChat;
import com.yourpackagename.adapters.AdapterChat;
import com.yourpackagename.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;

    private String receiptUid = "";
    private String myUid = "";
    private String chatPath = "";

    private Uri imageUri = null;
    private AdapterChat adapterChat;
    private ArrayList<ModelChat> chatArrayList;

    // Activity Result Launchers for Camera & Gallery
    private ActivityResultLauncher<String[]> requestCameraPermissions;
    private ActivityResultLauncher<String> requestStoragePermission;
    private ActivityResultLauncher<Intent> cameraActivityResultLauncher;
    private ActivityResultLauncher<Intent> galleryActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        receiptUid = getIntent().getStringExtra("receiptUid");
        myUid = firebaseAuth.getUid();
        chatPath = Utils.chatPath(receiptUid, myUid);

        setupActivityResultLaunchers();

        // Toolbar/back
        binding.toolbarBackBtn.setOnClickListener(v -> finish());

        // Send text message
        binding.sendBtn.setOnClickListener(v -> validateAndSendText());

        // Attach image
        binding.attachFab.setOnClickListener(v -> imagePickDialog());

        loadReceiptDetails();
        loadMessages();
    }

    private void loadReceiptDetails() {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users");
        userRef.child(receiptUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = "" + snapshot.child("name").getValue();
                String profileImageUrl = "" + snapshot.child("profileImageUrl").getValue();
                binding.toolbarTitleTv.setText(name);
                try {
                    Glide.with(ChatActivity.this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_person_grey)
                            .error(R.drawable.ic_person_grey)
                            .into(binding.toolbarProfileIv);
                } catch (Exception ignored) {}
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadMessages() {
        chatArrayList = new ArrayList<>();
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chats").child(chatPath);
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatArrayList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelChat chat = ds.getValue(ModelChat.class);
                    chatArrayList.add(chat);
                }
                adapterChat = new AdapterChat(ChatActivity.this, chatArrayList);
                binding.chatRv.setAdapter(adapterChat);
                binding.chatRv.scrollToPosition(chatArrayList.size() - 1);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void validateAndSendText() {
        String msg = binding.messageEt.getText().toString().trim();
        if (TextUtils.isEmpty(msg)) {
            Utils.toast(this, "Enter message to send...");
        } else {
            long timestamp = Utils.getTimestamp();
            sendMessage(Utils.MESSAGE_TYPE_TEXT, msg, timestamp);
            binding.messageEt.setText("");
        }
    }

    private void sendMessage(String messageType, String message, long timestamp) {
        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chats").child(chatPath);
        String messageId = chatRef.push().getKey();

        ModelChat modelChat = new ModelChat(
                messageId, messageType, message, myUid, receiptUid, timestamp
        );

        assert messageId != null;
        chatRef.child(messageId).setValue(modelChat)
                .addOnSuccessListener(unused -> {
                    // Update last seen/chat meta if needed
                })
                .addOnFailureListener(e -> Utils.toast(ChatActivity.this, "Failed: " + e.getMessage()));
    }

    // --- Image Sending Logic ---
    private void imagePickDialog() {
        PopupMenu popupMenu = new PopupMenu(this, binding.attachFab);
        popupMenu.getMenu().add(Menu.NONE, 1, 1, "Camera");
        popupMenu.getMenu().add(Menu.NONE, 2, 2, "Gallery");
        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) { // Camera
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestCameraPermissions.launch(new String[]{Manifest.permission.CAMERA});
                } else {
                    requestCameraPermissions.launch(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE});
                }
            } else if (id == 2) { // Gallery
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pickImageGallery();
                } else {
                    requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }
            return true;
        });
    }

    private void setupActivityResultLaunchers() {
        // Camera Permission
        requestCameraPermissions = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean granted = true;
                    for (Boolean b : result.values()) granted = granted && b;
                    if (granted) pickImageCamera();
                    else Utils.toast(this, "Camera or Storage permission denied!");
                });

        // Storage Permission
        requestStoragePermission = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) pickImageGallery();
                    else Utils.toast(this, "Permission denied!");
                });

        // Camera Intent Result
        cameraActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        uploadToFirebaseStorage();
                    } else {
                        Utils.toast(this, "Cancelled!");
                    }
                });

        // Gallery Intent Result
        galleryActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            imageUri = data.getData();
                            uploadToFirebaseStorage();
                        }
                    } else {
                        Utils.toast(this, "Cancelled!");
                    }
                });
    }

    private void pickImageCamera() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "ChatImage");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Chat Image (Camera)");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private void pickImageGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private void uploadToFirebaseStorage() {
        if (imageUri == null) return;
        progressDialog.setMessage("Uploading image...");
        progressDialog.show();

        long timestamp = Utils.getTimestamp();
        String fileName = "ChatImages/" + timestamp;

        StorageReference storageRef = FirebaseStorage.getInstance().getReference(fileName);
        storageRef.putFile(imageUri)
                .addOnProgressListener(snapshot -> {
                    double progress = 100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount();
                    progressDialog.setMessage("Uploading image: " + (int) progress + "%");
                })
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    progressDialog.dismiss();
                    sendMessage(Utils.MESSAGE_TYPE_IMAGE, uri.toString(), timestamp);
                }))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Utils.toast(this, "Failed to upload image: " + e.getMessage());
                });
    }
}
