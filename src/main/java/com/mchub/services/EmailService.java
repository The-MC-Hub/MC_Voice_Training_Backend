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
<body style="margin:0;padding:0;background:#0a0a0c;font-family:'Helvetica Neue',Arial,sans-serif;">
<table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#0a0a0c;padding:40px 16px;">
  <tr><td align="center">
    <table width="580" cellpadding="0" cellspacing="0" border="0"
      style="max-width:580px;width:100%%;border-radius:24px;overflow:hidden;box-shadow:0 24px 64px rgba(0,0,0,0.6);border:1px solid #1f1f23;">

      <!-- TOP ACCENT -->
      <tr><td style="background:linear-gradient(90deg,#f5a623,#fbbf24,#f5a623);height:3px;"></td></tr>

      <!-- HEADER -->
      <tr>
        <td style="background:#09090b;padding:22px 32px 18px;">
          <table width="100%%" cellpadding="0" cellspacing="0" border="0">
            <tr>
              <td>
                <table cellpadding="0" cellspacing="0" border="0">
                  <tr>
                    <td style="font-size:18px;font-weight:800;color:#fff;letter-spacing:-0.5px;">MC</td>
                    <td style="padding:0 3px;vertical-align:middle;">
                      <span style="display:inline-block;width:5px;height:5px;border-radius:50%%;background:#f5a623;"></span>
                    </td>
                    <td style="font-size:18px;font-weight:800;color:#fff;letter-spacing:-0.5px;">Hub</td>
                  </tr>
                </table>
                <p style="color:#3f3f46;font-size:9px;margin-top:2px;letter-spacing:1px;text-transform:uppercase;">AI Voice Training Platform</p>
              </td>
              <td align="right" style="vertical-align:middle;">
                <span style="display:inline-block;background:rgba(16,185,129,0.12);color:#34d399;font-size:9px;font-weight:700;padding:4px 10px;border-radius:4px;letter-spacing:1px;text-transform:uppercase;border:1px solid rgba(52,211,153,0.2);">✦ Xác thực</span>
              </td>
            </tr>
          </table>
        </td>
      </tr>

      <!-- HERO -->
      <tr>
        <td style="background:linear-gradient(135deg,#071a10 0%%,#0a2818 50%%,#0d3320 100%%;padding:0;position:relative;">
          <table width="100%%" cellpadding="0" cellspacing="0" border="0">
            <tr>
              <td style="padding:40px 32px 36px;">
                <p style="color:rgba(52,211,153,0.5);font-size:10px;letter-spacing:2px;text-transform:uppercase;margin-bottom:14px;font-weight:600;">Xác thực tài khoản</p>
                <h1 style="color:#ffffff;font-size:28px;font-weight:800;line-height:1.25;letter-spacing:-0.8px;margin:0 0 10px 0;">
                  Chào <span style="color:#34d399;">%s</span> 👋
                </h1>
                <p style="color:rgba(255,255,255,0.45);font-size:13px;margin:0;line-height:1.5;">Chỉ một bước nữa để bắt đầu hành trình luyện giọng.</p>
              </td>
              <td style="padding:40px 32px 36px 0;vertical-align:middle;" align="right">
                <!-- mic icon -->
                <div style="width:64px;height:64px;border-radius:16px;background:rgba(52,211,153,0.08);border:1px solid rgba(52,211,153,0.15);display:inline-flex;align-items:center;justify-content:center;">
                  <svg width="28" height="28" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <rect x="9" y="2" width="6" height="11" rx="3" fill="#34d399" opacity="0.8"/>
                    <path d="M5 10c0 3.866 3.134 7 7 7s7-3.134 7-7" stroke="#34d399" stroke-width="1.5" stroke-linecap="round"/>
                    <line x1="12" y1="17" x2="12" y2="21" stroke="#34d399" stroke-width="1.5" stroke-linecap="round"/>
                    <line x1="9" y1="21" x2="15" y2="21" stroke="#34d399" stroke-width="1.5" stroke-linecap="round"/>
                  </svg>
                </div>
              </td>
            </tr>
          </table>
        </td>
      </tr>

      <!-- THIN LINE -->
      <tr><td style="background:linear-gradient(90deg,transparent,#10b981,transparent);height:1px;"></td></tr>

      <!-- BODY -->
      <tr>
        <td style="background:#0d0d0f;padding:32px 32px 28px;">
          <div style="font-size:14px;line-height:1.75;color:#a1a1aa;">
            %s
          </div>
        </td>
      </tr>

      <!-- FOOTER -->
      <tr>
        <td style="background:#09090b;padding:18px 32px 24px;border-top:1px solid #1a1a1d;">
          <p style="color:#27272a;font-size:11px;line-height:1.8;text-align:center;margin:0;">
            Nếu bạn không tạo tài khoản này, hãy bỏ qua email này.<br/>
            <span style="color:#1f1f23;">© %s MC Hub · Việt Nam</span>
          </p>
        </td>
      </tr>

      <!-- BOTTOM ACCENT -->
      <tr><td style="background:linear-gradient(90deg,#f5a623,#fbbf24,#f5a623);height:3px;"></td></tr>

    </table>
  </td></tr>
</table>
</body>
</html>
""".formatted(recipientName, trustedHtmlBody, year);
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
