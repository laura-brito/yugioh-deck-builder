package com.example.yugiohdeckbuilder;


import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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

public class DeckActivity extends AppCompatActivity {

    private RecyclerView recyclerViewDeck;
    private DeckAdapter deckAdapter;
    private List<Card> deckList;
    private FirebaseFirestore db;
    // ID fixo para o usuário local
    private final String LOCAL_USER_ID = "localUser";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deck);

        db = FirebaseFirestore.getInstance();

        recyclerViewDeck = findViewById(R.id.recycler_view_deck);
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
                        deckAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        // Lógica para mostrar a bandeira correta
        MenuItem languageItem = menu.findItem(R.id.action_change_language);
        if (LocaleHelper.getLanguage(this).equals("pt")) {
            languageItem.setIcon(R.drawable.ic_flag_us);
        } else {
            languageItem.setIcon(R.drawable.ic_flag_br);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (id == R.id.action_change_language) {
            // Lógica para trocar o idioma
            if (LocaleHelper.getLanguage(this).equals("pt")) {
                LocaleHelper.setLocale(this, "en");
            } else {
                LocaleHelper.setLocale(this, "pt");
            }
            // Reinicia a activity para aplicar a mudança
            recreate();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}