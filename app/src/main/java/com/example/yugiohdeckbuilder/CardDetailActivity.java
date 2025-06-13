package com.example.yugiohdeckbuilder;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.yugiohdeckbuilder.model.Card;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class CardDetailActivity extends BaseActivity {

    private ImageView cardImageView;
    private TextView cardNameTextView, cardTypeTextView, cardDescTextView;
    private Button addRemoveButton;
    private FirebaseFirestore db;
    private DocumentReference cardInDeckRef;
    private Card currentCard;
    private boolean isInDeck = false;
    private final String LOCAL_USER_ID = "localUser";

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
            Toast.makeText(this, "Erro ao carregar dados da carta.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        cardInDeckRef = db.collection("users").document(LOCAL_USER_ID)
                .collection("deck").document(String.valueOf(currentCard.getId()));

        setupUI();
        loadCardData();
        checkIfCardIsInDeck();
    }

    private void setupUI() {
        cardImageView = findViewById(R.id.image_view_detail_card);
        cardNameTextView = findViewById(R.id.text_view_detail_card_name);
        cardTypeTextView = findViewById(R.id.text_view_detail_card_type);
        cardDescTextView = findViewById(R.id.text_view_detail_card_desc);
        addRemoveButton = findViewById(R.id.button_add_remove_deck);

        addRemoveButton.setOnClickListener(v -> {
            if (isInDeck) {
                removeFromDeck();
            } else {
                addToDeck();
            }
        });
    }

    private void loadCardData() {
        setTitle(currentCard.getName());
        cardNameTextView.setText(currentCard.getName());
        cardTypeTextView.setText(currentCard.getType());
        cardDescTextView.setText(currentCard.getDesc());

        if (currentCard.getCardImages() != null && !currentCard.getCardImages().isEmpty()) {
            Glide.with(this)
                    .load(currentCard.getCardImages().get(0).getImageUrl())
                    .placeholder(R.drawable.card_back)
                    .into(cardImageView);
        }
    }

    private void checkIfCardIsInDeck() {
        cardInDeckRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                isInDeck = true;
            } else {
                isInDeck = false;
            }
            updateButtonState();
        });
    }

    private void updateButtonState() {
        if (isInDeck) {
            addRemoveButton.setText("Remover do Deck");
        } else {
            addRemoveButton.setText("Adicionar ao Deck");
        }
    }

    private void addToDeck() {
        cardInDeckRef.set(currentCard)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Carta adicionada!", Toast.LENGTH_SHORT).show();
                    isInDeck = true;
                    updateButtonState();
                });
    }

    private void removeFromDeck() {
        cardInDeckRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Carta removida!", Toast.LENGTH_SHORT).show();
                    isInDeck = false;
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
