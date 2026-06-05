package ordertracker.service.impl;

import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ordertracker.entity.Order;
import ordertracker.repository.OrderRepository;
import ordertracker.service.ExportService;
import ordertracker.util.SecurityUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private final OrderRepository orderRepository;
    private final SecurityUtils   securityUtils;

    private static final String[] HEADERS = {
            "Order Number", "Status", "Total Amount", "Currency",
            "Shipping Address", "Payment Reference", "Tracking Number", "Created At"
    };

    @Override
    @Transactional(readOnly = true)
    public byte[] exportOrdersToCsv(Long userId) throws IOException {
        List<Order> orders = getOrders(userId);

        StringWriter sw = new StringWriter();
        try (CSVWriter writer = new CSVWriter(sw)) {
            writer.writeNext(HEADERS);
            for (Order o : orders) {
                writer.writeNext(toRow(o));
            }
        }
        log.info("CSV export: {} orders for userId={}", orders.size(), userId);
        return sw.toString().getBytes();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportOrdersToExcel(Long userId) throws IOException {
        List<Order> orders = getOrders(userId);

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Orders");

            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Order o : orders) {
                Row row = sheet.createRow(rowNum++);
                String[] data = toRow(o);
                for (int i = 0; i < data.length; i++) {
                    row.createCell(i).setCellValue(data[i] != null ? data[i] : "");
                }
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            log.info("Excel export: {} orders for userId={}", orders.size(), userId);
            return out.toByteArray();
        }
    }

    private List<Order> getOrders(Long userId) {
        if (userId != null && !securityUtils.isAdmin()) {
            userId = securityUtils.getCurrentUserId();
        }
        return userId != null
                ? orderRepository.findByUserId(userId, Pageable.unpaged()).getContent()
                : orderRepository.findAll();
    }

    private String[] toRow(Order o) {
        return new String[]{
                o.getOrderNumber(),
                o.getStatus().name(),
                o.getTotalAmount().toPlainString(),
                o.getCurrency(),
                o.getShippingAddress(),
                o.getPaymentReference(),
                o.getTrackingNumber(),
                o.getCreatedAt() != null ? o.getCreatedAt().toString() : ""
        };
    }
}