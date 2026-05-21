const { MongoClient } = require('mongodb');
async function run() {
    const uri = "mongodb+srv://trungle:Pitngu%401234@maindatabase.2tirj0y.mongodb.net/mchub?retryWrites=true&w=majority";
    const client = new MongoClient(uri);
    try {
        await client.connect();
        const db = client.db('mchub');
        const user = await db.collection('users').findOne({ email: "client1@gmail.com" });
        console.log("CLIENT1 USER:", JSON.stringify(user, null, 2));

        const testUser = await db.collection('users').findOne({ email: "checktest@gmail.com" });
        console.log("\nTEST USER:", JSON.stringify(testUser, null, 2));
    } finally {
        await client.close();
    }
}
run();
