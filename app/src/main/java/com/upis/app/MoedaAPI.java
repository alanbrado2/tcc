package com.upis.app;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface MoedaAPI {
    //O método GET pega um pedaço da URL que o retrofit usa, que seria basicamente o final da URL.
    @GET("json/USD-BRL")
    Call<List<Moeda>> getMoedas ();
    /*Aqui eu especifico que a lista de referência para o que o retrofit puxar
    será dentro da classe moeda, com os objetos declarados lá dentro.
    */
}


