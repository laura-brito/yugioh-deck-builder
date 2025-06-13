package com.example.yugiohdeckbuilder;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.yugiohdeckbuilder.model.Card;
import java.util.List;

public class DeckAdapter extends RecyclerView.Adapter<DeckAdapter.DeckViewHolder> {

    private Context context;
    private List<Card> cardList;

    public DeckAdapter(Context context, List<Card> cardList) {
        this.context = context;
        this.cardList = cardList;
    }

    @NonNull
    @Override
    public DeckViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_deck_card, parent, false);
        return new DeckViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeckViewHolder holder, int position) {
        Card card = cardList.get(position);
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
    }

    @Override
    public int getItemCount() {
        return cardList.size();
    }

    public static class DeckViewHolder extends RecyclerView.ViewHolder {
        ImageView cardImageView;
        public DeckViewHolder(@NonNull View itemView) {
            super(itemView);
            cardImageView = itemView.findViewById(R.id.image_view_deck_card);
        }
    }
}