package com.upis.app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    TextView compra_venda;
    TextView dolar;
    EditText mEditValor;
    Button mButton;

    /*Todas as declarações acima são referentes aos campos definidos
    na nossa activity main
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mButton = (Button) findViewById(R.id.converter); //Aqui eu especifico que o mButton significa o botão que declarei
        mButton.setOnClickListener(new View.OnClickListener() { //Aqui eu especifico que quando o botão for clicado, executar a classe conversão()
            @Override
            public void onClick(View v) {
                conversao();
            }
        });
        compra_venda_display(); //Aqui é o display inicial da cotação de compra/venda

    }


    public void compra_venda_display() {
        compra_venda = findViewById(R.id.cotacao);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://economia.awesomeapi.com.br/") //Restante da url que foi declarada na interface MoedaAPI
                .addConverterFactory(GsonConverterFactory.create()) //Aqui eu puxo a biblioteca do gson factory que faz a leitura do json importado da URL (API)
                .build();
        MoedaAPI MoedaAPI = retrofit.create(MoedaAPI.class);
        Call<List<Moeda>> call = MoedaAPI.getMoedas();
        call.enqueue(new Callback<List<Moeda>>() {
            @Override
            public void onResponse(Call<List<Moeda>> call, Response<List<Moeda>> response) {
                if (!response.isSuccessful()) { //Aqui é basicamente uma checagem para ver se a resposta deu sucesso na requisição get.
                    return;
                }
                List<Moeda> moedas = response.body(); //Aqui eu atribuo a lista de moeda aos objetos que consegui pelo body da API.
                for (Moeda moeda : moedas) {
                    String content = "";
                    content += "Compra: " + moeda.getBid() + "\n"; //Puxa a cotação para compra atual
                    content += "Venda: " + moeda.getAsk(); //Puxa a cotação para venda atual
                    //Para não ter que declarar mais de uma variável, simplesmente fiz um append das requisições acima em uma string só.
                    compra_venda.append(content); //Junta tudo e substitui na textview de compra_venda que foi declarada no activity_main
                }
            }

            @Override
            public void onFailure(Call<List<Moeda>> call, Throwable t) {

            }
        });
    }
    //Mesma coisa da classe de cima porém com algumas diferenças.
    public void conversao() {
        dolar = findViewById(R.id.dolar);
        mEditValor = (EditText) findViewById(R.id.edit_Real);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://economia.awesomeapi.com.br/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        MoedaAPI MoedaAPI = retrofit.create(MoedaAPI.class);
        Call<List<Moeda>> call = MoedaAPI.getMoedas();
        call.enqueue(new Callback<List<Moeda>>() {

            @Override
            public void onResponse(Call<List<Moeda>> call, Response<List<Moeda>> response) {
                if (!response.isSuccessful()) {
                    return;
                }
                List<Moeda> moedas = response.body();
                for (Moeda moeda : moedas) {
                    double compra = moeda.getBid(); //Puxa o valor de compra.
                    double real = Double.parseDouble(mEditValor.getText().toString()); //Puxa o valor que foi inserido pelo usuário
                    double resultado = compra * real; //Variável para realizar a multiplicação.
                    dolar.setText(Double.toString(resultado)); //Manipulação pro display dar certo, com valores double é meio chato.

                }
            }

            @Override
            public void onFailure(Call<List<Moeda>> call, Throwable t) {

            }
        });
    }
}
