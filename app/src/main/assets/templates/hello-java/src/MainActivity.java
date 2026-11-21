package ${PACKAGE};

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnAction = findViewById(R.id.btn_action);
        TextView tvTitle = findViewById(R.id.tv_title);

        btnAction.setOnClickListener(v -> {
            tvTitle.setText("Button Clicked!");
            Toast.makeText(this, "Action triggered successfully!", Toast.LENGTH_SHORT).show();
        });
    }
}
