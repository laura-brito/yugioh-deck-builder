package com.example.yugiohdeckbuilder;


import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.yugiohdeckbuilder.model.Card;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class CardSearchAdapter extends RecyclerView.Adapter<CardSearchAdapter.CardSearchViewHolder> {

    private final Context context;
    private final List<Card> cardList;
    private final Set<Integer> deckCardIds; // <-- NOVO
    private final FirebaseFirestore db;
    private final String LOCAL_USER_ID = "localUser";
    private final List<String> EXTRA_DECK_TYPES = Arrays.asList(
            "Fusion Monster", "Link Monster", "Pendulum Effect Fusion Monster",
            "Synchro Monster", "Synchro Pendulum Effect Monster", "Synchro Tuner Monster",
            "XYZ Monster", "XYZ Pendulum Effect Monster"
    );

    public CardSearchAdapter(Context context, List<Card> cardList, Set<Integer> deckCardIds) {
        this.context = context;
        this.cardList = cardList;
        this.deckCardIds = deckCardIds; // <-- NOVO
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public CardSearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_card_search, parent, false);
        return new CardSearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardSearchViewHolder holder, int position) {
        Card card = cardList.get(position);

        holder.cardNameTextView.setText(card.getName());

        if (card.getCardImages() != null && !card.getCardImages().isEmpty()) {
            Glide.with(context)
                    .load(card.getCardImages().get(0).getImageUrl())
                    .placeholder(R.drawable.card_back)
                    .into(holder.cardImageView);
        }

        if (deckCardIds.contains(card.getId())) {
            holder.addButton.setText(R.string.in_deck_button);
            holder.addButton.setEnabled(false);
        } else {
            holder.addButton.setText(R.string.add);
            holder.addButton.setEnabled(true);
            holder.addButton.setOnClickListener(v -> showDeckSelectionDialog(card, position));
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, CardDetailActivity.class);
            intent.putExtra("CARD_DATA", card);
            context.startActivity(intent);
        });
    }

    private void showDeckSelectionDialog(Card card, int position) {
        List<String> options = new ArrayList<>();
        if (EXTRA_DECK_TYPES.contains(card.getType())) {
            options.add(context.getString(R.string.extra_deck));
        } else {
            options.add(context.getString(R.string.main_deck));
        }
        options.add(context.getString(R.string.side_deck));

        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.add_to_deck_title))
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    String selectedOption = options.get(which);
                    String deckCollection;
                    if (selectedOption.equals(context.getString(R.string.main_deck))) deckCollection = "mainDeck";
                    else if (selectedOption.equals(context.getString(R.string.extra_deck))) deckCollection = "extraDeck";
                    else deckCollection = "sideDeck";

                    checkLimitAndAddToDeck(deckCollection, card, position);
                })
                .show();
    }

    private void checkLimitAndAddToDeck(String deckCollection, Card card, int position) {
        CollectionReference deckRef = db.collection("users").document(LOCAL_USER_ID).collection(deckCollection);
        int limit = getDeckLimit(deckCollection);

        deckRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.size() >= limit) {
                String deckName = getTranslatedDeckName(deckCollection);
                Toast.makeText(context, context.getString(R.string.deck_limit_reached, limit, deckName), Toast.LENGTH_LONG).show();
            } else {
                addToDeck(deckRef, card, position);
            }
        });
    }

    private String getTranslatedDeckName(String deckCollection) {
        if (deckCollection == null) return "";
        switch (deckCollection) {
            case "mainDeck":
                return context.getString(R.string.main_deck);
            case "extraDeck":
                return context.getString(R.string.extra_deck);
            case "sideDeck":
                return context.getString(R.string.side_deck);
            default:
                return deckCollection;
        }
    }

    private int getDeckLimit(String deckCollection) {
        switch (deckCollection) {
            case "mainDeck": return 60;
            case "extraDeck":
            case "sideDeck": return 15;
            default: return 0;
        }
    }

    private void addToDeck(CollectionReference deckRef, Card card, int position) {
        deckRef.document(String.valueOf(card.getId())).set(card)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, R.string.card_added_success, Toast.LENGTH_SHORT).show();
                    deckCardIds.add(card.getId());
                    notifyItemChanged(position);
                });
    }

    @Override
    public int getItemCount() {
        return cardList.size();
    }

    public static class CardSearchViewHolder extends RecyclerView.ViewHolder {
        ImageView cardImageView;
        TextView cardNameTextView;
        Button addButton;

        public CardSearchViewHolder(@NonNull View itemView) {
            super(itemView);
            cardImageView = itemView.findViewById(R.id.image_view_search_card_art);
            cardNameTextView = itemView.findViewById(R.id.text_view_search_card_name);
            addButton = itemView.findViewById(R.id.button_add_to_deck);
        }
    }
}