package com.example.yugiohdeckbuilder;


import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.yugiohdeckbuilder.api.ApiService;
import com.example.yugiohdeckbuilder.api.RetrofitClient;
import com.example.yugiohdeckbuilder.model.ApiResponse;
import com.example.yugiohdeckbuilder.model.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CardListActivity extends BaseActivity {

    private Spinner spinnerCardType;
    private RecyclerView recyclerViewCardSearch;
    private CardSearchAdapter cardSearchAdapter;
    private List<Card> cardList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_list);

        // Habilita o botão "voltar" na barra de ação
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        spinnerCardType = findViewById(R.id.spinner_card_type);
        recyclerViewCardSearch = findViewById(R.id.recycler_view_card_search);

        cardList = new ArrayList<>();
        cardSearchAdapter = new CardSearchAdapter(this, cardList);

        recyclerViewCardSearch.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewCardSearch.setAdapter(cardSearchAdapter);

        setupSpinner();
    }

    // Trata o clique no botão "voltar"
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Fecha a activity atual e volta para a anterior
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.card_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCardType.setAdapter(adapter);

        spinnerCardType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = parent.getItemAtPosition(position).toString();
                fetchCards(selectedType);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void fetchCards(String type) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<ApiResponse> call;

        if (Locale.getDefault().getLanguage().equals("pt")) {
            call = apiService.getCardsByLanguage(type, "pt");
        } else {
            call = apiService.getCardsInEnglish(type);
        }

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    cardList.clear();
                    cardList.addAll(response.body().getData());
                    cardSearchAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(CardListActivity.this, "Nenhuma carta encontrada.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Toast.makeText(CardListActivity.this, "Erro de rede.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}