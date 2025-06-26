package com.example.yugiohdeckbuilder;


import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
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
    private CardView typeSelectorCardView;
    private TextView selectedTypeTextView;
    private RecyclerView recyclerViewCardSearch;
    private CardSearchAdapter cardSearchAdapter;
    private List<Card> cardList;
    private View emptySearchView;
    private ProgressBar progressBar;

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
        setTitle(R.string.card_list);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();
        searchView = findViewById(R.id.search_view_cards);
        typeSelectorCardView = findViewById(R.id.card_view_type_selector);
        selectedTypeTextView = findViewById(R.id.text_view_selected_type);
        recyclerViewCardSearch = findViewById(R.id.recycler_view_card_search);
        emptySearchView = findViewById(R.id.empty_search_view);
        progressBar = findViewById(R.id.progress_bar_search);

        cardList = new ArrayList<>();

        loadDeckCardIdsAndSetupUI();
    }

    private void loadDeckCardIdsAndSetupUI() {
        // Mostra o loading enquanto os IDs são carregados
        progressBar.setVisibility(View.VISIBLE);
        recyclerViewCardSearch.setVisibility(View.GONE);
        emptySearchView.setVisibility(View.GONE);

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
        setupTypeSelector();

        // Inicia a primeira busca com valores padrão
        fetchCards(selectedTypeTextView.getText().toString(), searchView.getQuery().toString());
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                debounceHandler.removeCallbacks(debounceRunnable);
                fetchCards(selectedTypeTextView.getText().toString(), query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = () -> fetchCards(selectedTypeTextView.getText().toString(), newText);
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY_MS);
                return true;
            }
        });
    }


    private void setupTypeSelector() {
        typeSelectorCardView.setOnClickListener(v -> {
            final String[] cardTypes = getResources().getStringArray(R.array.card_types);
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.select_type_dialog_title))
                    .setItems(cardTypes, (dialog, which) -> {
                        String selectedType = cardTypes[which];
                        selectedTypeTextView.setText(selectedType);
                        fetchCards(selectedType, searchView.getQuery().toString());
                    })
                    .show();
        });
    }


    private void fetchCards(String type, String fname) {
        // Mostra o loading
        progressBar.setVisibility(View.VISIBLE);
        recyclerViewCardSearch.setVisibility(View.GONE);
        emptySearchView.setVisibility(View.GONE);

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
                progressBar.setVisibility(View.GONE); // Esconde o loading
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
                progressBar.setVisibility(View.GONE);
                cardList.clear();
                toggleEmptySearchState(false);
                cardSearchAdapter.notifyDataSetChanged();
                Toast.makeText(CardListActivity.this, getString(R.string.network_error, t.getMessage()), Toast.LENGTH_LONG).show();
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
            emptyText.setText(R.string.empty_search_message);
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