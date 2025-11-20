package com.bookverse.BookVerse.service;

import com.bookverse.BookVerse.entity.Book;
import com.bookverse.BookVerse.entity.InventoryTransaction;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Export inventory transactions to Excel
     */
    public byte[] exportInventoryTransactions(List<InventoryTransaction> transactions) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Inventory History");

        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // Create data style
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // Create date style
        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.cloneStyleFrom(dataStyle);
        CreationHelper createHelper = workbook.getCreationHelper();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Transaction ID", "Book Title", "Type", "Quantity", "Stock Before", "Stock After", 
                           "Reason", "Note", "Created By", "Created At", "Reference"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Create data rows
        int rowNum = 1;
        for (InventoryTransaction transaction : transactions) {
            Row row = sheet.createRow(rowNum++);
            
            int colNum = 0;
            row.createCell(colNum++).setCellValue(transaction.getTransactionId() != null ? transaction.getTransactionId() : 0);
            row.createCell(colNum++).setCellValue(transaction.getBook() != null ? transaction.getBook().getTitle() : "N/A");
            row.createCell(colNum++).setCellValue(transaction.getTransactionType() != null ? transaction.getTransactionType() : "");
            row.createCell(colNum++).setCellValue(transaction.getQuantity());
            row.createCell(colNum++).setCellValue(transaction.getStockBefore());
            row.createCell(colNum++).setCellValue(transaction.getStockAfter());
            row.createCell(colNum++).setCellValue(transaction.getReason() != null ? transaction.getReason() : "");
            row.createCell(colNum++).setCellValue(transaction.getNote() != null ? transaction.getNote() : "");
            row.createCell(colNum++).setCellValue(transaction.getCreatedBy() != null ? 
                    (transaction.getCreatedBy().getFullName() != null ? transaction.getCreatedBy().getFullName() : 
                     transaction.getCreatedBy().getUsername()) : "System");
            
            Cell dateCell = row.createCell(colNum++);
            if (transaction.getCreatedAt() != null) {
                dateCell.setCellValue(transaction.getCreatedAt().toString());
            } else {
                dateCell.setCellValue("");
            }
            
            String reference = "";
            if (transaction.getReferenceType() != null && transaction.getReferenceId() != null) {
                if ("ORDER".equals(transaction.getReferenceType())) {
                    reference = "Order #" + transaction.getReferenceId();
                } else {
                    reference = transaction.getReferenceType();
                }
            } else if (transaction.getReferenceType() != null) {
                reference = transaction.getReferenceType();
            }
            row.createCell(colNum++).setCellValue(reference);

            // Apply styles
            for (int i = 0; i < headers.length; i++) {
                Cell cell = row.getCell(i);
                if (cell != null) {
                    if (i == 9) { // Created At column
                        cell.setCellStyle(dateStyle);
                    } else {
                        cell.setCellStyle(dataStyle);
                    }
                }
            }
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            // Set minimum width
            if (sheet.getColumnWidth(i) < 3000) {
                sheet.setColumnWidth(i, 3000);
            }
        }

        // Write to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    /**
     * Export stock overview to Excel
     */
    public byte[] exportStockOverview(List<Book> books) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Stock Overview");

        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // Create data style
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // Create low stock style (red background)
        CellStyle lowStockStyle = workbook.createCellStyle();
        lowStockStyle.cloneStyleFrom(dataStyle);
        lowStockStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        lowStockStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Create out of stock style (red background)
        CellStyle outOfStockStyle = workbook.createCellStyle();
        outOfStockStyle.cloneStyleFrom(dataStyle);
        outOfStockStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        outOfStockStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Book ID", "Title", "Author", "Category", "Price", "Stock", "Status"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Create data rows
        int rowNum = 1;
        for (Book book : books) {
            Row row = sheet.createRow(rowNum++);
            
            int colNum = 0;
            row.createCell(colNum++).setCellValue(book.getBookId() != null ? book.getBookId() : 0);
            row.createCell(colNum++).setCellValue(book.getTitle() != null ? book.getTitle() : "");
            row.createCell(colNum++).setCellValue(book.getAuthor() != null ? book.getAuthor() : "");
            row.createCell(colNum++).setCellValue(book.getCategory() != null && book.getCategory().getName() != null ? 
                    book.getCategory().getName() : "");
            
            Cell priceCell = row.createCell(colNum++);
            if (book.getPrice() != null) {
                priceCell.setCellValue(book.getPrice().doubleValue());
            } else {
                priceCell.setCellValue(0.0);
            }
            
            Cell stockCell = row.createCell(colNum++);
            stockCell.setCellValue(book.getStock());
            
            String status = "";
            if (book.getStock() == 0) {
                status = "Out of Stock";
            } else if (book.getStock() < 10) {
                status = "Low Stock";
            } else {
                status = "In Stock";
            }
            row.createCell(colNum++).setCellValue(status);

            // Apply styles based on stock level
            for (int i = 0; i < headers.length; i++) {
                Cell cell = row.getCell(i);
                if (cell != null) {
                    if (book.getStock() == 0) {
                        cell.setCellStyle(outOfStockStyle);
                    } else if (book.getStock() < 10) {
                        cell.setCellStyle(lowStockStyle);
                    } else {
                        cell.setCellStyle(dataStyle);
                    }
                }
            }
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) < 3000) {
                sheet.setColumnWidth(i, 3000);
            }
        }

        // Write to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    /**
     * Export low stock and out of stock alerts to Excel
     */
    public byte[] exportStockAlerts(List<Book> alertBooks) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Stock Alerts");

        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // Create data style
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // Create low stock style
        CellStyle lowStockStyle = workbook.createCellStyle();
        lowStockStyle.cloneStyleFrom(dataStyle);
        lowStockStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        lowStockStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Create out of stock style
        CellStyle outOfStockStyle = workbook.createCellStyle();
        outOfStockStyle.cloneStyleFrom(dataStyle);
        outOfStockStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        outOfStockStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Book ID", "Title", "Author", "Category", "Price", "Stock", "Alert Type"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Create data rows
        int rowNum = 1;
        for (Book book : alertBooks) {
            Row row = sheet.createRow(rowNum++);
            
            int colNum = 0;
            row.createCell(colNum++).setCellValue(book.getBookId() != null ? book.getBookId() : 0);
            row.createCell(colNum++).setCellValue(book.getTitle() != null ? book.getTitle() : "");
            row.createCell(colNum++).setCellValue(book.getAuthor() != null ? book.getAuthor() : "");
            row.createCell(colNum++).setCellValue(book.getCategory() != null && book.getCategory().getName() != null ? 
                    book.getCategory().getName() : "");
            
            Cell priceCell = row.createCell(colNum++);
            if (book.getPrice() != null) {
                priceCell.setCellValue(book.getPrice().doubleValue());
            } else {
                priceCell.setCellValue(0.0);
            }
            
            Cell stockCell = row.createCell(colNum++);
            stockCell.setCellValue(book.getStock());
            
            String alertType = book.getStock() == 0 ? "Out of Stock" : "Low Stock";
            row.createCell(colNum++).setCellValue(alertType);

            // Apply styles based on stock level
            for (int i = 0; i < headers.length; i++) {
                Cell cell = row.getCell(i);
                if (cell != null) {
                    if (book.getStock() == 0) {
                        cell.setCellStyle(outOfStockStyle);
                    } else {
                        cell.setCellStyle(lowStockStyle);
                    }
                }
            }
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) < 3000) {
                sheet.setColumnWidth(i, 3000);
            }
        }

        // Write to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }
}

