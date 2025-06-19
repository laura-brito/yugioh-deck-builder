package com.example.yugiohdeckbuilder;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.yugiohdeckbuilder.model.LanguageItem;
import com.example.yugiohdeckbuilder.util.LocaleHelper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import android.Manifest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends BaseActivity {

    private static final String PREFS_NAME = "UserProfilePrefs";
    private static final String PROFILE_IMAGE_URI = "profileImageUri";
    private final String LOCAL_USER_ID = "localUser";

    private ImageView profileImageView;
    private TextView deckSummaryTextView;
    private Spinner languageSpinner;
    private List<LanguageItem> languageList;
    private boolean isUserAction = false;

    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;
    private Uri tempImageUri;

    // Launcher para pedir permissão da câmera
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    launchCamera();
                } else {
                    Toast.makeText(this, "Permissão da câmera negada.", Toast.LENGTH_SHORT).show();
                }
            });

    // Launcher para abrir a câmera
    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success) {
                    saveProfileImage(tempImageUri);
                }
            });

    // Launcher para abrir a galeria
    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    saveProfileImage(uri);
                }
            }
    );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        profileImageView = findViewById(R.id.image_view_profile);
        deckSummaryTextView = findViewById(R.id.text_view_deck_summary);
        languageSpinner = findViewById(R.id.spinner_language);

        profileImageView.setOnClickListener(v -> showImageSourceDialog());

        initLanguageList();
        setupLanguageSpinner();
        loadProfileData();
    }

    private void initLanguageList() {
        languageList = new ArrayList<>();
        languageList.add(new LanguageItem("English", "en", R.drawable.ic_flag_us));
        languageList.add(new LanguageItem("Português", "pt", R.drawable.ic_flag_br));
    }

    private void setupLanguageSpinner() {
        LanguageSpinnerAdapter adapter = new LanguageSpinnerAdapter(this, languageList);
        languageSpinner.setAdapter(adapter);

        String currentLangCode = LocaleHelper.getLanguage(this);
        for (int i = 0; i < languageList.size(); i++) {
            if (languageList.get(i).getLanguageCode().equals(currentLangCode)) {
                languageSpinner.setSelection(i, false); // false para não disparar onItemSelected
                break;
            }
        }

        languageSpinner.post(() -> languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                LanguageItem selectedLanguage = (LanguageItem) parent.getItemAtPosition(position);
                String selectedLangCode = selectedLanguage.getLanguageCode();

                if (!LocaleHelper.getLanguage(ProfileActivity.this).equals(selectedLangCode)) {
                    LocaleHelper.setLocale(ProfileActivity.this, selectedLangCode);
                    recreate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        }));
    }


    private void showImageSourceDialog() {
        String[] options = {"Tirar Foto", "Escolher da Galeria"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Escolha uma imagem");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) { // Tirar Foto
                checkCameraPermissionAndLaunch();
            } else if (which == 1) { // Escolher da Galeria
                galleryLauncher.launch("image/*");
            }
        });
        builder.show();
    }

    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        File imagePath = new File(getCacheDir(), "images");
        imagePath.mkdirs();
        File newFile = new File(imagePath, "profile_image.jpg");
        tempImageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", newFile);
        cameraLauncher.launch(tempImageUri);
    }

    private void saveProfileImage(Uri imageUri) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PROFILE_IMAGE_URI + LOCAL_USER_ID, imageUri.toString());
        editor.apply();
        loadProfileImage();
    }

    private void loadProfileData() {
        loadProfileImage();
        loadDeckSummary();
    }

    private void loadProfileImage() {
        String uriString = sharedPreferences.getString(PROFILE_IMAGE_URI + LOCAL_USER_ID, null);
        if (uriString != null) {
            Glide.with(this)
                    .load(Uri.parse(uriString))
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(profileImageView);
        }
    }

    private void loadDeckSummary() {
        Task<QuerySnapshot> mainDeckTask = db.collection("users").document(LOCAL_USER_ID).collection("mainDeck").get();
        Task<QuerySnapshot> extraDeckTask = db.collection("users").document(LOCAL_USER_ID).collection("extraDeck").get();
        Task<QuerySnapshot> sideDeckTask = db.collection("users").document(LOCAL_USER_ID).collection("sideDeck").get();

        Tasks.whenAllSuccess(mainDeckTask, extraDeckTask, sideDeckTask).addOnSuccessListener(results -> {
            int mainSize = ((QuerySnapshot) results.get(0)).size();
            int extraSize = ((QuerySnapshot) results.get(1)).size();
            int sideSize = ((QuerySnapshot) results.get(2)).size();
            int totalSize = mainSize + extraSize + sideSize;

            deckSummaryTextView.setText(getString(R.string.deck_summary_format, totalSize));
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}