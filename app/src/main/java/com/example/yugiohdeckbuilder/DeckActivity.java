package com.example.yugiohdeckbuilder;


import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.yugiohdeckbuilder.model.Card;
import com.example.yugiohdeckbuilder.util.LocaleHelper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class DeckActivity extends BaseActivity {

    private RecyclerView recyclerViewDeck;
    private DeckAdapter deckAdapter;
    private List<Object> combinedList;
    private FirebaseFirestore db;
    private View emptyDeckView;
    private final String LOCAL_USER_ID = "localUser";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deck);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setLogo(R.drawable.logo);
            getSupportActionBar().setDisplayUseLogoEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        db = FirebaseFirestore.getInstance();
        recyclerViewDeck = findViewById(R.id.recycler_view_deck);
        emptyDeckView = findViewById(R.id.empty_deck_view);
        FloatingActionButton fab = findViewById(R.id.fab_add_card);

        combinedList = new ArrayList<>();
        deckAdapter = new DeckAdapter(this, combinedList);

        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return deckAdapter.getItemViewType(position) == 0 ? 3 : 1;
            }
        });

        recyclerViewDeck.setLayoutManager(layoutManager);
        recyclerViewDeck.setAdapter(deckAdapter);
        fab.setOnClickListener(v -> startActivity(new Intent(DeckActivity.this, CardListActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllDecks();
    }

    private void loadAllDecks() {
        Task<QuerySnapshot> mainDeckTask = db.collection("users").document(LOCAL_USER_ID).collection("mainDeck").get();
        Task<QuerySnapshot> extraDeckTask = db.collection("users").document(LOCAL_USER_ID).collection("extraDeck").get();
        Task<QuerySnapshot> sideDeckTask = db.collection("users").document(LOCAL_USER_ID).collection("sideDeck").get();

        Tasks.whenAllSuccess(mainDeckTask, extraDeckTask, sideDeckTask).addOnSuccessListener(results -> {
            combinedList.clear();

            List<Card> mainDeckCards = ((QuerySnapshot) results.get(0)).toObjects(Card.class);
            List<Card> extraDeckCards = ((QuerySnapshot) results.get(1)).toObjects(Card.class);
            List<Card> sideDeckCards = ((QuerySnapshot) results.get(2)).toObjects(Card.class);

            if (!mainDeckCards.isEmpty()) {
                combinedList.add("Main Deck (" + mainDeckCards.size() + ")");
                combinedList.addAll(mainDeckCards);
            }
            if (!extraDeckCards.isEmpty()) {
                combinedList.add("Extra Deck (" + extraDeckCards.size() + ")");
                combinedList.addAll(extraDeckCards);
            }
            if (!sideDeckCards.isEmpty()) {
                combinedList.add("Side Deck (" + sideDeckCards.size() + ")");
                combinedList.addAll(sideDeckCards);
            }

            toggleEmptyState();
            deckAdapter.notifyDataSetChanged();
        });
    }

    private void toggleEmptyState() {
        if (combinedList.isEmpty()) {
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
