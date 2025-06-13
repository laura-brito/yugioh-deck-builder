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
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.yugiohdeckbuilder.model.Card;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class CardSearchAdapter extends RecyclerView.Adapter<CardSearchAdapter.CardSearchViewHolder> {

    private final Context context;
    private final List<Card> cardList;
    private final FirebaseFirestore db;
    private final String LOCAL_USER_ID = "localUser";

    public CardSearchAdapter(Context context, List<Card> cardList) {
        this.context = context;
        this.cardList = cardList;
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

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, CardDetailActivity.class);
            intent.putExtra("CARD_DATA", card);
            context.startActivity(intent);
        });

        // O botão de "Add" rápido pode ser mantido ou removido, dependendo da sua preferência.
        // Vamos mantê-lo por conveniência.
        holder.addButton.setOnClickListener(v -> addCardToDeck(card));
    }

    private void addCardToDeck(Card card) {
        db.collection("users").document(LOCAL_USER_ID)
                .collection("deck").document(String.valueOf(card.getId()))
                .set(card)
                .addOnSuccessListener(aVoid -> Toast.makeText(context, R.string.card_added_success, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(context, R.string.card_add_failed, Toast.LENGTH_SHORT).show());
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