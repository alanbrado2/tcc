package com.upis.app;

public class Moeda {
    //"Bid" representa o valor de compra.
    double bid;
    //"Ask" representa o valor de venda.
    double ask;
    //"pctChange" representa a variação da moeda.
    double pctChange;

    public double getBid() {
        return bid;
    }

    public double getAsk() {
        return ask;
    }

    public double getPctChange() {
        return pctChange;
    }
}
