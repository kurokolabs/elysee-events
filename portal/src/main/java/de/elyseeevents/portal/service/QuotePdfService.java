package de.elyseeevents.portal.service;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import de.elyseeevents.portal.model.Customer;
import de.elyseeevents.portal.model.Quote;
import de.elyseeevents.portal.model.QuoteItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Service
public class QuotePdfService {

    private static final DeviceRgb GOLD = new DeviceRgb(201, 168, 76);
    private static final DeviceRgb DARK = new DeviceRgb(26, 26, 26);
    private static final DeviceRgb MUTED = new DeviceRgb(107, 101, 96);
    private static final DeviceRgb SURFACE = new DeviceRgb(242, 239, 233);
    private static final ThreadLocal<NumberFormat> EUR = ThreadLocal.withInitial(
            () -> NumberFormat.getCurrencyInstance(Locale.GERMANY));

    @Value("${app.company.name}") private String COMPANY;
    @Value("${app.company.street}") private String STREET;
    @Value("${app.company.city}") private String CITY;
    @Value("${app.company.phone}") private String PHONE;
    @Value("${app.company.email}") private String EMAIL;
    @Value("${app.company.web}") private String WEB;
    @Value("${app.company.tax-id}") private String TAX_ID;
    @Value("${app.company.hrb}") private String HRB;
    @Value("${app.company.court}") private String COURT;
    @Value("${app.company.ceo}") private String CEO;

    public byte[] generate(Quote quote, Customer customer, List<QuoteItem> items) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
        try (pdf; Document doc = new Document(pdf, PageSize.A4)) {
        doc.setMargins(50, 50, 60, 50);

        // -- Absender-Block oben rechts + Marke links --
        Table top = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
                .useAllAvailableWidth().setMarginBottom(4);

        Cell brandCell = new Cell().setBorder(Border.NO_BORDER);
        brandCell.add(new Paragraph("\u00c9LYS\u00c9E EVENTS")
                .setFontSize(26).setFontColor(GOLD).setBold().setMarginBottom(2));
        brandCell.add(new Paragraph("Exklusives Catering & Eventservice")
                .setFontSize(9).setFontColor(MUTED).setMarginBottom(6));
        brandCell.add(new Paragraph(COMPANY + "\n" + STREET + "\n" + CITY)
                .setFontSize(9).setFontColor(MUTED).setFixedLeading(13));
        top.addCell(brandCell);

        Cell contactCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        contactCell.add(new Paragraph(
                "Tel: " + PHONE + "\n" + EMAIL + "\n" + WEB + "\n\n" +
                "USt-IdNr.: " + TAX_ID + "\nGesch\u00e4ftsf\u00fchrerin: " + CEO + "\n" + HRB + " | " + COURT)
                .setFontSize(9).setFontColor(MUTED).setFixedLeading(13));
        top.addCell(contactCell);
        doc.add(top);

        // Gold-Linie
        doc.add(new Paragraph("").setBorderBottom(new SolidBorder(GOLD, 2)).setMarginBottom(20));

        // -- Absender-Kurzzeile --
        doc.add(new Paragraph(COMPANY + " | " + STREET + " | " + CITY)
                .setFontSize(7).setFontColor(MUTED)
                .setBorderBottom(new SolidBorder(GOLD, 0.5f))
                .setMarginBottom(8).setPaddingBottom(2));

        // -- Empf\u00e4nger + Angebotsinfo --
        Table header = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
                .useAllAvailableWidth().setMarginBottom(24);

        // Empf\u00e4ngeradresse
        StringBuilder addr = new StringBuilder();
        addr.append(customer.getFullName());
        if (customer.getCompany() != null && !customer.getCompany().isEmpty())
            addr.append("\n").append(customer.getCompany());
        if (customer.getAddress() != null && !customer.getAddress().isEmpty())
            addr.append("\n").append(customer.getAddress());
        String plzCity = "";
        if (customer.getPostalCode() != null && !customer.getPostalCode().isEmpty()) plzCity += customer.getPostalCode() + " ";
        if (customer.getCity() != null && !customer.getCity().isEmpty()) plzCity += customer.getCity();
        if (!plzCity.isBlank()) addr.append("\n").append(plzCity.trim());

        Cell addrCell = new Cell().setBorder(Border.NO_BORDER);
        addrCell.add(new Paragraph("ANGEBOT F\u00dcR").setFontSize(8).setFontColor(MUTED).setBold()
                .setCharacterSpacing(1.5f).setMarginBottom(8));
        addrCell.add(new Paragraph(addr.toString()).setFontSize(10).setFontColor(DARK)
                .setFixedLeading(15));
        header.addCell(addrCell);

        // Angebotsinfo
        Cell infoCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        infoCell.add(new Paragraph("ANGEBOT").setFontSize(22).setFontColor(DARK).setBold()
                .setMarginBottom(12));
        infoCell.add(infoLine("Angebots-Nr.", quote.getQuoteNumber()));
        infoCell.add(infoLine("Angebotsdatum", formatDate(quote.getCreatedAt())));
        if (quote.getServicePeriodFrom() != null && !quote.getServicePeriodFrom().isEmpty()) {
            String period = quote.getServicePeriodFrom();
            if (quote.getServicePeriodTo() != null && !quote.getServicePeriodTo().isEmpty()
                    && !quote.getServicePeriodTo().equals(quote.getServicePeriodFrom())) {
                period += " bis " + quote.getServicePeriodTo();
            }
            infoCell.add(infoLine("Leistungszeitraum", period));
        }
        if (quote.getValidUntil() != null)
            infoCell.add(infoLine("Gültig bis", quote.getValidUntil()));
        header.addCell(infoCell);
        doc.add(header);

        // -- Einleitungstext --
        if (quote.getIntroText() != null && !quote.getIntroText().isBlank()) {
            doc.add(new Paragraph(quote.getIntroText())
                    .setFontSize(10).setFontColor(DARK).setFixedLeading(16).setMarginBottom(20));
        }

        // -- Positionen --
        Table table = new Table(UnitValue.createPercentArray(new float[]{5, 33, 12, 17, 17, 16}))
                .useAllAvailableWidth();

        table.addHeaderCell(thCell("Nr.", TextAlignment.CENTER));
        table.addHeaderCell(thCell("Beschreibung", TextAlignment.LEFT));
        table.addHeaderCell(thCell("Menge", TextAlignment.CENTER));
        table.addHeaderCell(thCell("Einzelpreis", TextAlignment.RIGHT));
        table.addHeaderCell(thCell("Betrag", TextAlignment.RIGHT));
        table.addHeaderCell(thCell("MwSt.", TextAlignment.CENTER));

        if (items != null && !items.isEmpty()) {
            int pos = 1;
            for (QuoteItem item : items) {
                table.addCell(tdCell(String.valueOf(pos++), TextAlignment.CENTER));
                table.addCell(tdCell(item.getDescription(), TextAlignment.LEFT));
                table.addCell(tdCell(formatQty(item.getQuantity()), TextAlignment.CENTER));
                table.addCell(tdCell(EUR.get().format(item.getUnitPrice()), TextAlignment.RIGHT));
                table.addCell(tdCell(EUR.get().format(item.getTotal()), TextAlignment.RIGHT));
                table.addCell(tdCell(item.getTaxTypeLabel(), TextAlignment.CENTER));
            }
        } else {
            table.addCell(tdCell("1", TextAlignment.CENTER));
            table.addCell(tdCell("Catering & Service", TextAlignment.LEFT));
            table.addCell(tdCell("1", TextAlignment.CENTER));
            table.addCell(tdCell(EUR.get().format(quote.getAmount()), TextAlignment.RIGHT));
            table.addCell(tdCell(EUR.get().format(quote.getAmount()), TextAlignment.RIGHT));
        }
        doc.add(table);

        // -- Summen --
        Table totals = new Table(UnitValue.createPercentArray(new float[]{60, 20, 20}))
                .useAllAvailableWidth().setMarginTop(4);

        totals.addCell(emptyCell());
        totals.addCell(sumLabel("Zwischensumme (Netto)"));
        totals.addCell(sumValue(EUR.get().format(quote.getAmount())));

        if (quote.getTaxAmount7() > 0) {
            totals.addCell(emptyCell());
            totals.addCell(sumLabel("MwSt. 7 % (Essen)"));
            totals.addCell(sumValue(EUR.get().format(quote.getTaxAmount7())));
        }
        if (quote.getTaxAmount19() > 0) {
            totals.addCell(emptyCell());
            totals.addCell(sumLabel("MwSt. 19 % (Getränke/Sonstiges)"));
            totals.addCell(sumValue(EUR.get().format(quote.getTaxAmount19())));
        }
        if (quote.getTaxAmount7() == 0 && quote.getTaxAmount19() == 0 && quote.getTaxAmount() > 0) {
            totals.addCell(emptyCell());
            totals.addCell(sumLabel("MwSt. " + String.format("%.1f", quote.getTaxRate()) + " %"));
            totals.addCell(sumValue(EUR.get().format(quote.getTaxAmount())));
        }

        totals.addCell(emptyCell());
        Cell tLabel = new Cell().setBorder(Border.NO_BORDER)
                .setBorderTop(new SolidBorder(GOLD, 2)).setPaddingTop(10);
        tLabel.add(new Paragraph("Gesamtbetrag").setFontSize(12).setBold().setFontColor(DARK));
        totals.addCell(tLabel);
        Cell tValue = new Cell().setBorder(Border.NO_BORDER)
                .setBorderTop(new SolidBorder(GOLD, 2)).setPaddingTop(10)
                .setTextAlignment(TextAlignment.RIGHT);
        tValue.add(new Paragraph(EUR.get().format(quote.getTotal()))
                .setFontSize(16).setBold().setFontColor(GOLD));
        totals.addCell(tValue);
        doc.add(totals);

        // -- Hinweise --
        if (quote.getNotes() != null && !quote.getNotes().isBlank()) {
            doc.add(new Paragraph("").setMarginTop(16));
            doc.add(new Paragraph("HINWEISE").setFontSize(8).setFontColor(MUTED).setBold()
                    .setCharacterSpacing(1.5f).setMarginBottom(4));
            doc.add(new Paragraph(quote.getNotes()).setFontSize(9).setFontColor(DARK));
        }

        // -- G\u00fcltigkeitshinweis --
        doc.add(new Paragraph("").setBorderBottom(new SolidBorder(SURFACE, 1))
                .setMarginTop(20).setMarginBottom(12));
        doc.add(new Paragraph("G\u00dcLTIGKEIT").setFontSize(8).setFontColor(MUTED).setBold()
                .setCharacterSpacing(1.5f).setMarginBottom(8));
        String validText = quote.getValidUntil() != null
                ? "Dieses Angebot ist g\u00fcltig bis zum " + quote.getValidUntil() + "."
                : "Dieses Angebot ist 30 Tage ab Angebotsdatum g\u00fcltig.";
        doc.add(new Paragraph(validText).setFontSize(9).setFontColor(DARK));

        // -- Footer --
        doc.add(new Paragraph("").setBorderBottom(new SolidBorder(GOLD, 0.5f))
                .setMarginTop(24).setMarginBottom(8));
        doc.add(new Paragraph(
                COMPANY + "  |  " + STREET + "  |  " + CITY + "\n" +
                "Tel: " + PHONE + "  |  " + EMAIL + "  |  " + WEB + "\n" +
                "Gesch\u00e4ftsf\u00fchrerin: " + CEO + "  |  USt-IdNr.: " + TAX_ID + "  |  " + COURT + " " + HRB)
                .setFontSize(7).setFontColor(MUTED)
                .setTextAlignment(TextAlignment.CENTER).setFixedLeading(11));

        } // auto-close doc and pdf
        return baos.toByteArray();
    }

    public byte[] generate(Quote quote, List<QuoteItem> items) {
        Customer pseudo = new Customer();
        pseudo.setFirstName(quote.getRecipientName() != null ? quote.getRecipientName() : "");
        pseudo.setLastName("");
        pseudo.setCompany(quote.getRecipientCompany());
        pseudo.setAddress(quote.getRecipientAddress());
        pseudo.setPostalCode(quote.getRecipientPostalCode());
        pseudo.setCity(quote.getRecipientCity());
        return generate(quote, pseudo, items);
    }

    private String formatDate(String datetime) {
        if (datetime == null) return "-";
        return datetime.length() >= 10 ? datetime.substring(0, 10) : datetime;
    }

    private String formatQty(Double qty) {
        if (qty == null) return "1";
        return qty == Math.floor(qty) ? String.valueOf(qty.intValue()) : String.format("%.2f", qty);
    }

    private Paragraph infoLine(String label, String value) {
        return new Paragraph(label + ": " + value)
                .setFontSize(9).setFontColor(DARK).setMarginBottom(2);
    }

    private Cell thCell(String text, TextAlignment align) {
        return new Cell().setBorder(Border.NO_BORDER).setBackgroundColor(SURFACE)
                .setPadding(8).setPaddingTop(10).setPaddingBottom(10)
                .setTextAlignment(align)
                .add(new Paragraph(text).setFontSize(8).setBold().setFontColor(MUTED)
                        .setCharacterSpacing(0.8f));
    }

    private Cell tdCell(String text, TextAlignment align) {
        return new Cell().setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(SURFACE, 0.5f))
                .setPadding(8).setTextAlignment(align)
                .add(new Paragraph(text).setFontSize(10).setFontColor(DARK));
    }

    private Cell emptyCell() { return new Cell().setBorder(Border.NO_BORDER); }

    private Cell sumLabel(String t) {
        return new Cell().setBorder(Border.NO_BORDER).setPaddingTop(5).setPaddingBottom(5)
                .add(new Paragraph(t).setFontSize(9).setFontColor(MUTED));
    }

    private Cell sumValue(String t) {
        return new Cell().setBorder(Border.NO_BORDER).setPaddingTop(5).setPaddingBottom(5)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph(t).setFontSize(10).setFontColor(DARK));
    }
}
