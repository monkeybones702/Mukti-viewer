# Security Specification (`security_spec.md`)

## 1. Data Invariants
1. **Strict Multi-Tenant Isolation**: Every document in `/users/{userId}/presets/{presetId}`, `/users/{userId}/health_logs/{logId}`, and `/users/{userId}/chat_messages/{messageId}` belongs exclusively to `userId` (`request.auth.uid == userId` and `data.userId == request.auth.uid`).
2. **Schema & Bounds Enforcement**: Every `create` and `update` operation validates exact allowed keys via `hasAll` / `hasOnly` and field types/sizes via `isValidPreset`, `isValidHealthIncident`, and `isValidChatMessage`.
3. **Immutable Audit Fields**: `id`, `userId`, and `createdAt` cannot be mutated after document creation (`incoming().createdAt == existing().createdAt`).
4. **Server Timestamp Bounds**: `createdAt` and `updatedAt` must be Firestore `timestamp` objects with `<= request.time`.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read/Write**: `request.auth == null` attempting `get`, `list`, `create`, `update`, or `delete`.
2. **Cross-User Read (`bob_456` reading `alice_123`)**: Authenticated user attempting to `get` or `list` `/users/alice_123/presets`.
3. **Identity Spoofing on Create**: Authenticated user `alice_123` writing to `/users/alice_123/presets/p1` with `userId: "bob_456"`.
4. **Shadow/Ghost Field Injection**: Adding `isAdmin: true` or `isVerified: true` on `create` or `update`.
5. **Immutable Field Mutation**: Updating `createdAt` or `userId` on an existing preset or health log.
6. **Out-of-Bounds Grid Dimensions**: Creating a preset with `gridColumns: 99` or `gridRows: 0` (must be 1..5 and 1..10).
7. **Invalid Health Status Enum**: Creating a health incident with `status: "HACKED"` (must be in `['HEALTHY', 'FROZEN', 'DROPPED', 'RECOVERING', 'RECOVERED']`).
8. **String Size Overflow (Denial of Wallet)**: Injecting a >500-char `description` or >100-char `name`.
9. **Future Timestamp Injection**: Passing a client timestamp in the future (`createdAt > request.time`).
10. **Explicit Null on Required Field**: Updating `name: null` on a preset.
11. **Invalid Document ID Poisoning**: Creating a document with special characters outside `^[a-zA-Z0-9_\-]+$`.
12. **Invalid Chat Role Enum**: Creating a chat message with `role: "system_override"` (must be `'user'` or `'model'`).
