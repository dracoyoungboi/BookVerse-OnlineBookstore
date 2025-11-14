package com.bookverse.BookVerse.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class QRCodeService {

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
     * Generate QR code for order
     * @param orderId Order ID
     * @param totalAmount Total amount
     * @return Base64 encoded QR code
     */
    public String generateOrderQRCode(Long orderId, Double totalAmount) {
        String qrText = String.format("Order ID: %d\nTotal Amount: %.2f USD\nStatus: Pending\nBookVerse Online Bookstore", 
                orderId, totalAmount);
        return generateQRCodeBase64(qrText, 300, 300);
    }
}

