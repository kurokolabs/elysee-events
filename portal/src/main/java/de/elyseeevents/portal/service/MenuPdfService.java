package de.elyseeevents.portal.service;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.DottedBorder;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Tab;
import com.itextpdf.layout.element.TabStop;
import com.itextpdf.layout.properties.TabAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import de.elyseeevents.portal.model.WeeklyMenu;
import de.elyseeevents.portal.util.BavarianHolidayUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Service
public class MenuPdfService {

    private static final DeviceRgb GOLD = new DeviceRgb(201, 168, 76);
    private static final DeviceRgb GOLD_LIGHT = new DeviceRgb(225, 210, 160);
    private static final DeviceRgb DARK = new DeviceRgb(26, 26, 26);
    private static final DeviceRgb MUTED = new DeviceRgb(140, 135, 128);
    private static final DeviceRgb SURFACE = new DeviceRgb(248, 246, 242);
    private static final DeviceRgb MEAT_RED = new DeviceRgb(168, 50, 45);
    private static final DeviceRgb VEG_GREEN = new DeviceRgb(90, 130, 40);
    private static final DeviceRgb WHITE = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb LINE_LIGHT = new DeviceRgb(225, 220, 210);
    private static final DateTimeFormatter DE_DATE = DateTimeFormatter.ofPattern("dd. MMMM yyyy", Locale.GERMANY);
    private static final DateTimeFormatter DE_SHORT = DateTimeFormatter.ofPattern("dd.MM.", Locale.GERMANY);

    private final BavarianHolidayUtil holidayUtil;

    public MenuPdfService(BavarianHolidayUtil holidayUtil) {
        this.holidayUtil = holidayUtil;
    }

    public byte[] generate(WeeklyMenu menu) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
        try (pdf; Document doc = new Document(pdf, PageSize.A4)) {
            doc.setMargins(36, 48, 30, 48);

            Map<String, String> holidays = Map.of();
            try {
                LocalDate monday = LocalDate.parse(menu.getWeekStart());
                holidays = holidayUtil.getHolidaysForWeek(monday, monday.plusDays(4));
            } catch (Exception ignored) {}

            // Logo-Wasserzeichen (sehr dezent)
            addLogoWatermark(pdf);

            // ── Header ──────────────────────────────────────────
            doc.add(new Paragraph("\u00C9LYS\u00C9E EVENTS")
                    .setFontSize(26).setFontColor(GOLD).setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setCharacterSpacing(4).setMarginBottom(0));

            doc.add(new Paragraph("W O C H E N K A R T E")
                    .setFontSize(8).setFontColor(MUTED).setTextAlignment(TextAlignment.CENTER)
                    .setCharacterSpacing(3).setMarginTop(2).setMarginBottom(10));

            // Feine Goldlinie
            addGoldLine(doc, 0.8f);

            // Dekoratives Ornament
            doc.add(new Paragraph("\u2022  \u2666  \u2022")
                    .setFontSize(7).setFontColor(GOLD_LIGHT).setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(4).setMarginBottom(4));

            addGoldLine(doc, 0.4f);

            // Wochen-Info
            String weekLabel = formatDateShort(menu.getWeekStart()) + " \u2013 " + formatDateLong(menu.getWeekEnd());
            int kw = getIsoWeek(menu.getWeekStart());
            doc.add(new Paragraph("Kalenderwoche " + kw)
                    .setFontSize(12).setFontColor(DARK).setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(8).setMarginBottom(1));
            doc.add(new Paragraph(weekLabel)
                    .setFontSize(9).setFontColor(MUTED).setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(14));

            // ── Tage ────────────────────────────────────────────
            String[] dayLabels = {"Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag"};
            String[] dayKeys = {"monday", "tuesday", "wednesday", "thursday", "friday"};
            String[] meats = {menu.getMondayMeat(), menu.getTuesdayMeat(), menu.getWednesdayMeat(), menu.getThursdayMeat(), menu.getFridayMeat()};
            String[] vegs = {menu.getMondayVeg(), menu.getTuesdayVeg(), menu.getWednesdayVeg(), menu.getThursdayVeg(), menu.getFridayVeg()};
            String[] meatPrices = {menu.getMondayMeatPrice(), menu.getTuesdayMeatPrice(), menu.getWednesdayMeatPrice(), menu.getThursdayMeatPrice(), menu.getFridayMeatPrice()};
            String[] vegPrices = {menu.getMondayVegPrice(), menu.getTuesdayVegPrice(), menu.getWednesdayVegPrice(), menu.getThursdayVegPrice(), menu.getFridayVegPrice()};

            for (int i = 0; i < 5; i++) {
                String holiday = holidays.get(dayKeys[i]);

                // Tag-Header: eleganter Gold-Balken
                Table dayHeader = new Table(1).useAllAvailableWidth()
                        .setBackgroundColor(GOLD).setMarginTop(i == 0 ? 0 : 8);
                dayHeader.addCell(new Cell().setBorder(Border.NO_BORDER)
                        .setPadding(5).setPaddingLeft(16).setPaddingRight(16)
                        .add(new Paragraph(dayLabels[i].toUpperCase())
                                .setFontSize(7.5f).setFontColor(WHITE).setBold()
                                .setCharacterSpacing(3)));
                doc.add(dayHeader);

                if (holiday != null) {
                    // Feiertag: elegante zentrierte Darstellung
                    Table holidayRow = new Table(1).useAllAvailableWidth()
                            .setBackgroundColor(SURFACE);
                    holidayRow.addCell(new Cell().setBorder(Border.NO_BORDER)
                            .setPadding(12)
                            .add(new Paragraph("\u2605  " + holiday + "  \u2605")
                                    .setFontSize(11).setFontColor(GOLD).setItalic()
                                    .setTextAlignment(TextAlignment.CENTER)));
                    doc.add(holidayRow);
                } else {
                    // Fleisch
                    if (meats[i] != null && !meats[i].isEmpty()) {
                        addDishRow(doc, meats[i], meatPrices[i], MEAT_RED, true);
                    }
                    // Vegetarisch
                    if (vegs[i] != null && !vegs[i].isEmpty()) {
                        addDishRow(doc, vegs[i], vegPrices[i], VEG_GREEN, false);
                    }
                }
            }

            // ── Hinweise ────────────────────────────────────────
            if (menu.getNotes() != null && !menu.getNotes().isEmpty()) {
                doc.add(new Paragraph("").setMarginTop(12));
                Table noteBox = new Table(1).useAllAvailableWidth();
                noteBox.addCell(new Cell().setBorder(Border.NO_BORDER)
                        .setBorderLeft(new SolidBorder(GOLD_LIGHT, 1.5f))
                        .setPadding(8).setPaddingLeft(14)
                        .add(new Paragraph(menu.getNotes())
                                .setFontSize(8.5f).setFontColor(MUTED).setItalic()));
                doc.add(noteBox);
            }

            // ── Footer ──────────────────────────────────────────
            doc.add(new Paragraph("").setMarginTop(14));
            addGoldLine(doc, 0.4f);
            doc.add(new Paragraph("\u00C9lys\u00E9e Event GmbH  \u00B7  Werner-von-Siemensstrasse 6  \u00B7  86159 Augsburg")
                    .setFontSize(7).setFontColor(MUTED).setTextAlignment(TextAlignment.CENTER).setMarginTop(6));
            doc.add(new Paragraph("Mo\u2013Fr 08:00\u201314:00 Uhr  \u00B7  verwaltung@elysee-events.de  \u00B7  www.elysee-events.de")
                    .setFontSize(7).setFontColor(MUTED).setTextAlignment(TextAlignment.CENTER));

        } catch (Exception e) {
            throw new RuntimeException("PDF-Generierung fehlgeschlagen", e);
        }
        return baos.toByteArray();
    }

    private void addDishRow(Document doc, String dish, String price, DeviceRgb accentColor, boolean first) {
        Table row = new Table(UnitValue.createPercentArray(new float[]{4, 72, 24}))
                .useAllAvailableWidth().setMarginTop(0);

        // Farbiger Akzent-Punkt links
        Cell dotCell = new Cell().setBorder(Border.NO_BORDER).setPadding(8).setPaddingLeft(16)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        if (!first) dotCell.setBorderTop(new SolidBorder(LINE_LIGHT, 0.3f));
        dotCell.add(new Paragraph("\u25CF").setFontSize(5).setFontColor(accentColor));
        row.addCell(dotCell);

        // Gerichtname mit gepunkteter Linie zum Preis
        Cell dishCell = new Cell().setBorder(Border.NO_BORDER).setPadding(8).setPaddingLeft(0)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        if (!first) dishCell.setBorderTop(new SolidBorder(LINE_LIGHT, 0.3f));
        dishCell.add(new Paragraph(dish).setFontSize(10).setFontColor(DARK));
        row.addCell(dishCell);

        // Preis rechts
        Cell priceCell = new Cell().setBorder(Border.NO_BORDER).setPadding(8).setPaddingRight(16)
                .setTextAlignment(TextAlignment.RIGHT).setVerticalAlignment(VerticalAlignment.MIDDLE);
        if (!first) priceCell.setBorderTop(new SolidBorder(LINE_LIGHT, 0.3f));
        if (price != null && !price.isEmpty()) {
            priceCell.add(new Paragraph(price + " \u20ac").setFontSize(10.5f).setFontColor(GOLD).setBold());
        }
        row.addCell(priceCell);

        doc.add(row);
    }

    private void addGoldLine(Document doc, float width) {
        Table line = new Table(UnitValue.createPercentArray(new float[]{15, 70, 15}))
                .useAllAvailableWidth();
        line.addCell(new Cell().setBorder(Border.NO_BORDER));
        line.addCell(new Cell().setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(GOLD_LIGHT, width)).setHeight(1));
        line.addCell(new Cell().setBorder(Border.NO_BORDER));
        doc.add(line);
    }

    private void addLogoWatermark(PdfDocument pdf) {
        try {
            ClassPathResource logoResource = new ClassPathResource("static/portal/img/elysee-logo.jpg");
            byte[] logoBytes = logoResource.getInputStream().readAllBytes();
            var imageData = ImageDataFactory.create(logoBytes);

            PdfCanvas canvas = new PdfCanvas(pdf.getPage(1));
            canvas.saveState();

            PdfExtGState gs = new PdfExtGState();
            gs.setFillOpacity(0.035f);
            gs.setStrokeOpacity(0.035f);
            canvas.setExtGState(gs);

            float pageW = PageSize.A4.getWidth();
            float pageH = PageSize.A4.getHeight();
            float imgW = 280;
            float imgH = imgW * imageData.getHeight() / imageData.getWidth();
            float x = (pageW - imgW) / 2;
            float y = (pageH - imgH) / 2;

            canvas.addImageAt(imageData, x, y, false);
            canvas.restoreState();
        } catch (Exception e) {
            // Fallback: Text-Wasserzeichen
            try {
                PdfCanvas canvas = new PdfCanvas(pdf.getPage(1));
                canvas.saveState();
                canvas.setFillColor(GOLD);
                PdfExtGState gs = new PdfExtGState();
                gs.setFillOpacity(0.03f);
                canvas.setExtGState(gs);
                canvas.beginText();
                canvas.setFontAndSize(com.itextpdf.kernel.font.PdfFontFactory.createFont(
                        com.itextpdf.io.font.constants.StandardFonts.TIMES_ROMAN), 280);
                float pw = PageSize.A4.getWidth();
                float ph = PageSize.A4.getHeight();
                canvas.setTextMatrix(pw / 2 - 150, ph / 2 - 60);
                canvas.showText("\u00C9E");
                canvas.endText();
                canvas.restoreState();
            } catch (Exception ignored) {}
        }
    }

    private String formatDateLong(String isoDate) {
        try { return LocalDate.parse(isoDate).format(DE_DATE); }
        catch (Exception e) { return isoDate; }
    }

    private String formatDateShort(String isoDate) {
        try { return LocalDate.parse(isoDate).format(DE_SHORT); }
        catch (Exception e) { return isoDate; }
    }

    private int getIsoWeek(String isoDate) {
        try { return LocalDate.parse(isoDate).get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear()); }
        catch (Exception e) { return 0; }
    }
}
