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
    private String cardLocation = null; // mainDeck, extraDeck, sideDeck, ou null
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

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();
        currentCard = (Card) getIntent().getSerializableExtra("CARD_DATA");
        if (currentCard == null) {
            finish();
            return;
        }

        setupUI();
        loadCardData();
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

    private void loadCardData() {
        // ... (já implementado em setupUI)
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
            addRemoveButton.setText("Remover do " + cardLocation);
        } else {
            addRemoveButton.setText("Adicionar ao Deck");
        }
    }

    private void showDeckSelectionDialog() {
        List<String> options = new ArrayList<>();
        // Regra: Tipos específicos só podem ir no Extra Deck
        if (EXTRA_DECK_TYPES.contains(currentCard.getType())) {
            options.add("Extra Deck");
        } else {
            options.add("Main Deck");
        }
        options.add("Side Deck");

        new AlertDialog.Builder(this)
                .setTitle("Adicionar em qual Deck?")
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    String selectedOption = options.get(which);
                    String deckCollection = "";
                    if (selectedOption.equals("Main Deck")) deckCollection = "mainDeck";
                    else if (selectedOption.equals("Extra Deck")) deckCollection = "extraDeck";
                    else if (selectedOption.equals("Side Deck")) deckCollection = "sideDeck";

                    checkLimitAndAddToDeck(deckCollection);
                })
                .show();
    }

    private void checkLimitAndAddToDeck(String deckCollection) {
        CollectionReference deckRef = db.collection("users").document(LOCAL_USER_ID).collection(deckCollection);
        int limit = getDeckLimit(deckCollection);

        deckRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.size() >= limit) {
                Toast.makeText(this, "Limite do " + deckCollection + " (" + limit + " cartas) atingido!", Toast.LENGTH_LONG).show();
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
                    Toast.makeText(this, "Carta adicionada!", Toast.LENGTH_SHORT).show();
                    cardLocation = deckRef.getId();
                    updateButtonState();
                });
    }

    private void removeFromDeck() {
        if (cardLocation == null) return;
        db.collection("users").document(LOCAL_USER_ID).collection(cardLocation)
                .document(String.valueOf(currentCard.getId())).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Carta removida!", Toast.LENGTH_SHORT).show();
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
