import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

public class GraphSignals {
    public static void  createWorkbooks(List<Double> doubleSignal, List<Integer> integerSignal) throws java.io.IOException {
        Workbook wb = new HSSFWorkbook();
        Sheet sheet1 = wb.createSheet("Sheet 1");

        // only plot one array
        if(doubleSignal != null && integerSignal == null) {
            Row doubleRow;
            // if array is longer than excel workbook horizontal length, do vertical (max 65536)
            if(doubleSignal.size() > 256) {
                for(int i = 0; i < 65536; i++) {
                    doubleRow = sheet1.createRow(i);
                    doubleRow.createCell(0).setCellValue(doubleSignal.get(i));
                }
                // otherwise, just fill first row
            } else {
                doubleRow = sheet1.createRow(0);
                for (int i = 0; i < 256; i++) {
                    doubleRow.createCell(i).setCellValue(doubleSignal.get(i));
                }
            }
        }
        if(integerSignal != null && doubleSignal == null) {
            Row integerRow;
            // if array is longer than excel workbook horizontal length, do vertical (max 65536)
            if(doubleSignal.size() > 256) {
                for(int i = 0; i < 65536; i++) {
                    integerRow = sheet1.createRow(i);
                    integerRow.createCell(0).setCellValue(doubleSignal.get(i));
                }
                // otherwise, just fill first row
            } else {
                integerRow = sheet1.createRow(0);
                for (int i = 0; i < 256; i++) {
                    integerRow.createCell(i).setCellValue(doubleSignal.get(i));
                }
            }
        }

        try  (OutputStream fileOut = new FileOutputStream("example_signal.xls")) {
            wb.write(fileOut);
        }
    }
}
