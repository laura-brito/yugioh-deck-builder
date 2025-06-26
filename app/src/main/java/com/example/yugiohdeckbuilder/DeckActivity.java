package com.example.yugiohdeckbuilder;


import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
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
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            getSupportActionBar().setCustomView(R.layout.action_bar_custom_logo);
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
        fab.setOnClickListener(v -> startActivity(new Intent(this, CardListActivity.class)));
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
                String header = getString(R.string.deck_header_format, getString(R.string.main_deck), mainDeckCards.size());
                combinedList.add(header);
                combinedList.addAll(mainDeckCards);
            }
            if (!extraDeckCards.isEmpty()) {
                String header = getString(R.string.deck_header_format, getString(R.string.extra_deck), extraDeckCards.size());
                combinedList.add(header);
                combinedList.addAll(extraDeckCards);
            }
            if (!sideDeckCards.isEmpty()) {
                String header = getString(R.string.deck_header_format, getString(R.string.side_deck), sideDeckCards.size());
                combinedList.add(header);
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
            emptyText.setText(R.string.empty_deck_message);
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
