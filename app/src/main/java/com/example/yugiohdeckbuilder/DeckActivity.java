package com.example.yugiohdeckbuilder;


import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.yugiohdeckbuilder.model.Card;
import com.example.yugiohdeckbuilder.util.LocaleHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class DeckActivity extends BaseActivity {

    private RecyclerView recyclerViewDeck;
    private DeckAdapter deckAdapter;
    private List<Card> deckList;
    private FirebaseFirestore db;
    private View emptyDeckView;
    private final String LOCAL_USER_ID = "localUser";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deck);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setLogo(R.drawable.yugioh_logo);
            getSupportActionBar().setDisplayUseLogoEnabled(true);
            getSupportActionBar().setTitle(""); // Remove o texto do título
        }

        db = FirebaseFirestore.getInstance();

        recyclerViewDeck = findViewById(R.id.recycler_view_deck);
        emptyDeckView = findViewById(R.id.empty_deck_view);
        FloatingActionButton fab = findViewById(R.id.fab_add_card);

        deckList = new ArrayList<>();
        deckAdapter = new DeckAdapter(this, deckList);

        recyclerViewDeck.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerViewDeck.setAdapter(deckAdapter);

        fab.setOnClickListener(v -> {
            startActivity(new Intent(DeckActivity.this, CardListActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDeck();
    }

    private void loadDeck() {
        db.collection("users").document(LOCAL_USER_ID).collection("deck")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        deckList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            deckList.add(document.toObject(Card.class));
                        }
                        toggleEmptyState();
                        deckAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void toggleEmptyState() {
        if (deckList.isEmpty()) {
            recyclerViewDeck.setVisibility(View.GONE);
            emptyDeckView.setVisibility(View.VISIBLE);
            TextView emptyText = emptyDeckView.findViewById(R.id.text_view_empty_state);
            emptyText.setText("Seu deck está vazio!\nToque no '+' para adicionar cartas.");
        } else {
            recyclerViewDeck.setVisibility(View.VISIBLE);
            emptyDeckView.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
