package com.upis.app;

import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends AppCompatActivity {
    static LocalDate data_inicio = LocalDate.now().minusDays(5); //Data atual -5 dias.
    static LocalDate data_fim = LocalDate.now(); //Data atual.
    static DateTimeFormatter data_format = DateTimeFormatter.ofPattern("dd-MM-YYYY"); //Formatação para adequar à request da API.
    static String inicio = data_inicio.format(data_format); //String para compor a previsão_URL como parâmetro.
    static String fim = data_fim.format(data_format); //String para compor a previsão_URL como parâmetro.
    static String previsao_URL = "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/CotacaoDolarPeriodo(dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)?@dataInicial='" + inicio + "'&@dataFinalCotacao='" + fim + "'&$top=100&$format=text/csv&$select=cotacaoCompra";
    String local_Temp; //Local de armazenamento temporário do arquivo baixado para a previsão.
    NumberFormat formatter = new DecimalFormat("#0.00"); //Formatação para os valores double.
    TextView cotac_Compra; //TextView da cotação atual.
    TextView cotac_Venda; //TextView da cotação atual.
    TextView dolar_View; //TextView da conversão.
    TextView previ_View; //TextView da previsão.
    EditText valor_Edit; //Edição do valor inserado pelo usuário para a conversão.
    Button conver_Button; //Botão para executar a conversão.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        conver_Button = (Button) findViewById(R.id.converter); //Atribuo o botão da activity_main ao botão criado.
        conver_Button.setOnClickListener(new View.OnClickListener() { //Faz com que quando o botão seja pressionado, execute a função conversão().
            @Override
            public void onClick(View v) {
                conversao_Func();
            }
        });
        cotacao_Func(); //Aqui é o display inicial da cotação de compra/venda
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build(); //Permite que as webrequests sejam feitas de forma síncrona.
        StrictMode.setThreadPolicy(policy); //Permite que as webrequests sejam feitas de forma síncrona.
        try { //Tratamento para execução da função de previsão, visto que a mesma pode ter uma exceção de URL incorreta.
            previsao_Func();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void cotacao_Func() {
        cotac_Compra = findViewById(R.id.cotacao_compra);
        cotac_Venda = findViewById(R.id.cotacao_venda);
        Retrofit retrofit = new Retrofit.Builder() //Cria o construtor do Retrofit.
                .baseUrl("https://economia.awesomeapi.com.br/") //Restante da url que foi declarada na interface MoedaAPI
                .addConverterFactory(GsonConverterFactory.create()) //O GsonFactory faz a leitura do jSON importado pela API.
                .build(); //Constrói o Json
        MoedaAPI MoedaAPI = retrofit.create(MoedaAPI.class);
        Call<List<Moeda>> call = MoedaAPI.getMoedas();
        call.enqueue(new Callback<List<Moeda>>() {
            @Override
            public void onResponse(Call<List<Moeda>> call, Response<List<Moeda>> response) {
                if (!response.isSuccessful()) { //Caso a resposta da API seja falha, a função retorná nada.
                    return;
                }
                List<Moeda> moedas = response.body(); //Atribuição da lista aos objetos adquiridos pelo corpo da requisição.
                for (Moeda moeda : moedas) {
                    cotac_Compra.setText(formatter.format(moeda.getBid()));
                    cotac_Venda.setText(formatter.format(moeda.getAsk()));
                }
            }

            @Override
            public void onFailure(Call<List<Moeda>> call, Throwable t) {
            }
        });
    }

    public void conversao_Func() {
        dolar_View = findViewById(R.id.dolar);
        valor_Edit = findViewById(R.id.edit_Real);
        Retrofit retrofit = new Retrofit.Builder() //Cria o construtor do Retrofit.
                .baseUrl("https://economia.awesomeapi.com.br/") //Restante da url que foi declarada na interface MoedaAPI
                .addConverterFactory(GsonConverterFactory.create()) //O GsonFactory faz a leitura do jSON importado pela API.
                .build(); //Constrói o Json
        MoedaAPI MoedaAPI = retrofit.create(MoedaAPI.class);
        Call<List<Moeda>> call = MoedaAPI.getMoedas();
        call.enqueue(new Callback<List<Moeda>>() {
            @Override
            public void onResponse(Call<List<Moeda>> call, Response<List<Moeda>> response) {
                if (!response.isSuccessful()) { //Caso a resposta da API seja falha, a função retorná nada.
                    return;
                }
                List<Moeda> moedas = response.body(); //Atribuição da lista aos objetos adquiridos pelo corpo da requisição.
                for (Moeda moeda : moedas) { //Pega o valor inserido pelo usuário e divide pela cotação de compra, depois atribui o resultado à TextView.
                    dolar_View.setText(formatter.format(Double.parseDouble(valor_Edit.getText().toString()) / moeda.getBid()));
                }
            }

            @Override
            public void onFailure(Call<List<Moeda>> call, Throwable t) {
            }
        });
    }

    public File downloadData(String[] args) throws MalformedURLException {
        if (args.length != 0) {
            local_Temp = args[0];
        } else {
            local_Temp = System.getProperty("java.io.tmpdir"); //Diretório temporário para armazenamento do arquivo baixado.
        }
        File filename = new File(local_Temp, "auto-mpg.data");
        BotUtil.downloadPage(new URL(MainActivity.previsao_URL), filename);
        return filename;
    }

    public void previsao_Func() throws MalformedURLException {
        previ_View = findViewById(R.id.previ);
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
        //Especifica que a coluna de regressão será a columnDolar
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
        //Define os dados para validação final (30% da base, sem embaralhar, semente fixa).
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
        String[] line = new String[1];   //  Vetor de entrada (Somente a cotação)
        double[] slice = new double[1];  // Vetor de saída (Somente UM double)
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
            //Seta o valor da TextView para o valor fornecido pela ENCOG como previsão final.
            previ_View.setText(formatter.format(Double.parseDouble(dolarPrevisto)));
        }
    }
}
