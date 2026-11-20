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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

function validUniversityPayload(uid, overrides = {}) {
  const past = new Date(Date.now() - 5000);
  return {
    userId: uid,
    name: "Stanford University",
    applicationDeadlineMillis: 1798848000000,
    applicationStatus: "IN_PROGRESS",
    targetSupplementCount: 3,
    financialAidDeadlineMillis: 1802649600000,
    cssProfileRequirement: "COMPULSORY",
    financialAidDocStatus: "STARTED",
    englishProficiency: "EASY_WAIVE",
    collegeTraits: "Quarter system, interdisciplinary CS+Humanities",
    createdAt: past,
    updatedAt: past,
    ...overrides,
  };
}

function validSupplementPayload(uid, universityId, overrides = {}) {
  const past = new Date(Date.now() - 5000);
  return {
    userId: uid,
    universityId,
    title: "Intellectual Vitality",
    status: "IN_PROGRESS",
    wordBucket: "W250",
    essayType: "Why Major",
    prompt: "Reflect on an idea or experience that makes you genuinely excited about learning.",
    brainstormNotes: "Connect robotics club mentoring to computational linguistics.",
    createdAt: past,
    updatedAt: past,
    ...overrides,
  };
}

function validTypePayload(uid, name = "Why Us", overrides = {}) {
  const past = new Date(Date.now() - 5000);
  return {
    userId: uid,
    name,
    normalizedName: name.trim().toLowerCase(),
    createdAt: past,
    updatedAt: past,
    ...overrides,
  };
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/universities`).get()
  );
  await assertFails(
    unauthDb
      .collection(`users/${ALICE_UID}/universities`)
      .doc("uni_1")
      .set(validUniversityPayload(ALICE_UID))
  );
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/supplements`).get()
  );
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/supplementTypes`).get()
  );
});

test("Cross-user isolation prevents Bob from reading or writing Alice's data", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db
      .collection(`users/${ALICE_UID}/universities`)
      .doc("uni_1")
      .set(validUniversityPayload(ALICE_UID));
    await db
      .collection(`users/${ALICE_UID}/supplements`)
      .doc("supp_1")
      .set(validSupplementPayload(ALICE_UID, "uni_1"));
    await db
      .collection(`users/${ALICE_UID}/supplementTypes`)
      .doc("type_1")
      .set(validTypePayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/universities`).get()
  );
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/universities`).doc("uni_1").get()
  );
  await assertFails(
    bobDb
      .collection(`users/${ALICE_UID}/universities`)
      .doc("uni_1")
      .update({ name: "Hijacked" })
  );
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/supplements`).doc("supp_1").delete()
  );
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/supplementTypes`).get()
  );
});

test("Owner can create, list, partially update, clear optional deadlines, and delete universities", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const uniRef = aliceDb
    .collection(`users/${ALICE_UID}/universities`)
    .doc("uni_1");

  await assertSucceeds(uniRef.set(validUniversityPayload(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/universities`).get()
  );

  await assertSucceeds(
    uniRef.update({
      applicationStatus: "ACCEPTED",
      financialAidDocStatus: "DONE",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field (shadow update) and immutable field tampering
  await assertFails(
    uniRef.update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    uniRef.update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  // Reject @DocumentId "id" inside body
  await assertFails(
    aliceDb
      .collection(`users/${ALICE_UID}/universities`)
      .doc("uni_2")
      .set(validUniversityPayload(ALICE_UID, { id: "uni_2" }))
  );
  // Reject future timestamp
  await assertFails(
    aliceDb
      .collection(`users/${ALICE_UID}/universities`)
      .doc("uni_3")
      .set(
        validUniversityPayload(ALICE_UID, {
          createdAt: new Date(Date.now() + 60000),
        })
      )
  );

  await assertSucceeds(uniRef.delete());
});

test("Supplements enforce parent university existence, valid enums, and immutable universityId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Orphaned supplement without parent university must fail
  await assertFails(
    aliceDb
      .collection(`users/${ALICE_UID}/supplements`)
      .doc("supp_orphan")
      .set(validSupplementPayload(ALICE_UID, "missing_uni"))
  );

  await assertSucceeds(
    aliceDb
      .collection(`users/${ALICE_UID}/universities`)
      .doc("uni_1")
      .set(validUniversityPayload(ALICE_UID))
  );

  const suppRef = aliceDb
    .collection(`users/${ALICE_UID}/supplements`)
    .doc("supp_1");
  await assertSucceeds(
    suppRef.set(validSupplementPayload(ALICE_UID, "uni_1"))
  );

  await assertSucceeds(
    suppRef.update({
      status: "READY_TO_SUBMIT",
      wordBucket: "W500_PLUS",
      essayType: "Community",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject changing universityId on update
  await assertFails(
    suppRef.update({
      universityId: "uni_other",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject invalid wordBucket enum
  await assertFails(
    suppRef.update({
      wordBucket: "W999",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("SupplementTypes enforce ownership, non-empty names, and list access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const typeRef = aliceDb
    .collection(`users/${ALICE_UID}/supplementTypes`)
    .doc("type_why_us");

  await assertSucceeds(typeRef.set(validTypePayload(ALICE_UID, "Why Us")));
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/supplementTypes`).get()
  );
  await assertSucceeds(
    typeRef.update({
      name: "Why This College",
      normalizedName: "why this college",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject empty name
  await assertFails(
    aliceDb
      .collection(`users/${ALICE_UID}/supplementTypes`)
      .doc("type_empty")
      .set(validTypePayload(ALICE_UID, ""))
  );
});
