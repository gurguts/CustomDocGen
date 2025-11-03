package com.customsdocgen.customsdocgen.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentData {
    private String contractNumber;        // Номер контракта
    private String contractDate;          // Дата контракта
    private String vehicleNumber;         // Номер автомобиля
    private String weight;                // Вес
    private String consigneeName;         // Название грузополучателя
    private String consigneeAddress;      // Адрес грузополучателя
    private String productNameEn;         // Товар на английском
    private String productNameUk;         // Товар на украинском
    private String receiverName;          // Название получателя
    private String receiverAddress;       // Адрес получателя
    private String batchNumber;           // Номер партии
    private String unloadingPlace;        // Место разгрузки товара
    private String unloadingCountry;      // Страна разгрузки товара
}

