package com.example.yugiohdeckbuilder;
import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import com.example.yugiohdeckbuilder.util.LocaleHelper;

public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        // Aplica o idioma salvo antes de qualquer outra coisa
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }
}
