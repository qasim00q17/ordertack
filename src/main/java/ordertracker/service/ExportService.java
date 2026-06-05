package ordertracker.service;

import java.io.IOException;

public interface ExportService {
    byte[] exportOrdersToCsv(Long userId) throws IOException;
    byte[] exportOrdersToExcel(Long userId) throws IOException;
}