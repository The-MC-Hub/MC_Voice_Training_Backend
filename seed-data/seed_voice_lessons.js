/**
 * ================================================================
 * MC VOICE TRAINING — Seed: voice_lessons (20 lessons)
 * ================================================================
 * Chạy lệnh:
 *   mongosh "mongodb+srv://<user>:<pass>@<host>/voice-tranning?retryWrites=true&w=majority" \
 *     --file seed-data/seed_voice_lessons.js
 *
 * Hoặc local:
 *   mongosh mongodb://localhost:27017/voice-tranning \
 *     --file seed-data/seed_voice_lessons.js
 * ================================================================
 */

use("voice-tranning");

// ── CLEAR ────────────────────────────────────────────────────────
print("🗑️  Xóa voice_lessons cũ...");
db["voice_lessons"].deleteMany({});
print("✅ Đã xóa\n");

// ── HELPERS ──────────────────────────────────────────────────────
const now = new Date();

// Unsplash thumbnails — kích thước 800x450 (16:9), free to use
// Format: https://images.unsplash.com/photo-<id>?w=800&h=450&fit=crop&auto=format
const THUMBS = {
  WEDDING: [
    "https://images.unsplash.com/photo-1519225421980-715cb0215aed?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1606800052052-a08af7148866?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1583939003579-730e3918a45a?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1465495976277-4387d4b0b4c6?w=800&h=450&fit=crop&auto=format",
  ],
  GALA: [
    "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1511578314322-379afb476865?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1505373877841-8d25f7d46678?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1569263979104-865ab7cd8d13?w=800&h=450&fit=crop&auto=format",
  ],
  CORPORATE: [
    "https://images.unsplash.com/photo-1591115765373-5207764f72e7?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1475721027785-f74eccf877e2?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1515187029135-18ee286d815b?w=800&h=450&fit=crop&auto=format",
  ],
  TALKSHOW: [
    "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1478737270197-2468169085b0?w=800&h=450&fit=crop&auto=format",
  ],
  CEREMONY: [
    "https://images.unsplash.com/photo-1532712938310-34cb3982ef74?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1524178232363-1fb2b075b655?w=800&h=450&fit=crop&auto=format",
  ],
  GENERAL: [
    "https://images.unsplash.com/photo-1560523159-4a9692d222f9?w=800&h=450&fit=crop&auto=format",
    "https://images.unsplash.com/photo-1546953304-5d96f43c2e94?w=800&h=450&fit=crop&auto=format",
  ],
};

let ti = (cat, idx) => THUMBS[cat][idx % THUMBS[cat].length];

// ── SEED DATA ────────────────────────────────────────────────────
const lessons = [

  // ── WEDDING (4 bài) ──────────────────────────────────────────
  {
    title: "Khai Mạc Lễ Cưới Truyền Thống",
    category: "WEDDING",
    difficulty: "Easy",
    description: "Luyện tập lời khai mạc lễ cưới truyền thống Việt Nam với giọng điệu ấm áp, trang trọng và đầy cảm xúc.",
    thumbnailUrl: ti("WEDDING", 0),
    videoUrl: null,
    content: `Kính thưa quý vị quan khách, họ hàng hai bên gia đình, và các bạn bè thân hữu yêu quý!

Hôm nay, trong không khí trang trọng và đầy hân hoan này, chúng ta cùng nhau chứng kiến một trong những khoảnh khắc đẹp nhất và ý nghĩa nhất của cuộc đời - Lễ Thành Hôn của đôi uyên ương Minh Khoa và Lan Anh.

Với tất cả tình yêu thương và lời chúc phúc chân thành nhất, tôi xin trân trọng tuyên bố buổi lễ cưới hôm nay chính thức bắt đầu!`,
    evaluationCriteria: [
      { aspect: "PRONUNCIATION", weight: 30, description: "Phát âm chuẩn tiếng Việt, rõ ràng từng chữ" },
      { aspect: "EMOTION",       weight: 35, description: "Giọng ấm áp, cảm xúc chân thành, không khô cứng" },
      { aspect: "PACING",        weight: 20, description: "Tốc độ vừa phải, có điểm nhấn đúng chỗ" },
      { aspect: "RHYTHM",        weight: 15, description: "Nhịp điệu tự nhiên, không bị ngắt quãng đột ngột" },
    ],
    evaluationHint: "Wedding opening toast — warm, joyful, sincere. Avoid robotic or overly formal tone.",
    targetWpmMin: 110,
    targetWpmMax: 130,
    passingScore: 65,
    createdAt: now, updatedAt: now,
  },

  {
    title: "Giới Thiệu Cô Dâu Chú Rể",
    category: "WEDDING",
    difficulty: "Medium",
    description: "Kỹ thuật giới thiệu đôi uyên ương lên sân khấu với phong cách hào hứng, tạo không khí vỡ òa.",
    thumbnailUrl: ti("WEDDING", 1),
    videoUrl: null,
    content: `Và bây giờ, khoảnh khắc mà tất cả chúng ta đang chờ đợi đã đến!

Xin quý vị hãy dành một tràng pháo tay thật nồng nhiệt để chào đón hai nhân vật chính của buổi tối hôm nay - Chú rể của chúng ta, anh Nguyễn Văn Minh - một người đàn ông tài năng, chân thành và hết lòng yêu thương - cùng với Cô dâu xinh đẹp của chúng ta, chị Trần Thị Hoa - người phụ nữ dịu dàng, thông minh và rạng rỡ nhất buổi tối hôm nay!

Xin mời đôi uyên ương tiến vào lễ đường trong tiếng nhạc và tiếng vỗ tay chào đón của toàn thể quý khách!`,
    evaluationCriteria: [
      { aspect: "EMOTION",       weight: 35, description: "Hào hứng, vui tươi, tạo cao trào cho khoảnh khắc" },
      { aspect: "PACING",        weight: 25, description: "Build-up chậm, đỉnh điểm mạnh khi gọi tên" },
      { aspect: "PRONUNCIATION", weight: 25, description: "Tên nhân vật rõ ràng, không vấp" },
      { aspect: "RHYTHM",        weight: 15, description: "Nhịp điệu cuốn hút, không đều đều" },
    ],
    evaluationHint: "Bride and groom introduction — build excitement, peak at name announcement, crowd energy.",
    targetWpmMin: 120,
    targetWpmMax: 145,
    passingScore: 70,
    createdAt: now, updatedAt: now,
  },

  {
    title: "Lời Chúc Phúc Cuối Buổi Tiệc Cưới",
    category: "WEDDING",
    difficulty: "Easy",
    description: "Luyện tập lời kết thúc buổi tiệc cưới - cảm ơn khách mời và gửi lời chúc phúc đến đôi uyên ương.",
    thumbnailUrl: ti("WEDDING", 2),
    videoUrl: null,
    content: `Kính thưa toàn thể quý vị!

Buổi tiệc cưới hạnh phúc của gia đình hai họ đã đến lúc khép lại trong không khí vô cùng ấm áp và tràn đầy niềm vui.

Thay mặt hai gia đình, tôi xin gửi lời cảm ơn chân thành nhất đến toàn thể quý khách đã dành thời gian quý báu đến chúc mừng và chia sẻ niềm hạnh phúc này. Sự hiện diện của quý vị chính là món quà vô giá nhất đối với đôi uyên ương.

Xin kính chúc đôi tân hôn trăm năm hạnh phúc, xây dựng được tổ ấm yêu thương bền vững. Chúc toàn thể quý khách sức khỏe dồi dào và mọi điều tốt đẹp. Xin trân trọng cảm ơn!`,
    evaluationCriteria: [
      { aspect: "EMOTION",       weight: 30, description: "Giọng chân thành, cảm kích, không sáo rỗng" },
      { aspect: "PRONUNCIATION", weight: 30, description: "Rõ ràng, chắc chắn cho phần kết" },
      { aspect: "PACING",        weight: 25, description: "Chậm rãi, điềm tĩnh — không vội vã" },
      { aspect: "ACCURACY",      weight: 15, description: "Đọc đúng script, không bỏ sót ý chính" },
    ],
    evaluationHint: "Wedding closing — warm gratitude, unhurried pace, sincere blessing tone.",
    targetWpmMin: 100,
    targetWpmMax: 120,
    passingScore: 65,
    createdAt: now, updatedAt: now,
  },

  {
    title: "Dẫn Chương Trình Tiệc Cưới Hiện Đại",
    category: "WEDDING",
    difficulty: "Hard",
    description: "Kỹ năng MC tiệc cưới phong cách hiện đại: năng động, hài hước đúng mực, kết nối khán giả liên tục.",
    thumbnailUrl: ti("WEDDING", 3),
    videoUrl: null,
    content: `Wow wow wow! Không khí tối nay thật sự quá tuyệt vời, đúng không quý vị? Tôi tin rằng đây sẽ là một đêm mà tất cả chúng ta sẽ còn nhớ mãi!

Để chào đón tiết mục tiếp theo - màn cắt bánh cưới đặc biệt của đôi uyên ương - xin quý vị hãy cùng tôi đếm ngược nhé! Ba... Hai... Một... Và cùng vỗ tay nào!

Ôi chao, nhìn hai bạn hạnh phúc thế này, tôi nghĩ cả khán phòng đêm nay ai cũng cảm nhận được tình yêu lan toả từ đôi uyên ương đang ngời sáng trên sân khấu. Đúng không ạ?`,
    evaluationCriteria: [
      { aspect: "EMOTION",       weight: 30, description: "Năng lượng cao, hài hước tự nhiên, không gượng gạo" },
      { aspect: "PACING",        weight: 25, description: "Biết tăng giảm nhịp, tạo đỉnh điểm đúng lúc" },
      { aspect: "RHYTHM",        weight: 25, description: "Nhịp điệu linh hoạt, tương tác với khán giả" },
      { aspect: "PRONUNCIATION", weight: 20, description: "Rõ ràng kể cả khi nói nhanh" },
    ],
    evaluationHint: "Modern wedding MC — high energy, playful but respectful, crowd interaction, countdown delivery.",
    targetWpmMin: 130,
    targetWpmMax: 160,
    passingScore: 75,
    createdAt: now, updatedAt: now,
  },

  // ── GALA (4 bài) ─────────────────────────────────────────────
  {
    title: "Khai Mạc Gala Dinner Cuối Năm",
    category: "GALA",
    difficulty: "Medium",
    description: "Luyện tập lời khai mạc Gala Dinner trang trọng, tạo không khí ấm áp và hứng khởi cho buổi tối.",
    thumbnailUrl: ti("GALA", 0),
    videoUrl: null,
    content: `Kính thưa Ban Lãnh Đạo Tập đoàn, kính thưa toàn thể quý vị đại biểu và các bạn đồng nghiệp thân mến!

Một năm đã trôi qua với bao thăng trầm và thử thách, nhưng chúng ta đã cùng nhau vượt qua và đạt được những thành tựu đáng tự hào. Tối nay, Gala Dinner Cuối Năm không chỉ là dịp để chúng ta nhìn lại chặng đường đã qua, mà còn là cơ hội để cùng nhau hướng đến những chân trời mới đầy hứa hẹn phía trước.

Tôi xin trân trọng tuyên bố Gala Dinner thường niên năm nay chính thức khai mạc!`,
    evaluationCriteria: [
      { aspect: "PRONUNCIATION", weight: 30, description: "Chuẩn mực, rõ ràng, phong cách corporate" },
      { aspect: "PACING",        weight: 25, description: "Chậm rãi, đĩnh đạc — không vội" },
      { aspect: "EMOTION",       weight: 25, description: "Trang trọng nhưng ấm áp, không lạnh lùng" },
      { aspect: "RHYTHM",        weight: 20, description: "Nhịp điệu đều đặn, có dấu lặng đúng chỗ" },
    ],
    evaluationHint: "Corporate gala opening — dignified, warm, celebratory. Balance formal register with genuine warmth.",
    targetWpmMin: 110,
    targetWpmMax: 130,
    passingScore: 70,
    createdAt: now, updatedAt: now,
  },

  {
    title: "Dẫn Phần Cocktail Reception",
    category: "GALA",
    difficulty: "Easy",
    description: "Kỹ năng dẫn dắt phần cocktail reception, tạo không khí giao lưu thoải mái trước chương trình chính.",
    thumbnailUrl: ti("GALA", 1),
    videoUrl: null,
    content: `Kính thưa quý vị!

Trong khi chúng ta thưởng thức những ly cocktail tinh tế và âm nhạc nhẹ nhàng của buổi tối, đây chính là thời điểm lý tưởng để giao lưu, kết nối và chia sẻ những câu chuyện thú vị với nhau.

Buffet cocktail đã được chuẩn bị chu đáo tại khu vực bên phải của sảnh. Xin quý vị tự nhiên thưởng thức và tận hưởng những phút giây thư giãn trước khi chương trình chính thức bắt đầu vào lúc bảy giờ tối.

Chúng tôi rất mong quý vị có một buổi tối thật vui vẻ và ý nghĩa. Cheers!`,
    evaluationCriteria: [
      { aspect: "EMOTION",       weight: 30, description: "Thoải mái, thân thiện, không quá cứng nhắc" },
      { aspect: "PRONUNCIATION", weight: 30, description: "Rõ ràng thông tin (giờ giấc, địa điểm)" },
      { aspect: "PACING",        weight: 25, description: "Nhẹ nhàng, không vội, phù hợp cocktail mood" },
      { aspect: "RHYTHM",        weight: 15, description: "Tự nhiên, conversational" },
    ],
    evaluationHint: "Cocktail reception announcement — relaxed, welcoming, informative without being stiff.",
    targetWpmMin: 110,
    targetWpmMax: 135,
    passingScore: 65,
    createdAt: now, updatedAt: now,
  },

  {
    title: "Giới Thiệu Ban Lãnh Đạo Tại Gala",
    category: "GALA",
    difficulty: "Medium",
    description: "Kỹ thuật giới thiệu ban lãnh đạo tại sự kiện Gala với sự trang trọng và tôn trọng đúng mực.",
    thumbnailUrl: ti("GALA", 2),
    videoUrl: null,
    content: `Kính thưa toàn thể quý vị!

Để chương trình Gala tối nay có thể diễn ra thành công, chúng tôi xin trân trọng giới thiệu sự hiện diện của những vị lãnh đạo tài ba và tận tâm đã dẫn dắt tập đoàn chúng ta vượt qua mọi thử thách trong năm vừa qua.

Kính mời quý vị đón chào: Ông Nguyễn Văn Thành - Chủ tịch Hội đồng Quản trị; Bà Trần Thị Minh Châu - Tổng Giám đốc Điều hành; cùng toàn thể quý vị thành viên Ban Lãnh Đạo Tập đoàn.

Xin dành một tràng pháo tay trân trọng nhất!`,
    evaluationCriteria: [
      { aspect: "PRONUNCIATION", weight: 35, description: "Tên và chức vụ phải rõ ràng, không vấp" },
      { aspect: "PACING",        weight: 30, description: "Dừng sau mỗi tên để tạo điểm nhấn" },
      { aspect: "EMOTION",       weight: 20, description: "Trang trọng, kính trọng — không thân mật quá" },
      { aspect: "RHYTHM",        weight: 15, description: "Nhịp điệu đều, không đọc liên tục như danh sách" },
    ],
    evaluationHint: "Executive introduction at gala — respectful, deliberate pacing between names, formal register.",
    targetWpmMin: 100,
    targetWpmMax: 120,
    passingScore: 70,
    createdAt: now, updatedAt: now,
  },

  {
    title: "Vinh Danh Nhân Viên Xuất Sắc",
    category: "GALA",
    difficulty: "Hard",
    description: "Dẫn dắt phần trao giải vinh danh nhân viên xuất sắc — tạo cảm xúc, hồi hộp và tự hào cho người được vinh danh.",
    thumbnailUrl: ti("GALA", 3),
    videoUrl: null,
    content: `Và bây giờ là khoảnh khắc được mong chờ nhất trong đêm nay - Lễ Vinh Danh Nhân Viên Xuất Sắc của Năm!

Người được vinh danh tối nay không chỉ xuất sắc về thành tích công việc, mà còn là tấm gương sáng về tinh thần cống hiến, lòng nhiệt huyết và đạo đức nghề nghiệp cho toàn thể công ty.

Sau đây, tôi xin trân trọng mời Ban Giám Đốc lên sân khấu để công bố tên của nhân viên xuất sắc nhất năm nay...

Và người nhận danh hiệu đó chính là... Anh Lê Hoàng Nam - Trưởng phòng Kinh doanh khu vực phía Nam! Xin nhiệt liệt chúc mừng!`,
    evaluationCriteria: [
      { aspect: "EMOTION",       weight: 35, description: "Build suspense → explosive reveal, genuine excitement" },
      { aspect: "PACING",        weight: 30, description: "Pause before name reveal — let silence build tension" },
      { aspect: "PRONUNCIATION", weight: 20, description: "Name + title crystal clear" },
      { aspect: "RHYTHM",        weight: 15, description: "Dramatic rhythm: slow build, fast peak" },
    ],
    evaluationHint: "Award reveal — build tension with slow deliberate pace, pause before name, then explosive announcement.",
    targetWpmMin: 115,
    targetWpmMax: 145,
    passingScore: 75,
    createdAt: now, updatedAt: now,
  },

  // ── CORPORATE (3 bài) ─────────────────────────────────────────
  {
    title: "Khai Mạc Hội Nghị Doanh Nghiệp",
    category: "CORPORATE",
    difficulty: "Medium",
    description: "Luyện tập dẫn chương trình khai mạc hội nghị doanh nghiệp quy mô lớn với phong cách chuyên nghiệp.",
    thumbnailUrl: ti("CORPORATE", 0),
    videoUrl: null,
    content: `Kính thưa Quý vị Đại biểu, các nhà lãnh đạo doanh nghiệp, các chuyên gia và toàn thể quý khách tham dự!

Chào mừng quý vị đến với Hội Nghị Thường Niên năm nay - nơi quy tụ những tư duy tiên phong và những cam kết mạnh mẽ cho sự phát triển bền vững của ngành.

Hội nghị hôm nay quy tụ hơn năm trăm đại biểu đến từ nhiều tỉnh thành và quốc gia khác nhau. Chúng tôi tin rằng những cuộc đối thoại, những ý tưởng và những quyết sách được đưa ra tại đây sẽ góp phần định hình tương lai của ngành trong những năm tới.

Trân trọng tuyên bố hội nghị chính thức khai mạc!`,
    evaluationCriteria: [
      { aspect: "PRONUNCIATION", weight: 35, description: "Corporate vocabulary chuẩn, không nhầm số liệu" },
      { aspect: "PACING",        weight: 30, description: "Đĩnh đạc, không vội — phong cách lãnh đạo" },
      { aspect: "EMOTION",       weight: 20, description: "Tự tin, uy tín, truyền cảm hứng" },
      { aspect: "ACCURACY",      weight: 15, description: "Số liệu (500 đại biểu) phải đọc đúng" },
    ],
    evaluationHint: "Corporate conference — authoritative, inspiring, precise with numbers and titles.",
    targetWpmMin: 115,
    targetWpmMax: 135,
    passingScore: 70,
    createdAt: now, updatedAt: now,
  },

  {
    title: "Giới Thiệu Diễn Giả Chính",
    category: "CORPORATE",
    difficulty: "Medium",
    description: "Kỹ thuật giới thiệu diễn giả chính tại hội thảo — tạo sự kỳ vọng và tôn vinh đúng mức cho diễn giả.",
    thumbnailUrl: ti("CORPORATE", 1),
    videoUrl: null,
    content: `Thưa toàn thể quý vị!

Tiếp theo chương trình, chúng tôi xin trân trọng giới thiệu đến quý vị vị diễn giả đặc biệt của buổi sáng hôm nay.

Ông là Tiến sĩ Phạm Tuấn Anh - chuyên gia hàng đầu trong lĩnh vực chuyển đổi số và đổi mới sáng tạo tại Việt Nam. Với hơn hai mươi năm kinh nghiệm thực tiễn và đã tư vấn cho hơn một trăm doanh nghiệp lớn trong và ngoài nước, ông sẽ chia sẻ những góc nhìn chiến lược về chủ đề "Chuyển Đổi Số - Chìa Khóa Thành Công Trong Kỷ Nguyên Mới".

Kính mời Tiến sĩ Phạm Tuấn Anh!`,
    evaluationCriteria: [
      { aspect: "PRONUNCIATION", weight: 35, description: "Học hàm, tên, số liệu kinh nghiệm rõ ràng" },
      { aspect: "PACING",        weight: 25, description: "Build-up, dừng nhẹ trước tên diễn giả" },
      { aspect: "EMOTION",       weight: 25, description: "Trân trọng, hào hứng — tạo kỳ vọng cho khán giả" },
      { aspect: "RHYTHM",        weight: 15, description: "Liền mạch, không đứt đoạn giữa thông tin" },
    ],
    evaluationHint: "Speaker intro — build credibility through credentials, create anticipation, respectful tone.",
    targetWpmMin: 115,
    targetWpmMax: 135,
    passingScore: 70,
    createdAt: now, updatedAt: now,
  },

  {
    title: "Kết Thúc Hội Nghị Và Cảm Ơn",
    category: "CORPORATE",
    difficulty: "Easy",
    description: "Luyện tập lời kết thúc hội nghị chuyên nghiệp, cảm ơn diễn giả, đơn vị tài trợ và đại biểu.",
    thumbnailUrl: ti("CORPORATE", 2),
    videoUrl: null,
    content: `Kính thưa toàn thể quý đại biểu!

Hội nghị của chúng ta đã đến hồi kết trong bầu không khí đầy phấn khởi và nhiều cảm hứng. Thay mặt Ban Tổ Chức, tôi xin chân thành cảm ơn tất cả các diễn giả đã chia sẻ những kiến thức và kinh nghiệm quý báu; cảm ơn các đơn vị tài trợ đã đồng hành và tạo điều kiện cho hội nghị được tổ chức thành công; và đặc biệt cảm ơn toàn thể quý đại biểu đã dành thời gian tham dự và đóng góp ý kiến sôi nổi trong suốt cả ngày hôm nay.

Chúng tôi mong được gặp lại quý vị tại hội nghị năm sau. Chúc quý vị sức khỏe, thành công và thượng lộ bình an!`,
    evaluationCriteria: [
      { aspect: "EMOTION",       weight: 30, description: "Chân thành, biết ơn, không công thức" },
      { aspect: "PRONUNCIATION", weight: 30, description: "Rõ ràng, đặc biệt phần liệt kê đối tượng cảm ơn" },
      { aspect: "PACING",        weight: 25, description: "Nhẹ nhàng, không vội kết thúc" },
      { aspect: "ACCURACY",      weight: 15, description: "Không bỏ sót đối tượng cảm ơn" },
    ],
    evaluationHint: "Conference closing — genuine gratitude, unhurried, inclusive (speakers, sponsors, attendees).",
    targetWpmMin: 105,
    targetWpmMax: 125,
    passingScore: 65,
    createdAt: now, updatedAt: now,
  },

  // ── TALKSHOW (2 bài) ─────────────────────────────────────────
  {
    title: "Mở Đầu Chương Trình Talkshow",
    category: "TALKSHOW",
    difficulty: "Medium",
    description: "Kỹ năng mở đầu talkshow thu hút — đặt vấn đề gây tò mò, giới thiệu chủ đề và khách mời hấp dẫn.",
    thumbnailUrl: ti("TALKSHOW", 0),
    videoUrl: null,
    content: `Xin chào tất cả mọi người và chào mừng đến với chương trình talkshow của chúng ta!

Các bạn có bao giờ tự hỏi: điều gì thực sự tạo nên sự thành công của một doanh nhân? Là tài năng thiên bẩm, hay là những thất bại được chuyển hóa thành bài học quý giá? Tối nay, chúng ta sẽ cùng nhau khám phá câu trả lời qua cuộc trò chuyện thẳng thắn và cởi mở với một doanh nhân đặc biệt.

Khách mời của chúng ta tối nay đã xây dựng từ con số không đến một doanh nghiệp trị giá hàng trăm tỷ đồng, và quan trọng hơn - ông sẵn sàng kể cho chúng ta nghe cả những lần vấp ngã. Hãy cùng chào đón ông Trần Đức Thịnh!`,
    evaluationCriteria: [
      { aspect: "EMOTION",       weight: 30, description: "Tạo tò mò, hứng khởi từ câu hỏi mở đầu" },
      { aspect: "PACING",        weight: 25, description: "Nhịp talkshow: thân mật, không quá trang trọng" },
      { aspect: "PRONUNCIATION", weight: 25, description: "Tự nhiên như đang nói chuyện thật" },
      { aspect: "RHYTHM",        weight: 20, description: "Build-up từ câu hỏi đến giới thiệu khách mời" },
    ],
    evaluationHint: "Talkshow opening — conversational, curious, build intrigue before guest reveal.",
    targetWpmMin: 125,
    targetWpmMax: 150,
    passingScore: 70,
    createdAt: now, updatedAt: now,
  },

  {
    title: "Dẫn Dắt Phần Hỏi - Đáp Talkshow",
    category: "TALKSHOW",
    difficulty: "Hard",
    description: "Kỹ thuật đặt câu hỏi và dẫn dắt cuộc trò chuyện trong talkshow — linh hoạt, sâu sắc và tạo điểm nhấn.",
    thumbnailUrl: ti("TALKSHOW", 1),
    videoUrl: null,
    content: `Cảm ơn ông rất nhiều vì câu chuyện vừa rồi! Thật sự tôi tin là ai trong khán phòng tối nay cũng đã cảm nhận được sức mạnh của sự kiên trì qua hành trình của ông.

Nhưng tôi muốn đi sâu hơn một chút - vào cái khoảnh khắc tối tăm nhất, khi doanh nghiệp đứng bên bờ vực thất bại, điều gì đã giữ ông ở lại? Gia đình? Trách nhiệm với nhân viên? Hay đơn giản là ông chưa biết mình đang bỏ cuộc là gì?

Và sau tất cả những điều đó, nếu phải gửi một lời khuyên duy nhất đến những bạn trẻ đang khởi nghiệp trong khán phòng hôm nay, ông sẽ nói gì?`,
    evaluationCriteria: [
      { aspect: "EMOTION",       weight: 30, description: "Thấu cảm, tò mò thật sự — không đọc câu hỏi" },
      { aspect: "PACING",        weight: 30, description: "Conversational, organic — không rush" },
      { aspect: "RHYTHM",        weight: 25, description: "Chuyển tiếp mượt từ khen → câu hỏi sâu → câu hỏi kết" },
      { aspect: "PRONUNCIATION", weight: 15, description: "Tự nhiên, không scripted" },
    ],
    evaluationHint: "Talkshow Q&A — empathetic, layered questions, conversational not journalistic interrogation.",
    targetWpmMin: 120,
    targetWpmMax: 150,
    passingScore: 75,
    createdAt: now, updatedAt: now,
  },

  // ── CEREMONY (2 bài) ─────────────────────────────────────────
  {
    title: "Khai Mạc Lễ Tốt Nghiệp",
    category: "CEREMONY",
    difficulty: "Medium",
    description: "Dẫn chương trình lễ tốt nghiệp — trang trọng, truyền cảm hứng và ghi dấu khoảnh khắc lịch sử của sinh viên.",
    thumbnailUrl: ti("CEREMONY", 0),
    videoUrl: null,
    content: `Kính thưa Ban Giám Hiệu, quý thầy cô giáo, quý phụ huynh và toàn thể các tân cử nhân thân mến!

Hôm nay là một ngày đặc biệt - một ngày mà các bạn đã nỗ lực, phấn đấu suốt bốn năm đại học để có được. Các bạn đứng ở đây không chỉ là kết thúc của một hành trình, mà là điểm khởi đầu của một chương mới - rộng lớn hơn, thú vị hơn và đầy tiềm năng hơn bao giờ hết.

Trân trọng tuyên bố Lễ Trao Bằng Tốt Nghiệp năm học này chính thức bắt đầu!`,
    evaluationCriteria: [
      { aspect: "EMOTION",       weight: 35, description: "Truyền cảm hứng, tự hào, cảm xúc đích thực" },
      { aspect: "PRONUNCIATION", weight: 30, description: "Rõ ràng, đĩnh đạc phù hợp học đường" },
      { aspect: "PACING",        weight: 20, description: "Vừa phải, có trọng lượng" },
      { aspect: "RHYTHM",        weight: 15, description: "Nhịp điệu tự nhiên, có điểm nhấn" },
    ],
    evaluationHint: "Graduation ceremony — inspiring, celebratory, honor the milestone. Mix pride with forward-looking energy.",
    targetWpmMin: 110,
    targetWpmMax: 130,
    passingScore: 70,
    createdAt: now, updatedAt: now,
  },

  {
    title: "Lễ Khánh Thành Công Trình",
    category: "CEREMONY",
    difficulty: "Hard",
    description: "Kỹ năng dẫn chương trình lễ khánh thành — mang tính lịch sử, tri ân và nhìn về tương lai.",
    thumbnailUrl: ti("CEREMONY", 1),
    videoUrl: null,
    content: `Kính thưa các đồng chí lãnh đạo, kính thưa quý vị đại biểu, và toàn thể bà con nhân dân!

Ngày hôm nay đánh dấu một cột mốc lịch sử quan trọng - sau hơn ba năm xây dựng với bao mồ hôi và tâm huyết của hàng nghìn công nhân, kỹ sư và những người tâm huyết, công trình hạ tầng trọng điểm này chính thức hoàn thành và đi vào hoạt động.

Đây không chỉ là một công trình xây dựng - đây là biểu tượng của ý chí, của sự đoàn kết và của khát vọng phát triển của chúng ta. Và hôm nay, chúng ta cùng nhau viết thêm một trang mới vào lịch sử của vùng đất này!

Kính mời quý vị cùng tham gia vào lễ khánh thành lịch sử!`,
    evaluationCriteria: [
      { aspect: "EMOTION",       weight: 30, description: "Lịch sử, tự hào, uy nghiêm — không drama" },
      { aspect: "PRONUNCIATION", weight: 30, description: "Chuẩn chính trị, không vấp từ ngữ trang trọng" },
      { aspect: "PACING",        weight: 25, description: "Chậm rãi, mỗi câu có trọng lượng" },
      { aspect: "RHYTHM",        weight: 15, description: "Tuyên ngôn, không đều đều" },
    ],
    evaluationHint: "Inauguration ceremony — official register, historic gravitas, patriotic but not theatrical.",
    targetWpmMin: 100,
    targetWpmMax: 120,
    passingScore: 75,
    createdAt: now, updatedAt: now,
  },

  // ── GENERAL (3 bài) ──────────────────────────────────────────
  {
    title: "Kỹ Thuật Hít Thở Và Kiểm Soát Giọng",
    category: "GENERAL",
    difficulty: "Easy",
    description: "Bài luyện tập nền tảng về hít thở đúng cách và kiểm soát âm lượng giọng nói — thiết yếu cho mọi MC.",
    thumbnailUrl: ti("GENERAL", 0),
    videoUrl: null,
    content: `Xin chào các bạn! Hãy bắt đầu với bài luyện tập hít thở cơ bản.

Hãy đứng thẳng, vai thả lỏng, và hít thở sâu vào bụng - không phải ngực. Khi hít vào, bụng phình ra. Khi thở ra, bụng xẹp xuống. Đây là nền tảng để có một giọng nói khỏe và vang.

Bây giờ hãy đọc to: "Xin chào" - và chú ý giữ hơi thở ổn định từ đầu đến cuối câu. Không để giọng xuống ở cuối câu. Năng lượng phải đều và ổn định. Thử lại nào!`,
    evaluationCriteria: [
      { aspect: "PACING",        weight: 30, description: "Tốc độ đều, không tăng đột ngột do thiếu hơi" },
      { aspect: "PRONUNCIATION", weight: 30, description: "Âm lượng đồng đều từ đầu đến cuối câu" },
      { aspect: "RHYTHM",        weight: 25, description: "Nhịp thở ổn định phản ánh qua nhịp đọc" },
      { aspect: "EMOTION",       weight: 15, description: "Tự tin, không lo lắng" },
    ],
    evaluationHint: "Breathing exercise — evaluate steady volume, no trailing off at end of sentences, consistent energy.",
    targetWpmMin: 100,
    targetWpmMax: 120,
    passingScore: 60,
    createdAt: now, updatedAt: now,
  },

  {
    title: "Luyện Ngữ Điệu Và Nhấn Nhá",
    category: "GENERAL",
    difficulty: "Medium",
    description: "Phân tích và luyện tập ngữ điệu tiếng Việt: các dấu giọng, nhấn nhá đúng chỗ, và tránh đọc đều đều.",
    thumbnailUrl: ti("GENERAL", 1),
    videoUrl: null,
    content: `Trong tiếng Việt, ngữ điệu là linh hồn của ngôn ngữ. Cùng một câu nhưng ngữ điệu khác nhau sẽ mang lại cảm xúc hoàn toàn khác.

Hãy thử đọc câu sau với ba cách khác nhau - Câu một: "Chào mừng quý vị đến với chương trình" - bình thường. Câu hai: nhấn mạnh vào "chào mừng". Câu ba: nhấn mạnh vào "quý vị".

Bạn có cảm nhận được sự khác biệt không? Khi nhấn đúng từ khóa, thông điệp trở nên rõ ràng và thuyết phục hơn nhiều. Đây là kỹ năng phân biệt MC chuyên nghiệp với người đọc script thông thường.`,
    evaluationCriteria: [
      { aspect: "RHYTHM",        weight: 35, description: "Ngữ điệu đa dạng, không đều đều một tông" },
      { aspect: "EMOTION",       weight: 30, description: "Nhấn nhá có chủ đích, không ngẫu nhiên" },
      { aspect: "PRONUNCIATION", weight: 20, description: "Dấu giọng 6 thanh tiếng Việt chuẩn" },
      { aspect: "PACING",        weight: 15, description: "Đủ chậm để ngữ điệu rõ" },
    ],
    evaluationHint: "Intonation training — listen for tonal variety, intentional stress on keywords, avoid monotone delivery.",
    targetWpmMin: 110,
    targetWpmMax: 130,
    passingScore: 70,
    createdAt: now, updatedAt: now,
  },

  {
    title: "Xử Lý Tình Huống Bất Ngờ Trên Sân Khấu",
    category: "GENERAL",
    difficulty: "Hard",
    description: "Luyện tập phản xạ ngôn ngữ khi đối mặt với sự cố: mic hỏng, khán giả ồn, chương trình delay.",
    thumbnailUrl: ti("GENERAL", 0),
    videoUrl: null,
    content: `Thưa quý vị, có lẽ quý vị đã nhận ra rằng chúng ta đang gặp một chút... thử thách kỹ thuật! Nhưng đó cũng chính là điều làm cho buổi tối hôm nay trở nên đáng nhớ, phải không ạ?

Trong khi đội ngũ kỹ thuật tài ba của chúng ta đang nhanh chóng khắc phục sự cố, tôi muốn tranh thủ khoảng thời gian quý báu này để hỏi thăm quý vị - buổi tối hôm nay quý vị cảm thấy thế nào? Phần nào trong chương trình làm quý vị ấn tượng nhất?

Được rồi! Tôi nhận được tin hiệu tốt từ đội kỹ thuật - chúng ta sẽ sớm tiếp tục chương trình. Cảm ơn sự kiên nhẫn tuyệt vời của quý vị!`,
    evaluationCriteria: [
      { aspect: "EMOTION",       weight: 35, description: "Bình tĩnh, hài hước nhẹ nhàng — không hốt hoảng" },
      { aspect: "PACING",        weight: 25, description: "Chậm rãi, kiểm soát — không vội vã" },
      { aspect: "RHYTHM",        weight: 25, description: "Improvised nhưng có cấu trúc: nhận vấn đề → bridge → kết" },
      { aspect: "PRONUNCIATION", weight: 15, description: "Rõ ràng kể cả khi ứng xử bất ngờ" },
    ],
    evaluationHint: "Crisis handling — calm humor, redirect audience attention, bridge to resolution. No panic energy.",
    targetWpmMin: 115,
    targetWpmMax: 140,
    passingScore: 75,
    createdAt: now, updatedAt: now,
  },

];

// ── INSERT ────────────────────────────────────────────────────────
print(`📝 Inserting ${lessons.length} voice lessons...`);
const result = db["voice_lessons"].insertMany(lessons);
print(`✅ Inserted ${result.insertedIds ? Object.keys(result.insertedIds).length : "?"} lessons`);

// ── VERIFY ────────────────────────────────────────────────────────
print("\n📊 Count by category:");
["WEDDING","GALA","CORPORATE","TALKSHOW","CEREMONY","GENERAL"].forEach(cat => {
  const n = db["voice_lessons"].countDocuments({ category: cat });
  print(`  ${cat}: ${n}`);
});
print(`  TOTAL: ${db["voice_lessons"].countDocuments({})}`);
print("\n✅ Seed hoàn tất!");
