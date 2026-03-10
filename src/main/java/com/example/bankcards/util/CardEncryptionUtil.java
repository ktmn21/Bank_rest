package com.example.bankcards.util;

import org.springframework.stereotype.Component;

@Component
public class CardEncryptionUtil {

    // Пока для TDD можно сделать "фиктивное" шифрование:
    public String encrypt(String cardNumber) {
        // в реальной реализации тут будет AES и т.п.
        return "enc:" + cardNumber;
    }

    public String decrypt(String encryptedCardNumber) {
        if (encryptedCardNumber != null && encryptedCardNumber.startsWith("enc:")) {
            return encryptedCardNumber.substring(4);
        }
        return encryptedCardNumber;
    }

    public String maskCardNumber(String encryptedCardNumber) {
        String plain = decrypt(encryptedCardNumber);
        if (plain == null || plain.length() < 4) {
            return "****";
        }
        String last4 = plain.substring(plain.length() - 4);
        return "**** **** **** " + last4;
    }
}
