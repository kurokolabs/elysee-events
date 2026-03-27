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
import de.elyseeevents.portal.model.Invoice;
import de.elyseeevents.portal.model.InvoiceItem;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Service
public class InvoicePdfService {

    private static final DeviceRgb GOLD = new DeviceRgb(201, 168, 76);
    private static final DeviceRgb DARK = new DeviceRgb(26, 26, 26);
    private static final DeviceRgb MUTED = new DeviceRgb(107, 101, 96);
    private static final DeviceRgb SURFACE = new DeviceRgb(242, 239, 233);
    private static final ThreadLocal<NumberFormat> EUR = ThreadLocal.withInitial(
            () -> NumberFormat.getCurrencyInstance(Locale.GERMANY));

    @org.springframework.beans.factory.annotation.Value("${app.company.name}") private String COMPANY;
    @org.springframework.beans.factory.annotation.Value("${app.company.street}") private String STREET;
    @org.springframework.beans.factory.annotation.Value("${app.company.city}") private String CITY;
    @org.springframework.beans.factory.annotation.Value("${app.company.phone}") private String PHONE;
    @org.springframework.beans.factory.annotation.Value("${app.company.email}") private String EMAIL;
    @org.springframework.beans.factory.annotation.Value("${app.company.web}") private String WEB;
    @org.springframework.beans.factory.annotation.Value("${app.company.tax-id}") private String TAX_ID;
    @org.springframework.beans.factory.annotation.Value("${app.company.hrb}") private String HRB;
    @org.springframework.beans.factory.annotation.Value("${app.company.court}") private String COURT;
    @org.springframework.beans.factory.annotation.Value("${app.company.bank}") private String BANK;
    @org.springframework.beans.factory.annotation.Value("${app.company.iban}") private String IBAN;
    @org.springframework.beans.factory.annotation.Value("${app.company.bic}") private String BIC;
    @org.springframework.beans.factory.annotation.Value("${app.company.ceo}") private String CEO;

    public byte[] generate(Invoice invoice, Customer customer, List<InvoiceItem> items) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
        try (pdf; Document doc = new Document(pdf, PageSize.A4)) {
        doc.setMargins(50, 50, 60, 50);

        // ── Absender-Block oben rechts + Marke links ────────
        Table top = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
                .useAllAvailableWidth().setMarginBottom(4);

        Cell brandCell = new Cell().setBorder(Border.NO_BORDER);
        brandCell.add(new Paragraph("ÉLYSÉE EVENTS")
                .setFontSize(26).setFontColor(GOLD).setBold().setMarginBottom(2));
        brandCell.add(new Paragraph("Exklusives Catering & Eventservice")
                .setFontSize(9).setFontColor(MUTED).setMarginBottom(6));
        brandCell.add(new Paragraph(COMPANY + "\n" + STREET + "\n" + CITY)
                .setFontSize(9).setFontColor(MUTED).setFixedLeading(13));
        top.addCell(brandCell);

        Cell contactCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        contactCell.add(new Paragraph(
                "Tel: " + PHONE + "\n" + EMAIL + "\n" + WEB + "\n\n" +
                "USt-IdNr.: " + TAX_ID + "\nGeschäftsführerin: " + CEO + "\n" + HRB + " | " + COURT)
                .setFontSize(9).setFontColor(MUTED).setFixedLeading(13));
        top.addCell(contactCell);
        doc.add(top);

        // Gold-Linie
        doc.add(new Paragraph("").setBorderBottom(new SolidBorder(GOLD, 2)).setMarginBottom(20));

        // ── Absender-Kurzzeile ──────────────────────────────
        doc.add(new Paragraph(COMPANY + " | " + STREET + " | " + CITY)
                .setFontSize(7).setFontColor(MUTED)
                .setBorderBottom(new SolidBorder(GOLD, 0.5f))
                .setMarginBottom(8).setPaddingBottom(2));

        // ── Empfänger + Rechnungsinfo ───────────────────────
        Table header = new Table(UnitValue.createPercentArray(new float[]{55, 45}))
                .useAllAvailableWidth().setMarginBottom(24);

        // Empfängeradresse
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
        addrCell.add(new Paragraph("RECHNUNG AN").setFontSize(8).setFontColor(MUTED).setBold()
                .setCharacterSpacing(1.5f).setMarginBottom(8));
        addrCell.add(new Paragraph(addr.toString()).setFontSize(10).setFontColor(DARK)
                .setFixedLeading(15));
        header.addCell(addrCell);

        // Rechnungsinfo
        Cell infoCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        infoCell.add(new Paragraph("RECHNUNG").setFontSize(22).setFontColor(DARK).setBold()
                .setMarginBottom(12));
        infoCell.add(infoLine("Rechnungs-Nr.", invoice.getInvoiceNumber()));
        infoCell.add(infoLine("Rechnungsdatum", formatDate(invoice.getCreatedAt())));
        // Leistungszeitraum
        if (invoice.getServicePeriodFrom() != null && !invoice.getServicePeriodFrom().isEmpty()) {
            String period = invoice.getServicePeriodFrom();
            if (invoice.getServicePeriodTo() != null && !invoice.getServicePeriodTo().isEmpty()
                    && !invoice.getServicePeriodTo().equals(invoice.getServicePeriodFrom())) {
                period += " bis " + invoice.getServicePeriodTo();
            }
            infoCell.add(infoLine("Leistungszeitraum", period));
        } else {
            infoCell.add(infoLine("Leistungsdatum", formatDate(invoice.getCreatedAt())));
        }
        if (invoice.getDueDate() != null)
            infoCell.add(infoLine("Fällig bis", invoice.getDueDate()));
        header.addCell(infoCell);
        doc.add(header);

        // ── Einleitungstext ────────────────────────────────
        if (invoice.getIntroText() != null && !invoice.getIntroText().isBlank()) {
            doc.add(new Paragraph(invoice.getIntroText())
                    .setFontSize(10).setFontColor(DARK).setFixedLeading(16).setMarginBottom(20));
        }

        // ── Positionen ──────────────────────────────────────
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
            for (InvoiceItem item : items) {
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
            table.addCell(tdCell(EUR.get().format(invoice.getAmount()), TextAlignment.RIGHT));
            table.addCell(tdCell(EUR.get().format(invoice.getAmount()), TextAlignment.RIGHT));
            table.addCell(tdCell("19%", TextAlignment.CENTER));
        }
        doc.add(table);

        // ── Summen ──────────────────────────────────────────
        Table totals = new Table(UnitValue.createPercentArray(new float[]{60, 20, 20}))
                .useAllAvailableWidth().setMarginTop(4);

        totals.addCell(emptyCell());
        totals.addCell(sumLabel("Zwischensumme (Netto)"));
        totals.addCell(sumValue(EUR.get().format(invoice.getAmount())));

        if (invoice.getTaxAmount7() > 0) {
            totals.addCell(emptyCell());
            totals.addCell(sumLabel("MwSt. 7 % (Essen)"));
            totals.addCell(sumValue(EUR.get().format(invoice.getTaxAmount7())));
        }
        if (invoice.getTaxAmount19() > 0) {
            totals.addCell(emptyCell());
            totals.addCell(sumLabel("MwSt. 19 % (Getränke/Sonstiges)"));
            totals.addCell(sumValue(EUR.get().format(invoice.getTaxAmount19())));
        }
        // Fallback für alte Rechnungen ohne getrennte Steuersätze
        if (invoice.getTaxAmount7() == 0 && invoice.getTaxAmount19() == 0 && invoice.getTaxAmount() > 0) {
            totals.addCell(emptyCell());
            totals.addCell(sumLabel("MwSt. " + String.format("%.1f", invoice.getTaxRate()) + " %"));
            totals.addCell(sumValue(EUR.get().format(invoice.getTaxAmount())));
        }

        totals.addCell(emptyCell());
        Cell tLabel = new Cell().setBorder(Border.NO_BORDER)
                .setBorderTop(new SolidBorder(GOLD, 2)).setPaddingTop(10);
        tLabel.add(new Paragraph("Gesamtbetrag").setFontSize(12).setBold().setFontColor(DARK));
        totals.addCell(tLabel);
        Cell tValue = new Cell().setBorder(Border.NO_BORDER)
                .setBorderTop(new SolidBorder(GOLD, 2)).setPaddingTop(10)
                .setTextAlignment(TextAlignment.RIGHT);
        tValue.add(new Paragraph(EUR.get().format(invoice.getTotal()))
                .setFontSize(16).setBold().setFontColor(GOLD));
        totals.addCell(tValue);
        doc.add(totals);

        // ── Notizen ─────────────────────────────────────────
        if (invoice.getNotes() != null && !invoice.getNotes().isBlank()) {
            doc.add(new Paragraph("").setMarginTop(16));
            doc.add(new Paragraph("HINWEISE").setFontSize(8).setFontColor(MUTED).setBold()
                    .setCharacterSpacing(1.5f).setMarginBottom(4));
            doc.add(new Paragraph(invoice.getNotes()).setFontSize(9).setFontColor(DARK));
        }

        // ── Bankverbindung ──────────────────────────────────
        doc.add(new Paragraph("").setBorderBottom(new SolidBorder(SURFACE, 1))
                .setMarginTop(20).setMarginBottom(12));
        doc.add(new Paragraph("ZAHLUNGSINFORMATIONEN").setFontSize(8).setFontColor(MUTED).setBold()
                .setCharacterSpacing(1.5f).setMarginBottom(8));

        Table bank = new Table(UnitValue.createPercentArray(new float[]{22, 78}))
                .useAllAvailableWidth();
        bank.addCell(bankLabel("Empfänger"));  bank.addCell(bankValue(COMPANY));
        bank.addCell(bankLabel("Bank"));       bank.addCell(bankValue(BANK));
        bank.addCell(bankLabel("IBAN"));       bank.addCell(bankValue(IBAN));
        bank.addCell(bankLabel("BIC"));        bank.addCell(bankValue(BIC));
        bank.addCell(bankLabel("Verwendungszweck")); bank.addCell(bankValue(invoice.getInvoiceNumber()));
        doc.add(bank);

        // ── Footer ──────────────────────────────────────────
        doc.add(new Paragraph("").setBorderBottom(new SolidBorder(GOLD, 0.5f))
                .setMarginTop(24).setMarginBottom(8));
        doc.add(new Paragraph(
                COMPANY + "  |  " + STREET + "  |  " + CITY + "\n" +
                "Tel: " + PHONE + "  |  " + EMAIL + "  |  " + WEB + "\n" +
                "Geschäftsführerin: " + CEO + "  |  USt-IdNr.: " + TAX_ID + "  |  " + COURT + " " + HRB)
                .setFontSize(7).setFontColor(MUTED)
                .setTextAlignment(TextAlignment.CENTER).setFixedLeading(11));

        } // auto-close doc and pdf
        return baos.toByteArray();
    }

    public byte[] generate(Invoice invoice, Customer customer) {
        return generate(invoice, customer, null);
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

    private Cell bankLabel(String t) {
        return new Cell().setBorder(Border.NO_BORDER).setPaddingBottom(3)
                .add(new Paragraph(t).setFontSize(8).setFontColor(MUTED));
    }

    private Cell bankValue(String t) {
        return new Cell().setBorder(Border.NO_BORDER).setPaddingBottom(3)
                .add(new Paragraph(t).setFontSize(9).setFontColor(DARK));
    }
}
