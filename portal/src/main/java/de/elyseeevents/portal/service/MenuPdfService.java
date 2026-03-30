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

    // Farben exakt wie die Website: --gold, --dark, --muted
    private static final DeviceRgb GOLD = new DeviceRgb(201, 168, 76);
    private static final DeviceRgb DARK = new DeviceRgb(26, 26, 26);
    private static final DeviceRgb MUTED = new DeviceRgb(107, 101, 96);
    private static final DeviceRgb WHITE = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb RULE = new DeviceRgb(215, 210, 200);

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
            doc.setMargins(50, 56, 40, 56);

            Map<String, String> holidays = Map.of();
            try {
                LocalDate monday = LocalDate.parse(menu.getWeekStart());
                holidays = holidayUtil.getHolidaysForWeek(monday, monday.plusDays(4));
            } catch (Exception ignored) {}

            addLogoWatermark(pdf);

            // ── Marke ───────────────────────────────────────
            doc.add(new Paragraph("\u00C9lys\u00E9e Events")
                    .setFontSize(28).setFontColor(GOLD)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(16));

            // Feine Linie
            rule(doc);

            // ── Woche ───────────────────────────────────────
            int kw = getIsoWeek(menu.getWeekStart());
            doc.add(new Paragraph("Speisekarte  \u00B7  Kalenderwoche " + kw)
                    .setFontSize(10).setFontColor(MUTED)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setCharacterSpacing(0.5f)
                    .setMarginTop(14).setMarginBottom(3));

            doc.add(new Paragraph(formatRange(menu.getWeekStart(), menu.getWeekEnd()))
                    .setFontSize(8.5f).setFontColor(MUTED)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

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
                        .setFontSize(7).setFontColor(GOLD).setBold()
                        .setCharacterSpacing(2.5f)
                        .setMarginTop(i == 0 ? 0 : 14).setMarginBottom(6));

                if (holiday != null) {
                    doc.add(new Paragraph(holiday)
                            .setFontSize(10.5f).setFontColor(MUTED).setItalic()
                            .setMarginBottom(2));
                } else {
                    if (m_[i] != null && !m_[i].isEmpty()) {
                        dishLine(doc, m_[i], mp_[i]);
                    }
                    if (v_[i] != null && !v_[i].isEmpty()) {
                        dishLine(doc, v_[i] + "  (V)", vp_[i]);
                    }
                }

                // Trennlinie nach jedem Tag
                rule(doc);
            }

            // ── Hinweise ────────────────────────────────────
            if (menu.getNotes() != null && !menu.getNotes().isEmpty()) {
                doc.add(new Paragraph(menu.getNotes())
                        .setFontSize(8).setFontColor(MUTED).setItalic()
                        .setMarginTop(12).setMarginBottom(0));
            }

            // ── Footer ──────────────────────────────────────
            doc.add(new Paragraph(
                    "\u00C9lys\u00E9e Event GmbH  \u00B7  Werner-von-Siemensstrasse 6  \u00B7  86159 Augsburg  \u00B7  Mo\u2013Fr 08\u201314 Uhr")
                    .setFontSize(6.5f).setFontColor(MUTED)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20));

        } catch (Exception e) {
            throw new RuntimeException("PDF-Generierung fehlgeschlagen", e);
        }
        return baos.toByteArray();
    }

    private void dishLine(Document doc, String dish, String price) {
        Table row = new Table(UnitValue.createPercentArray(new float[]{80, 20}))
                .useAllAvailableWidth().setMarginTop(0).setMarginBottom(0);

        Cell dishCell = new Cell().setBorder(Border.NO_BORDER)
                .setPaddingTop(3).setPaddingBottom(3)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        dishCell.add(new Paragraph(dish).setFontSize(10).setFontColor(DARK));
        row.addCell(dishCell);

        Cell priceCell = new Cell().setBorder(Border.NO_BORDER)
                .setPaddingTop(3).setPaddingBottom(3)
                .setTextAlignment(TextAlignment.RIGHT)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        if (price != null && !price.isEmpty()) {
            priceCell.add(new Paragraph(price + " \u20ac")
                    .setFontSize(10).setFontColor(DARK));
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
