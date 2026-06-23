package com.mchub.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${mchub.fe-url:http://localhost:5173}")
    private String feUrl;

    public void sendSimpleEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }

    public String getFeUrl() {
        return feUrl;
    }

    public void sendHtmlEmail(String to, String subject, String htmlContent) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    /**
     * Builds the branded MC Hub HTML email template.
     * @param recipientName  personalised name (or "bạn")
     * @param bodyText       plain-text content — newlines converted to <br> paragraphs
     * @param type           announcement type label for the banner tag
     */
    public String buildHtmlEmail(String recipientName, String bodyText, String type) {
        String safeBody    = escapeAndFormatBody(bodyText, recipientName);
        String bannerColor = bannerColorFor(type);
        String typeLabel   = typeLabelFor(type);
        String heroBg      = heroBgFor(type);
        String year        = String.valueOf(java.time.Year.now().getValue());

        return """
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1"/>
<title>MC Hub</title>
<style>
  * { margin:0; padding:0; box-sizing:border-box; }
  body { background:#f0f0f2; font-family:'Helvetica Neue',Arial,sans-serif; color:#18181b; }
  a { color:#f5a623; text-decoration:none; }
</style>
</head>
<body>
<table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#f0f0f2;padding:40px 16px;">
  <tr><td align="center">
    <table width="600" cellpadding="0" cellspacing="0" border="0"
      style="max-width:600px;width:100%%;border-radius:20px;overflow:hidden;box-shadow:0 8px 40px rgba(0,0,0,0.14);">

      <!-- ── TOP ACCENT BAR ── -->
      <tr>
        <td style="background:linear-gradient(90deg,#f5a623,#fbbf24,#f5a623);height:4px;"></td>
      </tr>

      <!-- ── HEADER ── -->
      <tr>
        <td style="background:#09090b;padding:24px 36px 20px;">
          <table width="100%%" cellpadding="0" cellspacing="0" border="0">
            <tr>
              <td>
                <table cellpadding="0" cellspacing="0" border="0">
                  <tr>
                    <td style="font-size:20px;font-weight:800;color:#fff;letter-spacing:-0.5px;">MC</td>
                    <td style="padding:0 4px;vertical-align:middle;">
                      <span style="display:inline-block;width:6px;height:6px;border-radius:50%%;background:#f5a623;margin-bottom:1px;"></span>
                    </td>
                    <td style="font-size:20px;font-weight:800;color:#fff;letter-spacing:-0.5px;">Hub</td>
                  </tr>
                </table>
                <p style="color:#52525b;font-size:10px;margin-top:3px;letter-spacing:0.8px;text-transform:uppercase;">AI Voice Training Platform</p>
              </td>
              <td align="right" style="vertical-align:middle;">
                <span style="display:inline-block;background:%s;color:#fff;font-size:10px;font-weight:700;padding:5px 12px;border-radius:4px;letter-spacing:0.8px;text-transform:uppercase;">%s</span>
              </td>
            </tr>
          </table>
        </td>
      </tr>

      <!-- ── HERO IMAGE BANNER ── -->
      <tr>
        <td style="background:%s;padding:0;position:relative;">
          <div style="padding:44px 36px 40px;">
            <p style="color:rgba(255,255,255,0.55);font-size:11px;letter-spacing:1.5px;text-transform:uppercase;margin-bottom:12px;">Thông báo từ MC Hub</p>
            <h1 style="color:#ffffff;font-size:28px;font-weight:800;line-height:1.2;letter-spacing:-0.5px;margin-bottom:10px;">
              Xin chào,<br/><span style="color:#f5a623;">%s</span> 👋
            </h1>
            <p style="color:rgba(255,255,255,0.5);font-size:12px;margin-top:8px;">Chúng tôi có một thông báo quan trọng dành cho bạn.</p>
          </div>
          <!-- decorative dots -->
          <div style="position:absolute;right:36px;top:50%%;transform:translateY(-50%%);">
            <svg width="80" height="80" viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg" style="opacity:0.12;">
              <circle cx="10" cy="10" r="4" fill="white"/><circle cx="30" cy="10" r="4" fill="white"/>
              <circle cx="50" cy="10" r="4" fill="white"/><circle cx="70" cy="10" r="4" fill="white"/>
              <circle cx="10" cy="30" r="4" fill="white"/><circle cx="30" cy="30" r="4" fill="white"/>
              <circle cx="50" cy="30" r="4" fill="white"/><circle cx="70" cy="30" r="4" fill="white"/>
              <circle cx="10" cy="50" r="4" fill="white"/><circle cx="30" cy="50" r="4" fill="white"/>
              <circle cx="50" cy="50" r="4" fill="white"/><circle cx="70" cy="50" r="4" fill="white"/>
              <circle cx="10" cy="70" r="4" fill="white"/><circle cx="30" cy="70" r="4" fill="white"/>
              <circle cx="50" cy="70" r="4" fill="white"/><circle cx="70" cy="70" r="4" fill="white"/>
            </svg>
          </div>
        </td>
      </tr>

      <!-- ── THIN GOLD LINE ── -->
      <tr>
        <td style="background:linear-gradient(90deg,#f5a623,#fbbf24);height:2px;"></td>
      </tr>

      <!-- ── CONTENT BODY ── -->
      <tr>
        <td style="background:#ffffff;padding:36px 36px 24px;">
          <div style="font-size:15px;line-height:1.8;color:#27272a;">
            %s
          </div>
        </td>
      </tr>

      <!-- ── CTA ── -->
      <tr>
        <td style="background:#ffffff;padding:8px 36px 40px;text-align:center;">
          <a href="%s/m/dashboard"
            style="display:inline-block;background:#f5a623;color:#000000;font-size:14px;font-weight:700;
                   padding:15px 40px;border-radius:10px;letter-spacing:0.2px;text-decoration:none;">
            Truy cập MC Hub &rarr;
          </a>
        </td>
      </tr>

      <!-- ── STATS STRIP ── -->
      <tr>
        <td style="background:#09090b;padding:0;">
          <table width="100%%" cellpadding="0" cellspacing="0" border="0">
            <tr>
              <td width="33%%" style="text-align:center;padding:22px 8px;border-right:1px solid #1f1f23;">
                <p style="color:#f5a623;font-size:20px;font-weight:800;letter-spacing:-0.5px;">500+</p>
                <p style="color:#52525b;font-size:10px;margin-top:4px;text-transform:uppercase;letter-spacing:0.5px;">MC tin dùng</p>
              </td>
              <td width="33%%" style="text-align:center;padding:22px 8px;border-right:1px solid #1f1f23;">
                <p style="color:#f5a623;font-size:20px;font-weight:800;letter-spacing:-0.5px;">94%%</p>
                <p style="color:#52525b;font-size:10px;margin-top:4px;text-transform:uppercase;letter-spacing:0.5px;">Độ chính xác AI</p>
              </td>
              <td width="33%%" style="text-align:center;padding:22px 8px;">
                <p style="color:#f5a623;font-size:20px;font-weight:800;letter-spacing:-0.5px;">50+</p>
                <p style="color:#52525b;font-size:10px;margin-top:4px;text-transform:uppercase;letter-spacing:0.5px;">Kịch bản MC</p>
              </td>
            </tr>
          </table>
        </td>
      </tr>

      <!-- ── FOOTER ── -->
      <tr>
        <td style="background:#09090b;padding:20px 36px 28px;border-top:1px solid #1f1f23;">
          <p style="color:#3f3f46;font-size:11px;line-height:1.7;text-align:center;">
            Bạn nhận được email này vì đã đăng ký tài khoản tại <strong style="color:#52525b;">mchub.vn</strong>.<br/>
            <a href="%s/m/settings" style="color:#71717a;">Huỷ đăng ký</a> &nbsp;·&nbsp;
            <a href="%s/m/dashboard" style="color:#71717a;">Quản lý thông báo</a><br/><br/>
            <span style="color:#27272a;">© %s MC Hub · Việt Nam</span>
          </p>
        </td>
      </tr>

      <!-- ── BOTTOM ACCENT BAR ── -->
      <tr>
        <td style="background:linear-gradient(90deg,#f5a623,#fbbf24,#f5a623);height:4px;"></td>
      </tr>

    </table>
  </td></tr>
</table>
</body>
</html>
""".formatted(
                bannerColor, typeLabel,   // header badge
                heroBg,                  // hero section background
                recipientName,           // greeting name
                safeBody,                // main content
                feUrl,                   // CTA href
                feUrl,                   // footer unsubscribe
                feUrl,                   // footer manage
                year                     // copyright
        );
    }

    /**
     * Verification email template — body is trusted HTML (caller constructs it),
     * so we do NOT escape it. Use only for OTP/magic-link emails.
     */
    public String buildVerificationEmail(String recipientName, String trustedHtmlBody) {
        String year = String.valueOf(java.time.Year.now().getValue());
        return """
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1"/>
<title>MC Hub — Xác thực email</title>
</head>
<body style="margin:0;padding:0;background:#f4f4f6;font-family:'Helvetica Neue',Arial,sans-serif;">
<table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#f4f4f6;padding:40px 16px;">
  <tr><td align="center">
    <table width="560" cellpadding="0" cellspacing="0" border="0"
      style="max-width:560px;width:100%%;border-radius:20px;overflow:hidden;
             box-shadow:0 4px 32px rgba(0,0,0,0.10);border:1px solid #e8e8ec;">

      <!-- TOP ACCENT BAR -->
      <tr>
        <td style="background:#f5a623;height:4px;font-size:0;line-height:0;">&nbsp;</td>
      </tr>

      <!-- HEADER (dark strip) -->
      <tr>
        <td style="background:#111113;padding:20px 32px 16px;">
          <table width="100%%" cellpadding="0" cellspacing="0" border="0">
            <tr>
              <td style="vertical-align:middle;">
                <table cellpadding="0" cellspacing="0" border="0">
                  <tr>
                    <td style="font-size:17px;font-weight:800;color:#ffffff;letter-spacing:-0.4px;font-family:'Helvetica Neue',Arial,sans-serif;">MC</td>
                    <td style="padding:0 4px 0 3px;vertical-align:middle;">
                      <table cellpadding="0" cellspacing="0" border="0">
                        <tr><td width="5" height="5" style="background:#f5a623;border-radius:50%%;font-size:0;line-height:0;">&nbsp;</td></tr>
                      </table>
                    </td>
                    <td style="font-size:17px;font-weight:800;color:#ffffff;letter-spacing:-0.4px;font-family:'Helvetica Neue',Arial,sans-serif;">Hub</td>
                  </tr>
                </table>
                <p style="color:#52525b;font-size:9px;margin:3px 0 0 0;letter-spacing:1.2px;text-transform:uppercase;font-family:'Helvetica Neue',Arial,sans-serif;">AI VOICE TRAINING PLATFORM</p>
              </td>
              <td align="right" style="vertical-align:middle;">
                <table cellpadding="0" cellspacing="0" border="0">
                  <tr>
                    <td style="background:#1d3a26;border:1px solid #2d5c3a;border-radius:5px;padding:4px 11px;">
                      <span style="color:#4ade80;font-size:9px;font-weight:700;letter-spacing:1px;text-transform:uppercase;font-family:'Helvetica Neue',Arial,sans-serif;">+ XAC THUC</span>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
          </table>
        </td>
      </tr>

      <!-- HERO (white with gold left border) -->
      <tr>
        <td style="background:#ffffff;padding:0;">
          <table width="100%%" cellpadding="0" cellspacing="0" border="0">
            <tr>
              <!-- Gold left accent bar -->
              <td width="4" style="background:#f5a623;font-size:0;line-height:0;">&nbsp;</td>
              <td style="padding:36px 32px 32px 28px;">
                <p style="color:#b45309;font-size:10px;letter-spacing:2px;text-transform:uppercase;margin:0 0 12px 0;font-weight:700;font-family:'Helvetica Neue',Arial,sans-serif;">X&#225;c th&#7921;c t&#224;i kho&#7843;n</p>
                <p style="color:#111113;font-size:26px;font-weight:800;line-height:1.25;letter-spacing:-0.5px;margin:0 0 8px 0;font-family:'Helvetica Neue',Arial,sans-serif;">
                  Ch&#224;o <span style="color:#d97706;">%s</span>
                </p>
                <p style="color:#71717a;font-size:13px;margin:0;line-height:1.6;font-family:'Helvetica Neue',Arial,sans-serif;">Ch&#7881; m&#7897;t b&#432;&#7899;c n&#7919;a &#273;&#7875; b&#7855;t &#273;&#7847;u h&#224;nh tr&#236;nh luy&#7879;n gi&#7885;ng.</p>
              </td>
              <td width="90" style="padding:0 28px 0 0;vertical-align:middle;" align="right">
                <!-- Mic icon using table (email-safe, no SVG render issues) -->
                <table cellpadding="0" cellspacing="0" border="0">
                  <tr>
                    <td width="64" height="64"
                      style="background:#fffbeb;border:1px solid #fde68a;border-radius:14px;text-align:center;vertical-align:middle;font-size:28px;line-height:64px;">
                      &#127908;
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
          </table>
        </td>
      </tr>

      <!-- GOLD DIVIDER -->
      <tr>
        <td style="background:#f5a623;height:1px;font-size:0;line-height:0;">&nbsp;</td>
      </tr>

      <!-- BODY (white) -->
      <tr>
        <td style="background:#ffffff;padding:28px 32px 24px;">
          <div style="font-size:14px;line-height:1.8;color:#3f3f46;font-family:'Helvetica Neue',Arial,sans-serif;">
            %s
          </div>
        </td>
      </tr>

      <!-- WEBSITE BANNER -->
      <tr>
        <td style="background:#111113;padding:18px 32px 16px;text-align:center;">
          <a href="%s" style="text-decoration:none;">
            <table cellpadding="0" cellspacing="0" border="0" style="margin:0 auto;">
              <tr>
                <td style="font-size:15px;font-weight:800;color:#ffffff;letter-spacing:-0.4px;font-family:'Helvetica Neue',Arial,sans-serif;">MC</td>
                <td style="padding:0 4px 0 3px;vertical-align:middle;">
                  <table cellpadding="0" cellspacing="0" border="0">
                    <tr><td width="5" height="5" style="background:#f5a623;border-radius:50%%;font-size:0;line-height:0;">&nbsp;</td></tr>
                  </table>
                </td>
                <td style="font-size:15px;font-weight:800;color:#ffffff;letter-spacing:-0.4px;font-family:'Helvetica Neue',Arial,sans-serif;">Hub</td>
              </tr>
            </table>
            <p style="color:#52525b;font-size:9px;margin:4px 0 0 0;letter-spacing:1.2px;text-transform:uppercase;font-family:'Helvetica Neue',Arial,sans-serif;">mchub.vn &nbsp;&bull;&nbsp; AI Voice Training</p>
          </a>
        </td>
      </tr>

      <!-- SOCIAL ICONS -->
      <tr>
        <td style="background:#111113;padding:0 32px 18px;text-align:center;">
          <table cellpadding="0" cellspacing="0" border="0" style="margin:0 auto;">
            <tr>
              <!-- Facebook -->
              <td style="padding:0 6px;">
                <a href="https://facebook.com/mchub.vn" style="text-decoration:none;">
                  <table cellpadding="0" cellspacing="0" border="0">
                    <tr>
                      <td width="32" height="32" style="background:#1877f2;border-radius:8px;text-align:center;vertical-align:middle;font-size:15px;line-height:32px;color:#ffffff;font-weight:800;font-family:'Helvetica Neue',Arial,sans-serif;">f</td>
                    </tr>
                  </table>
                </a>
              </td>
              <!-- TikTok -->
              <td style="padding:0 6px;">
                <a href="https://tiktok.com/@mchub.vn" style="text-decoration:none;">
                  <table cellpadding="0" cellspacing="0" border="0">
                    <tr>
                      <td width="32" height="32" style="background:#010101;border:1px solid #333;border-radius:8px;text-align:center;vertical-align:middle;font-size:13px;line-height:32px;color:#ffffff;font-family:'Helvetica Neue',Arial,sans-serif;">&#9835;</td>
                    </tr>
                  </table>
                </a>
              </td>
              <!-- YouTube -->
              <td style="padding:0 6px;">
                <a href="https://youtube.com/@mchub" style="text-decoration:none;">
                  <table cellpadding="0" cellspacing="0" border="0">
                    <tr>
                      <td width="32" height="32" style="background:#ff0000;border-radius:8px;text-align:center;vertical-align:middle;font-size:13px;line-height:32px;color:#ffffff;font-family:'Helvetica Neue',Arial,sans-serif;">&#9654;</td>
                    </tr>
                  </table>
                </a>
              </td>
            </tr>
          </table>
        </td>
      </tr>

      <!-- FOOTER -->
      <tr>
        <td style="background:#fafafa;padding:14px 32px 20px;border-top:1px solid #f0f0f2;">
          <p style="color:#a1a1aa;font-size:11px;line-height:1.8;text-align:center;margin:0;font-family:'Helvetica Neue',Arial,sans-serif;">
            N&#7871;u b&#7841;n kh&#244;ng t&#7841;o t&#224;i kho&#7843;n n&#224;y, h&#227;y b&#7887; qua email n&#224;y.<br/>
            <span style="color:#d1d1d4;">&#169; %s MC Hub &middot; Vi&#7879;t Nam</span>
          </p>
        </td>
      </tr>

      <!-- BOTTOM ACCENT BAR -->
      <tr>
        <td style="background:#f5a623;height:4px;font-size:0;line-height:0;">&nbsp;</td>
      </tr>

    </table>
  </td></tr>
</table>
</body>
</html>
""".formatted(recipientName, trustedHtmlBody, feUrl, year);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String escapeAndFormatBody(String text, String recipientName) {
        if (text == null) return "";
        // Personalize
        String personalized = text
                .replace("{{name}}", recipientName)
                .replace("{{email}}", "");
        // Escape HTML special chars
        String escaped = personalized
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        // Convert newlines to <br> and wrap paragraphs
        String[] paragraphs = escaped.split("\\n\\n+");
        StringBuilder sb = new StringBuilder();
        for (String para : paragraphs) {
            if (para.isBlank()) continue;
            sb.append("<p style=\"margin:0 0 16px 0;\">")
              .append(para.replace("\n", "<br/>"))
              .append("</p>");
        }
        return sb.toString();
    }

    private String heroBgFor(String type) {
        if (type == null) return "linear-gradient(135deg,#1a1a2e 0%,#16213e 50%,#0f3460 100%)";
        return switch (type) {
            case "NEW_LESSON"     -> "linear-gradient(135deg,#0f172a 0%,#1e3a5f 100%)";
            case "DISCOUNT"       -> "linear-gradient(135deg,#1a0a00 0%,#3d1f00 100%)";
            case "MAINTENANCE"    -> "linear-gradient(135deg,#1a0000 0%,#3d0a0a 100%)";
            case "SOCIAL_POST"    -> "linear-gradient(135deg,#1a0014 0%,#3d0030 100%)";
            case "FEATURE_UPDATE" -> "linear-gradient(135deg,#0e0020 0%,#2d0060 100%)";
            case "COMPETITION"    -> "linear-gradient(135deg,#001a0a 0%,#003d1a 100%)";
            default               -> "linear-gradient(135deg,#0c0c0f 0%,#1a1a1e 100%)";
        };
    }

    private String bannerColorFor(String type) {
        if (type == null) return "#f5a623";
        return switch (type) {
            case "DISCOUNT"       -> "#f5a623";
            case "MAINTENANCE"    -> "#ef4444";
            case "SOCIAL_POST"    -> "#ec4899";
            case "FEATURE_UPDATE" -> "#a855f7";
            case "COMPETITION"    -> "#10b981";
            case "NEW_LESSON"     -> "#3b82f6";
            default               -> "#f5a623";
        };
    }

    private String typeLabelFor(String type) {
        if (type == null) return "Thông báo";
        return switch (type) {
            case "NEW_LESSON"     -> "Bài học mới";
            case "DISCOUNT"       -> "Khuyến mãi";
            case "MAINTENANCE"    -> "Bảo trì";
            case "SOCIAL_POST"    -> "Bài đăng mới";
            case "FEATURE_UPDATE" -> "Tính năng mới";
            case "COMPETITION"    -> "Thi đấu";
            default               -> "Thông báo";
        };
    }
}
