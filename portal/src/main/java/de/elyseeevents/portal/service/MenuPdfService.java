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
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
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
    private static final DeviceRgb DARK = new DeviceRgb(26, 26, 26);
    private static final DeviceRgb MUTED = new DeviceRgb(107, 101, 96);
    private static final DeviceRgb WHITE = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb RULE = new DeviceRgb(215, 210, 200);
    private static final DeviceRgb MEAT = new DeviceRgb(168, 50, 45);
    private static final DeviceRgb VEG = new DeviceRgb(90, 130, 40);
    private static final DeviceRgb HOLIDAY_BG = new DeviceRgb(252, 250, 245);

    private static final DateTimeFormatter DE_LONG = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMANY);
    private static final DateTimeFormatter DE_SHORT = DateTimeFormatter.ofPattern("d. MMMM", Locale.GERMANY);

    private final BavarianHolidayUtil holidayUtil;

    public MenuPdfService(BavarianHolidayUtil holidayUtil) {
        this.holidayUtil = holidayUtil;
    }

    public byte[] generate(WeeklyMenu menu) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
        try (pdf; Document doc = new Document(pdf, PageSize.A4)) {
            doc.setMargins(40, 50, 30, 50);

            Map<String, String> holidays = Map.of();
            try {
                LocalDate monday = LocalDate.parse(menu.getWeekStart());
                holidays = holidayUtil.getHolidaysForWeek(monday, monday.plusDays(4));
            } catch (Exception ignored) {}

            addLogoWatermark(pdf);

            // ── Header ──────────────────────────────────────
            doc.add(new Paragraph("\u00C9lys\u00E9e Events")
                    .setFontSize(30).setFontColor(GOLD)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(12));

            rule(doc);

            int kw = getIsoWeek(menu.getWeekStart());
            doc.add(new Paragraph("Speisekarte  \u00B7  Kalenderwoche " + kw)
                    .setFontSize(11).setFontColor(MUTED)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setCharacterSpacing(0.5f)
                    .setMarginTop(12).setMarginBottom(2));

            doc.add(new Paragraph(formatRange(menu.getWeekStart(), menu.getWeekEnd()))
                    .setFontSize(9).setFontColor(MUTED)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(18));

            // ── Tage ────────────────────────────────────────
            String[] labels = {"Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag"};
            String[] keys   = {"monday", "tuesday", "wednesday", "thursday", "friday"};
            String[] m_     = {menu.getMondayMeat(), menu.getTuesdayMeat(), menu.getWednesdayMeat(), menu.getThursdayMeat(), menu.getFridayMeat()};
            String[] v_     = {menu.getMondayVeg(), menu.getTuesdayVeg(), menu.getWednesdayVeg(), menu.getThursdayVeg(), menu.getFridayVeg()};
            String[] mp_    = {menu.getMondayMeatPrice(), menu.getTuesdayMeatPrice(), menu.getWednesdayMeatPrice(), menu.getThursdayMeatPrice(), menu.getFridayMeatPrice()};
            String[] vp_    = {menu.getMondayVegPrice(), menu.getTuesdayVegPrice(), menu.getWednesdayVegPrice(), menu.getThursdayVegPrice(), menu.getFridayVegPrice()};

            for (int i = 0; i < 5; i++) {
                String holiday = holidays.get(keys[i]);

                // Tag-Name
                doc.add(new Paragraph(labels[i].toUpperCase())
                        .setFontSize(8).setFontColor(GOLD).setBold()
                        .setCharacterSpacing(2.5f)
                        .setMarginTop(i == 0 ? 0 : 16).setMarginBottom(8));

                if (holiday != null) {
                    // Feiertag: hervorgehoben mit Hintergrund und Gold-Rahmen
                    Table holidayBox = new Table(1).useAllAvailableWidth();
                    holidayBox.addCell(new Cell().setBorder(new SolidBorder(GOLD, 0.5f))
                            .setBackgroundColor(HOLIDAY_BG)
                            .setPadding(14)
                            .add(new Paragraph(holiday)
                                    .setFontSize(12).setFontColor(GOLD).setItalic()
                                    .setTextAlignment(TextAlignment.CENTER)));
                    doc.add(holidayBox);
                } else {
                    // Fleisch-Zeile
                    if (m_[i] != null && !m_[i].isEmpty()) {
                        dishLine(doc, "Fleisch / Fisch", m_[i], mp_[i], MEAT);
                    }
                    // Vegetarisch-Zeile
                    if (v_[i] != null && !v_[i].isEmpty()) {
                        dishLine(doc, "Vegetarisch", v_[i], vp_[i], VEG);
                    }
                }

                rule(doc);
            }

            // ── Hinweise ────────────────────────────────────
            if (menu.getNotes() != null && !menu.getNotes().isEmpty()) {
                doc.add(new Paragraph(menu.getNotes())
                        .setFontSize(8.5f).setFontColor(MUTED).setItalic()
                        .setMarginTop(16).setMarginBottom(0));
            }

            // ── Footer ──────────────────────────────────────
            doc.add(new Paragraph(
                    "\u00C9lys\u00E9e Event GmbH  \u00B7  Werner-von-Siemensstrasse 6  \u00B7  86159 Augsburg  \u00B7  Mo\u2013Fr 08\u201314 Uhr")
                    .setFontSize(6.5f).setFontColor(MUTED)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(24));

        } catch (Exception e) {
            throw new RuntimeException("PDF-Generierung fehlgeschlagen", e);
        }
        return baos.toByteArray();
    }

    private void dishLine(Document doc, String label, String dish, String price, DeviceRgb labelColor) {
        // 3-Spalten: Label | Gericht | Preis
        Table row = new Table(UnitValue.createPercentArray(new float[]{18, 62, 20}))
                .useAllAvailableWidth().setMarginTop(0).setMarginBottom(0);

        // Label (Fleisch/Fisch oder Vegetarisch)
        Cell labelCell = new Cell().setBorder(Border.NO_BORDER)
                .setPaddingTop(5).setPaddingBottom(5)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        labelCell.add(new Paragraph(label)
                .setFontSize(7).setFontColor(labelColor).setBold()
                .setCharacterSpacing(0.3f));
        row.addCell(labelCell);

        // Gerichtname
        Cell dishCell = new Cell().setBorder(Border.NO_BORDER)
                .setPaddingTop(5).setPaddingBottom(5)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        dishCell.add(new Paragraph(dish).setFontSize(10.5f).setFontColor(DARK));
        row.addCell(dishCell);

        // Preis
        Cell priceCell = new Cell().setBorder(Border.NO_BORDER)
                .setPaddingTop(5).setPaddingBottom(5)
                .setTextAlignment(TextAlignment.RIGHT)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        if (price != null && !price.isEmpty()) {
            priceCell.add(new Paragraph(price + " \u20ac")
                    .setFontSize(10.5f).setFontColor(GOLD));
        }
        row.addCell(priceCell);

        doc.add(row);
    }

    private void rule(Document doc) {
        Table line = new Table(1).useAllAvailableWidth().setMarginTop(4).setMarginBottom(0);
        line.addCell(new Cell().setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(RULE, 0.5f)).setHeight(1));
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
            gs.setFillOpacity(0.03f);
            gs.setStrokeOpacity(0.03f);
            canvas.setExtGState(gs);

            float pageW = PageSize.A4.getWidth();
            float pageH = PageSize.A4.getHeight();
            float imgW = 300;
            float imgH = imgW * imageData.getHeight() / imageData.getWidth();
            canvas.addImageAt(imageData, (pageW - imgW) / 2, (pageH - imgH) / 2, false);
            canvas.restoreState();
        } catch (Exception ignored) {}
    }

    private String formatRange(String startIso, String endIso) {
        try {
            LocalDate s = LocalDate.parse(startIso);
            LocalDate e = LocalDate.parse(endIso);
            if (s.getMonth() == e.getMonth()) {
                return s.getDayOfMonth() + ".\u2009\u2013\u2009" + e.format(DE_LONG);
            }
            return s.format(DE_SHORT) + "\u2009\u2013\u2009" + e.format(DE_LONG);
        } catch (Exception ex) { return startIso + " \u2013 " + endIso; }
    }

    private int getIsoWeek(String isoDate) {
        try { return LocalDate.parse(isoDate).get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear()); }
        catch (Exception e) { return 0; }
    }
}
