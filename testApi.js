
// Node 18+ has built in fetch
async function run() {
    console.log("Registering Test User...");
    const regResp = await fetch("http://localhost:5000/api/v1/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: "Tester", email: "checktest@gmail.com", password: "password123", role: "CLIENT" })
    });
    console.log("Reg Status:", regResp.status);
    console.log("Reg Body:", await regResp.text());

    console.log("\nLogging in with seeded user client1...");
    const log1 = await fetch("http://localhost:5000/api/v1/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: "client1@gmail.com", password: "password123" })
    });
    console.log("Log1 Status:", log1.status);
    console.log("Log1 Body:", await log1.text());

    console.log("\nLogging in with newly created checktest...");
    const log2 = await fetch("http://localhost:5000/api/v1/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: "checktest@gmail.com", password: "password123" })
    });
    console.log("Log2 Status:", log2.status);
    console.log("Log2 Body:", await log2.text());
}
run();
