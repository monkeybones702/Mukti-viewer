const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write presets or health logs", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("presets").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("health_logs").get());
});

test("Authenticated user: can create and read their own grid preset", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("presets").doc("preset_focus_4").set({
      id: "preset_focus_4",
      userId: ALICE_UID,
      name: "4-Window Focus",
      description: "Quad stream layout",
      gridColumns: 2,
      gridRows: 2,
      fitEntirePhone: true,
      proportionalZoom: false,
      masterAudioMuted: true,
      windowCount: 4,
      windowsJson: "[]",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("presets").doc("preset_focus_4").get()
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's presets or health logs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("health_logs").doc("log_1").set({
      id: "log_1",
      userId: ALICE_UID,
      windowId: "win_1",
      windowTitle: "UptoPlay",
      streamUrl: "https://www.uptoplay.net",
      status: "RECOVERED",
      reason: "Heartbeat stalled > 12s",
      recoveryAction: "Soft-refresh WebView reload",
      attemptCount: 1,
      latencyMs: 140,
      createdAt: now,
      updatedAt: now,
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("health_logs").doc("log_1").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("health_logs").where("userId", "==", ALICE_UID).get()
  );
});

test("Shadow field and invalid enum rejection on health logs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  // Invalid status enum
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("health_logs").doc("log_bad").set({
      id: "log_bad",
      userId: ALICE_UID,
      windowId: "win_1",
      windowTitle: "UptoPlay",
      streamUrl: "https://www.uptoplay.net",
      status: "INVALID_STATUS",
      reason: "Test",
      recoveryAction: "Soft-refresh",
      attemptCount: 1,
      latencyMs: 100,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Ghost field injection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("health_logs").doc("log_ghost").set({
      id: "log_ghost",
      userId: ALICE_UID,
      windowId: "win_1",
      windowTitle: "UptoPlay",
      streamUrl: "https://www.uptoplay.net",
      status: "RECOVERED",
      reason: "Test",
      recoveryAction: "Soft-refresh",
      attemptCount: 1,
      latencyMs: 100,
      isAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});
