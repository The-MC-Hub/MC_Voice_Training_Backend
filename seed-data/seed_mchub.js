/**
 * ================================================================
 * THE MC HUB — Complete Seed Data (v2.1 — 17 Collections)
 * ================================================================
 * Chạy lệnh:
 *   mongosh "mongodb+srv://trungle:Pitngu%401234@maindatabase.2tirj0y.mongodb.net/mchub?retryWrites=true&w=majority&appName=MainDatabase" --file seed-data/seed_mchub.js
 * ================================================================
 */

use("mchub");

// ── HELPERS ────────────────────────────────────────────────────
const rDate = (s, e) => new Date(s.getTime() + Math.random()*(e.getTime()-s.getTime()));
const pick  = arr => arr[Math.floor(Math.random()*arr.length)];
const rInt  = (min,max) => Math.floor(Math.random()*(max-min+1))+min;
const now   = new Date();
const D2024 = new Date("2024-01-01");
const D2025 = new Date("2025-01-01");
const D2026 = new Date("2026-01-01");
const D2026E= new Date("2026-12-31");

// BCrypt hash của "password123"
const HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHHy";

// ── CLEAR ALL ──────────────────────────────────────────────────
print("🗑️  Xóa dữ liệu cũ...");
["users","mcprofiles","bookings","bookingdetails","transactions","reviews",
 "schedules","notifications","messages","conversations","scripts",
 "favorites","reports","certificates","coupons","refreshtokens","auditlogs"
].forEach(col => db[col].deleteMany({}));
print("✅ Xóa xong!\n");

// ================================================================
// 1. USERS — 35 users (5 admin, 15 MC, 15 client)
// ================================================================
print("👥 [1/17] Tạo Users...");

const adminIds  = Array.from({length:5},  ()=>new ObjectId());
const mcIds     = Array.from({length:15}, ()=>new ObjectId());
const clientIds = Array.from({length:15}, ()=>new ObjectId());

const mcNames = [
  "Trần Minh Khoa","Nguyễn Thị Lan Anh","Lê Hoàng Nam","Phạm Thu Hằng","Đỗ Quang Vinh",
  "Võ Thị Mai Thy","Bùi Trọng Nghĩa","Hoàng Yến Nhi","Đinh Công Tuấn","Lý Thanh Thảo",
  "Phan Đức Hiếu","Tô Thị Bảo Châu","Ngô Xuân Trường","Vũ Khánh Linh","Trịnh Văn Hùng"
];
const clientNames = [
  "Nguyễn Văn An","Trần Thị Bình","Lê Minh Cường","Phạm Ngọc Dung","Đặng Hoài Đức",
  "Vũ Thị Hồng Nhung","Bùi Thanh Giang","Hoàng Trung Hiếu","Đinh Thị Lan","Cao Văn Khánh",
  "Tống Thị Mỹ Linh","Đỗ Minh Mạnh","Lý Thị Ngọc Ánh","Phan Thanh Phong","Chu Thị Quỳnh"
];
const adminNames = ["Nguyễn Quản Trị","Trần Hệ Thống","Lê Siêu Quản Lý","Phạm Điều Hành","Đỗ Kiểm Duyệt"];

const avatars = Array.from({length:20},(_,i)=>`https://i.pravatar.cc/150?img=${i+1}`);

const adminUsers = adminIds.map((id,i)=>({
  _id:id, name:adminNames[i], email:`admin${i+1}@mchub.vn`, password:HASH,
  role:"ADMIN", phoneNumber:`090${String(1000000+i).slice(-7)}`,
  avatar:avatars[i%20], bio:"Quản trị viên The MC Hub.",
  isVerified:true, isActive:true, mcProfile:null,
  createdAt:rDate(D2024,D2025), updatedAt:now
}));

const mcUsers = mcIds.map((id,i)=>({
  _id:id, name:mcNames[i], email:`mc${i+1}@mchub.vn`, password:HASH,
  role:"MC", phoneNumber:`091${String(2000000+i).slice(-7)}`,
  avatar:avatars[(i+5)%20],
  bio:`MC chuyên nghiệp với ${rInt(3,10)} năm kinh nghiệm trong ngành tổ chức sự kiện.`,
  isVerified:i<12, isActive:i!==14, mcProfile:null,
  createdAt:rDate(D2024,D2025), updatedAt:now
}));

const clientUsers = clientIds.map((id,i)=>({
  _id:id, name:clientNames[i], email:`client${i+1}@gmail.com`, password:HASH,
  role:"CLIENT", phoneNumber:`093${String(3000000+i).slice(-7)}`,
  avatar:avatars[(i+10)%20], bio:"",
  isVerified:i<10, isActive:true, mcProfile:null,
  createdAt:rDate(D2025,D2026), updatedAt:now
}));

db.users.insertMany([...adminUsers,...mcUsers,...clientUsers]);
print(`   ✅ ${db.users.countDocuments()} users`);

// ================================================================
// 2. MC PROFILES — 15 profiles
// ================================================================
print("🎤 [2/17] Tạo MCProfiles...");

const mcProfileIds = Array.from({length:15},()=>new ObjectId());

const regions = [
  ["TP. Hồ Chí Minh","Bình Dương"],["Hà Nội","Hải Phòng"],["Đà Nẵng","Huế"],
  ["Cần Thơ","An Giang"],["TP. Hồ Chí Minh"],["Hà Nội"],["Đà Nẵng"],
  ["TP. Hồ Chí Minh","Đồng Nai"],["Hà Nội","Bắc Ninh"],["Đà Nẵng","Quảng Nam"],
  ["TP. Hồ Chí Minh","Long An"],["Hà Nội","Hà Nam"],["Vũng Tàu","Bình Thuận"],
  ["Nha Trang","Khánh Hòa"],["Đà Lạt","Lâm Đồng"]
];
const stylesList = [
  ["Sang trọng","Chuyên nghiệp"],["Hài hước","Vui nhộn"],["Ấm áp","Gần gũi"],
  ["Năng động","Trẻ trung"],["Cổ điển","Lịch lãm"],["Hiện đại","Sáng tạo"],
  ["Thân thiện","Tự nhiên"],["Chuyên nghiệp","Lịch sự"],["Sôi động","Cuốn hút"],
  ["Tinh tế","Elegant"],["Trẻ trung","Sáng tạo"],["Nghiêm túc","Chuyên nghiệp"],
  ["Vui vẻ","Hóm hỉnh"],["Sang trọng","Ấm áp"],["Năng động","Chuyên nghiệp"]
];
const eventTypesList = [
  ["WEDDING","GALA_DINNER","CORPORATE_CONFERENCE"],
  ["GRADUATION","SEMINAR_WORKSHOP","PRODUCT_LAUNCH"],
  ["GALA_DINNER","YEAR_END_PARTY","TEAM_BUILDING"],
  ["WEDDING","ENGAGEMENT","BIRTHDAY_PARTY"],
  ["CORPORATE_CONFERENCE","SEMINAR_WORKSHOP","DIPLOMATIC_EVENT"],
  ["WEDDING","GALA_DINNER","COMPETITION"],
  ["GRAND_OPENING","PRODUCT_LAUNCH","CHARITY_EVENT"],
  ["YEAR_END_PARTY","TEAM_BUILDING","CORPORATE_CONFERENCE"],
  ["WEDDING","GALA_DINNER","DIPLOMATIC_EVENT"],
  ["FESTIVAL","CONCERT","COMPETITION"],
  ["SEMINAR_WORKSHOP","CORPORATE_CONFERENCE","GRADUATION"],
  ["FASHION_SHOW","COMPETITION","EXHIBITION"],
  ["WEDDING","FESTIVAL","BIRTHDAY_PARTY"],
  ["GRAND_OPENING","INAUGURATION","EXHIBITION"],
  ["EXHIBITION","FESTIVAL","CORPORATE_CONFERENCE"]
];
const biographies = [
  "Tôi là MC Minh Khoa, với hơn 8 năm kinh nghiệm dẫn chương trình tại TP.HCM. Phong cách linh hoạt giữa hài hước và chuyên nghiệp.",
  "MC Lan Anh — giọng nói truyền cảm, phong thái sang trọng. Đã dẫn hơn 500 đám cưới và sự kiện cao cấp tại Hà Nội.",
  "Tốt nghiệp Đại học Sân khấu Điện ảnh, tôi mang đến phong cách dẫn chương trình sáng tạo và chuyên nghiệp.",
  "MC Thu Hằng chuyên đám cưới và sự kiện gia đình. Sự ấm áp là thế mạnh giúp mọi sự kiện trở nên đáng nhớ.",
  "MC Quang Vinh chuyên hội nghị và sự kiện doanh nghiệp. Khả năng song ngữ Anh-Việt giúp tôi tự tin trong môi trường quốc tế.",
  "Mai Thy năng động và sôi nổi, phù hợp với các sự kiện trẻ trung như festival và concert.",
  "MC Trọng Nghĩa mang đến không khí vui vẻ, chuyên gameshow và teambulding.",
  "Yến Nhi — giọng ngọt ngào, duyên dáng. Tôi tạo nên kỷ niệm đẹp cho các đôi uyên ương.",
  "MC Công Tuấn 6 năm kinh nghiệm dẫn hội nghị, hội thảo. Ứng biến linh hoạt là thế mạnh của tôi.",
  "Thanh Thảo — trẻ trung và đầy nhiệt huyết. Tôi mang năng lượng tích cực cho mỗi sự kiện.",
  "MC Đức Hiếu chuyên văn hóa nghệ thuật, nền tảng học thuật vững giúp truyền tải đúng tinh thần sự kiện.",
  "Bảo Châu — xuất thân từ phát thanh viên, giọng đọc chuẩn xác là thế mạnh đặc biệt.",
  "MC Xuân Trường 7 năm tại miền Trung, chuyên sự kiện ngoài trời và lễ khánh thành.",
  "Khánh Linh — MC trẻ đoạt nhiều giải thưởng, phong cách hiện đại và sáng tạo.",
  "MC Văn Hùng giọng nam trầm ấm, chuyên gala và lễ trao giải cho doanh nghiệp lớn."
];

const langsList = [
  ["Tiếng Việt"],["Tiếng Việt","English"],["Tiếng Việt","English","中文"],
  ["Tiếng Việt"],["Tiếng Việt","English"],["Tiếng Việt"],["Tiếng Việt","English"],
  ["Tiếng Việt"],["Tiếng Việt","English"],["Tiếng Việt"],
  ["Tiếng Việt","English"],["Tiếng Việt"],["Tiếng Việt"],
  ["Tiếng Việt","English"],["Tiếng Việt","English"]
];

const mcProfiles = mcIds.map((uid,i)=>({
  _id: mcProfileIds[i],
  user: uid.toString(),
  regions: regions[i],
  experience: rInt(2,10),
  styles: stylesList[i],
  biography: biographies[i],
  personality: pick(["Hướng ngoại","Nhiệt tình","Linh hoạt","Điềm tĩnh","Sáng tạo"]),
  hostingStyle: pick(["Tự nhiên & Gần gũi","Chuyên nghiệp & Lịch lãm","Hài hước & Cuốn hút","Sang trọng & Tinh tế"]),
  notableEvents: [
    `Đám cưới tại ${pick(["Rex Hotel","Caravelle","Sheraton","JW Marriott"])} (${rInt(2021,2025)})`,
    `Gala ${pick(["Techcombank","Vingroup","FPT","MB Bank"])} (${rInt(2022,2025)})`,
    `Festival ${pick(["Âm nhạc Mùa Hè","Văn Hóa","Ẩm Thực"])} (${rInt(2023,2025)})`
  ],
  languages: langsList[i],
  rates: { min: rInt(2000000,5000000), max: rInt(8000000,20000000) },
  eventTypes: eventTypesList[i],
  status: i<12 ? "AVAILABLE" : "BUSY",
  showreels: [
    { url:`https://sample-videos.com/video321/mp4/720/big_buck_bunny_720p_${i+1}mb.mp4`, type:"VIDEO" },
    { url:`https://picsum.photos/seed/mc${i+1}/800/600`, type:"IMAGE" }
  ],
  audioSamples: [`https://www.soundhelix.com/examples/mp3/SoundHelix-Song-${(i%16)+1}.mp3`],
  eventPhotos: [
    `https://picsum.photos/seed/event${i*3+1}/1200/800`,
    `https://picsum.photos/seed/event${i*3+2}/1200/800`,
    `https://picsum.photos/seed/event${i*3+3}/1200/800`
  ],
  customPackages: [
    { name:"Gói Cơ Bản", description:"Dẫn chương trình tối đa 3h, phù hợp sự kiện nhỏ.", price:rInt(3000000,5000000), includes:["Kịch bản đơn giản","Trang phục chuẩn"] },
    { name:"Gói Tiêu Chuẩn", description:"Tối đa 6h, bao gồm tập dượt và kịch bản chi tiết.", price:rInt(6000000,10000000), includes:["Kịch bản chi tiết","Tập dượt 1 buổi","Hỗ trợ trang âm"] },
    { name:"Gói VIP", description:"Trọn ngày, kịch bản đa dạng, hỗ trợ sau sự kiện.", price:rInt(12000000,20000000), includes:["Kịch bản VIP","Tập dượt 2 buổi","Trang phục tùy chỉnh","Hỗ trợ sau sự kiện"] }
  ],
  rating: parseFloat((rInt(35,50)/10).toFixed(1)),
  reviewsCount: rInt(5,80),
  createdAt: rDate(D2024,D2025),
  updatedAt: now
}));

db.mcprofiles.insertMany(mcProfiles);

// Update mcProfile ref vào users
mcIds.forEach((uid,i) => db.users.updateOne({_id:uid},{$set:{mcProfile:mcProfileIds[i].toString()}}));
print(`   ✅ ${db.mcprofiles.countDocuments()} mcprofiles`);

// ================================================================
// 3. BOOKINGS — 35 bookings (Chéo nhau để tất cả user đều có data)
// ================================================================
print("📅 [3/17] Tạo Bookings...");

const bookingIds = Array.from({length:35},()=>new ObjectId());
const eventNames = [
  "Đám cưới Minh & Lan","Gala Dinner FPT 2025","Hội nghị Techcombank 2025","Lễ Tốt Nghiệp ĐHBK",
  "Festival Âm Nhạc Mùa Hè","Ra mắt iPhone 17","Year End Party Vingroup","Khai Trương CN HCM",
  "Hội Thảo AI & Future Tech","Đám cưới Hùng & Thu","Teambulding Công ty ABC","Miss IT 2025",
  "Kỷ Niệm 10 Năm Thành Lập","Workshop Marketing Digital","Tiệc Sinh Nhật VIP 50 tuổi",
  "Grand Opening TTTM Landmark","Concert Binz Live Show","Triển Lãm Công Nghệ 2025",
  "Lễ Đính Hôn Anh & Chi","Hội Nghị Y Tế Quốc Tế","StartUp Demo Day 2025",
  "Đám cưới Đức & Ngọc","Khánh Thành Nhà Máy Mới","Gala Trao Giải Kinh Doanh",
  "Khai Giảng Năm Học 2025","Tất Niên XYZ Corp","Fashion Show Thu Đông 2025",
  "Hội Thảo Bất Động Sản","Lễ Ra Mắt Edtech Startup","Đám cưới Long & Phương",
  "Seminar Đầu Tư Chứng Khoán","Lễ Tổng Kết Năm Học","Teambulding Hè 2025",
  "Hội Nghị Khách Hàng VIP","Khai Mạc Festival Văn Hóa"
];
const eventTypes = [
  "WEDDING","GALA_DINNER","CORPORATE_CONFERENCE","GRADUATION","FESTIVAL",
  "PRODUCT_LAUNCH","YEAR_END_PARTY","GRAND_OPENING","SEMINAR_WORKSHOP","WEDDING",
  "TEAM_BUILDING","COMPETITION","GRAND_OPENING","SEMINAR_WORKSHOP","BIRTHDAY_PARTY",
  "GRAND_OPENING","CONCERT","EXHIBITION","ENGAGEMENT","CORPORATE_CONFERENCE",
  "PRODUCT_LAUNCH","WEDDING","INAUGURATION","GALA_DINNER","GRADUATION",
  "YEAR_END_PARTY","FASHION_SHOW","SEMINAR_WORKSHOP","PRODUCT_LAUNCH","WEDDING",
  "SEMINAR_WORKSHOP","GRADUATION","TEAM_BUILDING","CORPORATE_CONFERENCE","FESTIVAL"
];
const locations = [
  "Rex Hotel, TP.HCM", "Caravelle Saigon, TP.HCM", "Daewoo Hotel, Hà Nội",
  "Danang Marriott, Đà Nẵng", "Vincom Convention, Hà Nội", "GEM Center, TP.HCM",
  "White Palace, TP.HCM", "TTNC Quốc Gia, Hà Nội", "Ariyana Condotel, Đà Nẵng",
  "Sofitel Saigon Plaza, TP.HCM", "JW Marriott Phú Quốc Emerald Bay",
  "Pullman Hanoi, Hà Nội", "Novotel Suites Saigon, TP.HCM", "The Garden Mall, TP.HCM",
  "Park Hyatt Saigon, TP.HCM"
];
const statuses = [
  // 25 COMPLETED cho nhiều người có review
  "COMPLETED","COMPLETED","COMPLETED","COMPLETED","COMPLETED",
  "COMPLETED","COMPLETED","COMPLETED","COMPLETED","COMPLETED",
  "COMPLETED","COMPLETED","COMPLETED","COMPLETED","COMPLETED",
  "COMPLETED","COMPLETED","COMPLETED","COMPLETED","COMPLETED",
  "COMPLETED","COMPLETED","COMPLETED","COMPLETED","COMPLETED",
  "ACCEPTED","ACCEPTED","PAID","PAID","PENDING",
  "PENDING","PENDING","CANCELLED","REJECTED","CANCELLED"
];
const payStatuses = statuses.map(s => {
  if(s === "COMPLETED") return "FULLY_PAID";
  if(s === "ACCEPTED") return "DEPOSIT_PAID";
  if(s === "PAID") return "FULLY_PAID";
  if(s === "CANCELLED" || s === "REJECTED") return "REFUNDED";
  return "PENDING";
});

const bookings = bookingIds.map((id,i)=>{
  // Mix để các user đan chéo nhau: client (i%15), mc ((i + floor(i/15))%15)
  // i=0: c0-m0. i=15: c0-m1. i=30: c0-m2. Ai cũng có 2-3 booking khác mc.
  const clientIdx = i % 15;
  const mcIdx = (i + Math.floor(i / 15)) % 15; 
  return {
    _id: id,
    client: clientIds[clientIdx].toString(),
    mc: mcIds[mcIdx].toString(),
    eventDate: rDate(new Date("2025-01-01"), new Date("2026-06-30")),
    eventName: eventNames[i],
    startTime: pick(["07:00","08:00","09:00","14:00","17:00","18:00","19:00"]),
    endTime: pick(["11:00","12:00","17:00","18:00","21:00","22:00","23:00"]),
    location: locations[i%locations.length],
    eventType: eventTypes[i],
    description: `Sự kiện ${eventNames[i]}. Khách hàng cần MC chuyên nghiệp phù hợp với phong cách sự kiện.`,
    audienceSize: rInt(50,600),
    budget: rInt(5000000,30000000),
    specialRequests: pick(["MC cần nói được tiếng Anh","Ưu tiên MC nữ","Trang phục công sở","Kinh nghiệm sự kiện ngoài trời","Không có yêu cầu đặc biệt",null]),
    price: rInt(3000000,20000000),
    couponCode: i<5 ? pick(["WEDDING20","CORP15","SUMMER25","VIP10","NEWUSER"]) : null,
    discountAmount: i<5 ? rInt(200000,2000000) : 0,
    status: statuses[i],
    paymentStatus: payStatuses[i],
    decidedAt: statuses[i]!=="PENDING" ? rDate(D2025,now) : null,
    createdAt: rDate(new Date("2024-10-01"),D2026),
    updatedAt: now
  };
});

db.bookings.insertMany(bookings);
print(`   ✅ ${db.bookings.countDocuments()} bookings`);

// ================================================================
// 4. BOOKING DETAILS — 35 (1:1 với booking)
// ================================================================
print("📋 [4/17] Tạo BookingDetails...");

const dressCodes = ["FORMAL","CASUAL","TRADITIONAL","FORMAL","FORMAL","CASUAL","FORMAL","THEMED","FORMAL","FORMAL","SPORTY","FORMAL","FORMAL","CASUAL","FORMAL"];
const venueTypes = ["INDOOR","INDOOR","INDOOR","INDOOR","OUTDOOR","INDOOR","INDOOR","HYBRID","INDOOR","INDOOR","OUTDOOR","INDOOR","INDOOR","INDOOR","INDOOR"];

const bookingDetails = bookingIds.map((bid,i)=>({
  _id: new ObjectId(),
  bookingId: bid.toString(),
  dressCode: dressCodes[i%15],
  venueType: venueTypes[i%15],
  hasStage: i%3===0,
  hasMicrophone: true,
  hasBackgroundMusic: i%4===0,
  hasProjector: i%5===0,
  timeline: [
    { time: "07:00", activity: "Chuẩn bị, kiểm tra âm thanh ánh sáng", durationMinutes: 60 },
    { time: "08:00", activity: "Đón khách, background music", durationMinutes: 30 },
    { time: "08:30", activity: "Khai mạc chương trình", durationMinutes: 15 },
    { time: "08:45", activity: "Phần chương trình chính", durationMinutes: 120 },
    { time: "10:45", activity: "Nghỉ giải lao, tiệc nhẹ", durationMinutes: 15 },
    { time: "11:00", activity: "Phần 2 / Tiệc / Giao lưu", durationMinutes: 90 },
    { time: "12:30", activity: "Bế mạc, tiễn khách", durationMinutes: 30 }
  ],
  specialGuestNames: i%3===0 ? ["Giám đốc điều hành","Đại biểu khách mời VIP"] : [],
  clientNotes: pick(["MC cần chuẩn bị kỹ phần giới thiệu sản phẩm","Chú ý giọng đọc chậm rãi để phiên dịch kịp",null]),
  mcNotes: pick(["Đã xem kịch bản, cần tập thêm phần tiếng Anh","Hiểu rõ yêu cầu",null]),
  venueAddress: locations[i%locations.length],
  createdAt: rDate(D2025,now)
}));

db.bookingdetails.insertMany(bookingDetails);
print(`   ✅ ${db.bookingdetails.countDocuments()} bookingdetails`);

// ================================================================
// 5. TRANSACTIONS — 35
// ================================================================
print("💳 [5/17] Tạo Transactions...");

const transactions = bookingIds.map((bid,i)=>{
  const bk = bookings[i];
  let type = "DEPOSIT";
  let amount = bk.price * 0.3;
  let status = "COMPLETED";

  if (bk.paymentStatus === "FULLY_PAID") {
    if(i%2===0) { type="FINAL_PAYMENT"; amount=bk.price*0.7; }
    else { type="FULL_PAYMENT"; amount=bk.price; }
  } else if (bk.paymentStatus === "REFUNDED") {
    type="REFUND"; amount=bk.price*0.3;
  } else if (bk.paymentStatus === "PENDING") {
    status="PENDING";
  }

  return {
    _id: new ObjectId(),
    booking: bid.toString(),
    client: bk.client,
    mc: bk.mc,
    amount: Math.round(amount),
    type: type,
    status: status,
    platformFee: Math.round(amount*0.05),
    transactionId: `TXN_MCHUB_${Date.now()}_${i}`,
    payosOrderCode: 200000+i,
    payosPaymentLinkId: `PAY_${String(i+1).padStart(8,"0")}`,
    paidAt: status==="COMPLETED" ? rDate(D2025,now) : null,
    createdAt: rDate(new Date("2024-11-01"),D2026),
    updatedAt: now
  };
});

db.transactions.insertMany(transactions);
print(`   ✅ ${db.transactions.countDocuments()} transactions`);

// ================================================================
// 6. REVIEWS — 25 (Cho 25 completed bookings)
// ================================================================
print("⭐ [6/17] Tạo Reviews...");

const comments = [
  "MC dẫn chương trình tuyệt vời, đúng giờ và rất chuyên nghiệp!",
  "Giọng nói truyền cảm, phong thái lịch lãm. Đám cưới hoàn hảo hơn nhờ MC.",
  "Rất hài lòng! MC linh hoạt xử lý tình huống bất ngờ rất tốt.",
  "MC có kinh nghiệm, biết cách làm không khí buổi tiệc thêm sôi động.",
  "Chuyên nghiệp từ đầu đến cuối. Sẽ mời lại cho sự kiện sau!",
  "Giọng nói hay, nội dung sáng tạo. Ban tổ chức và khách mời đều khen.",
  "MC hỗ trợ rất nhiệt tình trước và sau sự kiện. Cảm ơn rất nhiều!",
  "Phong cách dẫn phù hợp hoàn toàn với chủ đề sự kiện của chúng tôi.",
  "Ấn tượng với sự chuẩn bị kỹ và khả năng ứng biến nhanh nhẹn.",
  "MC mang năng lượng tích cực cho toàn bộ buổi lễ. Rất hài lòng!",
  "Dịch vụ tốt, đúng cam kết. Khách mời hỏi về MC để giới thiệu.",
  "MC tự nhiên và hóm hỉnh, giữ không khí luôn vui vẻ suốt sự kiện.",
  "Lần đầu thuê MC qua app, kết quả vượt kỳ vọng. Quá tuyệt!",
  "Chất lượng tương xứng với giá tiền. Sự kiện trơn tru theo kế hoạch.",
  "MC lắng nghe yêu cầu và điều chỉnh phong cách rất phù hợp.",
  "Đám cưới hoàn hảo. Cảm ơn MC đã tạo nên những kỷ niệm đẹp!",
  "Hội nghị thành công, phần lớn nhờ MC dẫn dắt chuyên nghiệp.",
  "MC hiểu sản phẩm và trình bày rất thuyết phục, ấn tượng!",
  "HR đều khen MC cho Teambulding. Rất sáng tạo và nhiệt tình.",
  "Tiệc Gala sang trọng, MC phù hợp hoàn toàn với đẳng cấp sự kiện.",
  "Tạm ổn, MC cần cải thiện xử lý tình huống bất ngờ thêm một chút.",
  "Khá tốt nhưng đôi khi nói hơi nhanh. Tổng thể khách phản hồi tích cực.",
  "Sự kiện thành công, MC làm việc nhiệt tình và đúng yêu cầu đề ra.",
  "Concert sôi động, MC warm-up khán giả rất hiệu quả!",
  "Workshop hiệu quả, MC biết khai thác tương tác với khán giả rất tốt."
];

const reviews = Array.from({length:25},(_,i)=>{
  return {
    _id: new ObjectId(),
    booking: bookingIds[i].toString(),  // 25 cái đầu tiên đều COMPLETED
    mc: bookings[i].mc,
    client: bookings[i].client,
    rating: i<20 ? rInt(4,5) : rInt(3,4),
    comment: comments[i],
    createdAt: rDate(D2025,now)
  }
});

db.reviews.insertMany(reviews);
print(`   ✅ ${db.reviews.countDocuments()} reviews`);

// ================================================================
// 7. SCHEDULES — 35
// ================================================================
print("🗓️  [7/17] Tạo Schedules...");

const schedStatuses = ["AVAILABLE","BOOKED","UNAVAILABLE","PENDING","BOOKED","AVAILABLE","UNAVAILABLE"];

const schedules = Array.from({length:35},(_,i)=>{
  const bk = bookings[i]; 
  return {
    _id: new ObjectId(),
    mc: bk.mc,  // Lưu lịch tương ứng với MC của booking đó
    date: rDate(D2025,D2026E),
    startTime: pick(["07:00","08:00","09:00","13:00","14:00","17:00","18:00"]),
    endTime: pick(["12:00","13:00","17:00","18:00","21:00","22:00","23:00"]),
    status: schedStatuses[i%7],
    bookingId: schedStatuses[i%7]==="BOOKED" ? bk._id.toString() : null,
    note: schedStatuses[i%7]==="UNAVAILABLE"?"Bận việc gia đình":null,
    createdAt: rDate(D2025,now)
  }
});

db.schedules.insertMany(schedules);
print(`   ✅ ${db.schedules.countDocuments()} schedules`);

// ================================================================
// 8. CONVERSATIONS — 35 (Tất cả booking đều có chat)
// ================================================================
print("💬 [8/17] Tạo Conversations...");

const convIds = Array.from({length:35},()=>new ObjectId());

const conversations = convIds.map((id,i)=>({
  _id: id,
  participants: [bookings[i].client, bookings[i].mc],
  bookingId: bookingIds[i].toString(),
  lastMessage: null,
  isActive: i<30,
  createdAt: rDate(D2025,now),
  updatedAt: now
}));

db.conversations.insertMany(conversations);
print(`   ✅ ${db.conversations.countDocuments()} conversations`);

// ================================================================
// 9. MESSAGES — 105 (Mỗi convo có 3 tin nhắn)
// ================================================================
print("✉️  [9/17] Tạo Messages...");

const messages = [];
for (let c = 0; c < 35; c++) {
  const conv = conversations[c];
  const bk = bookings[c];
  const client = bk.client;
  const mc = bk.mc;
  
  messages.push({
    _id: new ObjectId(), conversationId: conv._id.toString(),
    senderId: client, type: "TEXT", bookingId: bk._id.toString(),
    content: "Chào MC, mình muốn hỏi chi tiết thêm về booking này.",
    readBy: [client, mc], createdAt: new Date(conv.createdAt.getTime() + 1000)
  });
  messages.push({
    _id: new ObjectId(), conversationId: conv._id.toString(),
    senderId: mc, type: "TEXT", bookingId: bk._id.toString(),
    content: "Chào bạn. Rất vui được hỗ trợ bạn. Bạn cần yêu cầu cụ thể nào?",
    readBy: [client, mc], createdAt: new Date(conv.createdAt.getTime() + 60000)
  });
  messages.push({
    _id: new ObjectId(), conversationId: conv._id.toString(),
    senderId: client, type: "TEXT", bookingId: bk._id.toString(),
    content: "Mình muốn ưu tiên trang phục là vest trắng cho hợp với chủ đề cưới.",
    readBy: [client, mc], createdAt: new Date(conv.createdAt.getTime() + 120000)
  });
}

db.messages.insertMany(messages);

// Cập nhật lastMessage vào conversations
convIds.forEach((cid,i)=>{
  const lastMsg = messages.filter(m=>m.conversationId===cid.toString()).pop();
  if(lastMsg) db.conversations.updateOne({_id:cid},{$set:{lastMessage:lastMsg._id.toString()}});
});
print(`   ✅ ${db.messages.countDocuments()} messages`);

// ================================================================
// 10. NOTIFICATIONS — 70
// ================================================================
print("🔔 [10/17] Tạo Notifications...");

const notifData = [
  {type:"BOOKING_REQUEST",title:"Yêu cầu đặt lịch mới",body:"Bạn có một yêu cầu đặt lịch mới."},
  {type:"NEW_MESSAGE",title:"Tin nhắn mới",body:"Bạn có tin nhắn mới từ khách hàng."}
];

const notifications = [];
for (let i = 0; i < 35; i++) {
  const bk = bookings[i];
  notifications.push({
    _id: new ObjectId(), user: bk.mc, senderId: bk.client,
    title: notifData[0].title, body: notifData[0].body, type: notifData[0].type,
    relatedId: bk._id.toString(), relatedModel: "Booking", linkAction: `/bookings/${bk._id}`,
    isRead: true, createdAt: bk.createdAt
  });
  notifications.push({
    _id: new ObjectId(), user: bk.client, senderId: bk.mc,
    title: notifData[1].title, body: notifData[1].body, type: notifData[1].type,
    relatedId: bk._id.toString(), relatedModel: "Booking", linkAction: `/bookings/${bk._id}`,
    isRead: i < 20, createdAt: new Date(bk.createdAt.getTime() + 86400000)
  });
}

db.notifications.insertMany(notifications);
print(`   ✅ ${db.notifications.countDocuments()} notifications`);

// ================================================================
// 11. SCRIPTS — 30
// ================================================================
print("📜 [11/17] Tạo Scripts...");

const scriptData = [
  {title:"Kịch Bản Đám Cưới Sang Trọng",category:"WEDDING",lang:"vi",tags:["đám cưới"]},
  {title:"Script Gala Dinner Doanh Nghiệp",category:"CORPORATE",lang:"vi",tags:["gala"]},
  {title:"MC Script - Birthday Party Trendy",category:"BIRTHDAY",lang:"vi",tags:["sinh nhật"]},
  {title:"Kịch Bản Hội Nghị Chuyên Nghiệp",category:"CONFERENCE",lang:"vi",tags:["hội nghị"]},
  {title:"Grand Opening Script - Khai Trương",category:"GRAND_OPENING",lang:"vi",tags:["khai trương"]},
  {title:"Lễ Bế Giảng Năm Học 2025",category:"CONFERENCE",lang:"vi",tags:["trường học"]},
  {title:"Concert Host Script",category:"CONCERT",lang:"vi",tags:["âm nhạc"]},
  {title:"Kịch Bản Lễ Tốt Nghiệp",category:"GRADUATION",lang:"vi",tags:["tốt nghiệp"]},
  {title:"Year End Party Vui Nhộn",category:"YEAR_END_PARTY",lang:"vi",tags:["tất niên"]},
  {title:"Lễ Cắt Băng Khánh Thành",category:"GRAND_OPENING",lang:"vi",tags:["khánh thành"]},
  {title:"Kịch Bản Đám Cưới Ngoài Trời",category:"WEDDING",lang:"vi",tags:["ngoài trời"]}
];

const adminId = adminIds[0].toString();
const scripts = Array.from({length:30},(_,i)=>{
  const sd = scriptData[i%scriptData.length];
  return {
    _id: new ObjectId(),
    createdBy: i<5 ? adminId : mcIds[i%15].toString(),
    title: sd.title + ` ${i+1}`,
    category: sd.category,
    content: `[Khai mạc]\nChào mừng sự kiện ${sd.title}. Kính thưa quý vị...\n[Kết thúc]\nXin cảm ơn.`,
    tags: sd.tags,
    isPublic: true,
    favorites: rInt(0,200),
    viewCount: rInt(50,2000),
    language: sd.lang,
    createdAt: rDate(D2024,now)
  }
});

db.scripts.insertMany(scripts);
print(`   ✅ ${db.scripts.countDocuments()} scripts`);

// ================================================================
// 12. FAVORITES — 35
// ================================================================
print("❤️  [12/17] Tạo Favorites...");

const favorites = [];
for(let i=0; i<35; i++){
  favorites.push({
    _id: new ObjectId(),
    clientId: clientIds[i%15].toString(),
    mcUserId: mcIds[(i+5)%15].toString(), // Shift 5 để chéo với booking
    createdAt: rDate(D2025,now)
  });
}

// Deduplicate
const favUnique = favorites.filter((f,i,a)=>a.findIndex(v=>v.clientId===f.clientId && v.mcUserId===f.mcUserId)===i);
db.favorites.insertMany(favUnique);
print(`   ✅ ${db.favorites.countDocuments()} favorites`);

// ================================================================
// 13. REPORTS — 20
// ================================================================
print("🚨 [13/17] Tạo Reports...");

const reports = Array.from({length:20},(_,i)=>({
  _id: new ObjectId(),
  reporterId: clientIds[i%15].toString(),
  reportedId: mcIds[(i+3)%15].toString(),
  bookingId: bookingIds[i].toString(),
  reason: "NO_SHOW",
  description: "Tranh chấp phát sinh từ yêu cầu kịch bản muộn màng.",
  status: i%2===0 ? "RESOLVED" : "UNDER_REVIEW",
  adminNote: i%2===0 ? "Đã xác minh 2 bên." : null,
  resolvedBy: i%2===0 ? adminIds[0].toString() : null,
  createdAt: rDate(D2025,now)
}));

db.reports.insertMany(reports);
print(`   ✅ ${db.reports.countDocuments()} reports`);

// ================================================================
// 14. CERTIFICATES — 30
// ================================================================
print("🏆 [14/17] Tạo Certificates...");
const certificates = Array.from({length:30},(_,i)=>({
  _id: new ObjectId(),
  mcProfileId: mcProfileIds[i%15].toString(),
  name: "Chứng chỉ MC cấp quản trị quốc tế " + i,
  issuer: "Học Viện Kỹ Năng Sống",
  issuedDate: rDate(new Date("2020-01-01"),new Date("2024-01-01")).toISOString().split("T")[0],
  expiredDate: null,
  imageUrl: `https://picsum.photos/seed/cert${i}/900/600`,
  isVerified: i<25,
  verifiedBy: i<25 ? adminIds[0].toString() : null,
  verifiedAt: i<25 ? rDate(D2025,now) : null,
  createdAt: rDate(D2024,D2025)
}));

db.certificates.insertMany(certificates);
print(`   ✅ ${db.certificates.countDocuments()} certificates`);

// ================================================================
// 15. COUPONS — 10
// ================================================================
print("🎫 [15/17] Tạo Coupons...");
const coupons = Array.from({length:10},(_,i)=>({
  _id: new ObjectId(),
  code: "MCHUB" + (10+i),
  description: "Mã giảm giá nội bộ",
  discountType: "PERCENT",
  discountValue: 0.15,
  minBookingValue: 1000000,
  maxDiscountAmount: 500000,
  maxUses: 100,
  usedCount: i*2,
  applicableEventTypes: [],
  expiredAt: rDate(D2026,new Date("2027-12-31")),
  isActive: true,
  createdBy: adminIds[0].toString(),
  createdAt: D2025, updatedAt: now
}));

db.coupons.insertMany(coupons);
print(`   ✅ ${db.coupons.countDocuments()} coupons`);

// ================================================================
// 16. REFRESH TOKENS — 0 (Skip vì reset login)
// ================================================================
print("🔑 [16/17] (Bỏ qua RefreshTokens)");

// ================================================================
// 17. AUDIT LOGS — 35
// ================================================================
print("📋 [17/17] Tạo AuditLogs...");

const allUserIds = [...adminIds,...mcIds,...clientIds].map(u=>u.toString());
const auditLogs = Array.from({length:35},(_,i)=>{
  return {
    _id: new ObjectId(),
    userId: allUserIds[i%allUserIds.length],
    action: "AUTH_LOGIN",
    resource: "User",
    resourceId: allUserIds[i%allUserIds.length],
    details: '{"login":"success"}',
    ipAddress: "127.0.0.1",
    userAgent: "Mozilla/5.0",
    status: "SUCCESS",
    createdAt: rDate(D2025,now)
  };
});

db.auditlogs.insertMany(auditLogs);
print(`   ✅ ${db.auditlogs.countDocuments()} auditlogs`);

// ================================================================
// TỔNG KẾT
// ================================================================
print("\n🎉 ══════════════════════════════════════════");
print("   THE MC HUB — SEED DATA HOÀN TẤT (v2.1)");
print("══════════════════════════════════════════");
print(`   👥  users:          ${db.users.countDocuments()}`);
print(`   🎤  mcprofiles:     ${db.mcprofiles.countDocuments()}`);
print(`   📅  bookings:       ${db.bookings.countDocuments()}`);
print(`   📋  bookingdetails: ${db.bookingdetails.countDocuments()}`);
print(`   💳  transactions:   ${db.transactions.countDocuments()}`);
print(`   ⭐  reviews:        ${db.reviews.countDocuments()}`);
print(`   🗓️   schedules:      ${db.schedules.countDocuments()}`);
print(`   💬  conversations:  ${db.conversations.countDocuments()}`);
print(`   ✉️   messages:       ${db.messages.countDocuments()}`);
print(`   🔔  notifications:  ${db.notifications.countDocuments()}`);
print(`   📜  scripts:        ${db.scripts.countDocuments()}`);
print(`   ❤️   favorites:      ${db.favorites.countDocuments()}`);
print(`   🚨  reports:        ${db.reports.countDocuments()}`);
print(`   🏆  certificates:   ${db.certificates.countDocuments()}`);
print(`   🎫  coupons:        ${db.coupons.countDocuments()}`);
print(`   📋  auditlogs:      ${db.auditlogs.countDocuments()}`);
print("══════════════════════════════════════════");
print("   🔐 Tài khoản Test:");
print("      Admin:  admin1@mchub.vn   / password123");
print("      MC:     mc1@mchub.vn      / password123");
print("      Client: client1@gmail.com / password123");
print("══════════════════════════════════════════\n");
