package com.bookverse.BookVerse.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class QRCodeService {

    @Value("${app.bank.account.number}")
    private String bankAccountNumber;

    @Value("${app.bank.account.holder}")
    private String bankAccountHolder;

    @Value("${app.bank.name}")
    private String bankName;

    @Value("${app.bank.branch}")
    private String bankBranch;

    @Value("${app.bank.qr.code.url}")
    private String bankQRCodeUrl;

    /**
     * Generate QR code as Base64 string
     * @param text Text to encode in QR code
     * @param width Width of QR code image
     * @param height Height of QR code image
     * @return Base64 encoded PNG image
     */
    public String generateQRCodeBase64(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 2);

            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngData);
        } catch (WriterException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generate QR code for order (legacy method - now returns bank QR code URL)
     * @param orderId Order ID
     * @param totalAmount Total amount
     * @return Bank QR code URL or generated QR code
     */
    public String generateOrderQRCode(Long orderId, Double totalAmount) {
        // Return bank QR code URL if available
        if (bankQRCodeUrl != null && !bankQRCodeUrl.trim().isEmpty()) {
            return bankQRCodeUrl;
        }
        
        // Fallback: Generate QR code with bank info
        String qrText = String.format("Order ID: %d\nTotal Amount: %.2f VND\nBank: %s\nAccount: %s\nHolder: %s", 
                orderId, totalAmount, bankName, bankAccountNumber, bankAccountHolder);
        return generateQRCodeBase64(qrText, 300, 300);
    }

    /**
     * Get bank account information
     */
    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public String getBankAccountHolder() {
        return bankAccountHolder;
    }

    public String getBankName() {
        return bankName;
    }

    public String getBankBranch() {
        return bankBranch;
    }

    public String getBankQRCodeUrl() {
        return bankQRCodeUrl;
    }
}

