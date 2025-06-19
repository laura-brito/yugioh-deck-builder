package com.example.yugiohdeckbuilder;


import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.yugiohdeckbuilder.api.ApiService;
import com.example.yugiohdeckbuilder.api.RetrofitClient;
import com.example.yugiohdeckbuilder.model.ApiResponse;
import com.example.yugiohdeckbuilder.model.Card;
import com.example.yugiohdeckbuilder.util.LocaleHelper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CardListActivity extends BaseActivity {

    private SearchView searchView;
    private Spinner spinnerCardType;
    private RecyclerView recyclerViewCardSearch;
    private CardSearchAdapter cardSearchAdapter;
    private List<Card> cardList;
    private View emptySearchView;

    // --- LÓGICA DE DECK ---
    private FirebaseFirestore db;
    private Set<Integer> deckCardIds = new HashSet<>();
    private final String LOCAL_USER_ID = "localUser";

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;
    private static final long DEBOUNCE_DELAY_MS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_list);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();
        searchView = findViewById(R.id.search_view_cards);
        spinnerCardType = findViewById(R.id.spinner_card_type);
        recyclerViewCardSearch = findViewById(R.id.recycler_view_card_search);
        emptySearchView = findViewById(R.id.empty_search_view);

        cardList = new ArrayList<>();

        loadDeckCardIdsAndSetupUI();
    }

    private void loadDeckCardIdsAndSetupUI() {
        Task<QuerySnapshot> mainDeckTask = db.collection("users").document(LOCAL_USER_ID).collection("mainDeck").get();
        Task<QuerySnapshot> extraDeckTask = db.collection("users").document(LOCAL_USER_ID).collection("extraDeck").get();
        Task<QuerySnapshot> sideDeckTask = db.collection("users").document(LOCAL_USER_ID).collection("sideDeck").get();

        Tasks.whenAllSuccess(mainDeckTask, extraDeckTask, sideDeckTask).addOnSuccessListener(results -> {
            deckCardIds.clear();
            for (Object result : results) {
                for (QueryDocumentSnapshot document : (QuerySnapshot) result) {
                    Card card = document.toObject(Card.class);
                    deckCardIds.add(card.getId());
                }
            }
            // Apenas após carregar os IDs, configure o adapter e os listeners
            setupAdapterAndListeners();
        });
    }

    private void setupAdapterAndListeners() {
        cardSearchAdapter = new CardSearchAdapter(this, cardList, deckCardIds);
        recyclerViewCardSearch.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCardSearch.setAdapter(cardSearchAdapter);
        setupSearchView();
        setupSpinner();
    }


    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                debounceHandler.removeCallbacks(debounceRunnable);
                String selectedType = (String) spinnerCardType.getSelectedItem();
                fetchCards(selectedType, query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = () -> {
                    String selectedType = (String) spinnerCardType.getSelectedItem();
                    fetchCards(selectedType, newText);
                };
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY_MS);
                return true;
            }
        });
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.card_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCardType.setAdapter(adapter);

        spinnerCardType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                debounceHandler.removeCallbacks(debounceRunnable);
                String selectedType = parent.getItemAtPosition(position).toString();
                String currentQuery = searchView.getQuery().toString();
                fetchCards(selectedType, currentQuery);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void fetchCards(String type, String fname) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<ApiResponse> call;

        String lang = LocaleHelper.getLanguage(this);
        if (lang.equals("pt")) {
            call = apiService.getCardsByLanguage(type, "pt", fname);
        } else {
            call = apiService.getCardsInEnglish(type, fname);
        }

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                cardList.clear();
                boolean hasData = response.isSuccessful() && response.body() != null && response.body().getData() != null;

                if (hasData) {
                    cardList.addAll(response.body().getData());
                }

                toggleEmptySearchState(!cardList.isEmpty());
                cardSearchAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                cardList.clear();
                toggleEmptySearchState(false);
                cardSearchAdapter.notifyDataSetChanged();
                Toast.makeText(CardListActivity.this, "Erro de rede: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void toggleEmptySearchState(boolean hasData) {
        if (hasData) {
            recyclerViewCardSearch.setVisibility(View.VISIBLE);
            emptySearchView.setVisibility(View.GONE);
        } else {
            recyclerViewCardSearch.setVisibility(View.GONE);
            emptySearchView.setVisibility(View.VISIBLE);
            TextView emptyText = emptySearchView.findViewById(R.id.text_view_empty_state);
            emptyText.setText("Nenhuma carta encontrada.");
        }
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

