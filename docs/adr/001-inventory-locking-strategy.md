# ADR-001: Inventory Locking Strategy

- **Status:** Accepted
- **Date:** 2026-09-03
- **Decision Owners:** Saurabh

## 1. Context

The Inventory Reservation & Order Fulfillment Platform must support concurrent reservation requests without overselling stock.

For example, if an inventory item has:

- `on_hand = 5`
- `reserved = 0`
- `available = 5`

and 20 concurrent requests each attempt to reserve quantity `1`, exactly 5 reservations must succeed and the remaining 15 must fail.

The inventory update operation is a read-modify-write operation:

1. Read the current inventory.
2. Verify sufficient available quantity.
3. Increase `reserved`.
4. Persist the updated inventory.

Without proper locking, two or more transactions can read the same available quantity and both update it, resulting in overselling.

The application also needs to support multi-warehouse allocation and administrative inventory adjustments while maintaining the same consistency guarantees.

## 2. Decision

We use **database-level pessimistic row locking** for inventory mutations.

Spring Data JPA repository methods use `PESSIMISTIC_WRITE` locking when inventory is being allocated, released, or adjusted.

Example:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
    SELECT i
    FROM Inventory i
    WHERE i.tenantId = :tenantId
      AND i.productId = :productId
    ORDER BY i.warehouseId
    """)
List<Inventory> findForUpdate(
    UUID tenantId,
    UUID productId
);
```

The lock is acquired inside a database transaction and is held until the transaction completes.

Inventory reads that do not modify stock use normal non-locking queries. This avoids unnecessary database locks and allows read-only operations to remain lightweight.

### Transaction boundary

Inventory allocation and reservation creation execute within a single transaction.

The transaction:

- Locks the relevant inventory rows.
- Calculates available stock.
- Allocates stock.
- Updates reserved.
- Creates the reservation/allocation records.
- Persists the idempotency record.
- Persists the corresponding domain event.
- Commits atomically.

If the transaction fails, the database rolls back the inventory and reservation changes together.

## 3. Why Pessimistic Locking

Pessimistic locking was selected because inventory is a highly contended resource and correctness is more important than avoiding database locks.

Advantages
- Prevents concurrent transactions from modifying the same inventory row simultaneously.
- Provides strong protection against overselling.
- Uses PostgreSQL's transactional locking guarantees.
- Works naturally with Spring Data JPA transactions.
- Easy to reason about for reservation, release, and stock adjustment operations.
- Handles high-contention scenarios deterministically.

The concurrency acceptance test validates this approach by executing 20 parallel reservations against stock of 5 and verifying that exactly 5 succeed.

## 4. Alternative Considered: Optimistic Locking

An alternative was to add a JPA \@Version` column`
Under optimistic locking, concurrent transactions could read the same inventory row and attempt updates. PostgreSQL/JPA would detect conflicting updates and one transaction would fail with an optimistic locking exception.
This approach can work well when contention is low.
However, reservation traffic can create significant contention on popular products. Failed transactions would then require retry logic, making the reservation path more complex.
For this system, the explicit pessimistic lock provides simpler and more predictable behavior.

## 5. Consequences

### Positive

- Prevents inventory overselling under concurrent requests.
- Provides predictable reservation behavior.
- Keeps inventory and reservation changes atomic within a transaction.
- Works naturally with PostgreSQL and Spring Data JPA.

### Negative

- Concurrent requests for the same inventory row may wait for locks.
- Long-running transactions can increase lock contention.
- Multiple inventory rows must be locked in a consistent order to reduce deadlock risk.

## 6. Result

The system uses PostgreSQL pessimistic row-level locking as the primary concurrency-control mechanism for inventory mutations.

The approach is validated by the concurrency acceptance test: 20 concurrent quantity-1 reservations against stock of 5 result in exactly 5 successful reservations and no overselling.