package com.example.yugiohdeckbuilder;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.yugiohdeckbuilder.model.Card;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CardDetailActivity extends BaseActivity {

    private Button addRemoveButton;
    private FirebaseFirestore db;
    private Card currentCard;
    private String cardLocation = null;
    private final String LOCAL_USER_ID = "localUser";
    private final List<String> EXTRA_DECK_TYPES = Arrays.asList(
            "Fusion Monster", "Link Monster", "Pendulum Effect Fusion Monster",
            "Synchro Monster", "Synchro Pendulum Effect Monster", "Synchro Tuner Monster",
            "XYZ Monster", "XYZ Pendulum Effect Monster"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_detail);

        setTitle(R.string.card_details);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();
        currentCard = (Card) getIntent().getSerializableExtra("CARD_DATA");
        if (currentCard == null) {
            Toast.makeText(this, R.string.load_card_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        findCardLocation();
    }

    private void setupUI() {
        ImageView cardImageView = findViewById(R.id.image_view_detail_card);
        TextView cardNameTextView = findViewById(R.id.text_view_detail_card_name);
        TextView cardTypeTextView = findViewById(R.id.text_view_detail_card_type);
        TextView cardDescTextView = findViewById(R.id.text_view_detail_card_desc);
        addRemoveButton = findViewById(R.id.button_add_remove_deck);

        cardNameTextView.setText(currentCard.getName());
        cardTypeTextView.setText(currentCard.getType());
        cardDescTextView.setText(currentCard.getDesc());

        if (currentCard.getCardImages() != null && !currentCard.getCardImages().isEmpty()) {
            Glide.with(this).load(currentCard.getCardImages().get(0).getImageUrl()).placeholder(R.drawable.card_back).into(cardImageView);
        }

        addRemoveButton.setOnClickListener(v -> {
            if (cardLocation != null) {
                removeFromDeck();
            } else {
                showDeckSelectionDialog();
            }
        });
    }

    private void findCardLocation() {
        Task<DocumentSnapshot> mainTask = db.collection("users").document(LOCAL_USER_ID).collection("mainDeck").document(String.valueOf(currentCard.getId())).get();
        Task<DocumentSnapshot> extraTask = db.collection("users").document(LOCAL_USER_ID).collection("extraDeck").document(String.valueOf(currentCard.getId())).get();
        Task<DocumentSnapshot> sideTask = db.collection("users").document(LOCAL_USER_ID).collection("sideDeck").document(String.valueOf(currentCard.getId())).get();

        Tasks.whenAllComplete(mainTask, extraTask, sideTask).addOnCompleteListener(task -> {
            if (mainTask.isSuccessful() && mainTask.getResult().exists()) cardLocation = "mainDeck";
            else if (extraTask.isSuccessful() && extraTask.getResult().exists()) cardLocation = "extraDeck";
            else if (sideTask.isSuccessful() && sideTask.getResult().exists()) cardLocation = "sideDeck";
            else cardLocation = null;
            updateButtonState();
        });
    }

    private void updateButtonState() {
        if (cardLocation != null) {
            String translatedDeckName = getTranslatedDeckName(cardLocation);
            addRemoveButton.setText(getString(R.string.remove_from_deck_button, translatedDeckName));
        } else {
            addRemoveButton.setText(R.string.add_to_deck_button);
        }
    }

    private String getTranslatedDeckName(String deckCollection) {
        if (deckCollection == null) return "";
        switch (deckCollection) {
            case "mainDeck":
                return getString(R.string.main_deck);
            case "extraDeck":
                return getString(R.string.extra_deck);
            case "sideDeck":
                return getString(R.string.side_deck);
            default:
                return deckCollection;
        }
    }

    private void showDeckSelectionDialog() {
        List<String> options = new ArrayList<>();
        if (EXTRA_DECK_TYPES.contains(currentCard.getType())) {
            options.add(getString(R.string.extra_deck));
        } else {
            options.add(getString(R.string.main_deck));
        }
        options.add(getString(R.string.side_deck));

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_to_deck_title)
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    String selectedOption = options.get(which);
                    String deckCollection;
                    if (selectedOption.equals(getString(R.string.main_deck))) deckCollection = "mainDeck";
                    else if (selectedOption.equals(getString(R.string.extra_deck))) deckCollection = "extraDeck";
                    else deckCollection = "sideDeck";

                    checkLimitAndAddToDeck(deckCollection);
                })
                .show();
    }

    private void checkLimitAndAddToDeck(String deckCollection) {
        CollectionReference deckRef = db.collection("users").document(LOCAL_USER_ID).collection(deckCollection);
        int limit = getDeckLimit(deckCollection);

        deckRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.size() >= limit) {
                String deckName = getTranslatedDeckName(deckCollection);
                Toast.makeText(this, getString(R.string.deck_limit_reached, limit, deckName), Toast.LENGTH_LONG).show();
            } else {
                addToDeck(deckRef);
            }
        });
    }

    private int getDeckLimit(String deckCollection) {
        switch (deckCollection) {
            case "mainDeck": return 60;
            case "extraDeck":
            case "sideDeck": return 15;
            default: return 0;
        }
    }

    private void addToDeck(CollectionReference deckRef) {
        deckRef.document(String.valueOf(currentCard.getId())).set(currentCard)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.card_added_success, Toast.LENGTH_SHORT).show();
                    cardLocation = deckRef.getId();
                    updateButtonState();
                });
    }

    private void removeFromDeck() {
        if (cardLocation == null) return;
        db.collection("users").document(LOCAL_USER_ID).collection(cardLocation)
                .document(String.valueOf(currentCard.getId())).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.card_removed_success, Toast.LENGTH_SHORT).show();
                    cardLocation = null;
                    updateButtonState();
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