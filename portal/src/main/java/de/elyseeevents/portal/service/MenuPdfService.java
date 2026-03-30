package de.elyseeevents.portal.service;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import de.elyseeevents.portal.model.WeeklyMenu;
import de.elyseeevents.portal.util.BavarianHolidayUtil;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Service
public class MenuPdfService {

    private static final DeviceRgb GOLD = new DeviceRgb(201, 168, 76);
    private static final DeviceRgb DARK = new DeviceRgb(26, 26, 26);
    private static final DeviceRgb MUTED = new DeviceRgb(107, 101, 96);
    private static final DeviceRgb SURFACE = new DeviceRgb(242, 239, 233);
    private static final DeviceRgb MEAT_RED = new DeviceRgb(192, 57, 43);
    private static final DeviceRgb VEG_GREEN = new DeviceRgb(107, 142, 35);
    private static final DeviceRgb WHITE = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb LINE_GREY = new DeviceRgb(230, 230, 230);
    private static final DateTimeFormatter DE_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY);

    private final BavarianHolidayUtil holidayUtil;

    public MenuPdfService(BavarianHolidayUtil holidayUtil) {
        this.holidayUtil = holidayUtil;
    }

    public byte[] generate(WeeklyMenu menu) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
        try (pdf; Document doc = new Document(pdf, PageSize.A4)) {
            doc.setMargins(30, 40, 30, 40);

            Map<String, String> holidays = Map.of();
            try {
                LocalDate monday = LocalDate.parse(menu.getWeekStart());
                holidays = holidayUtil.getHolidaysForWeek(monday, monday.plusDays(4));
            } catch (Exception ignored) {}

            watermark(pdf);

            // Header
            String weekLabel = formatDate(menu.getWeekStart()) + " \u2013 " + formatDate(menu.getWeekEnd());
            String kwLabel = "KW " + getIsoWeek(menu.getWeekStart());

            doc.add(new Paragraph("\u00C9LYS\u00C9E EVENTS")
                    .setFontSize(24).setFontColor(GOLD).setBold()
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(1));
            doc.add(new Paragraph("Wochenkarte")
                    .setFontSize(9).setFontColor(MUTED).setTextAlignment(TextAlignment.CENTER)
                    .setCharacterSpacing(3).setMarginBottom(4));

            Table line = new Table(1).useAllAvailableWidth().setMarginBottom(4);
            line.addCell(new Cell().setBorder(Border.NO_BORDER).setBorderBottom(new SolidBorder(GOLD, 1.5f)).setHeight(1));
            doc.add(line);

            doc.add(new Paragraph(kwLabel + "  |  " + weekLabel)
                    .setFontSize(11).setFontColor(DARK).setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(12));

            // Tage
            String[] dayLabels = {"Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag"};
            String[] dayKeys = {"monday", "tuesday", "wednesday", "thursday", "friday"};
            String[] meats = {menu.getMondayMeat(), menu.getTuesdayMeat(), menu.getWednesdayMeat(), menu.getThursdayMeat(), menu.getFridayMeat()};
            String[] vegs = {menu.getMondayVeg(), menu.getTuesdayVeg(), menu.getWednesdayVeg(), menu.getThursdayVeg(), menu.getFridayVeg()};
            String[] meatPrices = {menu.getMondayMeatPrice(), menu.getTuesdayMeatPrice(), menu.getWednesdayMeatPrice(), menu.getThursdayMeatPrice(), menu.getFridayMeatPrice()};
            String[] vegPrices = {menu.getMondayVegPrice(), menu.getTuesdayVegPrice(), menu.getWednesdayVegPrice(), menu.getThursdayVegPrice(), menu.getFridayVegPrice()};

            for (int i = 0; i < 5; i++) {
                String holiday = holidays.get(dayKeys[i]);

                // Tag-Header (kompakter)
                Table dayHeader = new Table(1).useAllAvailableWidth()
                        .setBackgroundColor(GOLD).setMarginTop(i == 0 ? 0 : 6);
                dayHeader.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(4).setPaddingLeft(12)
                        .add(new Paragraph(dayLabels[i].toUpperCase())
                                .setFontSize(8).setFontColor(WHITE).setBold()
                                .setCharacterSpacing(2)));
                doc.add(dayHeader);

                if (holiday != null) {
                    Table holidayRow = new Table(1).useAllAvailableWidth().setBackgroundColor(SURFACE);
                    holidayRow.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(10)
                            .add(new Paragraph(holiday)
                                    .setFontSize(12).setFontColor(GOLD).setItalic()
                                    .setTextAlignment(TextAlignment.CENTER)));
                    doc.add(holidayRow);
                } else {
                    if (meats[i] != null && !meats[i].isEmpty()) {
                        addDishRow(doc, "FLEISCH / FISCH", meats[i], meatPrices[i], MEAT_RED, true);
                    }
                    if (vegs[i] != null && !vegs[i].isEmpty()) {
                        addDishRow(doc, "VEGETARISCH", vegs[i], vegPrices[i], VEG_GREEN, false);
                    }
                }
            }

            // Hinweise
            if (menu.getNotes() != null && !menu.getNotes().isEmpty()) {
                doc.add(new Paragraph("").setMarginTop(10));
                Table noteBox = new Table(1).useAllAvailableWidth().setBackgroundColor(SURFACE);
                noteBox.addCell(new Cell().setBorder(Border.NO_BORDER)
                        .setBorderLeft(new SolidBorder(GOLD, 2)).setPadding(8).setPaddingLeft(14)
                        .add(new Paragraph(menu.getNotes())
                                .setFontSize(9).setFontColor(MUTED).setItalic()));
                doc.add(noteBox);
            }

            // Footer
            doc.add(new Paragraph("").setMarginTop(12));
            Table footLine = new Table(1).useAllAvailableWidth();
            footLine.addCell(new Cell().setBorder(Border.NO_BORDER).setBorderTop(new SolidBorder(GOLD, 0.5f)).setHeight(1));
            doc.add(footLine);
            doc.add(new Paragraph("\u00C9lys\u00E9e Event GmbH  \u00B7  Werner-von-Siemensstrasse 6  \u00B7  86159 Augsburg")
                    .setFontSize(7).setFontColor(MUTED).setTextAlignment(TextAlignment.CENTER).setMarginTop(6));
            doc.add(new Paragraph("Mo\u2013Fr 08:00\u201314:00  \u00B7  verwaltung@elysee-events.de  \u00B7  www.elysee-events.de")
                    .setFontSize(7).setFontColor(MUTED).setTextAlignment(TextAlignment.CENTER));

        } catch (Exception e) {
            throw new RuntimeException("PDF-Generierung fehlgeschlagen", e);
        }
        return baos.toByteArray();
    }

    private void addDishRow(Document doc, String label, String dish, String price, DeviceRgb labelColor, boolean first) {
        Table row = new Table(UnitValue.createPercentArray(new float[]{18, 58, 24}))
                .useAllAvailableWidth().setMarginTop(0);

        Cell labelCell = new Cell().setBorder(Border.NO_BORDER).setPadding(7).setPaddingLeft(12)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        if (!first) labelCell.setBorderTop(new SolidBorder(LINE_GREY, 0.5f));
        labelCell.add(new Paragraph(label).setFontSize(6.5f).setFontColor(labelColor).setBold().setCharacterSpacing(0.8f));
        row.addCell(labelCell);

        Cell dishCell = new Cell().setBorder(Border.NO_BORDER).setPadding(7)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        if (!first) dishCell.setBorderTop(new SolidBorder(LINE_GREY, 0.5f));
        dishCell.add(new Paragraph(dish).setFontSize(10).setFontColor(DARK));
        row.addCell(dishCell);

        Cell priceCell = new Cell().setBorder(Border.NO_BORDER).setPadding(7).setPaddingRight(12)
                .setTextAlignment(TextAlignment.RIGHT).setVerticalAlignment(VerticalAlignment.MIDDLE);
        if (!first) priceCell.setBorderTop(new SolidBorder(LINE_GREY, 0.5f));
        if (price != null && !price.isEmpty()) {
            priceCell.add(new Paragraph(price + " \u20ac").setFontSize(11).setFontColor(GOLD).setBold());
        }
        row.addCell(priceCell);

        doc.add(row);
    }

    private void watermark(PdfDocument pdf) {
        try {
            PdfCanvas canvas = new PdfCanvas(pdf.getPage(1));
            canvas.saveState();
            canvas.setFillColor(GOLD);
            PdfExtGState gs = new PdfExtGState();
            gs.setFillOpacity(0.04f);
            canvas.setExtGState(gs);
            canvas.beginText();
            canvas.setFontAndSize(com.itextpdf.kernel.font.PdfFontFactory.createFont(
                    com.itextpdf.io.font.constants.StandardFonts.TIMES_ROMAN), 280);
            float pageW = PageSize.A4.getWidth();
            float pageH = PageSize.A4.getHeight();
            canvas.setTextMatrix(pageW / 2 - 150, pageH / 2 - 60);
            canvas.showText("\u00C9E");
            canvas.endText();
            canvas.restoreState();
        } catch (Exception ignored) {}
    }

    private String formatDate(String isoDate) {
        try { return LocalDate.parse(isoDate).format(DE_DATE); }
        catch (Exception e) { return isoDate; }
    }

    private int getIsoWeek(String isoDate) {
        try { return LocalDate.parse(isoDate).get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear()); }
        catch (Exception e) { return 0; }
    }
}
