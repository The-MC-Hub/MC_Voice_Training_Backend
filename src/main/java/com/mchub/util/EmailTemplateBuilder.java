package com.mchub.util;

import com.mchub.models.Announcement;
import java.time.LocalDateTime;

public final class EmailTemplateBuilder {

  private EmailTemplateBuilder() {
    // Utility class
  }

  public static String buildAnnouncementEmail(Announcement.EmailDesignData d) {
    if (d == null) {
      return "<p>Empty design</p>";
    }

    String logoHtml =
        (d.getLogoUrl() != null && !d.getLogoUrl().isBlank())
            ? """
                  <img src="%s" alt="Logo" style="max-height:50px;display:block;margin:0 auto 20px auto;"/>
                  """
                .formatted(d.getLogoUrl())
            : "";

    String bannerHtml =
        (d.getBannerUrl() != null && !d.getBannerUrl().isBlank())
            ? """
                  <img src="%s" alt="Banner" style="width:100%%;max-width:600px;height:auto;display:block;border-radius:8px;margin-bottom:24px;"/>
                  """
                .formatted(d.getBannerUrl())
            : "";

    String buttonHtml =
        (d.getButtonLink() != null
                && !d.getButtonLink().isBlank()
                && d.getButtonText() != null
                && !d.getButtonText().isBlank())
            ? """
                  <div style="text-align:center;margin-top:32px;margin-bottom:16px;">
                      <a href="%s" target="_blank" style="background-color:#f5a623;color:#000;text-decoration:none;padding:12px 28px;font-weight:600;border-radius:6px;display:inline-block;font-size:16px;">%s</a>
                  </div>
                  """
                .formatted(d.getButtonLink(), d.getButtonText())
            : "";

    String title = d.getTitle() != null ? d.getTitle() : "Thông báo từ MC Hub";
    String description = d.getDescription() != null ? d.getDescription() : "";
    int currentYear = LocalDateTime.now().getYear();

    return """
               <!DOCTYPE html>
               <html lang="vi">
               <head>
                   <meta charset="UTF-8">
                   <meta name="viewport" content="width=device-width,initial-scale=1.0">
                   <title>%s</title>
                   <style>
                       body { margin:0; padding:0; background-color:#f3f4f6; font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif; color:#1f2937; }
                       .container { max-width:600px; margin:30px auto; background-color:#ffffff; border-radius:12px; overflow:hidden; box-shadow:0 4px 6px -1px rgba(0,0,0,0.1); }
                       .header { background-color:#0f172a; padding:32px 24px; text-align:center; }
                       .header h1 { color:#f5a623; margin:0; font-size:24px; font-weight:700; }
                       .body { padding:32px 24px; line-height:1.6; }
                       .footer { background-color:#f8fafc; padding:20px 24px; text-align:center; font-size:12px; color:#64748b; border-top:1px solid #e2e8f0; }
                   </style>
               </head>
               <body>
                   <div class="container">
                       <div class="header">
                           %s
                           <h1>%s</h1>
                       </div>
                       <div class="body">
                           %s
                           <div style="font-size:15px;color:#374151;">%s</div>
                           %s
                       </div>
                       <div class="footer">
                           <p>© %d MC Hub Platform. All rights reserved.</p>
                       </div>
                   </div>
               </body>
               </html>
               """
        .formatted(title, logoHtml, title, bannerHtml, description, buttonHtml, currentYear);
  }
}
