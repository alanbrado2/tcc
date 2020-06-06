package com.upis.app;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.encog.ConsoleStatusReportable;
import org.encog.bot.BotUtil;
import org.encog.mathutil.error.ErrorCalculation;
import org.encog.mathutil.error.ErrorCalculationMode;
import org.encog.ml.MLRegression;
import org.encog.ml.data.MLData;
import org.encog.ml.data.versatile.NormalizationHelper;
import org.encog.ml.data.versatile.VersatileMLDataSet;
import org.encog.ml.data.versatile.columns.ColumnDefinition;
import org.encog.ml.data.versatile.columns.ColumnType;
import org.encog.ml.data.versatile.sources.CSVDataSource;
import org.encog.ml.data.versatile.sources.VersatileDataSource;
import org.encog.ml.factory.MLMethodFactory;
import org.encog.ml.model.EncogModel;
import org.encog.util.csv.CSVFormat;
import org.encog.util.csv.ReadCSV;


import android.os.Bundle;
import android.os.StrictMode;
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
    static String DATA_URL = "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/CotacaoDolarPeriodo(dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)?@dataInicial='01-06-2020'&@dataFinalCotacao='06-06-2020'&$top=100&$format=text/csv&$select=cotacaoCompra";
    TextView compra_venda;
    TextView dolar;
    TextView previ;
    EditText mEditValor;
    Button mButton;
    String tempPath;

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
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);
        try {
            previsao();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
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
                    double resultado = real / compra; //Variável para realizar a multiplicação.
                    dolar.setText(Double.toString(resultado)); //Manipulação pro display dar certo, com valores double é meio chato.

                }
            }

            @Override
            public void onFailure(Call<List<Moeda>> call, Throwable t) {

            }
        });
    }

    public File downloadData(String[] args) throws MalformedURLException {
        if (args.length != 0) {
            tempPath = args[0];
        } else {
            tempPath = System.getProperty("java.io.tmpdir");
        }

        File filename = new File(tempPath, "auto-mpg.data");
        BotUtil.downloadPage(new URL(MainActivity.DATA_URL), filename);
        System.out.println("Downloading sunspot dataset to: " + filename);
        return filename;
    }

    public void previsao() throws MalformedURLException {
        previ = findViewById(R.id.previ);

        //Seleciona o método de erro (rms = root mean square error)
        ErrorCalculation.setMode(ErrorCalculationMode.RMS);

        //Define o arquivo com os dados de entrada.
        String[] args = new String[0];
        File filename = downloadData(args);

        //Mapeia o arquivo de entrada em um "VersatileDatasource"
        CSVFormat format = new CSVFormat(',', ' ');
        VersatileDataSource source = new CSVDataSource(filename, true, format);
        VersatileMLDataSet data = new VersatileMLDataSet(source);
        data.getNormHelper().setFormat(format);

        //Define o formato do arquivo e especifica a coluna.
        ColumnDefinition columnDolar = data.defineSourceColumn("cotacaoCompra", ColumnType.continuous);

        //Analisa o arquivo.
        data.analyze();

        //Aqui eu especifico que a coluna de regressão será a columnDolar
        data.defineInput(columnDolar);
        data.defineOutput(columnDolar);

        //Cria rede neural do tipo feedfoward.
        EncogModel model = new EncogModel(data);
        model.selectMethod(data, MLMethodFactory.TYPE_FEEDFORWARD);

        //Normaliza os dados, a configuração está como automática a partir do modelo escolhido.
        data.normalize();

        //Define série temporal.
        data.setLeadWindowSize(1);
        data.setLagWindowSize(3);

        //Define os dados para validação final.
        //30% da base, sem embaralhar, semente fixa.
        model.holdBackValidation(0.3, false, 1001);

        //Seleciona o tipo de treinamento de acordo com o modelo.
        model.selectTrainingType(data);

        //Faz o treinamento dos dados com validação cruzada de 3 dobras.
        //Retorna o melhor método encontrado, no caso será BasicNetwork
        MLRegression bestMethod = (MLRegression) model.crossvalidate(3, false);

        //Inicia os parâmetros de normalização.
        NormalizationHelper helper = data.getNormHelper();

        //Formata e lê como csv.
        ReadCSV csv = new ReadCSV(filename, true, format);
        String[] line = new String[1];   //  VETOR DE ENTRADA (SÓ A COTAÇÃO)
        double[] slice = new double[1];  // VETOR DE SAÍDA (SÓ UM DOUBLE!)

        //Pegas as últimas 5 cotações do arquivo.
        MLData input = helper.allocateInputVector(5);
        //Processa a regressão.
        String saida = "";
        while (csv.next()) {  //Lê as próximas linhas do arquivo csv.
            StringBuilder result = new StringBuilder();
            //Pega a cotação do dólar no registro do csv.
            line[0] = csv.get(0);
            //Normaliza a entrada.
            helper.normalizeInputVector(line, slice, false);
            //Calcula a regressão.
            MLData output = bestMethod.compute(input);
            //Captura a coluna 0 do vetor de retorno, que é o valor previsto.
            String dolarPrevisto = helper.denormalizeOutputVectorToString(output)[0];
            //Troca o valor do campo de texto que é nulo para o valor previsto pela framework encog.
            previ.setText(dolarPrevisto);
        }
    }
}
