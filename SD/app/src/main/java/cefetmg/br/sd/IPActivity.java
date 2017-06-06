package cefetmg.br.sd;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

public class IPActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ip);
    }

    public void onClickSetIP(View view) {
        String value = ((EditText) findViewById(R.id.editIP)).getText().toString();
        Intent intent = new Intent(getBaseContext(), MainActivity.class);
        intent.putExtra("SERVER_IP", value);
        startActivity(intent);
    }
}
