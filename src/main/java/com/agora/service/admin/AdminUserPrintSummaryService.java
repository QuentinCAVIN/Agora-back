package com.agora.service.admin;

import com.agora.dto.response.admin.AdminUserDetailResponseDto;
import com.agora.dto.response.admin.AdminUserGroupSnippetDto;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

/**
 * Génère une fiche PDF imprimable pour le secrétariat (cahier § GET .../print-summary).
 */
@Service
public class AdminUserPrintSummaryService {

    public byte[] buildPdf(AdminUserDetailResponseDto user) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 11);

            document.add(new Paragraph("Fiche utilisateur — AGORA", titleFont));
            document.add(new Paragraph(" ", normal));

            document.add(new Paragraph("Identifiant", bold));
            document.add(new Paragraph(nvl(user.id()), normal));
            document.add(new Paragraph(" ", normal));

            document.add(new Paragraph("Nom complet", bold));
            document.add(new Paragraph(nvl(user.firstName()) + " " + nvl(user.lastName()), normal));
            document.add(new Paragraph(" ", normal));

            document.add(new Paragraph("Email", bold));
            document.add(new Paragraph(user.email() != null && !user.email().isBlank() ? user.email() : "—", normal));
            document.add(new Paragraph(" ", normal));

            document.add(new Paragraph("Telephone", bold));
            document.add(new Paragraph(nvl(user.phone()).isBlank() ? "—" : user.phone(), normal));
            document.add(new Paragraph(" ", normal));

            document.add(new Paragraph("Type de compte", bold));
            document.add(new Paragraph(nvl(user.accountType()), normal));
            document.add(new Paragraph(" ", normal));

            document.add(new Paragraph("Statut", bold));
            document.add(new Paragraph(nvl(user.status()), normal));
            document.add(new Paragraph(" ", normal));

            if (user.internalRef() != null && !user.internalRef().isBlank()) {
                document.add(new Paragraph("Reference interne (tutelle)", bold));
                document.add(new Paragraph(user.internalRef(), normal));
                document.add(new Paragraph(" ", normal));
            }

            if (user.notesAdmin() != null && !user.notesAdmin().isBlank()) {
                document.add(new Paragraph("Notes administrateur", bold));
                document.add(new Paragraph(user.notesAdmin(), normal));
                document.add(new Paragraph(" ", normal));
            }

            document.add(new Paragraph("Groupes et tarifs", bold));
            if (user.groups() == null || user.groups().isEmpty()) {
                document.add(new Paragraph("Aucun groupe", normal));
            } else {
                for (AdminUserGroupSnippetDto g : user.groups()) {
                    document.add(new Paragraph(
                            " - " + nvl(g.name()) + " : " + nvl(g.discountLabel()),
                            normal));
                }
            }
            document.add(new Paragraph(" ", normal));

            document.add(new Paragraph("Cree le", bold));
            document.add(new Paragraph(nvl(user.createdAt()), normal));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Generation du PDF impossible.", e);
        }
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}
